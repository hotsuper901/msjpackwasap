package com.msj.stickerstudio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msj.stickerstudio.ui.theme.*

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 20.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(Violet, Magenta, Cyan))),
            contentAlignment = Alignment.Center
        ) {
            Text("MSJ", color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(18.dp))
        Text("MSJ STICKER STUDIO", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "Video \u2192 animated sticker \u00B7 music sticker \u00B7 ringtone \u2014 straight into WhatsApp.",
            color = TextDim,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
        Spacer(Modifier.height(26.dp))

        Column(Modifier.fillMaxWidth()) {
            Text("FEATURES", color = TextDim, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(10.dp))
            Feature(Icons.Rounded.AutoAwesome, "Animated WebP stickers", "Native WhatsApp sticker keyboard support with tray icon.")
            Feature(Icons.Rounded.Movie, "GIF stickers", "Classic looping GIF format, also registered as sticker packs.")
            Feature(Icons.Rounded.MusicNote, "Music stickers", "WebM with VP9 video + Opus audio \u2014 the clip keeps its sound.")
            Feature(Icons.Rounded.GraphicEq, "Ringtone extraction", "One tap to pull a 192 kbps MP3 soundtrack out of any video.")
            Feature(Icons.Rounded.Tune, "Full control", "Size, frame rate, quality, emojis, pack name \u2014 all yours.")
        }

        Spacer(Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceCard)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CREATED BY", color = TextDim, fontSize = 11.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Cyan)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "M.S.J",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        letterSpacing = 4.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Magenta)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Design \u00B7 Code \u00B7 FFmpeg pipeline \u2014 all M.S.J \u00B7 v1.0.0",
                    color = TextDim.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Made with \u2764 in a dark room with cold coffee.",
            color = TextDim.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Violet.copy(alpha = 0.4f), Magenta.copy(alpha = 0.4f)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Cyan, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(desc, color = TextDim, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}
