package grmv.android.fdk.screen.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import grmv.android.fdk.screen.LocalContentPaddingDefaults
import grmv.android.fdk.screen.ScaffoldScope
import grmv.android.fdk.screen.ContentPaddingDefaults

/**
 * A full-size [Column] scoped to a [ScaffoldScope], intended for static (non-scrolling) screen bodies.
 *
 * Content overflows the visible area without scroll support — use [FdKitScrollableScreen] when the
 * layout may not fit on screen. The column fills its parent and applies [contentPadding] so content
 * respects the standard screen margins.
 *
 * @param modifier Applied to the column before the fill and padding modifiers.
 * @param contentPadding Padding around [content]; defaults to the current [ContentPaddingDefaults].
 * @param content Column body, receives [ColumnScope] for weight/alignment utilities.
 */
@Composable
fun ScaffoldScope.FdKitScreenColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = LocalContentPaddingDefaults.current.contentPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        content = content,
    )
}
