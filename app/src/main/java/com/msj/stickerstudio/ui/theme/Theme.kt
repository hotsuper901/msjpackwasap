package com.msj.stickerstudio.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

// ---------- M.S.J brand palette ----------
val Midnight = Color(0xFF0A0614)
val DeepSpace = Color(0xFF120B22)
val Violet = Color(0xFF7B2FF7)
val Magenta = Color(0xFFE026A0)
val Cyan = Color(0xFF19D3FF)
val Gold = Color(0xFFFFC94D)
val SurfaceCard = Color(0xE61B1230)
val TextPrimary = Color(0xFFF3EEFF)
val TextDim = Color(0xFFB9AEE0)

val BrandGradient = Brush.linearGradient(listOf(Violet, Magenta, Cyan))

private val DarkColors = darkColorScheme(
    primary = Magenta,
    onPrimary = Color.White,
    secondary = Cyan,
    onSecondary = Color(0xFF001418),
    tertiary = Violet,
    background = Midnight,
    onBackground = TextPrimary,
    surface = DeepSpace,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextDim,
    outline = Color(0xFF4A3D7A),
    error = Color(0xFFFF5C7A),
    onError = Color.White
)

private val Shapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10),
    small = androidx.compose.foundation.shape.RoundedCornerShape(14),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36)
)

@Composable
fun MSJTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        shapes = Shapes,
        typography = Typography(),
        content = content
    )
}

/**
 * Animated mesh gradient — slowly drifting nebula behind every screen.
 * Brand centerpiece of the M.S.J look.
 */
@Composable
fun NebulaBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "nebula")
    val driftA by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Reverse),
        label = "driftA"
    )
    val driftB by transition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "driftB"
    )
    val driftC by transition.animateFloat(
        initialValue = 0.3f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(13000, easing = LinearEasing), RepeatMode.Reverse),
        label = "driftC"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Midnight)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val scale = 1.6f
                    translationX = (driftA - 0.5f) * size.width * 0.35f
                    translationY = (driftB - 0.5f) * size.height * 0.35f
                    this.scaleX = scale
                    this.scaleY = scale
                    alpha = 0.75f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x66463BCF), Color(0x33463BCF), Color.Transparent),
                        center = Offset(0.2f, 0.15f),
                        radius = 1300f
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val scale = 1.5f
                    translationX = (driftB - 0.5f) * size.width * 0.4f
                    translationY = (driftA - 0.5f) * size.height * 0.4f
                    this.scaleX = scale
                    this.scaleY = scale
                    alpha = 0.7f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x55E026A0), Color(0x22E026A0), Color.Transparent),
                        center = Offset(0.85f, 0.3f),
                        radius = 1200f
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val scale = 1.4f
                    translationX = (driftC - 0.5f) * size.width * 0.5f
                    translationY = (driftC - 0.5f) * size.height * 0.5f
                    this.scaleX = scale
                    this.scaleY = scale
                    alpha = 0.55f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x4419D3FF), Color(0x1819D3FF), Color.Transparent),
                        center = Offset(0.5f, 0.9f),
                        radius = 1100f
                    )
                )
        )
    }
}

/** Shared glass card surface with brand outline. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.background(
            brush = Brush.verticalGradient(listOf(Color(0x30FFFFFF), Color(0x0AFFFFFF))),
            shape = Shapes.large
        )
    ) {
        content()
    }
}
