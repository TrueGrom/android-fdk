package grmv.android.fdk.screen.paging

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import grmv.android.fdk.screen.content.FdkItemTransitions
import grmv.android.fdk.screen.content.LocalContentTransitionScope
import grmv.android.fdk.screen.content.LocalContentTransitions

/**
 * The load-state half of the paging slot DSL, shared by [FdkPagingScopeBuilder] (list) and
 * [FdkPagingGridScopeBuilder] (grid).
 *
 * Every slot here is optional: [Loading], [EmptyError], [RefreshError], [AppendLoading] and [AppendError]
 * fall back to the current [LocalPagingDefaults] — the same instance for both layouts — and [Empty] renders
 * nothing. Precedence for one call site: this DSL slot > [LocalPagingDefaults] >
 * [Material3PagingDefaults].
 *
 * The slots receive [FdkPagingSlotScope] rather than a layout-specific item scope, which is what
 * lets a list and a grid share one [PagingDefaults] implementation.
 */
interface FdkPagingSlotsBuilder {
    /**
     * Initial loading content, filling the viewport where one is known; defaults to a centered
     * progress indicator.
     *
     * A slot in a grid *section* ([pagingItems] without a `slotViewport`) has no viewport to fill
     * and wraps its content instead — which is what a section wants.
     */
    fun Loading(content: @Composable FdkPagingSlotScope.() -> Unit)

    /**
     * Refresh error content for the **empty** layout, filling the viewport where one is known (see
     * [Loading]); defaults to a message with a retry button.
     *
     * This and [RefreshError] are driven by the same failed refresh; the names say when each one
     * renders, not what failed. Shown only while there is nothing else to show — a refresh that
     * fails over loaded items leaves them on screen and goes to [RefreshError] instead.
     *
     * [e] is the failure the refresh ended on, live for the current load state. Word it through
     * [ErrorEffectsDefaults.errorMessage][grmv.android.fdk.screen.error.ErrorEffectsDefaults] — the
     * mapper behind the error dialogs and snackbars — so that one failure reads the same wherever
     * the app surfaces it.
     */
    fun EmptyError(content: @Composable FdkPagingSlotScope.(e: Throwable, retry: () -> Unit) -> Unit)

    /**
     * Refresh error content shown **above loaded items**; defaults to the append error's
     * presentation (see [PagingDefaults.RefreshError]).
     *
     * Emitted first, after whatever `Header` put above it, for as long as the refresh stays failed
     * and the layout has items. A failed pull-to-refresh over a loaded list would otherwise be
     * silent: the indicator retracts and nothing else happens. While it is up it is the only error
     * surface — [AppendError] stands down, since one
     * [retry][androidx.paging.compose.LazyPagingItems.retry] restarts both loads.
     *
     * [e] is the failure the refresh ended on, worded like [EmptyError]'s.
     */
    fun RefreshError(content: @Composable FdkPagingSlotScope.(e: Throwable, retry: () -> Unit) -> Unit)

    /** Content shown when the content is empty; defaults to empty. */
    fun Empty(content: @Composable FdkPagingSlotScope.() -> Unit)

    /**
     * Previous-page loading header; defaults to the append loader (see
     * [PagingDefaults.PrependLoading]).
     *
     * Emitted above the loaded items, and only by a source that pages backwards. Unrelated to the
     * `Header` slot, which is the call site's own static content.
     */
    fun PrependLoading(content: @Composable FdkPagingSlotScope.() -> Unit)

    /**
     * Previous-page error header; defaults to the append error's presentation. Suppressed while
     * [RefreshError] is up, for the reason given on [AppendError].
     *
     * [e] is the failure the prepend ended on, worded like [EmptyError]'s.
     */
    fun PrependError(content: @Composable FdkPagingSlotScope.(e: Throwable, retry: () -> Unit) -> Unit)

    /** Append (next page) loading footer; defaults to a small progress indicator. */
    fun AppendLoading(content: @Composable FdkPagingSlotScope.() -> Unit)

