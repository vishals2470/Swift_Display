package com.swiftcockpit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swiftcockpit.ui.theme.CockpitColors

@Composable
fun NavigationPanel(onMaps: () -> Unit, modifier: Modifier = Modifier) {
    CockpitPanel(modifier) {
        Column(Modifier.fillMaxSize().padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PanelLabel("NAVIGATION")
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.NearMe, null, tint = CockpitColors.Orange, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(14.dp))
            val shape = RoundedCornerShape(16.dp)
            Box(
                Modifier.weight(1f).fillMaxWidth().clip(shape)
                    .background(Brush.linearGradient(listOf(Color(0xFF1A2730), Color(0xFF10171D), Color(0xFF172129))))
                    .border(1.dp, CockpitColors.Line, shape)
                    .clickable(onClick = onMaps),
                contentAlignment = Alignment.Center
            ) {
                MapArtwork()
                Column(
                    Modifier.clip(RoundedCornerShape(14.dp)).background(CockpitColors.Ink.copy(alpha = .72f))
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Map, null, tint = CockpitColors.White, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("Live map coming next", color = CockpitColors.White, fontSize = CockpitType.Body, fontWeight = FontWeight.SemiBold)
                    Text("Tap to open Google Maps", color = CockpitColors.Muted, fontSize = CockpitType.Label)
                }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton("NAVIGATE", Icons.Filled.Navigation, onMaps, Modifier.fillMaxWidth())
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
                if (index == 0) CockpitColors.Orange.copy(alpha = .6f) else Color(0xFF46535E).copy(alpha = .55f),
                points[i], points[i + 1], if (index == 0) 4.dp.toPx() else 1.6.dp.toPx(), cap = StrokeCap.Round
            )
        }
    }
}
