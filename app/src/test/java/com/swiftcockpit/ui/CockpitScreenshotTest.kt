package com.swiftcockpit.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swiftcockpit.location.SpeedState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
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
    val compose = createAndroidComposeRule<ComponentActivity>()

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
        shadowOf(Looper.getMainLooper()).idle()

        // Draw the window directly; Compose's captureToImage() waits for a real frame and hangs
        // under Robolectric.
        val view = compose.activity.window.decorView
        val metrics = compose.activity.resources.displayMetrics
        if (view.width == 0 || view.height == 0) {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
        }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File(System.getProperty("screenshots.dir") ?: "build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        // Small preview copy for quick review in CI logs.
        val preview = Bitmap.createScaledBitmap(bitmap, bitmap.width / 2, bitmap.height / 2, true)
        File(dir, "$name.jpg").outputStream().use { preview.compress(Bitmap.CompressFormat.JPEG, 65, it) }
    }
}