    /**
     * Append (next page) error footer; defaults to a message with a retry button.
     *
     * Suppressed while [RefreshError] is up: retrying restarts every failed load state at once, so
     * one failed page and one failed refresh would otherwise be reported twice and each retry would
     * silently fix the other.
     *
     * [e] is the failure the append ended on, worded like [EmptyError]'s.
     */
    fun AppendError(content: @Composable FdkPagingSlotScope.(e: Throwable, retry: () -> Unit) -> Unit)

    /**
     * Overrides the load-state slot animation for this call site.
     *
     * Return `null` to emit slots without animating — this is how a call site opts out of an
     * app-wide [LocalContentTransitions] provider. When this slot is left unset, the current
     * [LocalContentTransitions] applies, which itself defaults to
     * [grmv.android.fdk.screen.content.FdkNoContentTransitions] (no animation).
     *
     * [spec] runs in composition, so it may read `MaterialTheme` and other composition locals. It
     * runs inside each emitted slot, not once per call site, so its result is not expected to change
     * at runtime and anything it `remember`s lives and dies with that one slot.
     */
    fun transition(spec: @Composable () -> FdkItemTransitions?)
}

/** The resolved load-state slots of one call site, layout-independent. */
internal class PagingSlots(
    val loading: @Composable FdkPagingSlotScope.() -> Unit,
    val emptyError: @Composable FdkPagingSlotScope.(e: Throwable, retry: () -> Unit) -> Unit,
    val refreshError: @Composable FdkPagingSlotScope.(e: Throwable, retry: () -> Unit) -> Unit,
    /** `null` when the call site left [FdkPagingSlotsBuilder.Empty] unset. */
    val empty: (@Composable FdkPagingSlotScope.() -> Unit)?,
    val prependLoading: @Composable FdkPagingSlotScope.() -> Unit,
    val prependError: @Composable FdkPagingSlotScope.(e: Throwable, retry: () -> Unit) -> Unit,
    val appendLoading: @Composable FdkPagingSlotScope.() -> Unit,
    val appendError: @Composable FdkPagingSlotScope.(e: Throwable, retry: () -> Unit) -> Unit,
    // Set-but-null means "opt out"; see the matching note in RemoteDataContent.
    val transition: (@Composable () -> FdkItemTransitions?)?,
)

/** Collects the [FdkPagingSlotsBuilder] half of a DSL call into a [PagingSlots]. */
internal abstract class PagingSlotsBuilderImpl : FdkPagingSlotsBuilder {
    private var loading: @Composable FdkPagingSlotScope.() -> Unit = {
        with(LocalPagingDefaults.current) { Loading() }
    }
    private var emptyError: @Composable FdkPagingSlotScope.(e: Throwable, retry: () -> Unit) -> Unit =
        { e, retry -> with(LocalPagingDefaults.current) { EmptyError(e, retry) } }
    private var refreshError: @Composable FdkPagingSlotScope.(e: Throwable, retry: () -> Unit) -> Unit =
        { e, retry -> with(LocalPagingDefaults.current) { RefreshError(e, retry) } }
    private var empty: (@Composable FdkPagingSlotScope.() -> Unit)? = null
    private var prependLoading: @Composable FdkPagingSlotScope.() -> Unit = {
        with(LocalPagingDefaults.current) { PrependLoading() }
    }
    private var prependError: @Composable FdkPagingSlotScope.(e: Throwable, retry: () -> Unit) -> Unit =
        { e, retry -> with(LocalPagingDefaults.current) { PrependError(e, retry) } }
    private var appendLoading: @Composable FdkPagingSlotScope.() -> Unit = {
        with(LocalPagingDefaults.current) { AppendLoading() }
    }
    private var appendError: @Composable FdkPagingSlotScope.(e: Throwable, retry: () -> Unit) -> Unit =
        { e, retry -> with(LocalPagingDefaults.current) { AppendError(e, retry) } }
    private var transition: (@Composable () -> FdkItemTransitions?)? = null

