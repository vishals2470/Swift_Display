package com.swiftcockpit.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedEvaluatorTest {

    private val second = 1_000_000_000L
    private val evaluator = SpeedEvaluator()

    private fun fix(atSeconds: Long, speedMps: Float?, accuracyMps: Float? = 0.5f) =
        GpsFix(atSeconds * second, speedMps, accuracyMps)

    private fun SpeedState.kmh() = (this as SpeedState.Live).kmh

    @Test
    fun `no fix yet is acquiring`() {
        assertEquals(SpeedState.Acquiring, evaluator.state(10 * second))
    }

    @Test
    fun `fresh fix is live and converted to kmh`() {
        evaluator.onFix(fix(10, speedMps = 25f))
        val state = evaluator.state(10 * second + 500_000_000L)
        assertEquals(90f, state.kmh(), 0.01f)
        assertEquals(1.8f, (state as SpeedState.Live).accuracyKmh!!, 0.01f)
    }

    @Test
    fun `signal loss turns into acquiring instead of freezing the last speed`() {
        evaluator.onFix(fix(10, speedMps = 25f))
        assertTrue(evaluator.state(13 * second) is SpeedState.Live)
        assertEquals(SpeedState.Acquiring, evaluator.state(13 * second + 1))
    }

    @Test
    fun `old cached fix is never shown as live`() {
        evaluator.onFix(fix(10, speedMps = 25f))
        assertEquals(SpeedState.Acquiring, evaluator.state(3_600 * second))
    }

    @Test
    fun `fix without speed is ignored`() {
        evaluator.onFix(fix(10, speedMps = null))
        assertEquals(SpeedState.Acquiring, evaluator.state(10 * second))
    }

    @Test
    fun `inaccurate fix is ignored and last good reading is kept`() {
        evaluator.onFix(fix(10, speedMps = 20f))
        evaluator.onFix(fix(11, speedMps = 40f, accuracyMps = 9f))
        assertEquals(72f, evaluator.state(11 * second).kmh(), 0.01f)
    }

    @Test
    fun `fix without accuracy is accepted`() {
        evaluator.onFix(fix(10, speedMps = 10f, accuracyMps = null))
        val state = evaluator.state(10 * second) as SpeedState.Live
        assertEquals(36f, state.kmh, 0.01f)
        assertEquals(null, state.accuracyKmh)
    }

    @Test
    fun `parking jitter reads as zero`() {
        evaluator.onFix(fix(10, speedMps = 0.4f))
        assertEquals(0f, evaluator.state(10 * second).kmh(), 0f)
    }

    @Test
    fun `out of order fix does not replace newer one`() {
        evaluator.onFix(fix(10, speedMps = 20f))
        evaluator.onFix(fix(9, speedMps = 5f))
        assertEquals(72f, evaluator.state(10 * second).kmh(), 0.01f)
    }

    @Test
    fun `invalid speeds are ignored`() {
        evaluator.onFix(fix(10, speedMps = Float.NaN))
        evaluator.onFix(fix(10, speedMps = -1f))
        assertEquals(SpeedState.Acquiring, evaluator.state(10 * second))
    }

    @Test
    fun `reset clears the reading`() {
        evaluator.onFix(fix(10, speedMps = 20f))
        evaluator.reset()
        assertEquals(SpeedState.Acquiring, evaluator.state(10 * second))
    }
}
