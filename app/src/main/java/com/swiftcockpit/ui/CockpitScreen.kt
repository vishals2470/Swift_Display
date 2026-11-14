package com.swiftcockpit.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swiftcockpit.location.LocationPermission
import com.swiftcockpit.location.SpeedState
import com.swiftcockpit.system.ExternalApps
import com.swiftcockpit.ui.theme.CockpitColors.Ink
import com.swiftcockpit.ui.theme.CockpitColors.Line
import com.swiftcockpit.ui.theme.CockpitColors.Muted
import com.swiftcockpit.ui.theme.CockpitColors.Orange
import com.swiftcockpit.ui.theme.CockpitColors.OrangeSoft
import com.swiftcockpit.ui.theme.CockpitColors.Panel
import com.swiftcockpit.ui.theme.CockpitColors.Warning
import com.swiftcockpit.ui.theme.CockpitColors.White
import kotlinx.coroutines.delay
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val GAUGE_MAX_KMH = 220f

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

@Composable
fun CockpitScreen(viewModel: CockpitViewModel = viewModel()) {
    val context = LocalContext.current
    val speed by viewModel.speed.collectAsStateWithLifecycle()
    val permission by viewModel.locationPermission.collectAsStateWithLifecycle()

    // Permission can change in system settings while we are in the background.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermission()
        onPauseOrDispose { }
    }

    var userInitiatedRequest by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        viewModel.refreshPermission()
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

    // Ask once on first launch instead of hiding the request behind a small button.
    var autoRequested by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(permission) {
        if (permission == LocationPermission.NONE && !autoRequested) {
            autoRequested = true
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    val onGpsAction: () -> Unit = {
        when (speed) {
            SpeedState.PermissionRequired, SpeedState.PreciseLocationRequired -> {
                userInitiatedRequest = true
                permissionLauncher.launch(LOCATION_PERMISSIONS)
            }
            else -> ExternalApps.openLocationSettings(context)
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Ink, surface = Panel, primary = Orange,
            onBackground = White, onSurface = White
        )
    ) {
        Box(Modifier.fillMaxSize()) {
            CockpitBackground()
            Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 18.dp)) {
                Header(speed)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SpeedPanel(speed, Modifier.weight(0.95f).fillMaxHeight())
                    NavigationPanel(
                        modifier = Modifier.weight(1.55f).fillMaxHeight(),
                        onMaps = { ExternalApps.openMaps(context) }
                    )
                    RightRail(
                        modifier = Modifier.weight(0.82f).fillMaxHeight(),
                        onMusic = { ExternalApps.openMusic(context) },
                        onApps = { ExternalApps.openHome(context) },
                        onSettings = { ExternalApps.openSystemSettings(context) }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Footer(speed, onGpsAction)
            }
        }
    }
}

/** Short status and the footer call-to-action for each speed state. */
private data class GpsStatus(val pill: String, val icon: ImageVector, val message: String, val action: String?)

private fun SpeedState.status(): GpsStatus = when (this) {
    is SpeedState.Live -> GpsStatus("GPS LOCK", Icons.Filled.GpsFixed, "GPS DATA RECEIVING", null)
    SpeedState.Acquiring -> GpsStatus("GPS SEARCH", Icons.Filled.GpsNotFixed, "WAITING FOR GPS SIGNAL", "GPS SETTINGS")
    SpeedState.GpsDisabled -> GpsStatus("GPS OFF", Icons.Filled.GpsOff, "DEVICE LOCATION IS OFF", "TURN ON GPS")
    SpeedState.PermissionRequired -> GpsStatus("NO ACCESS", Icons.Filled.LocationDisabled, "LOCATION PERMISSION REQUIRED", "ALLOW LOCATION")
    SpeedState.PreciseLocationRequired -> GpsStatus("APPROX ONLY", Icons.Filled.LocationDisabled, "PRECISE LOCATION REQUIRED FOR SPEED", "ALLOW PRECISE")
}

@Composable
private fun CockpitBackground() {
    Canvas(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFF10151B), Ink, Color(0xFF090B0E)))
        )
    ) {
        val step = 34.dp.toPx()
        for (x in 0..(size.width / step).toInt()) {
            for (y in 0..(size.height / step).toInt()) {
                drawCircle(Color.White.copy(alpha = 0.018f), radius = 1.1f, center = Offset(x * step, y * step))
            }
        }
        drawRect(
            Brush.horizontalGradient(listOf(Color.Transparent, Orange.copy(alpha = .07f), Color.Transparent)),
            topLeft = Offset(0f, size.height * .82f),
            size = Size(size.width, size.height * .18f)
        )
    }
}

