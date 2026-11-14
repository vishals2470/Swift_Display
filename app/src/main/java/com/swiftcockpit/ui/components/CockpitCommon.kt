package com.swiftcockpit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swiftcockpit.ui.theme.CockpitColors

/** Type scale chosen for reading at arm's length; nothing smaller than [Label]. */
object CockpitType {
    val Label = 13.sp
    val Body = 15.sp
    val Title = 20.sp
}

private val PanelShape = RoundedCornerShape(20.dp)

@Composable
fun CockpitPanel(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .shadow(12.dp, PanelShape)
            .clip(PanelShape)
            .background(Brush.linearGradient(listOf(Color(0xFF1A222B), CockpitColors.Panel, Color(0xFF0E1318))))
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color(0xFF3A4652), Color(0xFF202832), Color(0xFF303B45))),
                PanelShape
            ),
        content = content
    )
}

@Composable
fun PanelLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier = modifier, color = CockpitColors.Muted, fontSize = CockpitType.Label,
        letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold
    )
}

/** Primary action: large touch target (≥ 56 dp) for use in a moving car. */
@Composable
fun PrimaryButton(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CockpitColors.Orange, contentColor = CockpitColors.Ink),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, CockpitColors.Orange),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CockpitColors.OrangeSoft),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(text, fontSize = CockpitType.Body, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
fun StatusPill(icon: ImageVector, label: String, highlighted: Boolean, warning: Boolean) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.clip(shape).background(Color(0xFF1B232B)).border(1.dp, CockpitColors.Line, shape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null, modifier = Modifier.size(20.dp),
            tint = when {
                highlighted -> CockpitColors.Orange
                warning -> CockpitColors.Warning
                else -> CockpitColors.Muted
            }
        )
        Spacer(Modifier.width(10.dp))
        Text(label, color = CockpitColors.White, fontSize = 14.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
    }
}
