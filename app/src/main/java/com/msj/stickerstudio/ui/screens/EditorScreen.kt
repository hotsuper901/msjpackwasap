package com.msj.stickerstudio.ui.screens

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.msj.stickerstudio.*
import com.msj.stickerstudio.ui.GradientButton
import com.msj.stickerstudio.ui.Routes
import com.msj.stickerstudio.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val EMOJI_SET = listOf(
    "\uD83D\uDE00", "\uD83D\uDE02", "\uD83D\uDE0D", "\uD83D\uDE2D", "\uD83D\uDE0E",
    "\uD83D\uDD25", "\u2764\uFE0F", "\uD83D\uDC9C", "\uD83C\uDFB6", "\uD83D\uDC51",
    "\uD83D\uDC4D", "\uD83D\uDCA5", "\uD83C\uDF1F", "\uD83D\uDE18", "\uD83E\uDD70"
)

@Composable
fun EditorScreen(navController: NavHostController) {
    val context = LocalContext.current
    var inputUri by remember { mutableStateOf<Uri?>(null) }
    var inputName by remember { mutableStateOf("") }
    var packName by remember { mutableStateOf("M.S.J Pack") }
    var stickerTitle by remember { mutableStateOf("MSJ Sticker") }
    var format by remember { mutableStateOf(OutputFormat.ANIMATED_WEBP) }
    var size by remember { mutableStateOf(512) }
    var fps by remember { mutableStateOf(15f) }
    var quality by remember { mutableStateOf(70f) }
    var withMusic by remember { mutableStateOf(true) }
    var emojis by remember { mutableStateOf(listOf("\uD83D\uDE00")) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            inputUri = uri
            inputName = WhatsAppHandler.queryDisplayName(context, uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 20.dp, bottom = 24.dp)
    ) {
        Text("NEW STICKER", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Text("Pick a video, tune it, convert it.", color = TextDim, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))

        // ---------- Video picker ----------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (inputUri == null) 150.dp else 210.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceCard)
                .clickable { picker.launch("video/*") },
            contentAlignment = Alignment.Center
        ) {
            if (inputUri == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Brush.linearGradient(listOf(Violet, Magenta, Cyan))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("TAP TO PICK A VIDEO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("MP4, WebM, MOV \u00B7 any length", color = TextDim, fontSize = 12.sp)
                }
            } else {
                VideoPreviewCard(inputUri!!, inputName)
            }
        }

        Spacer(Modifier.height(20.dp))

        // ---------- Text fields ----------
        OutlinedTextField(
            value = packName,
            onValueChange = { packName = it.take(30) },
            label = { Text("Pack name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors(),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = stickerTitle,
            onValueChange = { stickerTitle = it.take(30) },
            label = { Text("Sticker title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(Modifier.height(20.dp))

        // ---------- Format chips ----------
        Text("OUTPUT FORMAT", color = TextDim, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutputFormat.entries.forEach { f ->
                FormatRow(
                    format = f,
                    selected = format == f,
                    onSelect = { format = f }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ---------- Size chips ----------
        Text("STICKER SIZE", color = TextDim, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(256, 512).forEach { s ->
                FilterChip(
                    selected = size == s,
                    onClick = { size = s },
                    label = { Text("${s}px") },
                    shape = RoundedCornerShape(14.dp),
                    colors = chipColors()
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---------- FPS ----------
        Text("FRAME RATE  ${fps.toInt()} fps", color = TextDim, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Slider(
            value = fps,
            onValueChange = { fps = it },
            valueRange = 5f..30f,
            steps = 24,
            colors = sliderColors()
        )

        Spacer(Modifier.height(10.dp))

        // ---------- Quality ----------
        Text("QUALITY  ${quality.toInt()}%", color = TextDim, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Slider(
            value = quality,
            onValueChange = { quality = it },
            valueRange = 10f..100f,
            colors = sliderColors()
        )

        Spacer(Modifier.height(14.dp))

        // ---------- Music toggle ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceCard)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Cyan)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Keep music / audio", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Bakes the clip's sound into music stickers", color = TextDim, fontSize = 11.sp)
            }
            Switch(checked = withMusic, onCheckedChange = { withMusic = it })
        }

        Spacer(Modifier.height(20.dp))

        // ---------- Emoji chips ----------
        Text("STICKER EMOJIS (tap to add/remove)", color = TextDim, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            EMOJI_SET.forEach { emoji ->
                val selected = emojis.contains(emoji)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) Magenta.copy(alpha = 0.85f) else SurfaceCard)
                        .clickable {
                            emojis = if (selected) emojis - emoji
                            else (emojis + emoji).take(5)
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(emoji, fontSize = 17.sp)
                }
            }
        }

        Spacer(Modifier.height(26.dp))

        GradientButton(
            text = "CONVERT \u2699",
            enabled = inputUri != null && emojis.isNotEmpty()
        ) {
            Session.inputUri = inputUri
            Session.inputName = inputName
            Session.settings = ConvertSettings(
                format = format,
                size = size,
                fps = fps.toInt(),
                quality = quality.toInt(),
                withMusic = withMusic,
                emojis = emojis,
                stickerTitle = stickerTitle,
                packName = packName
            )
            navController.navigate(Routes.CONVERT)
        }
    }
}

@Composable
private fun VideoPreviewCard(uri: Uri, name: String) {
    val context = LocalContext.current
    var frame by remember { mutableStateOf<Bitmap?>(null) }
    var durationMs by remember { mutableStateOf(0L) }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                durationMs = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L)
                frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                retriever.release()
            } catch (_: Exception) { }
        }
    }

    Box(Modifier.fillMaxSize()) {
        frame?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xB3000000))))
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.VideoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "$name  \u00B7  ${durationMs / 1000}s",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Icon(
            Icons.Rounded.Refresh,
            contentDescription = "Change video",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color(0x66000000))
                .padding(8.dp)
                .size(22.dp)
        )
    }
}

