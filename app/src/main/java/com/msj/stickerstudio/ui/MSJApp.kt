package com.msj.stickerstudio.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.msj.stickerstudio.ui.screens.AboutScreen
import com.msj.stickerstudio.ui.screens.ConvertScreen
import com.msj.stickerstudio.ui.screens.EditorScreen
import com.msj.stickerstudio.ui.screens.HomeScreen
import com.msj.stickerstudio.ui.screens.PacksScreen
import com.msj.stickerstudio.ui.theme.NebulaBackground

object Routes {
    const val HOME = "home"
    const val EDITOR = "editor"
    const val CONVERT = "convert"
    const val PACKS = "packs"
    const val ABOUT = "about"
}

@Composable
fun MSJApp(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Box(Modifier.fillMaxSize()) {
        NebulaBackground()
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                if (currentRoute != Routes.CONVERT) {
                    AppBottomBar(navController, currentRoute)
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                composable(Routes.HOME) { HomeScreen(navController) }
                composable(Routes.EDITOR) { EditorScreen(navController) }
                composable(Routes.CONVERT) { ConvertScreen(navController) }
                composable(Routes.PACKS) { PacksScreen() }
                composable(Routes.ABOUT) { AboutScreen() }
            }
        }
    }
}
