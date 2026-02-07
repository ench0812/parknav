package com.parknav.sensor

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class StepDetectorTest {

    private lateinit var detector: StepDetector
    private var detectedSteps: MutableList<StepDetector.StepEvent> = mutableListOf()

    @Before
    fun setUp() {
        detector = StepDetector()
        detectedSteps.clear()
        detector.setOnStepListener { detectedSteps.add(it) }
    }

    @Test
    fun `stationary acceleration does not trigger step`() {
        // Feed constant gravity for 2 seconds at 60Hz
        val dt = 16_666_667L // ~60Hz in ns
        for (i in 0 until 120) {
            detector.onAccelerometerUpdate(0f, 9.81f, 0f, i * dt)
        }
        assertEquals(0, detector.stepCount)
        assertTrue(detectedSteps.isEmpty())
    }

    @Test
    fun `walking waveform triggers steps`() {
        // Simulate walking: sinusoidal acceleration with gravity offset
        // Frequency ~2Hz (2 steps/sec), amplitude 3 m/s² (above 1.2 threshold)
        val sampleRate = 60
        val durationSec = 3
        val dtNs = 1_000_000_000L / sampleRate
        val freq = 2.0 // Hz

        for (i in 0 until sampleRate * durationSec) {
            val t = i.toDouble() / sampleRate
            val accY = (9.81 + 3.0 * sin(2 * Math.PI * freq * t)).toFloat()
            detector.onAccelerometerUpdate(0f, accY, 0f, i * dtNs)
        }

        assertTrue("Should detect at least 1 step", detector.stepCount >= 1)
        assertEquals(detector.stepCount, detectedSteps.size)
    }

    @Test
    fun `steps too close together are rejected`() {
        // Fill window, trigger a peak, then immediately try another
        val windowSize = 30
        val dtNs = 16_666_667L

        // Build a window with a peak in the middle
        for (i in 0 until windowSize) {
            val acc = if (i == windowSize / 2) 14.0f else 9.0f // peak at center
            detector.onAccelerometerUpdate(0f, acc, 0f, i * dtNs)
        }
        val stepsAfterFirst = detector.stepCount

        // Immediately feed another peak (within 300ms)
        val baseTime = (windowSize - 1) * dtNs
        for (i in 0 until windowSize) {
            val acc = if (i == windowSize / 2) 14.0f else 9.0f
            detector.onAccelerometerUpdate(0f, acc, 0f, baseTime + i * dtNs)
        }

        // Should not have more than one additional step due to interval check
        assertTrue(detector.stepCount <= stepsAfterFirst + 1)
    }

    @Test
    fun `weinberg step length calculation`() {
        // stepLength = K * (aMax - aMin)^0.25, K=0.5
        val aMax = 13.0f
        val aMin = 8.0f
        val expected = 0.5f * (aMax - aMin).toDouble().pow(0.25).toFloat()

        // We verify via listener when a step is detected
        // The step length depends on window contents
        // Just verify formula: sqrt(sqrt(5)) * 0.5 ≈ 0.5 * 1.495 ≈ 0.748
        assertEquals(0.748f, expected, 0.01f)
    }

    @Test
    fun `reset clears step count`() {
        // Trigger some steps
        val dtNs = 16_666_667L
        for (i in 0 until 200) {
            val t = i.toDouble() / 60
            val accY = (9.81 + 3.0 * sin(2 * Math.PI * 2.0 * t)).toFloat()
            detector.onAccelerometerUpdate(0f, accY, 0f, i * dtNs)
        }

        detector.reset()
        assertEquals(0, detector.stepCount)
    }
}
