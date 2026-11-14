package com.swiftcockpit.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swiftcockpit.location.LocationPermission
import com.swiftcockpit.location.SpeedState
import com.swiftcockpit.media.NowPlaying
import com.swiftcockpit.system.ExternalApps
import com.swiftcockpit.ui.components.CockpitPanel
import com.swiftcockpit.ui.components.CockpitType
import com.swiftcockpit.ui.components.MediaPanel
import com.swiftcockpit.ui.components.NavigationPanel
import com.swiftcockpit.ui.components.SpeedPanel
import com.swiftcockpit.ui.components.StatusPill
import com.swiftcockpit.ui.components.gpsStatus
import com.swiftcockpit.ui.theme.CockpitColors
import com.swiftcockpit.ui.theme.ScaledToReference
import kotlinx.coroutines.delay
import java.util.Date
import java.util.Locale

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/** Everything the dashboard can ask the outside world to do. */
@Immutable
data class CockpitActions(
    val onGpsAction: () -> Unit = {},
    val onMaps: () -> Unit = {},
    val onOpenPlayer: () -> Unit = {},
    val onGrantMediaAccess: () -> Unit = {},
    val onPlayPause: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onPrevious: () -> Unit = {},
    val onApps: () -> Unit = {},
    val onSettings: () -> Unit = {},
)

@Composable
fun CockpitScreen(viewModel: CockpitViewModel = viewModel()) {
    val context = LocalContext.current
    val speed by viewModel.speed.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val permission by viewModel.locationPermission.collectAsStateWithLifecycle()

    // Permissions can change in system settings while we are in the background.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissions()
        onPauseOrDispose { }
    }

    var userInitiatedRequest by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        viewModel.refreshPermissions()
        val precise = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val activity = context.findActivity()
        // After a denial, "no rationale" means the system will no longer show the dialog
        // ("don't ask again"), so the only way forward is the app's settings page.
        if (!precise && userInitiatedRequest && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        ) {
            ExternalApps.openAppPermissionSettings(context)
        }
        userInitiatedRequest = false
    }

    // Ask once on first launch instead of hiding the request behind a button.
    var autoRequested by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(permission) {
        if (permission == LocationPermission.NONE && !autoRequested) {
            autoRequested = true
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    val actions = remember(viewModel, context) {
        CockpitActions(
            onGpsAction = {
                when (viewModel.speed.value) {
                    SpeedState.PermissionRequired, SpeedState.PreciseLocationRequired -> {
                        userInitiatedRequest = true
                        permissionLauncher.launch(LOCATION_PERMISSIONS)
                    }
                    else -> ExternalApps.openLocationSettings(context)
                }
            },
            onMaps = { ExternalApps.openMaps(context) },
            onOpenPlayer = { ExternalApps.openMusic(context) },
            onGrantMediaAccess = { ExternalApps.openNotificationAccessSettings(context) },
            onPlayPause = viewModel::playPause,
            onNext = viewModel::next,
            onPrevious = viewModel::previous,
            onApps = { ExternalApps.openHome(context) },
            onSettings = { ExternalApps.openSystemSettings(context) },
        )
    }

    CockpitContent(speed = speed, nowPlaying = nowPlaying, actions = actions)
}

/**
 * Stateless dashboard laid out on a 1280×720 dp reference canvas and scaled to the real screen.
 * Everything shown comes from parameters (used by screenshot tests).
 */
@Composable
internal fun CockpitContent(speed: SpeedState, nowPlaying: NowPlaying, actions: CockpitActions) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = CockpitColors.Ink, surface = CockpitColors.Panel, primary = CockpitColors.Orange,
            onBackground = CockpitColors.White, onSurface = CockpitColors.White
        )
    ) {
        Box(Modifier.fillMaxSize()) {
            CockpitBackground()
            ScaledToReference {
                Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 18.dp)) {
                    Header(speed)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SpeedPanel(speed, actions.onGpsAction, Modifier.weight(1f).fillMaxHeight())
                        NavigationPanel(actions.onMaps, Modifier.weight(1.3f).fillMaxHeight())
                        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            MediaPanel(
                                nowPlaying = nowPlaying,
                                onOpenPlayer = actions.onOpenPlayer,
                                onGrantAccess = actions.onGrantMediaAccess,
                                onPlayPause = actions.onPlayPause,
                                onNext = actions.onNext,
                                onPrevious = actions.onPrevious,
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                ShortcutTile("APPS", Icons.Filled.Apps, actions.onApps, Modifier.weight(1f))
                                ShortcutTile("SETTINGS", Icons.Filled.Settings, actions.onSettings, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CockpitBackground() {
    Canvas(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFF10151B), CockpitColors.Ink, Color(0xFF090B0E)))
        )
    ) {
        val step = 34.dp.toPx()
        for (x in 0..(size.width / step).toInt()) {
            for (y in 0..(size.height / step).toInt()) {
                drawCircle(Color.White.copy(alpha = 0.02f), radius = 1.2f, center = Offset(x * step, y * step))
            }
        }
        drawRect(
            Brush.horizontalGradient(listOf(Color.Transparent, CockpitColors.Orange.copy(alpha = .07f), Color.Transparent)),
            topLeft = Offset(0f, size.height * .82f),
            size = Size(size.width, size.height * .18f)
        )
    }
}

@Composable
private fun Header(speed: SpeedState) {
    val status = speed.gpsStatus()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(5.dp).height(30.dp).clip(CircleShape).background(CockpitColors.Orange))
        Spacer(Modifier.width(12.dp))
        Text("SWIFT", color = CockpitColors.White, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
        Text(" / COCKPIT", color = CockpitColors.Muted, fontSize = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
        Spacer(Modifier.weight(1f))
        StatusPill(status.icon, status.pill, highlighted = speed is SpeedState.Live, warning = status.isProblem)
        Spacer(Modifier.width(24.dp))
        Clock()
    }
}

/** Owns its own ticking state so the rest of the screen is not recomposed every second. */
@Composable
private fun Clock() {
    val context = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1_000L - System.currentTimeMillis() % 1_000L)
        }
    }
    val is24h = DateFormat.is24HourFormat(context)
    val minute = now.time / 60_000L
    val time = remember(minute, is24h) { DateFormat.format(if (is24h) "HH:mm" else "h:mm", now).toString() }
    val date = remember(minute) { DateFormat.format("EEE, dd MMM", now).toString().uppercase(Locale.getDefault()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(date, color = CockpitColors.Muted, fontSize = 15.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(14.dp))
        Text(time, color = CockpitColors.White, fontSize = 38.sp, fontWeight = FontWeight.Light, style = TextStyle(fontFeatureSettings = "tnum"))
    }
}

@Composable
private fun ShortcutTile(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CockpitPanel(modifier.height(84.dp).clickable(onClick = onClick)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = CockpitColors.Orange, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, color = CockpitColors.White, fontSize = CockpitType.Body, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
