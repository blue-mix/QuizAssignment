//package ui
//
//import androidx.compose.runtime.Immutable
//import androidx.compose.runtime.compositionLocalOf
//import androidx.compose.runtime.staticCompositionLocalOf
//import androidx.compose.ui.graphics.Color
//
//val Black: Color = Color(0xFF000000)
//val Gray900: Color = Color(0xFF282828)
//val Gray800: Color = Color(0xFF4b4b4b)
//val Gray700: Color = Color(0xFF5e5e5e)
//val Gray600: Color = Color(0xFF727272)
//val Gray500: Color = Color(0xFF868686)
//val Gray400: Color = Color(0xFFC7C7C7)
//val Gray300: Color = Color(0xFFDFDFDF)
//val Gray200: Color = Color(0xFFE2E2E2)
//val Gray100: Color = Color(0xFFF7F7F7)
//val Gray50: Color = Color(0xFFFFFFFF)
//val White: Color = Color(0xFFFFFFFF)
//
//val Red900: Color = Color(0xFF520810)
//val Red800: Color = Color(0xFF950f22)
//val Red700: Color = Color(0xFFbb032a)
//val Red600: Color = Color(0xFFde1135)
//val Red500: Color = Color(0xFFf83446)
//val Red400: Color = Color(0xFFfc7f79)
//val Red300: Color = Color(0xFFffb2ab)
//val Red200: Color = Color(0xFFffd2cd)
//val Red100: Color = Color(0xFFffe1de)
//val Red50: Color = Color(0xFFfff0ee)
//
//val Blue900: Color = Color(0xFF276EF1)
//val Blue800: Color = Color(0xFF3F7EF2)
//val Blue700: Color = Color(0xFF578EF4)
//val Blue600: Color = Color(0xFF6F9EF5)
//val Blue500: Color = Color(0xFF87AEF7)
//val Blue400: Color = Color(0xFF9FBFF8)
//val Blue300: Color = Color(0xFFB7CEFA)
//val Blue200: Color = Color(0xFFCFDEFB)
//val Blue100: Color = Color(0xFFE7EEFD)
//val Blue50: Color = Color(0xFFFFFFFF)
//
//val Green950: Color = Color(0xFF0B4627)
//val Green900: Color = Color(0xFF16643B)
//val Green800: Color = Color(0xFF1A7544)
//val Green700: Color = Color(0xFF178C4E)
//val Green600: Color = Color(0xFF1DAF61)
//val Green500: Color = Color(0xFF1FC16B)
//val Green400: Color = Color(0xFF3EE089)
//val Green300: Color = Color(0xFF84EBB4)
//val Green200: Color = Color(0xFFC2F5DA)
//val Green100: Color = Color(0xFFD0FBE9)
//val Green50: Color = Color(0xFFE0FAEC)
//
//@Immutable
//data class Colors(
//    val primary: Color,
//    val onPrimary: Color,
//    val secondary: Color,
//    val onSecondary: Color,
//    val tertiary: Color,
//    val onTertiary: Color,
//    val error: Color,
//    val onError: Color,
//    val success: Color,
//    val onSuccess: Color,
//    val disabled: Color,
//    val onDisabled: Color,
//    val surface: Color,
//    val onSurface: Color,
//    val background: Color,
//    val onBackground: Color,
//    val outline: Color,
//    val transparent: Color = Color.Transparent,
//    val white: Color = White,
//    val black: Color = Black,
//    val text: Color,
//    val textSecondary: Color,
//    val textDisabled: Color,
//    val scrim: Color,
//    val elevation: Color,
//)
//
//internal val LightColors =
//    Colors(
//        primary = Black,
//        onPrimary = White,
//        secondary = Gray400,
//        onSecondary = Black,
//        tertiary = Blue900,
//        onTertiary = White,
//        surface = Gray200,
//        onSurface = Black,
//        error = Red600,
//        onError = White,
//        success = Green600,
//        onSuccess = White,
//        disabled = Gray100,
//        onDisabled = Gray500,
//        background = White,
//        onBackground = Black,
//        outline = Gray300,
//        transparent = Color.Transparent,
//        white = White,
//        black = Black,
//        text = Black,
//        textSecondary = Gray700,
//        textDisabled = Gray400,
//        scrim = Color.Black.copy(alpha = 0.32f),
//        elevation = Gray700,
//    )
//
//internal val DarkColors =
//    Colors(
//        primary = White,
//        onPrimary = Black,
//        secondary = Gray400,
//        onSecondary = White,
//        tertiary = Blue300,
//        onTertiary = Black,
//        surface = Gray900,
//        onSurface = White,
//        error = Red400,
//        onError = Black,
//        success = Green700,
//        onSuccess = Black,
//        disabled = Gray700,
//        onDisabled = Gray500,
//        background = Black,
//        onBackground = White,
//        outline = Gray800,
//        transparent = Color.Transparent,
//        white = White,
//        black = Black,
//        text = White,
//        textSecondary = Gray300,
//        textDisabled = Gray600,
//        scrim = Color.Black.copy(alpha = 0.72f),
//        elevation = Gray200,
//    )
//
//val LocalColors = staticCompositionLocalOf { LightColors }
//val LocalContentColor = compositionLocalOf { Color.Black }
//val LocalContentAlpha = compositionLocalOf { 1f }
//
//fun Colors.contentColorFor(backgroundColor: Color): Color {
//    return when (backgroundColor) {
//        primary -> onPrimary
//        secondary -> onSecondary
//        tertiary -> onTertiary
//        surface -> onSurface
//        error -> onError
//        success -> onSuccess
//        disabled -> onDisabled
//        background -> onBackground
//        else -> Color.Unspecified
//    }
//}

package ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ══════════════════════════════════════════════════════════════
//  NEUTRAL GRAYS — Softer, easier on eyes for medical UI
// ══════════════════════════════════════════════════════════════
val Black: Color = Color(0xFF1A1A1A)  // Softer black, less harsh
val Gray900: Color = Color(0xFF2C2C2C)
val Gray800: Color = Color(0xFF424242)
val Gray700: Color = Color(0xFF616161)
val Gray600: Color = Color(0xFF757575)
val Gray500: Color = Color(0xFF9E9E9E)
val Gray400: Color = Color(0xFFBDBDBD)
val Gray300: Color = Color(0xFFE0E0E0)
val Gray200: Color = Color(0xFFEEEEEE)
val Gray100: Color = Color(0xFFF5F5F5)
val Gray50: Color = Color(0xFFFAFAFA)
val White: Color = Color(0xFFFFFFFF)

// ══════════════════════════════════════════════════════════════
//  CRITICAL / ERROR — Medical red (serious alerts)
// ══════════════════════════════════════════════════════════════
val Red900: Color = Color(0xFF8B0000)  // Dark red for critical text
val Red800: Color = Color(0xFFC62828)
val Red700: Color = Color(0xFFD32F2F)
val Red600: Color = Color(0xFFE53935)  // Primary critical color
val Red500: Color = Color(0xFFF44336)
val Red400: Color = Color(0xFFEF5350)
val Red300: Color = Color(0xFFE57373)
val Red200: Color = Color(0xFFEF9A9A)
val Red100: Color = Color(0xFFFFCDD2)
val Red50: Color = Color(0xFFFFEBEE)

// ══════════════════════════════════════════════════════════════
//  WARNING / CAUTION — Warm amber for warnings
// ══════════════════════════════════════════════════════════════
val Amber900: Color = Color(0xFFFF6F00)
val Amber800: Color = Color(0xFFFF8F00)
val Amber700: Color = Color(0xFFFFA000)
val Amber600: Color = Color(0xFFFFB300)  // Primary warning color
val Amber500: Color = Color(0xFFFFC107)
val Amber400: Color = Color(0xFFFFCA28)
val Amber300: Color = Color(0xFFFFD54F)
val Amber200: Color = Color(0xFFFFE082)
val Amber100: Color = Color(0xFFFFECB3)
val Amber50: Color = Color(0xFFFFF8E1)

// ══════════════════════════════════════════════════════════════
//  PRIMARY / ACCENT — Medical blue (trust, calm, professional)
// ══════════════════════════════════════════════════════════════
val Blue900: Color = Color(0xFF0D47A1)
val Blue800: Color = Color(0xFF1565C0)
val Blue700: Color = Color(0xFF1976D2)
val Blue600: Color = Color(0xFF1E88E5)  // Primary brand color
val Blue500: Color = Color(0xFF2196F3)
val Blue400: Color = Color(0xFF42A5F5)
val Blue300: Color = Color(0xFF64B5F6)
val Blue200: Color = Color(0xFF90CAF9)
val Blue100: Color = Color(0xFFBBDEFB)
val Blue50: Color = Color(0xFFE3F2FD)

// ══════════════════════════════════════════════════════════════
//  SUCCESS / NORMAL — Medical green (healthy, stable)
// ══════════════════════════════════════════════════════════════
val Green950: Color = Color(0xFF1B5E20)
val Green900: Color = Color(0xFF2E7D32)
val Green800: Color = Color(0xFF388E3C)
val Green700: Color = Color(0xFF43A047)
val Green600: Color = Color(0xFF4CAF50)  // Primary success color
val Green500: Color = Color(0xFF66BB6A)
val Green400: Color = Color(0xFF81C784)
val Green300: Color = Color(0xFF9CCC65)
val Green200: Color = Color(0xFFC5E1A5)
val Green100: Color = Color(0xFFDCEDC8)
val Green50: Color = Color(0xFFF1F8E9)