    final override fun Loading(content: @Composable FdkPagingSlotScope.() -> Unit) { loading = content }
    final override fun EmptyError(content: @Composable FdkPagingSlotScope.(e: Throwable, retry: () -> Unit) -> Unit) { emptyError = content }
    final override fun RefreshError(content: @Composable FdkPagingSlotScope.(e: Throwable, retry: () -> Unit) -> Unit) { refreshError = content }
    final override fun Empty(content: @Composable FdkPagingSlotScope.() -> Unit) { empty = content }
    final override fun PrependLoading(content: @Composable FdkPagingSlotScope.() -> Unit) { prependLoading = content }
    final override fun PrependError(content: @Composable FdkPagingSlotScope.(e: Throwable, retry: () -> Unit) -> Unit) { prependError = content }
    final override fun AppendLoading(content: @Composable FdkPagingSlotScope.() -> Unit) { appendLoading = content }
    final override fun AppendError(content: @Composable FdkPagingSlotScope.(e: Throwable, retry: () -> Unit) -> Unit) { appendError = content }
    final override fun transition(spec: @Composable () -> FdkItemTransitions?) { transition = spec }

    protected fun buildSlots() =
        PagingSlots(
            loading,
            emptyError,
            refreshError,
            empty,
            prependLoading,
            prependError,
            appendLoading,
            appendError,
            transition,
        )
}

/**
 * Resolves the slot animation: the call site's own [FdkPagingSlotsBuilder.transition] if it set one,
 * otherwise the app-wide [LocalContentTransitions].
 *
 * Read in composition, inside the emitted item, so the `LazyGridScope`/`LazyListScope` extensions
 * can resolve it without being composable themselves.
 */
@Composable
internal fun resolveSlotTransitions(slots: PagingSlots): FdkItemTransitions? {
    val perCall = slots.transition
    return if (perCall != null) perCall() else LocalContentTransitions.current.itemTransitions()
}

/**
 * Lazy item key of the [FdkPagingSlotsBuilder.RefreshError] banner; [KeepRefreshErrorInView] looks
 * it up among the visible items.
 */
internal const val REFRESH_ERROR_SLOT_KEY = "paging_slot_refresh_error"

/**
 * Emits the load-state slots around [loadedItems] in the order a paged layout needs them, through
 * the layout-specific [slot] emitter.
 *
 * The one copy of the refresh/append/empty branching: a list and a grid cannot decide differently
 * what "empty" or "the append page failed" means, because neither owns this. [slot] is the only
 * layout-specific part — a `LazyListScope.item`, or a full-span `LazyGridScope.item`.
 *
 * The full-viewport slots are the empty-state ones: loaded items are never replaced by a loader or
 * an error (see [expand]), and a refresh that fails over them is reported by the
 * [FdkPagingSlotsBuilder.RefreshError] banner emitted ahead of them. Being ahead of the rows, it
 * would land above the viewport of a layout resting at its top; [KeepRefreshErrorInView] is what
 * brings it into view there.
 *
 * @param isRefreshing suppresses the full-viewport loader while a pull-to-refresh is in flight, so
 *   it does not flash on top of the pull indicator.
 * @param slot emits one keyed load-state item; `fadeOut` is `false` for the full-viewport slots
 *   (see [PagingSlot]). The error slots read their throwable from [items] inside that item's
 *   composition, so the slot words the current failure rather than the one that emitted it.
 * @param loadedItems emits the loaded items themselves; runs only in the `ready` state.
 */
