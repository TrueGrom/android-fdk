package grmv.android.fdk.screen.paging

import androidx.annotation.FloatRange
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.isSpecified

/**
 * Receiver for a paging load-state slot, in a list ([PagingContent]) or a grid ([PagingGridContent],
 * [pagingItems]) alike.
 *
 * `LazyItemScope` and `LazyGridItemScope` are unrelated types, and only the former carries
 * `fillParentMax*`. This scope is the intersection both layouts can honour, so a single
 * [PagingDefaults] instance words a failure the same way whether the content happens to be laid out
 * as rows or as tiles — a grid is not a second place to implement the app's loaders.
 *
 * In a list every member delegates straight to `LazyItemScope`. In a grid the slot is emitted as a
 * full-span item, so [fillParentMaxWidth] is the grid's content width; [fillParentMaxHeight] needs
 * the viewport height, which a lazy grid does not hand to its items — [PagingGridContent] measures
 * it and passes it down, while [pagingItems] takes it as a parameter and, left unset, treats
 * [fillParentMaxHeight] as a no-op (the slot then wraps its content). See [pagingItems] for when
 * that matters.
 */
@Stable
interface FdkPagingSlotScope {
    /**
     * Fills [fraction] of the parent's content width. See [LazyItemScope.fillParentMaxWidth].
     *
     * @param fraction share of the parent's content width to fill, between `0` and `1`.
     */
    fun Modifier.fillParentMaxWidth(
        @FloatRange(from = 0.0, to = 1.0) fraction: Float = 1f,
    ): Modifier

    /**
     * Fills [fraction] of the parent's content height. See [LazyItemScope.fillParentMaxHeight].
     *
     * @param fraction share of the parent's content height to fill, between `0` and `1`.
     */
    fun Modifier.fillParentMaxHeight(
        @FloatRange(from = 0.0, to = 1.0) fraction: Float = 1f,
    ): Modifier

    /**
     * Fills [fraction] of the parent's content size. See [LazyItemScope.fillParentMaxSize].
     *
     * @param fraction share of the parent's content size to fill, between `0` and `1`.
     */
    fun Modifier.fillParentMaxSize(
        @FloatRange(from = 0.0, to = 1.0) fraction: Float = 1f,
    ): Modifier

    /**
     * Animates this slot's appearance, disappearance and placement, exactly as
     * [LazyItemScope.animateItem] / [LazyGridItemScope.animateItem] do — the slot is a keyed item in
     * both layouts.
     *
     * @param fadeInSpec spec for the slot appearing; `null` to appear without animating.
     * @param placementSpec spec for the slot moving; `null` to move without animating.
     * @param fadeOutSpec spec for the slot leaving; `null` to leave without animating.
     */
    fun Modifier.animateItem(
        fadeInSpec: FiniteAnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
        placementSpec: FiniteAnimationSpec<IntOffset>? =
            spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntOffset.VisibilityThreshold,
            ),
        fadeOutSpec: FiniteAnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
    ): Modifier
}

/** [FdkPagingSlotScope] over a `LazyColumn` item — every member is the `LazyItemScope` original. */
internal class ListPagingSlotScope(
    private val itemScope: LazyItemScope,
) : FdkPagingSlotScope {
    override fun Modifier.fillParentMaxWidth(fraction: Float): Modifier =
        with(itemScope) { fillParentMaxWidth(fraction) }

    override fun Modifier.fillParentMaxHeight(fraction: Float): Modifier =
        with(itemScope) { fillParentMaxHeight(fraction) }

    override fun Modifier.fillParentMaxSize(fraction: Float): Modifier =
        with(itemScope) { fillParentMaxSize(fraction) }

    override fun Modifier.animateItem(
        fadeInSpec: FiniteAnimationSpec<Float>?,
        placementSpec: FiniteAnimationSpec<IntOffset>?,
        fadeOutSpec: FiniteAnimationSpec<Float>?,
    ): Modifier = with(itemScope) { animateItem(fadeInSpec, placementSpec, fadeOutSpec) }
}

/**
 * [FdkPagingSlotScope] over a full-span `LazyVerticalGrid` item.
 *
 * [viewport] is the grid's content area (viewport minus content padding); either dimension may be
 * unspecified. A full-span item is already measured against the grid's finite content width, so an
 * unspecified width falls back to `fillMaxWidth` and loses nothing. Height has no such fallback:
 * lazy items are measured with an infinite main axis, so without the measured viewport the slot can
 * only wrap its content.
 */
internal class GridPagingSlotScope(
    private val itemScope: LazyGridItemScope,
    private val viewport: DpSize,
) : FdkPagingSlotScope {
    override fun Modifier.fillParentMaxWidth(fraction: Float): Modifier =
        viewport.width.orNull()?.let { width(it * fraction) } ?: fillMaxWidth(fraction)

    override fun Modifier.fillParentMaxHeight(fraction: Float): Modifier =
        viewport.height.orNull()?.let { height(it * fraction) } ?: this

    override fun Modifier.fillParentMaxSize(fraction: Float): Modifier =
        fillParentMaxWidth(fraction).fillParentMaxHeight(fraction)

    override fun Modifier.animateItem(
        fadeInSpec: FiniteAnimationSpec<Float>?,
        placementSpec: FiniteAnimationSpec<IntOffset>?,
        fadeOutSpec: FiniteAnimationSpec<Float>?,
    ): Modifier = with(itemScope) { animateItem(fadeInSpec, placementSpec, fadeOutSpec) }

    // Infinite is as unusable as unspecified here: it reaches us from a grid measured in an
    // unbounded parent, where there is no viewport to fill.
    private fun Dp.orNull(): Dp? = takeIf { it.isSpecified && it.isFinite }
}
