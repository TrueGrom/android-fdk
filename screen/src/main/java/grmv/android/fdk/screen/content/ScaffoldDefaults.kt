package grmv.android.fdk.screen.content

import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/** Default FAB placement for `FdKitBaseScaffold`. */
val ScaffoldDefaults.fabPosition: FabPosition get() = FabPosition.End

/** Default container color for `FdKitBaseScaffold`. */
@Composable
@ReadOnlyComposable
fun ScaffoldDefaults.containerColor(): Color = MaterialTheme.colorScheme.background
