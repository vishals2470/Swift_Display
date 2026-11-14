package com.swiftcockpit.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swiftcockpit.location.SpeedState
import com.swiftcockpit.ui.theme.CockpitColors
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val GAUGE_MAX_KMH = 180f // Swift top speed is ~165 km/h; a tighter scale moves visibly in town.

@Composable
fun SpeedPanel(speed: SpeedState, onGpsAction: () -> Unit, modifier: Modifier = Modifier) {
    val live = speed as? SpeedState.Live
    val status = speed.gpsStatus()
    CockpitPanel(modifier) {
        Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PanelLabel("SPEED")
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(10.dp).clip(CircleShape).background(if (live != null) CockpitColors.Orange else CockpitColors.Muted))
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val animated by animateFloatAsState(
                    live?.kmh ?: 0f, tween(450, easing = FastOutSlowInEasing), label = "speed"
                )
                SpeedGauge(fraction = if (live != null) animated / GAUGE_MAX_KMH else 0f, modifier = Modifier.fillMaxSize())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Never show a number that is not a fresh GPS reading.
                    Text(
                        if (live != null) animated.toInt().toString() else "--",
                        color = if (live != null) CockpitColors.White else CockpitColors.Muted,
                        fontSize = 104.sp, fontWeight = FontWeight.Light, letterSpacing = (-3).sp,
                        fontFeatureSettings = "tnum" // fixed-width digits: no jitter as the number changes
                    )
                    Text("km/h", color = CockpitColors.OrangeSoft, fontSize = 18.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (live != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("GPS", color = CockpitColors.White, fontSize = CockpitType.Body, fontWeight = FontWeight.Bold)
                    Text(
                        live.accuracyKmh?.let { "±%.0f km/h".format(Locale.US, it) } ?: "LIVE",
                        color = CockpitColors.Orange, fontSize = CockpitType.Body, fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    status.message, color = if (status.isProblem) CockpitColors.Warning else CockpitColors.Muted,
                    fontSize = CockpitType.Body, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
                if (status.action != null) {
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton(status.action, onGpsAction, Modifier.fillMaxWidth())
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
        val radius = side * .44f
        val stroke = side * .03f
        val start = 135f
        val sweep = 270f
        val arcTopLeft = Offset(cx - radius, cy - radius)
        val arcSize = Size(radius * 2, radius * 2)
        drawArc(CockpitColors.Line, start, sweep, false, arcTopLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        val clamped = fraction.coerceIn(0f, 1f)
        if (clamped > 0f) drawArc(
            Brush.sweepGradient(
                listOf(CockpitColors.Orange.copy(alpha = .35f), CockpitColors.OrangeSoft, CockpitColors.Orange, CockpitColors.Orange.copy(alpha = .35f)),
                Offset(cx, cy)
            ),
            start, sweep * clamped, false, arcTopLeft, arcSize,
            style = Stroke(stroke * 1.2f, cap = StrokeCap.Round)
        )
        val ticks = 36 // one every 5 km/h; long tick every 20 km/h
        for (i in 0..ticks) {
            val angle = Math.toRadians(start + sweep * i / ticks.toDouble())
            val major = i % 4 == 0
            val inner = radius - if (major) stroke * 2.4f else stroke * 1.2f
            val outer = radius + stroke * .55f
            val c = cos(angle).toFloat()
            val s = sin(angle).toFloat()
            drawLine(
                if (major) CockpitColors.White.copy(alpha = .6f) else CockpitColors.Muted.copy(alpha = .35f),
                Offset(cx + c * inner, cy + s * inner), Offset(cx + c * outer, cy + s * outer),
                if (major) 2.5.dp.toPx() else 1.dp.toPx()
            )
        }
    }
}
