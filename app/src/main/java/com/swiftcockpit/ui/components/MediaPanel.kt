package com.swiftcockpit.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swiftcockpit.media.NowPlaying
import com.swiftcockpit.ui.theme.CockpitColors

@Composable
fun MediaPanel(
    nowPlaying: NowPlaying,
    onOpenPlayer: () -> Unit,
    onGrantAccess: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CockpitPanel(modifier) {
        Column(Modifier.fillMaxSize().padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PanelLabel("MEDIA")
                Spacer(Modifier.weight(1f))
                val source = (nowPlaying as? NowPlaying.Active)?.sourceApp
                if (source != null) {
                    Text(source, color = CockpitColors.Muted, fontSize = CockpitType.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(16.dp))
            when (nowPlaying) {
                is NowPlaying.Active -> ActiveMedia(nowPlaying, onOpenPlayer, onPlayPause, onNext, onPrevious)
                NowPlaying.Idle -> MediaMessage(
                    title = "Nothing playing",
                    body = "Start music in any app and it appears here.",
                    action = "OPEN MUSIC", onAction = onOpenPlayer
                )
                NowPlaying.AccessRequired -> MediaMessage(
                    title = "Show what's playing",
                    body = "Allow notification access so the song and controls appear here. " +
                        "If the switch is greyed out: App info → ⋮ → Allow restricted settings.",
                    action = "ALLOW ACCESS", onAction = onGrantAccess
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ActiveMedia(
    media: NowPlaying.Active,
    onOpenPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onOpenPlayer),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val artShape = RoundedCornerShape(14.dp)
        Box(
            Modifier.size(84.dp).clip(artShape).background(Color(0xFF222B34)),
            contentAlignment = Alignment.Center
        ) {
            val art = media.art
            if (art != null) {
                val image = remember(art) { art.asImageBitmap() }
                Image(image, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Filled.MusicNote, null, tint = CockpitColors.Orange, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                media.title, color = CockpitColors.White, fontSize = CockpitType.Title,
                fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 24.sp
            )
            if (media.artist != null) {
                Spacer(Modifier.height(4.dp))
                Text(media.artist, color = CockpitColors.Muted, fontSize = CockpitType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    Spacer(Modifier.weight(1f))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        RoundControl(Icons.Filled.SkipPrevious, 60.dp, filled = false, onClick = onPrevious)
        RoundControl(if (media.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, 76.dp, filled = true, onClick = onPlayPause)
        RoundControl(Icons.Filled.SkipNext, 60.dp, filled = false, onClick = onNext)
    }
}

@Composable
private fun RoundControl(icon: ImageVector, size: Dp, filled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape)
            .background(if (filled) CockpitColors.Orange else Color(0xFF222B34))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (filled) CockpitColors.Ink else CockpitColors.White, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.MediaMessage(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
) {
    Text(title, color = CockpitColors.White, fontSize = CockpitType.Title, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
    Text(body, color = CockpitColors.Muted, fontSize = CockpitType.Label, lineHeight = 18.sp)
    Spacer(Modifier.weight(1f))
    SecondaryButton(action, onAction, Modifier.fillMaxWidth())
}
