package ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
data class Shapes(
    // Small components: Tooltips, Tags, Badges
    val small: RoundedCornerShape = RoundedCornerShape(4.dp),

    // Medium components: Buttons, TextFields, Small Cards
    val medium: RoundedCornerShape = RoundedCornerShape(8.dp),

    // Large components: Patient Cards, Action Sheets, Dialogs
    val large: RoundedCornerShape = RoundedCornerShape(16.dp),

    // Extra Large: Top Bar bottom corners, Hero sections
    val extraLarge: RoundedCornerShape = RoundedCornerShape(24.dp)
)

val LocalShapes = staticCompositionLocalOf { Shapes() }