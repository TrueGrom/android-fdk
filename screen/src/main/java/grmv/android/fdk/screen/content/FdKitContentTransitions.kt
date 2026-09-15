package grmv.android.fdk.screen.content

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.IntOffset
import grmv.android.fdk.screen.FdkScreenDefaults
import grmv.android.fdk.state.RemoteData

/**
 * App-wide transition used when a content building block swaps between its state slots.
 *
 * Applies to `Fetchable` ([RemoteData] content), whose [RemoteData.Loading] / [RemoteData.Fetched] /
 * [RemoteData.Error] slots are otherwise exchanged in a single frame, and to paged content
 * (`PagingContent`, `PagingGridContent`, `pagingItems`), whose load-state slots otherwise appear and
 * disappear between two frames.
 *
 * Resolution order for a given call site:
 *
 * 1. **Per-call slot** — [RemoteDataScopeBuilder.transition] (for `Fetchable`) or
 *    [grmv.android.fdk.screen.paging.FdkPagingSlotsBuilder.transition] (for `PagingContent` and the
 *    paged grids) always wins for that one call site.
 * 2. **App-wide default** — otherwise the current [LocalContentTransitions] is used: [transform]
 *    for `Fetchable`, [itemTransitions] for the paged layouts.
 * 3. **Library fallback** — [LocalContentTransitions] itself defaults to [FdkNoContentTransitions],
 *    i.e. no animation at all.
 *
 * Precedence: per-call `transition` slot > [LocalContentTransitions] > [FdkNoContentTransitions].
 *
 * Animation is therefore **opt-in**: without a provider the slots swap in a single frame, and no
 * `AnimatedContent` is composed at all. To fade every state swap in the app, install a provider
 * once, high in the
 * composition via [ProvideContentTransitions] (or through [FdkScreenDefaults]), e.g. at the
 * app/theme root:
 *
 * ```
 * ProvideContentTransitions(FdkFadeContentTransitions) {
 *     AppContent()
 * }
 * ```
 *
 * A slot that wants to animate its own children — staggering them, or handing one to a shared
 * element — reads [LocalContentTransitionScope].
 *
 * While a transition runs, **both** slots stay composed: the outgoing one keeps its effects running
 * and keeps accepting input until the exit animation ends.
 *
 * That window is short but real — a couple of hundred milliseconds with the default fade — and it
 * is long enough to double-fire. The concrete case: the user taps Retry in an `Error` slot, the
 * state moves to `Loading`, and the still-fading `Error` slot accepts a second tap on the same
 * button, sending a second request. Any slot whose action is not idempotent must guard itself; the
 * transition will not disable input for you.
 *
 * Note: this type is `@Immutable` — implementations MUST be truly immutable, or recomposition may be skipped.
 *
 * @see FdkFadeContentTransitions
 */
@Immutable
interface FdkContentTransitions {
    /**
     * The transform applied when the rendered state slot changes, or `null` to swap slots without
     * animating.
     *
     * Called in composition, so implementations may read `MaterialTheme` and other composition
     * locals — unlike `AnimatedContent`'s own `transitionSpec`, which is not composable and
     * therefore cannot resolve theme-derived animation specs.
     */
    @Composable
    fun transform(): ContentTransform?

    /**
     * Specs for animating the appearance, disappearance and placement of a list's load-state slots,
     * or `null` to have them appear and disappear without animating.
     *
     * Used by `PagingContent`, `PagingGridContent` and the `pagingItems` extensions for their
     * loading / error / empty / append slots. The loaded items
     * themselves are never animated: a refresh replaces the whole list, and animating that turns
     * into a cascade of placement animations rather than a transition.
     *
     * Abstract like [transform] on purpose: an implementation that animated only one of the two
     * building blocks would leave the other silently un-animated.
     */
    @Composable
    fun itemTransitions(): FdkItemTransitions?
}

/**
 * Animation specs for a list slot, mapped onto `Modifier.animateItem`.
 *
 * A `null` spec disables that part of the animation, matching `animateItem`'s own contract. The
 * defaults are `animateItem`'s own: a [Spring.StiffnessMediumLow] spring for each.
 *
 * **What this does and does not smooth.** These specs reach the load-state slots only, never the
 * loaded items, so a paged list's first render is unchanged: the initial loader is present from the
 * list's first frame, which a lazy layout does not animate in, and it is then removed instantly
 * (see [fadeOut]) as the items appear un-animated. In practice the animation shows on the append
 * path — the next-page footer fading in and out, and the items above it settling into place — and
 * on the error and empty slots when they replace a loader.
 *
 * The loaded items are left alone deliberately: a refresh replaces every key at once, so animating
 * them turns a single swap into a cascade of per-item fades and placement animations, which reads
 * worse than the swap it replaces and fights the pull-to-refresh gesture. Cross-fading a paged list
 * as a whole is out of reach here: the refresh state lives inside `PagingContent`, so it can only be
 * wrapped in a `Fetchable` by a screen that already tracks its own [RemoteData] alongside the
 * paging flow.
 *
 * Note: this type is `@Immutable` — implementations MUST be truly immutable, or recomposition may be skipped.
 * That extends to the specs it carries: supply only immutable,
 * value-equatable [FiniteAnimationSpec] implementations. The Compose built-ins (`spring`, `tween`,
 * `keyframes`) all qualify; a custom spec that mutates after construction will silently miss
 * recompositions.
 *
 * @param fadeIn spec for a slot entering the list.
 * @param fadeOut spec for a slot leaving the layout. The paged layouts ignore this for their
 *   full-viewport slots (loading / error / empty), which cover the whole viewport and would
 *   otherwise fade out on top of the content that just replaced them; it applies only to the append
 *   slots.
 * @param placement spec for a slot moving because content above it changed size.
 */
