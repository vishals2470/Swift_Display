package com.swiftcockpit.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlin.math.min

/** The layout is designed on a 1280×720 dp canvas (a 1080p screen at "smallest width 720"). */
const val REFERENCE_WIDTH_DP = 1280f
const val REFERENCE_HEIGHT_DP = 720f

/**
 * Scales every dp and sp in [content] uniformly so the dashboard looks the same on any screen:
 * a smaller or lower-density display shrinks it, a larger one grows it. The layout still fills
 * the whole screen; if the aspect ratio differs from 16:9 the extra room goes to flexible panels.
 *
 * The system font-size setting is deliberately ignored: this is a fixed, glanceable cockpit
 * layout whose type sizes are already chosen for reading at arm's length.
 */
@Composable
fun ScaledToReference(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = min(maxWidth.value / REFERENCE_WIDTH_DP, maxHeight.value / REFERENCE_HEIGHT_DP)
        val base = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(base.density * scale, fontScale = 1f)) {
            content()
        }
    }
}
