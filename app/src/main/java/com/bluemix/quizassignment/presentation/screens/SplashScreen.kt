package com.bluemix.quizassignment.presentation.screens


import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bluemix.quizassignment.presentation.viewmodels.SplashUiState
import com.bluemix.quizassignment.presentation.viewmodels.SplashViewModel

import org.koin.androidx.compose.koinViewModel
import ui.AppTheme
import ui.components.Text
import ui.components.progressindicators.CircularProgressIndicator

// ─────────────────────────────────────────────────────────────────────────────
// Palette tokens — shared across all screens in this file's package
// ─────────────────────────────────────────────────────────────────────────────
internal val ColorBackground   = Color(0xFF0A0A0A)
internal val ColorSurface      = Color(0xFF141414)
internal val ColorSurfaceHigh  = Color(0xFF1E1E1E)
internal val ColorAccent       = Color(0xFFD4F03C)   // electric chartreuse
internal val ColorCorrect      = Color(0xFF39D98A)   // neon green
internal val ColorWrong        = Color(0xFFFF3B5C)   // neon crimson
internal val ColorTextPrimary  = Color(0xFFF5F5F0)
internal val ColorTextSecondary= Color(0xFF8A8A8A)
internal val ColorBorder       = Color(0xFF2A2A2A)

/**
 * Entry-point screen composable for the Splash stage.
 *
 * Observes [SplashViewModel.uiState] and calls [onNavigateToHome] exactly
 * once when the state transitions to [SplashUiState.NavigateToHome].
 * All navigation decisions are driven by state — this composable never
 * calls the NavController directly.
 */
@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Trigger navigation as a side-effect, keyed on state to fire once.
    LaunchedEffect(state) {
        if (state is SplashUiState.NavigateToHome) onNavigateToHome()
    }

    SplashContent()
}

/**
 * Stateless content composable — receives no ViewModel reference.
 * Previews and tests can render this without any DI setup.
 */
@Composable
private fun SplashContent() {
    // Fade-in animation for the logo group
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier          = Modifier.alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // ── Wordmark ──────────────────────────────────────────────────────
            Text(
                text  = "Grammar",
                color = ColorTextPrimary,
                style = AppTheme.typography.h1.copy(
                    fontSize   = 48.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp,
                ),
            )
            Text(
                text  = "Flow",
                color = ColorAccent,
                style = AppTheme.typography.h1.copy(
                    fontSize   = 48.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle  = FontStyle.Italic,
                    letterSpacing = (-2).sp,
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Tagline ───────────────────────────────────────────────────────
            Text(
                text  = "Master English Grammar",
                color = ColorTextSecondary,
                style = AppTheme.typography.body2.copy(
                    letterSpacing = 2.sp,
                    fontSize      = 11.sp,
                ),
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ── Lumo indeterminate circular indicator ─────────────────────────
            CircularProgressIndicator(
                modifier    = Modifier.size(28.dp),
                color       = ColorAccent,
                trackColor  = ColorSurfaceHigh,
                strokeWidth = 2.dp,
            )
        }
    }
}