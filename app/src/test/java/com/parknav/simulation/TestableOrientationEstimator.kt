package com.parknav.simulation

import com.parknav.sensor.SensorData
import kotlin.math.atan2

/**
 * Pure-JVM replacement for OrientationEstimator that avoids Android dependencies.
 * Uses atan2(magX, magY) for magnetometer heading (matches our mag encoding:
 *   magX = H * sin(θ), magY = H * cos(θ)  →  atan2(magX, magY) = θ
 * Applies complementary filter with gyroscope integration, same as production code.
 */
class TestableOrientationEstimator(
    private val alpha: Float = 0.98f
) {
    var heading: Float = 0f
        private set

    private var lastTimestamp: Long = 0L
    private var initialized = false

    fun update(data: SensorData) {
        val magHeading = atan2(data.magX, data.magY) // returns radians in [-π, π]

        if (!initialized) {
            heading = magHeading
            lastTimestamp = data.timestamp
            initialized = true
            return
        }

        val dt = (data.timestamp - lastTimestamp) * 1e-9f
        lastTimestamp = data.timestamp

        if (dt <= 0 || dt > 1.0f) {
            heading = magHeading
            return
        }

        val gyroHeading = heading + data.gyroZ * dt
        heading = alpha * gyroHeading + (1 - alpha) * magHeading
        heading = ((heading % (2 * Math.PI).toFloat()) + (2 * Math.PI).toFloat()) % (2 * Math.PI).toFloat()
    }

    fun reset() {
        heading = 0f
        initialized = false
        lastTimestamp = 0L
    }
}
