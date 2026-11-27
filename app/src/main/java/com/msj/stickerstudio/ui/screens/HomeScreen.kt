package com.msj.stickerstudio.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.msj.stickerstudio.ui.Routes
import com.msj.stickerstudio.ui.theme.*

@Composable
fun HomeScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 28.dp, bottom = 20.dp)
    ) {
        // ---------- Hero ----------
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(Violet, Magenta, Cyan))),
                contentAlignment = Alignment.Center
            ) {
                Text("MSJ", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "STICKER STUDIO",
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    "video \u2192 sticker \u00B7 music \u00B7 WhatsApp",
                    color = Cyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(26.dp))

        // ---------- Big create card ----------
        val pulse = rememberInfiniteTransition(label = "pulse")
        val glow by pulse.animateFloat(
            initialValue = 0.55f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "glow"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Violet.copy(alpha = 0.95f),
                            Magenta.copy(alpha = glow),
                            Cyan.copy(alpha = 0.85f)
                        )
                    )
                )
                .clickable { navController.navigate(Routes.EDITOR) }
                .padding(26.dp)
        ) {
            Column {
                Icon(
                    Icons.Rounded.VideoLibrary,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "CREATE A STICKER",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Turn any video into an animated WhatsApp sticker, a music sticker with sound, or a ringtone.",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "START \u2192",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ---------- Feature cards ----------
        FeatureCard(
            icon = Icons.Rounded.MusicNote,
            title = "Music Sticker",
            desc = "Keep the sound of the clip \u2014 WebM with VP8 video + Vorbis audio, ready to share.",
            accent = Cyan
        )
        Spacer(Modifier.height(12.dp))
        FeatureCard(
            icon = Icons.Rounded.GraphicEq,
            title = "Ringtone Extract",
            desc = "Pull the soundtrack out of any video as a high-quality M4A (AAC).",
            accent = Gold
        )
        Spacer(Modifier.height(12.dp))
        FeatureCard(
            icon = Icons.Rounded.Collections,
            title = "My Packs",
            desc = "See every pack you've added to WhatsApp, in one place.",
            accent = Magenta,
            onClick = { navController.navigate(Routes.PACKS) }
        )

        Spacer(Modifier.height(26.dp))

        // ---------- Creator credit ----------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DeepSpace.copy(alpha = 0.7f))
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Crafted with ", color = TextDim, fontSize = 13.sp)
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Magenta,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(" by", color = TextDim, fontSize = 13.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "M.S.J",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = 3.sp
                )
                Text(
                    "v1.0.0 \u00B7 Sticker Studio",
                    color = TextDim.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    accent: Color,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceCard)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(3.dp))
            Text(desc, color = TextDim, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}
