package com.parknav.simulation

import com.parknav.data.Position
import com.parknav.sensor.SensorData
import kotlin.math.*

/**
 * Simulated path with ground truth for validation.
 */
data class SimulatedPath(
    val sensorData: List<SensorData>,
    val groundTruth: List<Position>,
    val totalDistance: Float,
    val description: String
)

/**
 * Generates simulated IMU sensor data for known paths.
 *
 * Coordinate system matches PDREngine:
 *   X = east (heading=π/2), Z = south (heading=π), north = heading 0
 *   posX += stepLength * sin(heading)
 *   posZ += stepLength * cos(heading)
 *
 * Magnetometer encoding for heading θ (radians, 0=north clockwise):
 *   magX = 20 * sin(θ)   (east component)
 *   magY = 20 * cos(θ)   (north component)
 *   magZ = -40            (downward, typical)
 *
 * This makes atan2(magY, magX) recover the heading in the way
 * TestableOrientationEstimator expects.
 */
object PathSimulator {

    private const val SAMPLE_INTERVAL_NS = 20_000_000L // 20ms = 50Hz
    private const val GRAVITY = 9.81f
    private const val WALK_FREQ = 2.0    // 2 Hz step frequency
    private const val WALK_AMP = 4.0     // m/s² amplitude on top of gravity
    private const val MAG_HORIZONTAL = 20f
    private const val MAG_VERTICAL = -40f

    // ── public API ────────────────────────────────────────────────

    fun simulateStraightWalk(
        distance: Float,
        heading: Float,      // radians, 0 = north
        stepLength: Float = 0.7f
    ): SimulatedPath {
        val steps = (distance / stepLength).roundToInt()
        val stepsPerSec = WALK_FREQ
        val walkDurationMs = (steps / stepsPerSec * 1000).toLong()
        val totalSamples = (walkDurationMs / 20).toInt()

        val sensorData = mutableListOf<SensorData>()
        val groundTruth = mutableListOf(Position(0f, 0f, 0f)) // start

        val dx = stepLength * sin(heading)
        val dz = stepLength * cos(heading)

        var currentX = 0f
        var currentZ = 0f
        var stepIndex = 0

        for (i in 0 until totalSamples) {
            val t = i * 0.02 // seconds
            val ts = i * SAMPLE_INTERVAL_NS

            // Walking waveform – sinusoidal on the Y-axis (vertical in portrait hold)
            val walkComponent = WALK_AMP * sin(2 * PI * WALK_FREQ * t)
            val accY = (GRAVITY + walkComponent).toFloat()

            sensorData += SensorData(
                accX = 0f, accY = accY, accZ = 0f,
                gyroX = 0f, gyroY = 0f, gyroZ = 0f,
                magX = MAG_HORIZONTAL * sin(heading),
                magY = MAG_HORIZONTAL * cos(heading),
                magZ = MAG_VERTICAL,
                timestamp = ts
            )

            // Ground-truth position: advance one step at each peak
            // Peak occurs when sin(...) = 1 → t = (0.25 + n) / freq
            val cyclePos = (WALK_FREQ * t) % 1.0
            if (cyclePos in 0.24..0.26 && stepIndex < steps) {
                stepIndex++
                currentX += dx
                currentZ += dz
                groundTruth += Position(currentX, 0f, currentZ)
            }
        }

        // Ensure we have the final position
        if (stepIndex < steps) {
            currentX = steps * dx
            currentZ = steps * dz
            groundTruth += Position(currentX, 0f, currentZ)
        }

        return SimulatedPath(
            sensorData = sensorData,
            groundTruth = groundTruth,
            totalDistance = distance,
            description = "Straight walk %.0fm heading %.0f°".format(distance, Math.toDegrees(heading.toDouble()))
        )
    }

