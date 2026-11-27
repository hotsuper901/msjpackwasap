package com.msj.stickerstudio.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.msj.stickerstudio.*
import com.msj.stickerstudio.ui.GradientButton
import com.msj.stickerstudio.ui.Routes
import com.msj.stickerstudio.ui.theme.*
import java.io.File

private sealed interface ConvertState {
    data class Working(val progress: Int, val step: String) : ConvertState
    data class Success(val file: File, val packId: String?, val info: String) : ConvertState
    data class Failure(val message: String) : ConvertState
}

@Composable
fun ConvertScreen(navController: NavHostController) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<ConvertState>(ConvertState.Working(0, "Preparing FFmpeg\u2026")) }

    LaunchedEffect(Unit) {
        val uri = Session.inputUri
        val settings = Session.settings
        if (uri == null) {
            state = ConvertState.Failure("No video selected. Go back and pick one.")
            return@LaunchedEffect
        }

        val exports = WhatsAppHandler.exportsDir(context)
        val base = "msj_${System.currentTimeMillis()}"

        try {
            when (settings.format) {
                OutputFormat.ANIMATED_WEBP, OutputFormat.GIF -> {
                    state = ConvertState.Working(5, "Rendering frames\u2026")
                    val stickerFile = File(exports, "$base${settings.format.ext}")
                    val conv = FfmpegUtils.convertToSticker(
                        context, uri, settings.format, settings.size, settings.fps, settings.quality, stickerFile
                    ) { p -> state = ConvertState.Working(p.coerceIn(0, 80), "Rendering frames\u2026 ${p}%") }
                    if (!conv.success) {
                        state = ConvertState.Failure(conv.message)
                        return@LaunchedEffect
                    }

                    state = ConvertState.Working(85, "Making tray icon\u2026")
                    val trayIcon = File(exports, "$base-tray.webp")
                    val tray = FfmpegUtils.makeTrayIcon(context, uri, trayIcon)
                    if (!tray.success) {
                        state = ConvertState.Failure(tray.message)
                        return@LaunchedEffect
                    }

                    state = ConvertState.Working(92, "Adding pack to WhatsApp\u2026")
                    val added = WhatsAppHandler.addStickerPack(
                        context,
                        settings.packName,
                        trayIcon,
                        listOf(Sticker(stickerFile, settings.emojis))
                    )
                    if (!added.success) {
                        state = ConvertState.Failure(added.message)
                        return@LaunchedEffect
                    }
                    val registered = WhatsAppHandler.getStickerPacks(context)
                        .any { it.id == added.message }
                    Session.lastOutput = stickerFile.absolutePath
                    Session.lastOutputFormat = settings.format
                    state = ConvertState.Success(
                        stickerFile,
                        added.message,
                        if (registered) {
                            "Added to WhatsApp \u2714\nOpen a chat \u2192 sticker picker \u2192 \u2295 \u2192 My Stickers.\n" +
                                "If it's not there yet, force-stop WhatsApp once and reopen."
                        } else {
                            "WhatsApp didn't register the pack yet.\nForce-stop WhatsApp once and tap OPEN WHATSAPP \u2014 " +
                                "then re-check under \u2295 \u2192 My Stickers."
                        }
                    )
                }

                OutputFormat.MUSIC_STICKER -> {
                    state = ConvertState.Working(8, "Encoding VP9 video + Opus audio\u2026")
                    val outFile = File(exports, "$base-music.webm")
                    val conv = FfmpegUtils.convertToMusicSticker(
                        context, uri, settings.size, settings.fps, settings.withMusic, outFile
                    ) { p -> state = ConvertState.Working(p.coerceIn(0, 95), "Encoding\u2026 ${p}%") }
                    if (!conv.success) {
                        state = ConvertState.Failure(conv.message)
                        return@LaunchedEffect
                    }
                    Session.lastOutput = outFile.absolutePath
                    Session.lastOutputFormat = settings.format
                    state = ConvertState.Success(outFile, null, "Music sticker ready. Share it to WhatsApp or any chat.")
                }

                OutputFormat.RINGTONE -> {
                    state = ConvertState.Working(8, "Extracting soundtrack\u2026")
                    val outFile = File(exports, "$base-ringtone.mp3")
                    val conv = FfmpegUtils.extractRingtone(context, uri, outFile
                    ) { p -> state = ConvertState.Working(p.coerceIn(0, 95), "Extracting audio\u2026 ${p}%") }
                    if (!conv.success) {
                        state = ConvertState.Failure(conv.message)
                        return@LaunchedEffect
                    }
                    Session.lastOutput = outFile.absolutePath
                    Session.lastOutputFormat = settings.format
                    state = ConvertState.Success(outFile, null, "Ringtone extracted \u2014 share it or set it as your sound.")
                }
            }
        } catch (e: Exception) {
            state = ConvertState.Failure(e.message ?: "Something broke during conversion")
        }
    }

    Box(Modifier.fillMaxSize().padding(22.dp), contentAlignment = Alignment.Center) {
        when (val s = state) {
            is ConvertState.Working -> WorkingView(s.progress, s.step)
            is ConvertState.Success -> SuccessView(s, navController)
            is ConvertState.Failure -> FailureView(s.message, navController)
        }
    }
}

