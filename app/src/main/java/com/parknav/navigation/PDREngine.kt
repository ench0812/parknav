package com.parknav.navigation

import com.parknav.data.Position
import com.parknav.sensor.OrientationEstimator
import com.parknav.sensor.SensorData
import com.parknav.sensor.StepDetector
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pedestrian Dead Reckoning engine.
 * Combines step detection + orientation estimation to compute trajectory.
 */
class PDREngine {

    val stepDetector = StepDetector()
    val orientationEstimator = OrientationEstimator()

    private var posX = 0f
    private var posZ = 0f

    val position: Position get() = Position(posX, 0f, posZ)

    init {
        stepDetector.setOnStepListener { event ->
            val heading = orientationEstimator.heading
            // In our coordinate system: X = east, Z = south (screen coordinates for 2D map)
            posX += event.stepLength * sin(heading)
            posZ += event.stepLength * cos(heading)
        }
    }

    fun onSensorUpdate(data: SensorData) {
        stepDetector.onAccelerometerUpdate(data.accX, data.accY, data.accZ, data.timestamp)
        orientationEstimator.update(data)
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
