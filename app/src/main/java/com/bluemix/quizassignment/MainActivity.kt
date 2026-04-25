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
import ui.components.Surface


/**
 * The single Activity for GrammarFlow.
 *
 * ── Why one Activity? ─────────────────────────────────────────────────────────
 * The entire screen graph is managed by Jetpack Navigation Compose. Each
 * "screen" is a composable destination — not a separate Activity or Fragment.
 * A single-Activity architecture eliminates inter-Activity transition overhead,
 * shared ViewModel complexity, and Intent-based argument passing.
 *
 * ── enableEdgeToEdge() ────────────────────────────────────────────────────────
 * Called before [setContent] to opt into the full-bleed display mode introduced
 * in Android 15 (and back-ported by AndroidX). This draws the app behind both
 * the status bar and the navigation bar, eliminating the two coloured system UI
 * strips. Individual screens control their own inset padding via
 * [androidx.compose.foundation.layout.statusBarsPadding] and
 * [androidx.compose.foundation.layout.navigationBarsPadding].
 *
 * ── LumoTheme ─────────────────────────────────────────────────────────────────
 * [AppTheme] (Lumo UI's generated theme wrapper) is the outermost composable,
 * providing the full token system — colours, typography, shapes, and elevation —
 * to every composable in the tree via [androidx.compose.runtime.CompositionLocal].
 * No theme token should be hard-coded below this level.
 *
 * ── Surface ───────────────────────────────────────────────────────────────────
 * The Lumo [Surface] fills the entire screen and paints the OLED-black
 * background that every screen in GrammarFlow inherits. Individual screens
 * paint their own backgrounds on top; this Surface acts as a fallback for
 * animated transitions where the incoming composable hasn't drawn yet.
 *
 * ── NavController lifetime ────────────────────────────────────────────────────
 * [rememberNavController] is called here (not inside [GrammarFlowNavHost]) so
 * the controller survives Compose recompositions triggered by configuration
 * changes. It lives in the composition's saved state and is reconstructed with
 * the correct back-stack after process death via the Navigation back-stack
 * restoration mechanism.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before setContent() to apply edge-to-edge window flags.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            GrammarFlowApp()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Root composable — extracted from setContent for testability and previews
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Root composable tree for the entire application.
 *
 * Extracted from [MainActivity.onCreate] so that:
 *  - Compose Preview tools can render the root without a real Activity.
 *  - UI tests can call [GrammarFlowApp] directly inside a [ComposeContentTestRule].
 *  - [MainActivity] stays a minimal orchestrator with no business logic.
 *
 * @param navController Defaults to [rememberNavController] for production use.
 *                      Override in tests to supply a [TestNavHostController].
 */
@Composable
fun GrammarFlowApp(
    navController: NavHostController = rememberNavController(),
) {
    AppTheme {
        // Lumo Surface fills the screen and provides the OLED background as a
        // safe fallback colour during screen transitions.
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A)),   // GrammarFlow OLED black
        ) {
            GrammarFlowNavHost(navController = navController)
        }
    }
}