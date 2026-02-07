package com.parknav.simulation

import com.parknav.data.Position

data class ValidationReport(
    val pathDescription: String,
    val totalDistance: Float,
    val maxError: Float,
    val avgError: Float,
    val closingError: Float,
    val stepCountExpected: Int,
    val stepCountActual: Int,
    val passed: Boolean,
    val details: String
)

/**
 * Feeds simulated sensor data into a TestablePDREngine and compares
 * the resulting trajectory against ground truth positions.
 */
object PathValidator {

    fun validate(
        path: SimulatedPath,
        engine: TestablePDREngine,
        maxAllowedError: Float = 5f,
        maxClosingError: Float = 3f
    ): ValidationReport {
        engine.reset()

        // Feed all sensor data
        for (data in path.sensorData) {
            engine.onSensorUpdate(data)
        }

        val finalPos = engine.position
        val expectedFinal = path.groundTruth.lastOrNull() ?: Position(0f, 0f, 0f)
        val origin = path.groundTruth.firstOrNull() ?: Position(0f, 0f, 0f)

        // Closing error: distance from final position back to origin
        // (meaningful for loop paths; for non-loops it's just the endpoint error)
        val endpointError = finalPos.distanceTo2D(expectedFinal)

        // For rectangle/loop paths, closing error = distance from final to start
        val closingError = if (origin.distanceTo2D(expectedFinal) < 0.1f) {
            // It's a loop path — closing error is meaningful
            finalPos.distanceTo2D(origin)
        } else {
            endpointError
        }

        val stepCountExpected = (path.groundTruth.size - 1).coerceAtLeast(0)
        val stepCountActual = engine.stepCount

        val passed = endpointError <= maxAllowedError && closingError <= maxClosingError

        val details = buildString {
            appendLine("═══ Validation Report ═══")
            appendLine("Path: ${path.description}")
            appendLine("Total distance: %.1fm".format(path.totalDistance))
            appendLine("Steps expected: $stepCountExpected, actual: $stepCountActual")
            appendLine("Expected final: (%.2f, %.2f)".format(expectedFinal.x, expectedFinal.z))
            appendLine("Actual final:   (%.2f, %.2f)".format(finalPos.x, finalPos.z))
            appendLine("Endpoint error: %.2fm".format(endpointError))
            appendLine("Closing error:  %.2fm".format(closingError))
            appendLine("Result: ${if (passed) "✅ PASSED" else "❌ FAILED"}")
            appendLine("═════════════════════════")
        }

        return ValidationReport(
            pathDescription = path.description,
            totalDistance = path.totalDistance,
            maxError = endpointError,
            avgError = endpointError, // simplified: we only sample at the end
            closingError = closingError,
            stepCountExpected = stepCountExpected,
            stepCountActual = stepCountActual,
            passed = passed,
            details = details
        )
    }
}