// ══════════════════════════════════════════════════════════════
//  TEAL — Secondary accent (vitals, data viz)
// ══════════════════════════════════════════════════════════════
val Teal900: Color = Color(0xFF004D40)
val Teal800: Color = Color(0xFF00695C)
val Teal700: Color = Color(0xFF00796B)
val Teal600: Color = Color(0xFF00897B)
val Teal500: Color = Color(0xFF009688)
val Teal400: Color = Color(0xFF26A69A)
val Teal300: Color = Color(0xFF4DB6AC)
val Teal200: Color = Color(0xFF80CBC4)
val Teal100: Color = Color(0xFFB2DFDB)
val Teal50: Color = Color(0xFFE0F2F1)

@Immutable
data class Colors(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val error: Color,
    val onError: Color,
    val warning: Color,      // NEW: Dedicated warning color
    val onWarning: Color,    // NEW
    val success: Color,
    val onSuccess: Color,
    val disabled: Color,
    val onDisabled: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,  // NEW: For cards/elevated surfaces
    val background: Color,
    val onBackground: Color,
    val outline: Color,
    val transparent: Color = Color.Transparent,
    val white: Color = White,
    val black: Color = Black,
    val text: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val scrim: Color,
    val elevation: Color,
)

// ══════════════════════════════════════════════════════════════
//  LIGHT THEME — Clean, medical-grade aesthetics
// ══════════════════════════════════════════════════════════════
internal val LightColors =
    Colors(
        primary = Blue600,              // Professional medical blue
        onPrimary = White,
        secondary = Teal600,            // Teal for secondary actions
        onSecondary = White,
        tertiary = Blue700,
        onTertiary = White,
        surface = White,                // Clean white cards
        onSurface = Gray900,
        surfaceVariant = Gray50,        // Slightly off-white for depth
        error = Red600,                 // Critical alerts
        onError = White,
        warning = Amber600,             // NEW: Warning state
        onWarning = Gray900,
        success = Green600,             // Healthy/normal state
        onSuccess = White,
        disabled = Gray200,
        onDisabled = Gray500,
        background = Gray50,            // Soft background, not harsh white
        onBackground = Gray900,
        outline = Gray300,
        transparent = Color.Transparent,
        white = White,
        black = Black,
        text = Gray900,                 // High contrast text
        textSecondary = Gray600,        // Readable secondary text
        textDisabled = Gray400,
        scrim = Color.Black.copy(alpha = 0.32f),
        elevation = Gray200,
    )

// ══════════════════════════════════════════════════════════════
//  DARK THEME — AMOLED-friendly, reduced eye strain for night use
// ══════════════════════════════════════════════════════════════
internal val DarkColors =
    Colors(
        primary = Blue400,              // Lighter blue for dark mode
        onPrimary = Gray900,
        secondary = Teal400,            // Lighter teal
        onSecondary = Gray900,
        tertiary = Blue300,
        onTertiary = Gray900,
        surface = Gray900,              // Dark card surfaces
        onSurface = Gray100,
        surfaceVariant = Gray800,       // Elevated cards
        error = Red400,                 // Softer red for dark mode
        onError = Gray900,
        warning = Amber400,             // NEW: Softer warning for dark
        onWarning = Gray900,
        success = Green500,             // Softer green for dark mode
        onSuccess = Gray900,
        disabled = Gray700,
        onDisabled = Gray500,
        background = Black,             // True black for AMOLED
        onBackground = Gray100,
        outline = Gray700,
        transparent = Color.Transparent,
        white = White,
        black = Black,
        text = Gray100,                 // High contrast on dark
        textSecondary = Gray400,
        textDisabled = Gray600,
        scrim = Color.Black.copy(alpha = 0.72f),
        elevation = Gray300,
    )

val LocalColors = staticCompositionLocalOf { LightColors }
val LocalContentColor = compositionLocalOf { Color.Black }
val LocalContentAlpha = compositionLocalOf { 1f }

fun Colors.contentColorFor(backgroundColor: Color): Color {
    return when (backgroundColor) {
        primary -> onPrimary
        secondary -> onSecondary
        tertiary -> onTertiary
        surface -> onSurface
        surfaceVariant -> onSurface
        error -> onError
        warning -> onWarning
        success -> onSuccess
        disabled -> onDisabled
        background -> onBackground
        else -> Color.Unspecified
    }
}