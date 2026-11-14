package com.swiftcockpit.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.ui.graphics.vector.ImageVector
import com.swiftcockpit.location.SpeedState

/** How each speed state is presented: header pill, explanation and the fix-it action. */
data class GpsStatus(
    val pill: String,
    val icon: ImageVector,
    val message: String,
    val action: String?,
    val isProblem: Boolean,
)

fun SpeedState.gpsStatus(): GpsStatus = when (this) {
    is SpeedState.Live -> GpsStatus("GPS LOCK", Icons.Filled.GpsFixed, "Receiving GPS", null, false)
    SpeedState.Acquiring -> GpsStatus("GPS SEARCH", Icons.Filled.GpsNotFixed, "Waiting for GPS signal…", null, false)
    SpeedState.GpsDisabled -> GpsStatus("GPS OFF", Icons.Filled.GpsOff, "Device location is off", "TURN ON GPS", true)
    SpeedState.PermissionRequired -> GpsStatus("NO ACCESS", Icons.Filled.LocationDisabled, "Location permission needed", "ALLOW LOCATION", true)
    SpeedState.PreciseLocationRequired -> GpsStatus("APPROX ONLY", Icons.Filled.LocationDisabled, "Precise location needed for speed", "ALLOW PRECISE", true)
}
