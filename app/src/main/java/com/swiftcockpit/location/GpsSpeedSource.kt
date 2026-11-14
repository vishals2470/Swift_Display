package com.swiftcockpit.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

enum class LocationPermission { NONE, APPROXIMATE, PRECISE }

/**
 * Speed straight from the GPS chip via [LocationManager.GPS_PROVIDER].
 *
 * Deliberately not the Play Services fused provider: fused blends Wi-Fi/cell positions and
 * smooths output, which is good for "where am I" but worse for real-time ground speed, and it
 * would make the app depend on Play Services being present.
 */
class GpsSpeedSource(private val context: Context) {

    fun permission(): LocationPermission = when {
        granted(Manifest.permission.ACCESS_FINE_LOCATION) -> LocationPermission.PRECISE
        granted(Manifest.permission.ACCESS_COARSE_LOCATION) -> LocationPermission.APPROXIMATE
        else -> LocationPermission.NONE
    }

    /** Collect on the main thread. Re-collect when [permission] changes. */
    fun states(permission: LocationPermission): Flow<SpeedState> = when (permission) {
        LocationPermission.NONE -> flowOf(SpeedState.PermissionRequired)
        LocationPermission.APPROXIMATE -> flowOf(SpeedState.PreciseLocationRequired)
        LocationPermission.PRECISE -> gpsStates()
    }

    @SuppressLint("MissingPermission") // only reached with PRECISE permission
    private fun gpsStates(): Flow<SpeedState> = callbackFlow {
        val manager = context.getSystemService(LocationManager::class.java)
        if (manager == null || !manager.allProviders.contains(LocationManager.GPS_PROVIDER)) {
            send(SpeedState.GpsDisabled)
            awaitClose { }
            return@callbackFlow
        }

        val evaluator = SpeedEvaluator()
        var gpsEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        fun publish() {
            trySend(
                if (gpsEnabled) evaluator.state(SystemClock.elapsedRealtimeNanos())
                else SpeedState.GpsDisabled
            )
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                evaluator.onFix(location.toFix())
                publish()
            }

            override fun onProviderEnabled(provider: String) {
                gpsEnabled = true
                publish()
            }

            override fun onProviderDisabled(provider: String) {
                gpsEnabled = false
                evaluator.reset()
                publish()
            }
        }

        try {
            // Registering while GPS is off is fine: onProviderEnabled fires when the user turns it on.
            manager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, UPDATE_INTERVAL_MS, 0f, listener, Looper.getMainLooper()
            )
            // A cached fix only counts if it is still fresh; the evaluator's staleness rule decides.
            manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { evaluator.onFix(it.toFix()) }
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call; the next resume re-checks it.
            trySend(SpeedState.PermissionRequired)
        }
        publish()

        // Re-evaluate between fixes so a lost signal turns into "--" within the stale window.
        val ticker = launch {
            while (true) {
                delay(TICK_MS)
                publish()
            }
        }

        awaitClose {
            ticker.cancel()
            manager.removeUpdates(listener)
        }
    }.distinctUntilChanged()

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val UPDATE_INTERVAL_MS = 1_000L
        const val TICK_MS = 500L
    }
}

internal fun Location.toFix() = GpsFix(
    elapsedRealtimeNanos = elapsedRealtimeNanos,
    speedMps = if (hasSpeed()) speed else null,
    speedAccuracyMps = if (hasSpeedAccuracy()) speedAccuracyMetersPerSecond else null,
)
