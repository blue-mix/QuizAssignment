//package ui
//
//import androidx.compose.foundation.LocalIndication
//import androidx.compose.foundation.isSystemInDarkTheme
//import androidx.compose.foundation.text.selection.LocalTextSelectionColors
//import androidx.compose.foundation.text.selection.TextSelectionColors
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.CompositionLocalProvider
//import androidx.compose.runtime.ReadOnlyComposable
//import androidx.compose.runtime.remember
//import androidx.compose.ui.graphics.Color
//import ui.foundation.ripple
//
//object AppTheme {
//    val colors: Colors
//        @ReadOnlyComposable @Composable
//        get() = LocalColors.current
//
//    val typography: Typography
//        @ReadOnlyComposable @Composable
//        get() = LocalTypography.current
//}
//
//@Composable
//fun AppTheme(
//    isDarkTheme: Boolean = isSystemInDarkTheme(),
//    content: @Composable () -> Unit,
//) {
//    val rippleIndication = ripple()
//    val selectionColors = rememberTextSelectionColors(LightColors)
//    val typography = provideTypography()
//    val colors = if (isDarkTheme) DarkColors else LightColors
//
//    CompositionLocalProvider(
//        LocalColors provides colors,
//        LocalTypography provides typography,
//        LocalIndication provides rippleIndication,
//        LocalTextSelectionColors provides selectionColors,
//        LocalContentColor provides colors.contentColorFor(colors.background),
//        LocalTextStyle provides typography.body1,
//        content = content,
//    )
//}
//
//@Composable
//fun contentColorFor(color: Color): Color {
//    return AppTheme.colors.contentColorFor(color)
//}
//
//@Composable
//internal fun rememberTextSelectionColors(colorScheme: Colors): TextSelectionColors {
//    val primaryColor = colorScheme.primary
//    return remember(primaryColor) {
//        TextSelectionColors(
//            handleColor = primaryColor,
//            backgroundColor = primaryColor.copy(alpha = TextSelectionBackgroundOpacity),
//        )
//    }
//}
//
//internal const val TextSelectionBackgroundOpacity = 0.4f

package ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import ui.AppTheme.shapes
import ui.foundation.ripple

object AppTheme {
    val colors: Colors
        @ReadOnlyComposable @Composable
        get() = LocalColors.current

    val typography: Typography
        @ReadOnlyComposable @Composable
        get() = LocalTypography.current
    val shapes: Shapes
        @ReadOnlyComposable @Composable get() = LocalShapes.current
}

@Composable
fun AppTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (isDarkTheme) DarkColors else LightColors

    val rippleIndication = ripple()
    val selectionColors = rememberTextSelectionColors(colors)
    val typography = provideTypography()

    CompositionLocalProvider(
        LocalColors provides colors,
        LocalTypography provides typography,
        LocalShapes provides shapes,
        LocalIndication provides rippleIndication,
        LocalTextSelectionColors provides selectionColors,
        LocalContentColor provides colors.contentColorFor(colors.background),
        LocalTextStyle provides typography.body1,
        content = content,
    )
}

@Composable
fun contentColorFor(color: Color): Color {
    return AppTheme.colors.contentColorFor(color)
}

@Composable
internal fun rememberTextSelectionColors(colorScheme: Colors): TextSelectionColors {
    val primaryColor = colorScheme.primary
    return remember(primaryColor) {
        TextSelectionColors(
            handleColor = primaryColor,
            backgroundColor = primaryColor.copy(alpha = TextSelectionBackgroundOpacity),
        )
    }
}

internal const val TextSelectionBackgroundOpacity = 0.4f