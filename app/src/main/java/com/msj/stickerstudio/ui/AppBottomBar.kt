package com.msj.stickerstudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.msj.stickerstudio.ui.theme.Cyan
import com.msj.stickerstudio.ui.theme.DeepSpace
import com.msj.stickerstudio.ui.theme.Magenta
import com.msj.stickerstudio.ui.theme.Violet

private data class BarItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppBottomBar(navController: NavHostController, currentRoute: String?) {
    val items = listOf(
        BarItem(Routes.HOME, "Home", Icons.Rounded.Home),
        BarItem(Routes.EDITOR, "Create", Icons.Rounded.AddCircle),
        BarItem(Routes.PACKS, "Packs", Icons.Rounded.Collections),
        BarItem(Routes.ABOUT, "About", Icons.Rounded.Info)
    )

    NavigationBar(
        containerColor = DeepSpace.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(DeepSpace.copy(alpha = 0.6f))
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) Brush.linearGradient(listOf(Violet, Magenta, Cyan))
                                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = {
                    Text(
                        item.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/** Gradient pill button used across screens. */
@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (enabled) Brush.horizontalGradient(listOf(Violet, Magenta, Cyan))
                else Brush.horizontalGradient(listOf(Color(0xFF3A3152), Color(0xFF3A3152)))
            )
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (enabled) Color.White else Color(0xFF9C93B8),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            letterSpacing = 1.sp
        )
    }
}
