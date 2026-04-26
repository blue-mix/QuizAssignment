package com.bluemix.quizassignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ui.AppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.bluemix.quizassignment.presentation.navigation.GrammarFlowNavHost
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.core.annotation.KoinExperimentalAPI
import ui.components.Surface

class MainActivity : ComponentActivity() {

    @OptIn(KoinExperimentalAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before setContent() to apply edge-to-edge window flags.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            KoinAndroidContext {
                GrammarFlowApp()
            }
        }
    }
}
@Composable
fun GrammarFlowApp(
    navController: NavHostController = rememberNavController(),
) {
    AppTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A)),
        ) {
            GrammarFlowNavHost(navController = navController)
        }
    }
}