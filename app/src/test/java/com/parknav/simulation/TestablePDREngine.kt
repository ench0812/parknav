package com.parknav.simulation

import com.parknav.data.Position
import com.parknav.sensor.SensorData
import com.parknav.sensor.StepDetector
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure-JVM PDREngine that uses TestableOrientationEstimator instead of
 * the production OrientationEstimator (which depends on Android APIs).
 * Logic is identical to the production PDREngine.
 */
class TestablePDREngine {

    val stepDetector = StepDetector()
    val orientationEstimator = TestableOrientationEstimator()

    private var posX = 0f
    private var posZ = 0f

    val position: Position get() = Position(posX, 0f, posZ)
    val stepCount: Int get() = stepDetector.stepCount

    init {
        stepDetector.setOnStepListener { event ->
            val heading = orientationEstimator.heading
            posX += event.stepLength * sin(heading)
            posZ += event.stepLength * cos(heading)
        }
    }

    fun onSensorUpdate(data: SensorData) {
        orientationEstimator.update(data)
        stepDetector.onAccelerometerUpdate(data.accX, data.accY, data.accZ, data.timestamp)
    }

    fun reset() {
        posX = 0f
        posZ = 0f
        stepDetector.reset()
        orientationEstimator.reset()
    }

    fun setPosition(x: Float, z: Float) {
        posX = x
        posZ = z
    }
}
