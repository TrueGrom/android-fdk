package grmv.android.fdk.uikit.content

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Vertically centers [content] within the remaining column space using weighted spacers.
 *
 * [verticalBias] shifts the center point: `0f` pins content to the top, `1f` to the bottom,
 * `0.5f` (default) produces true center. Values outside `[0f, 1f]` are clamped.
 * The column must have available height for the spacers to take effect — typically used inside
 * a full-size scrolling or column screen that fills the parent.
 *
 * @param verticalBias Fraction of remaining space placed above [content]; `0.5f` = true center.
 * @param content Column body to center, receives [ColumnScope].
 */
@Composable
fun ColumnScope.FdKitCenteredVertically(
    verticalBias: Float = 0.5f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val topWeight = verticalBias.coerceIn(0f, 1f)
    val bottomWeight = 1f - topWeight
    Spacer(modifier = Modifier.weight(topWeight))
    content()
    Spacer(modifier = Modifier.weight(bottomWeight))
}

/**
 * Centers [content] both vertically and horizontally within the column.
 *
 * Combines [FdKitCenteredVertically] (weighted spacers) with [FdKitCenterBox] (full-width horizontal
 * centering). [verticalBias] has the same semantics as in [FdKitCenteredVertically]: `0.5f` is true
 * center, `0f` is top, `1f` is bottom.
 *
 * @param verticalBias Fraction of remaining vertical space placed above [content]; `0.5f` = true center.
 * @param content Column body to center; receives [ColumnScope].
 */
@Composable
fun ColumnScope.FdKitCenteredVerticallyHorizontally(
    verticalBias: Float = 0.5f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val topWeight = verticalBias.coerceIn(0f, 1f)
    val bottomWeight = 1f - topWeight
    Spacer(modifier = Modifier.weight(topWeight))
    FdKitCenterBox { content() }
    Spacer(modifier = Modifier.weight(bottomWeight))
}
