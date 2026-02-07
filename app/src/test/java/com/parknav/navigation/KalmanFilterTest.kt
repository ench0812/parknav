package com.parknav.navigation

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class KalmanFilterTest {

    private lateinit var filter: KalmanFilter

    @Before
    fun setUp() {
        filter = KalmanFilter()
    }

    @Test
    fun `first update returns input value`() {
        assertEquals(5.0f, filter.update(5.0f), 0.001f)
    }

    @Test
    fun `converges to constant input`() {
        filter.update(0f) // init
        repeat(100) { filter.update(10f) }
        assertEquals(10f, filter.update(10f), 0.01f)
    }

    @Test
    fun `smooths noisy input`() {
        val target = 5.0f
        val noisy = listOf(5.5f, 4.3f, 5.8f, 4.7f, 5.2f, 4.9f, 5.6f, 4.4f, 5.1f, 5.3f)
        filter.update(target)
        var totalInputDeviation = 0f
        var totalOutputDeviation = 0f
        for (v in noisy) {
            val out = filter.update(v)
            totalInputDeviation += abs(v - target)
            totalOutputDeviation += abs(out - target)
        }
        assertTrue("Output should be smoother than input",
            totalOutputDeviation < totalInputDeviation)
    }

    @Test
    fun `reset returns to uninitialized state`() {
        filter.update(100f)
        filter.update(100f)
        filter.reset()
        // After reset, first update should return input directly
        assertEquals(42f, filter.update(42f), 0.001f)
    }

    @Test
    fun `high process noise tracks input faster`() {
        val slowFilter = KalmanFilter(processNoise = 0.001f, measurementNoise = 1f)
        val fastFilter = KalmanFilter(processNoise = 1f, measurementNoise = 0.001f)

        slowFilter.update(0f)
        fastFilter.update(0f)

        val slowResult = slowFilter.update(10f)
        val fastResult = fastFilter.update(10f)

        assertTrue("Fast filter should track closer to 10",
            abs(fastResult - 10f) < abs(slowResult - 10f))
    }
}
