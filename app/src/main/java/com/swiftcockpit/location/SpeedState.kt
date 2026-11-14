package com.swiftcockpit.location

/** Everything the dashboard needs to know about speed. Only [Live] ever carries a number. */
sealed interface SpeedState {
    /** Location permission not granted (or never asked). */
    data object PermissionRequired : SpeedState

    /** Only approximate location granted: GPS speed is unavailable without precise location. */
    data object PreciseLocationRequired : SpeedState

    /** Permission is fine but device location / GPS is switched off. */
    data object GpsDisabled : SpeedState

    /** GPS is on but there is no fresh, trustworthy fix (cold start, tunnel, garage, signal loss). */
    data object Acquiring : SpeedState

    /** A fresh GPS reading. [accuracyKmh] is the 68% confidence interval, when the chip reports it. */
    data class Live(val kmh: Float, val accuracyKmh: Float?) : SpeedState
}

/** A platform-independent view of one GPS fix, so the logic can be unit-tested. */
data class GpsFix(
    /** [android.location.Location.getElapsedRealtimeNanos]: monotonic, immune to wall-clock changes. */
    val elapsedRealtimeNanos: Long,
    /** Ground speed in m/s, or null if the fix carries no speed. */
    val speedMps: Float?,
    /** Speed accuracy in m/s, or null if not reported. */
    val speedAccuracyMps: Float?,
)