@Composable
private fun Header(speed: SpeedState) {
    val status = speed.status()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(4.dp).height(24.dp).clip(CircleShape).background(Orange))
                Spacer(Modifier.width(10.dp))
                Text("SWIFT", color = White, fontSize = 21.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                Text(" / COCKPIT", color = Muted, fontSize = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
            }
            Text("DRIVER INTERFACE  •  V0.2", color = Muted, fontSize = 9.sp, letterSpacing = 2.sp, modifier = Modifier.padding(start = 14.dp, top = 3.dp))
        }
        Spacer(Modifier.weight(1f))
        StatusPill(status.icon, status.pill, highlighted = speed is SpeedState.Live)
        Spacer(Modifier.width(12.dp))
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
    val time = remember(now.time / 60_000L, is24h) {
        DateFormat.format(if (is24h) "HH:mm" else "h:mm", now).toString()
    }
    val date = remember(now.time / 60_000L) {
        DateFormat.format("EEE, dd MMM", now).toString().uppercase(Locale.getDefault())
    }
    Column(horizontalAlignment = Alignment.End) {
        Text(time, color = White, fontSize = 24.sp, fontWeight = FontWeight.Light, letterSpacing = 1.sp)
        Text(date, color = Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
    }
}

@Composable
private fun SpeedPanel(speed: SpeedState, modifier: Modifier = Modifier) {
    val live = speed as? SpeedState.Live
    CockpitPanel(modifier) {
        Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("LIVE SPEED", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(6.dp).clip(CircleShape).background(if (live != null) Orange else Muted))
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val animated by animateFloatAsState(
                    live?.kmh ?: 0f, tween(450, easing = FastOutSlowInEasing), label = "speed"
                )
                SpeedGauge(fraction = if (live != null) animated / GAUGE_MAX_KMH else 0f, modifier = Modifier.fillMaxSize())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Never show a number that is not a fresh GPS reading.
                    Text(
                        if (live != null) animated.toInt().toString() else "--",
                        color = if (live != null) White else Muted,
                        fontSize = 62.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp
                    )
                    Text("km/h", color = OrangeSoft, fontSize = 12.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("SOURCE", color = Muted, fontSize = 8.sp, letterSpacing = 1.5.sp)
                    Text("GPS", color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("ACCURACY", color = Muted, fontSize = 8.sp, letterSpacing = 1.5.sp)
                    Text(
                        when {
                            live == null -> "NO FIX"
                            live.accuracyKmh == null -> "ACTIVE"
                            else -> "±%.0f km/h".format(Locale.US, live.accuracyKmh)
                        },
                        color = if (live != null) Orange else Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedGauge(fraction: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val side = min(size.width, size.height)
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = side * .40f
        val stroke = side * .025f
        val start = 135f
        val sweep = 270f
        val arcTopLeft = Offset(cx - radius, cy - radius)
        val arcSize = Size(radius * 2, radius * 2)
        drawArc(Line, start, sweep, false, arcTopLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        val clamped = fraction.coerceIn(0f, 1f)
        if (clamped > 0f) drawArc(
            Brush.sweepGradient(listOf(Orange.copy(alpha = .35f), OrangeSoft, Orange, Orange.copy(alpha = .35f)), Offset(cx, cy)),
            start, sweep * clamped, false, arcTopLeft, arcSize,
            style = Stroke(stroke * 1.2f, cap = StrokeCap.Round)
        )
        for (i in 0..40) {
            val angle = Math.toRadians(start + sweep * i / 40.0)
            val major = i % 5 == 0
            val inner = radius - if (major) stroke * 2.2f else stroke * 1.1f
            val outer = radius + stroke * .55f
            val c = cos(angle).toFloat()
            val s = sin(angle).toFloat()
            drawLine(
                if (major) White.copy(alpha = .55f) else Muted.copy(alpha = .35f),
                Offset(cx + c * inner, cy + s * inner), Offset(cx + c * outer, cy + s * outer),
                if (major) 2.dp.toPx() else 1.dp.toPx()
            )
        }
    }
}

@Composable
private fun NavigationPanel(modifier: Modifier, onMaps: () -> Unit) {
    CockpitPanel(modifier) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("NAVIGATION", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.NearMe, null, tint = Orange, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1A2730), Color(0xFF10171D), Color(0xFF172129))))
                    .border(1.dp, Line, RoundedCornerShape(14.dp))
                    .clickable(onClick = onMaps),
                contentAlignment = Alignment.Center
            ) {
                MapArtwork()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Map, null, tint = White.copy(alpha = .9f), modifier = Modifier.size(30.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("TAP TO OPEN MAPS", color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text("Live navigation via installed map app", color = Muted, fontSize = 9.sp)
                }
                Text("MAP PREVIEW", color = Muted.copy(alpha = .65f), fontSize = 8.sp, letterSpacing = 1.5.sp, modifier = Modifier.align(Alignment.TopStart).padding(12.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("DESTINATION", color = Muted, fontSize = 8.sp, letterSpacing = 1.5.sp)
                    Text("Set in your map app", color = White, fontSize = 12.sp)
                }
                Button(
                    onClick = onMaps,
                    colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Ink),
                    shape = RoundedCornerShape(9.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Icon(Icons.Filled.Navigation, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("NAVIGATE", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}

/** Placeholder road artwork; replaced by the live embedded map in phase 5. */
@Composable
private fun MapArtwork() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val roads = listOf(
            listOf(Offset(w * .05f, h * .72f), Offset(w * .28f, h * .55f), Offset(w * .48f, h * .62f), Offset(w * .70f, h * .30f), Offset(w * .96f, h * .20f)),
            listOf(Offset(w * .12f, h * .05f), Offset(w * .32f, h * .30f), Offset(w * .48f, h * .62f), Offset(w * .55f, h * .95f)),
            listOf(Offset(w * .72f, h * .02f), Offset(w * .68f, h * .30f), Offset(w * .82f, h * .55f), Offset(w * .95f, h * .83f)),
            listOf(Offset(w * .02f, h * .35f), Offset(w * .30f, h * .34f), Offset(w * .58f, h * .18f), Offset(w * .92f, h * .48f))
        )
        roads.forEachIndexed { index, points ->
            for (i in 0 until points.lastIndex) drawLine(
                if (index == 0) Orange.copy(alpha = .85f) else Color(0xFF46535E).copy(alpha = .55f),
                points[i], points[i + 1], if (index == 0) 3.dp.toPx() else 1.4.dp.toPx(), cap = StrokeCap.Round
            )
        }
        drawCircle(Orange.copy(alpha = .18f), 17.dp.toPx(), Offset(w * .48f, h * .62f))
        drawCircle(Orange, 5.dp.toPx(), Offset(w * .48f, h * .62f))
    }
}

@Composable
private fun RightRail(modifier: Modifier, onMusic: () -> Unit, onApps: () -> Unit, onSettings: () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CockpitPanel(Modifier.weight(1f).fillMaxWidth().clickable(onClick = onMusic)) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MusicNote, null, tint = Orange, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("MEDIA", color = Muted, fontSize = 10.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Your music", color = White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("Open media player", color = Muted, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PlayCircleFilled, null, tint = Orange, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("OPEN PLAYER", color = OrangeSoft, fontSize = 9.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        CockpitPanel(Modifier.weight(.82f).fillMaxWidth().clickable(onClick = onApps)) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Icon(Icons.Filled.GridView, null, tint = Orange, modifier = Modifier.size(20.dp))
                Column {
                    Text("ALL APPS", color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Open the home screen", color = Muted, fontSize = 10.sp)
                }
            }
        }
        CockpitPanel(Modifier.height(58.dp).fillMaxWidth().clickable(onClick = onSettings)) {
            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Settings, null, tint = Muted, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(10.dp))
                Text("SYSTEM SETTINGS", color = White, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, null, tint = Orange)
            }
        }
    }
}

@Composable
private fun Footer(speed: SpeedState, onGpsAction: () -> Unit) {
    val status = speed.status()
    val problem = speed !is SpeedState.Live && speed != SpeedState.Acquiring
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(Orange))
            Spacer(Modifier.width(8.dp))
            Text("SWIFT VXI", color = White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Text("   /   DRIVER COCKPIT", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
        }
        Spacer(Modifier.weight(1f))
        Text(status.message, color = if (problem) Warning else Muted, fontSize = 9.sp, letterSpacing = 1.1.sp)
        if (status.action != null) {
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = onGpsAction, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
                Text(status.action, color = OrangeSoft, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun StatusPill(icon: ImageVector, label: String, highlighted: Boolean) {
    Row(
        Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF1B232B))
            .border(1.dp, Line, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (highlighted) Orange else Muted, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = White, fontSize = 8.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CockpitPanel(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.shadow(12.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1A222B), Panel, Color(0xFF0E1318))))
            .border(1.dp, Brush.linearGradient(listOf(Color(0xFF3A4652), Color(0xFF202832), Color(0xFF303B45))), RoundedCornerShape(18.dp)),
        content = content
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