@Composable
private fun WorkingView(progress: Int, step: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Violet, Magenta, Cyan)))
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxSize(),
                color = Color.White,
                trackColor = Color(0x33FFFFFF),
                strokeWidth = 8.dp
            )
            Text("$progress%", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
        Spacer(Modifier.height(26.dp))
        Text("CONVERTING", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        Text(step, color = Cyan, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Magenta,
            trackColor = Color(0xFF3A3152)
        )
        Spacer(Modifier.height(12.dp))
        Text("Powered by FFmpeg \u00B7 M.S.J Studio", color = TextDim.copy(alpha = 0.6f), fontSize = 11.sp)
    }
}

@Composable
private fun SuccessView(s: ConvertState.Success, navController: NavHostController) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Cyan, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(14.dp))
        Text("DONE!", color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(6.dp))
        Text(s.info, color = TextDim, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
        Spacer(Modifier.height(22.dp))

        if (s.file.extension == "webp" || s.file.extension == "gif") {
            AsyncImage(
                model = s.file,
                contentDescription = null,
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceCard)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Violet.copy(alpha = 0.7f), Magenta.copy(alpha = 0.7f)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (s.file.extension == "mp3") Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            s.file.name,
            color = TextDim,
            fontSize = 11.sp,
            maxLines = 1
        )
        Spacer(Modifier.height(24.dp))

        if (s.packId != null) {
            GradientButton("OPEN WHATSAPP", Modifier.fillMaxWidth(0.85f)) {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_MAIN).apply {
                            setPackage("com.whatsapp")
                            addCategory(Intent.CATEGORY_LAUNCHER)
                        }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        } else {
            GradientButton("SHARE", Modifier.fillMaxWidth(0.85f)) {
                shareFile(context, s.file)
            }
            Spacer(Modifier.height(10.dp))
            GradientButton("SEND TO WHATSAPP", Modifier.fillMaxWidth(0.85f)) {
                shareFileTo(context, s.file, "com.whatsapp")
            }
            Spacer(Modifier.height(12.dp))
        }

        OutlinedButton(
            onClick = { navController.navigate(Routes.EDITOR) { popUpTo(Routes.HOME) } },
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("MAKE ANOTHER", color = Cyan, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } }) {
            Text("Back to home", color = TextDim)
        }
    }
}

@Composable
private fun FailureView(message: String, navController: NavHostController) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0x33FF5C7A)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFFF5C7A), modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("CONVERSION FAILED", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Text(message, color = TextDim, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
        Spacer(Modifier.height(24.dp))
        GradientButton("TRY AGAIN", Modifier.fillMaxWidth(0.8f)) {
            navController.navigate(Routes.EDITOR) { popUpTo(Routes.HOME) }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } }) {
            Text("Back to home", color = TextDim)
        }
    }
}

private fun shareFile(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "com.msj.stickerstudio.fileprovider", file)
    val mime = when (file.extension) {
        "webm" -> "video/webm"
        "mp3" -> "audio/mpeg"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        else -> "*/*"
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share with"))
}

private fun shareFileTo(context: android.content.Context, file: File, packageName: String) {
    val uri = FileProvider.getUriForFile(context, "com.msj.stickerstudio.fileprovider", file)
    val mime = when (file.extension) {
        "webm" -> "video/webm"
        "mp3" -> "audio/mpeg"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        else -> "*/*"
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setPackage(packageName)
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        shareFile(context, file)
    }
}
