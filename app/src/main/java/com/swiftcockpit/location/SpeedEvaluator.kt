package com.swiftcockpit.location

/**
 * Turns a stream of GPS fixes into a [SpeedState] the driver can trust.
 *
 * Rules:
 * - Fixes without speed, or with speed accuracy worse than [maxSpeedAccuracyMps], are ignored.
 * - The last good fix is shown until it is older than [staleAfterNanos]; after that the state
 *   becomes [SpeedState.Acquiring] so a frozen number is never displayed.
 * - Speeds below [stationaryThresholdKmh] are shown as 0 to hide GPS jitter while parked.
 *
 * Not thread-safe: call from a single thread (the main thread in the app).
 */
class SpeedEvaluator(
    private val staleAfterNanos: Long = 3_000_000_000L,
    private val maxSpeedAccuracyMps: Float = 5f,
    private val stationaryThresholdKmh: Float = 2f,
) {
    private var lastGood: GpsFix? = null

    fun onFix(fix: GpsFix) {
        val speed = fix.speedMps ?: return
        if (speed.isNaN() || speed < 0f) return
        val accuracy = fix.speedAccuracyMps
        if (accuracy != null && accuracy > maxSpeedAccuracyMps) return
        val previous = lastGood
        if (previous != null && fix.elapsedRealtimeNanos < previous.elapsedRealtimeNanos) return
        lastGood = fix
    }

    fun reset() {
        lastGood = null
    }

    fun state(nowElapsedRealtimeNanos: Long): SpeedState {
        val fix = lastGood ?: return SpeedState.Acquiring
        val age = nowElapsedRealtimeNanos - fix.elapsedRealtimeNanos
        if (age > staleAfterNanos) return SpeedState.Acquiring
        val kmh = fix.speedMps!! * MPS_TO_KMH
        return SpeedState.Live(
            kmh = if (kmh < stationaryThresholdKmh) 0f else kmh,
            accuracyKmh = fix.speedAccuracyMps?.let { it * MPS_TO_KMH },
        )
    }

    companion object {
        const val MPS_TO_KMH = 3.6f
    }
}