@Composable
private fun FormatRow(format: OutputFormat, selected: Boolean, onSelect: () -> Unit) {
    val icon = when (format) {
        OutputFormat.ANIMATED_WEBP -> Icons.Rounded.AutoAwesome
        OutputFormat.GIF -> Icons.Rounded.Movie
        OutputFormat.MUSIC_STICKER -> Icons.Rounded.MusicNote
        OutputFormat.RINGTONE -> Icons.Rounded.GraphicEq
    }
    val desc = when (format) {
        OutputFormat.ANIMATED_WEBP -> "Animated sticker pack \u2192 WhatsApp sticker keyboard"
        OutputFormat.GIF -> "Animated GIF for sharing \u00B7 WebP twin added to WhatsApp"
        OutputFormat.MUSIC_STICKER -> "WebM with sound (VP8 + Vorbis) \u2192 share or send as video"
        OutputFormat.RINGTONE -> "M4A (AAC) soundtrack from the video"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Brush.horizontalGradient(listOf(Violet.copy(alpha = 0.55f), Magenta.copy(alpha = 0.45f))) else Brush.linearGradient(listOf(SurfaceCard, SurfaceCard)))
            .clickable(onClick = onSelect)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (selected) Cyan.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) Cyan else TextDim, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(format.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(desc, color = TextDim, fontSize = 11.sp)
        }
        if (selected) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Cyan, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Magenta,
    unfocusedBorderColor = Color(0xFF4A3D7A),
    focusedLabelColor = Magenta,
    unfocusedLabelColor = TextDim,
    cursorColor = Cyan,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = SurfaceCard.copy(alpha = 0.5f),
    unfocusedContainerColor = SurfaceCard.copy(alpha = 0.5f)
)

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Magenta.copy(alpha = 0.85f),
    selectedLabelColor = Color.White,
    containerColor = SurfaceCard,
    labelColor = TextDim
)

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = Cyan,
    activeTrackColor = Magenta,
    activeTickColor = Color.White,
    inactiveTrackColor = Color(0xFF3A3152),
    inactiveTickColor = Color(0xFF3A3152)
)
