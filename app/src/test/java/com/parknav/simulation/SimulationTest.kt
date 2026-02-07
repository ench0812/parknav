package com.parknav.simulation

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Adversarial validation tests: generate known paths with PathSimulator,
 * feed through TestablePDREngine, and verify output trajectory matches ground truth.
 */
class SimulationTest {

    private lateinit var engine: TestablePDREngine

    @Before
    fun setUp() {
        engine = TestablePDREngine()
    }

    @Test
    fun `straight walk 50m north - error under 5m`() {
        val path = PathSimulator.simulateStraightWalk(
            distance = 50f,
            heading = 0f,  // north
            stepLength = 0.7f
        )
        val report = PathValidator.validate(path, engine, maxAllowedError = 5f)
        println(report.details)
        println("Steps detected: ${report.stepCountActual} (expected ~${report.stepCountExpected})")
        assertTrue("Endpoint error ${report.maxError}m exceeds 5m", report.maxError <= 5f)
    }

    @Test
    fun `straight walk 30m east - error under 3m`() {
        val path = PathSimulator.simulateStraightWalk(
            distance = 30f,
            heading = (Math.PI / 2).toFloat(), // east
            stepLength = 0.7f
        )
        val report = PathValidator.validate(path, engine, maxAllowedError = 3f)
        println(report.details)
        assertTrue("Endpoint error ${report.maxError}m exceeds 3m", report.maxError <= 3f)
    }

    @Test
    fun `L shape turn - error under 5m`() {
        val path = PathSimulator.simulateLShape(legLength1 = 20f, legLength2 = 15f)
        val report = PathValidator.validate(path, engine, maxAllowedError = 5f)
        println(report.details)
        assertTrue("Endpoint error ${report.maxError}m exceeds 5m", report.maxError <= 5f)
    }

    @Test
    fun `rectangle loop - closing error under 3m`() {
        val path = PathSimulator.simulateRectangle(width = 10f, height = 15f)
        val report = PathValidator.validate(path, engine, maxAllowedError = 5f, maxClosingError = 3f)
        println(report.details)
        assertTrue("Closing error ${report.closingError}m exceeds 3m", report.closingError <= 3f)
    }

    @Test
    fun `stationary - no drift`() {
        val path = PathSimulator.simulateStationary(durationMs = 5000)
        val report = PathValidator.validate(path, engine, maxAllowedError = 0.1f)
        println(report.details)
        assertEquals("No steps expected", 0, report.stepCountActual)
        assertTrue("Drift ${report.maxError}m exceeds 0.1m", report.maxError <= 0.1f)
    }
}
