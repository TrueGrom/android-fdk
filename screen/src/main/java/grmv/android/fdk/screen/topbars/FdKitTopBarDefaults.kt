package grmv.android.fdk.screen.topbars

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

/**
 * App-wide default theming for the `FdKit*TopBar` presets.
 *
 * Provide a custom instance once high in the composition via [ProvideTopBarDefaults] to skin every
 * FdKit top bar with your design system's colors, insets, expanded height, and back-navigation icon.
 * Each call site still overrides any slot through its own parameter; an unset parameter falls back
 * to the current [TopBarDefaults].
 *
 * Precedence: per-call parameter > [LocalTopBarDefaults] > [Material3TopBarDefaults].
 *
 * Note: this type is `@Immutable` — implementations MUST be truly immutable, or recomposition may be skipped.
 */
@Immutable
@OptIn(ExperimentalMaterial3Api::class)
interface TopBarDefaults {
    /** Colors applied to every preset; mirrors [TopAppBarDefaults.topAppBarColors]. */
    @Composable
    fun colors(): TopAppBarColors

    /** Window insets applied to every preset. */
    val windowInsets: WindowInsets
        @Composable get

    /** Expanded height for the single-row presets ([FdKitTopBarTextTitle], [FdKitFeatureTopBar]). */
    val expandedHeight: Dp

    /** Leading back-navigation icon; renders nothing when [onNavigateBack] is null. */
    @Composable
    fun NavigationIcon(onNavigateBack: (() -> Unit)?)

    /** Leading icon for [FdKitFeatureTopBar] (profile, menu, or branded icon); renders nothing by default. */
    @Composable
    fun FeatureNavigationIcon()
}

/** Material3 fallback used until a consumer provides its own [TopBarDefaults]. */
@OptIn(ExperimentalMaterial3Api::class)
internal object Material3TopBarDefaults : TopBarDefaults {
    @Composable
    override fun colors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors()

    override val windowInsets: WindowInsets
        @Composable get() = TopAppBarDefaults.windowInsets

    override val expandedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight

    @Composable
    override fun NavigationIcon(onNavigateBack: (() -> Unit)?) {
        BackNavigationIcon(onNavigateBack)
    }

    @Composable
    override fun FeatureNavigationIcon() {}
}

/** App-wide [TopBarDefaults] for the `FdKit*TopBar` presets; defaults to [Material3TopBarDefaults]. */
val LocalTopBarDefaults = staticCompositionLocalOf<TopBarDefaults> { Material3TopBarDefaults }

/** Sets the app-wide top bar [defaults] for [content]. */
@Composable
fun ProvideTopBarDefaults(defaults: TopBarDefaults, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTopBarDefaults provides defaults, content = content)
}
