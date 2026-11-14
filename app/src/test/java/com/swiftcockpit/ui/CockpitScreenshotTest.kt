package com.swiftcockpit.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swiftcockpit.location.SpeedState
import com.swiftcockpit.media.NowPlaying
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the dashboard for visual review. Not a pass/fail comparison:
 * images are written to app/build/screenshots and published by CI.
 *
 * Default: the head unit (1080p, smallest width 720 dp). Extra cases check that the layout
 * scales to other screen sizes.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1280dp-h720dp-land-hdpi")
class CockpitScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val song = NowPlaying.Active(
        title = "Kesariya", artist = "Arijit Singh", sourceApp = "Spotify", art = null, isPlaying = true
    )

    @Test
    fun live() = snapshot("01_live_playing", SpeedState.Live(62f, 1.8f), song)

    @Test
    fun acquiring() = snapshot("02_gps_searching_media_access", SpeedState.Acquiring, NowPlaying.AccessRequired)

    @Test
    fun gpsOff() = snapshot("03_gps_off_idle", SpeedState.GpsDisabled, NowPlaying.Idle)

    @Test
    fun permissionRequired() = snapshot("04_permission_required", SpeedState.PermissionRequired, NowPlaying.Idle)

    @Test
    fun preciseRequired() = snapshot(
        "05_precise_location_paused", SpeedState.PreciseLocationRequired, song.copy(isPlaying = false)
    )

    /** A common 1024×600 head unit: the same layout, scaled down. */
    @Test
    @Config(qualifiers = "w1024dp-h600dp-land-mdpi")
    fun smallScreen() = snapshot("06_scaled_1024x600", SpeedState.Live(48f, 1.2f), song)

    /** A 16:10 screen: extra height goes to the flexible panels. */
    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-hdpi")
    fun tallerScreen() = snapshot("07_scaled_1920x1200", SpeedState.Live(48f, 1.2f), song)

    private fun snapshot(name: String, speed: SpeedState, media: NowPlaying) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CockpitContent(speed = speed, nowPlaying = media, actions = CockpitActions())
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
