package grmv.android.fdk.uikit.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Fills the available [Box] space and centers [content] within it.
 *
 * Must be called inside a [BoxScope] (i.e. directly within a `Box { }` lambda). Use this when a
 * single child should be centered inside a parent that already has a defined size.
 */
@Composable
fun BoxScope.FdKitCenterBox(
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

/**
 * Horizontally centers [content] across the full column width using a [Box] that fills width.
 *
 * Unlike [BoxScope.FdKitCenterBox] (which fills the entire parent box), this variant fills only the
 * column width and does not constrain height — suitable for centering a single item within a
 * column row.
 *
 * @param content item to center horizontally within the column width.
 */
@Composable
fun ColumnScope.FdKitCenterBox(
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        content()
    }
}