    fun simulateTurn(
        fromHeading: Float,
        toHeading: Float,
        steps: Int = 3
    ): SimulatedPath {
        // Turn over `steps` walking steps, smoothly rotating heading
        val stepsPerSec = WALK_FREQ
        val durationMs = (steps / stepsPerSec * 1000).toLong()
        val totalSamples = (durationMs / 20).toInt()

        val sensorData = mutableListOf<SensorData>()
        val groundTruth = mutableListOf(Position(0f, 0f, 0f))

        // Shortest angular difference
        var delta = toHeading - fromHeading
        while (delta > PI) delta -= (2 * PI).toFloat()
        while (delta < -PI) delta += (2 * PI).toFloat()

        val angularRate = delta / (durationMs / 1000f) // rad/s

        var currentX = 0f
        var currentZ = 0f
        var stepIndex = 0
        val stepLength = 0.7f

        for (i in 0 until totalSamples) {
            val t = i * 0.02
            val ts = i * SAMPLE_INTERVAL_NS
            val progress = (t / (durationMs / 1000.0)).coerceIn(0.0, 1.0).toFloat()
            val currentHeading = fromHeading + delta * progress

            val walkComponent = WALK_AMP * sin(2 * PI * WALK_FREQ * t)
            val accY = (GRAVITY + walkComponent).toFloat()

            sensorData += SensorData(
                accX = 0f, accY = accY, accZ = 0f,
                gyroX = 0f, gyroY = 0f, gyroZ = angularRate,
                magX = MAG_HORIZONTAL * sin(currentHeading),
                magY = MAG_HORIZONTAL * cos(currentHeading),
                magZ = MAG_VERTICAL,
                timestamp = ts
            )

            val cyclePos = (WALK_FREQ * t) % 1.0
            if (cyclePos in 0.24..0.26 && stepIndex < steps) {
                stepIndex++
                currentX += stepLength * sin(currentHeading)
                currentZ += stepLength * cos(currentHeading)
                groundTruth += Position(currentX, 0f, currentZ)
            }
        }

        return SimulatedPath(
            sensorData = sensorData,
            groundTruth = groundTruth,
            totalDistance = steps * stepLength,
            description = "Turn from %.0f° to %.0f° in %d steps".format(
                Math.toDegrees(fromHeading.toDouble()),
                Math.toDegrees(toHeading.toDouble()),
                steps
            )
        )
    }

    fun simulateLShape(legLength1: Float, legLength2: Float): SimulatedPath {
        val heading1 = 0f              // north
        val heading2 = (PI / 2).toFloat() // east (90° right turn)

        val leg1 = simulateStraightWalk(legLength1, heading1)
        val turn = simulateTurn(heading1, heading2, steps = 3)
        val leg2 = simulateStraightWalk(legLength2, heading2)

        return combinePaths(
            listOf(leg1, turn, leg2),
            "L-shape: %.0fm north + %.0fm east".format(legLength1, legLength2),
            legLength1 + legLength2
        )
    }

    fun simulateRectangle(width: Float, height: Float): SimulatedPath {
        val headings = listOf(0f, (PI / 2).toFloat(), PI.toFloat(), (3 * PI / 2).toFloat())
        val lengths = listOf(height, width, height, width)

        val segments = mutableListOf<SimulatedPath>()
        for (i in 0 until 4) {
            segments += simulateStraightWalk(lengths[i], headings[i])
            if (i < 3) {
                segments += simulateTurn(headings[i], headings[i + 1], steps = 2)
            } else {
                segments += simulateTurn(headings[3], headings[0], steps = 2)
            }
        }

        return combinePaths(
            segments,
            "Rectangle %.0fm × %.0fm".format(width, height),
            2 * (width + height)
        )
    }

    fun simulateStationary(durationMs: Long): SimulatedPath {
        val totalSamples = (durationMs / 20).toInt()
        val sensorData = (0 until totalSamples).map { i ->
            SensorData(
                accX = 0f, accY = GRAVITY, accZ = 0f,
                gyroX = 0f, gyroY = 0f, gyroZ = 0f,
                magX = 0f, magY = MAG_HORIZONTAL, magZ = MAG_VERTICAL,
                timestamp = i * SAMPLE_INTERVAL_NS
            )
        }

        return SimulatedPath(
            sensorData = sensorData,
            groundTruth = listOf(Position(0f, 0f, 0f)),
            totalDistance = 0f,
            description = "Stationary %dms".format(durationMs)
        )
    }

    // ── helpers ───────────────────────────────────────────────────

    private fun combinePaths(
        paths: List<SimulatedPath>,
        description: String,
        totalDistance: Float
    ): SimulatedPath {
        val allSensor = mutableListOf<SensorData>()
        val allTruth = mutableListOf<Position>()

        var timeOffset = 0L
        var posOffsetX = 0f
        var posOffsetZ = 0f

        for (path in paths) {
            // Append sensor data with shifted timestamps
            for (d in path.sensorData) {
                allSensor += d.copy(timestamp = d.timestamp + timeOffset)
            }
            val maxTs = path.sensorData.maxOfOrNull { it.timestamp } ?: 0L
            timeOffset += maxTs + SAMPLE_INTERVAL_NS

            // Append ground truth with position offset (skip first = origin of segment)
            val truthToAdd = if (allTruth.isEmpty()) path.groundTruth else path.groundTruth.drop(1)
            for (p in truthToAdd) {
                allTruth += Position(p.x + posOffsetX, 0f, p.z + posOffsetZ)
            }

            // Update offset from last truth point of this segment
            if (path.groundTruth.isNotEmpty()) {
                val last = path.groundTruth.last()
                posOffsetX += last.x
                posOffsetZ += last.z
            }
        }

        return SimulatedPath(
            sensorData = allSensor,
            groundTruth = allTruth,
            totalDistance = totalDistance,
            description = description
        )
    }

    private fun Float.roundToInt(): Int = kotlin.math.round(this).toInt()
}