@Immutable
data class FdkItemTransitions(
    val fadeIn: FiniteAnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
    val fadeOut: FiniteAnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
    val placement: FiniteAnimationSpec<IntOffset>? =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold,
        ),
)

/**
 * Swaps state slots in a single frame, with no transition. The library-wide default.
 *
 * Public, unlike the `Material3*Defaults` fallbacks of the neighbouring contracts, because turning
 * animation back off is a real call: pass it to [ProvideContentTransitions] or
 * [FdkScreenDefaults] to reset a subtree that an outer provider animates. A single call site opts
 * out with its own `transition { null }` instead.
 */
object FdkNoContentTransitions : FdkContentTransitions {
    @Composable
    override fun transform(): ContentTransform? = null

    @Composable
    override fun itemTransitions(): FdkItemTransitions? = null
}

/**
 * Cross-fades state slots with the Compose default fade specs.
 *
 * The container size is **not** animated ([ContentTransform.sizeTransform] is `null`): a
 * loader-sized box growing into a full page is a visible reflow, especially inside a scrollable
 * screen. While both slots are present the container is sized to their union and settles onto the
 * incoming slot once the outgoing one leaves, so a large slot fading out holds the container open
 * until the end of the fade. This is only visible where both slots wrap their content — when both
 * fill the available space, as inside `FdKitScrollableScreen` or with the default `fillMaxSize`
 * loading and error slots, the union is the viewport and the container never resizes at all.
 *
 * For lists it returns the default [FdkItemTransitions] — `Modifier.animateItem`'s own springs —
 * which the paged layouts apply to their load-state slots, dropping the fade-out on the
 * full-viewport ones (see [FdkItemTransitions.fadeOut]).
 *
 * To align the fade with an app's own motion system — e.g. `MaterialTheme.motionScheme` once
 * material3 makes it public; it is `internal` in 1.4.0 — implement [FdkContentTransitions] and
 * build the [ContentTransform] from your own specs:
 *
 * ```
 * object AppContentTransitions : FdkContentTransitions {
 *     @Composable override fun transform(): ContentTransform {
 *         val spec = AppMotion.emphasized<Float>()
 *         return ContentTransform(fadeIn(spec), fadeOut(spec), sizeTransform = null)
 *     }
 * }
 * ```
 */
object FdkFadeContentTransitions : FdkContentTransitions {
    // Neither value depends on the composition, so both are built once rather than per
    // recomposition. Note that ContentTransform itself is not immutable — targetContentZIndex is
    // snapshot state — so this shared instance must never be mutated by a caller.
    private val fadeTransform =
        ContentTransform(
            targetContentEnter = fadeIn(),
            initialContentExit = fadeOut(),
            sizeTransform = null,
        )
    private val fadeItemTransitions = FdkItemTransitions()

    @Composable
    override fun transform(): ContentTransform = fadeTransform

    @Composable
    override fun itemTransitions(): FdkItemTransitions = fadeItemTransitions
}

/** App-wide [FdkContentTransitions]; defaults to [FdkNoContentTransitions] (no animation). */
val LocalContentTransitions =
    staticCompositionLocalOf<FdkContentTransitions> { FdkNoContentTransitions }

/**
 * The [AnimatedVisibilityScope] of the state-slot transition running around the content being
 * composed, or `null` when this content is not animated.
 *
 * `Fetchable` publishes its scope here only on the animated path and shadows it back to `null` on
 * the un-animated one, and `PagingContent` always shadows it — so a slot never inherits the scope
 * of an animated ancestor by mistake, and `null` really does mean "this slot is not animating".
 *
 * A slot can use it to animate its own children rather than only fading as a whole. Shared elements
 * take it as a parameter, while `Modifier.animateEnterExit` is a member of [AnimatedVisibilityScope]
 * and needs it as a receiver — `with(transition) { Modifier.animateEnterExit(...) }`:
 *
 * ```
 * Fetched { item ->
 *     val transition = LocalContentTransitionScope.current
 *     with(sharedTransitionScope) {
 *         Thumbnail(
 *             item,
 *             modifier = transition?.let {
 *                 Modifier.sharedElement(rememberSharedContentState("thumbnail"), it)
 *             } ?: Modifier,
 *         )
 *     }
 * }
 * ```
 *
 * `null` is the normal case, not an error: with animation off there is no transition to attach to,
 * and the slot renders as it always did — always branch on it rather than asserting. Shared
 * elements additionally need a `SharedTransitionScope`, which comes from the consumer's own
 * `SharedTransitionLayout`, not from this library.
 *
 * `PagingContent` never publishes a scope: its slots animate through `Modifier.animateItem`, a
 * lazy-layout item animation with no [AnimatedVisibilityScope] behind it.
 */
val LocalContentTransitionScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** Sets the app-wide state-slot [transitions] for [content]. */
@Composable
fun ProvideContentTransitions(
    transitions: FdkContentTransitions,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalContentTransitions provides transitions, content = content)
}
