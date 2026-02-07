package com.parknav.sensor

import android.hardware.SensorManager as AndroidSensorManager
import kotlin.math.atan2

/**
 * Complementary filter fusing magnetometer heading with gyroscope integration.
 * heading = alpha * gyroHeading + (1-alpha) * magHeading
 */
class OrientationEstimator(
    private val alpha: Float = 0.98f // gyro weight
) {
    var heading: Float = 0f // radians, 0 = north, clockwise
        private set

    private var lastTimestamp: Long = 0L
    private var initialized = false

    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    fun update(sensorData: SensorData) {
        val magHeading = computeMagHeading(sensorData)

        if (!initialized) {
            heading = magHeading
            lastTimestamp = sensorData.timestamp
            initialized = true
            return
        }

        val dt = (sensorData.timestamp - lastTimestamp) * 1e-9f
        lastTimestamp = sensorData.timestamp

        if (dt <= 0 || dt > 1.0f) {
            heading = magHeading
            return
        }

        // Integrate gyro Z (yaw) - device Y axis in portrait
        val gyroYaw = sensorData.gyroZ
        val gyroHeading = heading + gyroYaw * dt

        // Complementary filter
        heading = alpha * gyroHeading + (1 - alpha) * magHeading

        // Normalize to [0, 2π)
        heading = ((heading % (2 * Math.PI).toFloat()) + (2 * Math.PI).toFloat()) % (2 * Math.PI).toFloat()
    }

    private fun computeMagHeading(data: SensorData): Float {
        val gravity = floatArrayOf(data.accX, data.accY, data.accZ)
        val geomagnetic = floatArrayOf(data.magX, data.magY, data.magZ)

        val success = AndroidSensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
        return if (success) {
            AndroidSensorManager.getOrientation(rotationMatrix, orientation)
            orientation[0] // azimuth in radians
        } else {
            atan2(data.magY, data.magX)
        }
    }

    fun reset() {
        heading = 0f
        initialized = false
        lastTimestamp = 0L
    }
}