internal fun <T : Any> emitPagingSlots(
    items: LazyPagingItems<T>,
    slots: PagingSlots,
    isRefreshing: Boolean,
    slot: (key: String, fadeOut: Boolean, content: @Composable FdkPagingSlotScope.() -> Unit) -> Unit,
    loadedItems: () -> Unit,
) {
    items.ifEmpty {
        // Nothing is emitted for an unset Empty slot: a zero-height item is still a line, and the
        // layout's arrangement would space it away from whatever a Header put above it.
        slots.empty?.let { emptySlot ->
            slot("paging_slot_empty", false) { emptySlot(this) }
        }
    }
    items.expand(
        isRefreshing = isRefreshing,
        ready = {
            // One retry restarts every failed load state, so while the refresh banner is up it is
            // the only error surface: two more would report one outcome and each would silently fix
            // the others'.
            val refreshFailed = items.refreshErrorOrNull() != null
            items.ifRefreshErrorBanner {
                // Inserted above the first loaded row, which a lazy layout keeps pinned by key; see
                // KeepRefreshErrorInView for how the banner is brought into view at the top.
                val emitted = items.refreshErrorOrNull()
                slot(REFRESH_ERROR_SLOT_KEY, true) {
                    val e = items.refreshErrorOrNull() ?: emitted
                    if (e != null) slots.refreshError(this, e) { items.retry() }
                }
            }
            items.ifPrependPageLoading {
                slot("paging_slot_prepend_loading", true) { slots.prependLoading(this) }
            }
            items.ifPrependPageError(unless = refreshFailed) {
                val emitted = items.prependErrorOrNull()
                slot("paging_slot_prepend_error", true) {
                    val e = items.prependErrorOrNull() ?: emitted
                    if (e != null) slots.prependError(this, e) { items.retry() }
                }
            }
            loadedItems()
            items.ifAppendPageLoading {
                slot("paging_slot_append_loading", true) { slots.appendLoading(this) }
            }
            items.ifAppendPageError(unless = refreshFailed) {
                // Total here — we are inside the append-Error branch — but only as of this emission;
                // the slot re-reads the live state when it composes and falls back to this.
                val emitted = items.appendErrorOrNull()
                slot("paging_slot_append_error", true) {
                    val e = items.appendErrorOrNull() ?: emitted
                    if (e != null) slots.appendError(this, e) { items.retry() }
                }
            }
        },
        loading = {
            slot("paging_slot_loading", false) { slots.loading(this) }
        },
        error = {
            val emitted = items.refreshErrorOrNull()
            slot("paging_slot_empty_error", false) {
                val e = items.refreshErrorOrNull() ?: emitted
                if (e != null) slots.emptyError(this, e) { items.retry() }
            }
        },
    )
}

/**
 * Renders one load-state [content] slot in [scope], animating its appearance, removal and placement
 * when the resolved transitions are non-null.
 *
 * With no transitions the slot is emitted exactly as written, without the wrapping [Box] — the
 * animated and non-animated paths stay layout-identical to what the call site would produce on its
 * own, apart from the wrapper the animation itself requires.
 *
 * @param scope the layout's slot receiver, which also carries the item animation modifier.
 * @param slots the call site's resolved slots, consulted here only for its transition override.
 * @param fadeOut `false` for the full-viewport slots: a departing full-screen loader would fade out
 *   on top of the content that just replaced it. They are removed at once and only their arrival is
 *   animated.
 * @param content the slot to render.
 */
@Composable
internal fun PagingSlot(
    scope: FdkPagingSlotScope,
    slots: PagingSlots,
    fadeOut: Boolean,
    content: @Composable FdkPagingSlotScope.() -> Unit,
) {
    val transitions = resolveSlotTransitions(slots)
    // Lazy items animate through Modifier.animateItem, which exposes no AnimatedVisibilityScope,
    // so the slot never inherits an animated ancestor's scope either.
    CompositionLocalProvider(LocalContentTransitionScope provides null) {
        if (transitions == null) {
            scope.content()
        } else {
            Box(
                modifier = with(scope) {
                    Modifier.animateItem(
                        fadeInSpec = transitions.fadeIn,
                        placementSpec = transitions.placement,
                        fadeOutSpec = transitions.fadeOut.takeIf { fadeOut },
                    )
                },
            ) {
                scope.content()
            }
        }
    }
}
