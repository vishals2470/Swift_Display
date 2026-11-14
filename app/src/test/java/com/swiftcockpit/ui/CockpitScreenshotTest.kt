package com.swiftcockpit.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swiftcockpit.location.SpeedState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the dashboard at 1920×1080 px for visual review. Not a pass/fail comparison:
 * images are written to app/build/screenshots and published by CI.
 *
 * "hdpi" = 1080p at 240 dpi (a 1280×720 dp layout); "mdpi" = 1080p at 160 dpi (1920×1080 dp).
 * Which one matches the head unit depends on its configured display density.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1280dp-h720dp-land-hdpi")
class CockpitScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun live() = snapshot("01_live_62kmh_hdpi", SpeedState.Live(62f, 1.8f))

    @Test
    fun acquiring() = snapshot("02_gps_searching_hdpi", SpeedState.Acquiring)

    @Test
    fun gpsOff() = snapshot("03_gps_off_hdpi", SpeedState.GpsDisabled)

    @Test
    fun permissionRequired() = snapshot("04_permission_required_hdpi", SpeedState.PermissionRequired)

    @Test
    fun preciseRequired() = snapshot("05_precise_location_required_hdpi", SpeedState.PreciseLocationRequired)

    @Test
    @Config(qualifiers = "w1920dp-h1080dp-land-mdpi")
    fun liveMdpi() = snapshot("06_live_62kmh_mdpi", SpeedState.Live(62f, 1.8f))

    private fun snapshot(name: String, speed: SpeedState) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CockpitContent(speed = speed, onGpsAction = {}, onMaps = {}, onMusic = {}, onApps = {}, onSettings = {})
        }
        compose.mainClock.advanceTimeBy(1_000)
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(System.getProperty("screenshots.dir") ?: "build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        File(dir, "$name.jpg").outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
    }
}
