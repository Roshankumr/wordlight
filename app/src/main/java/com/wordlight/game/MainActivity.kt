package com.wordlight.game

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wordlight.game.ui.navigation.WordlightNavHost
import com.wordlight.game.ui.theme.WordlightTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host. All screens are Compose destinations inside [WordlightNavHost];
 * the NavController's back stack is saved/restored by the framework, so rotation and
 * process death keep the user on the same screen.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            WordlightTheme {
                WordlightNavHost()
            }
        }
    }
}
