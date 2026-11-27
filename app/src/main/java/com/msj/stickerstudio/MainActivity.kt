package com.msj.stickerstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.msj.stickerstudio.ui.MSJApp
import com.msj.stickerstudio.ui.theme.MSJTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            MSJTheme {
                MSJApp(navController = rememberNavController())
            }
        }
    }
}
