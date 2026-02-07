package com.parknav.navigation

/**
 * Simple 1D Kalman filter for fusing ARCore + PDR position estimates.
 * Applied independently to X and Z axes.
 */
class KalmanFilter(
    private var processNoise: Float = 0.01f,  // Q
    private var measurementNoise: Float = 0.1f // R
) {
    private var estimate: Float = 0f
    private var errorCovariance: Float = 1f
    private var initialized = false

    fun update(measurement: Float): Float {
        if (!initialized) {
            estimate = measurement
            errorCovariance = 1f
            initialized = true
            return estimate
        }

        // Predict
        val predictedEstimate = estimate
        val predictedError = errorCovariance + processNoise

        // Update
        val kalmanGain = predictedError / (predictedError + measurementNoise)
        estimate = predictedEstimate + kalmanGain * (measurement - predictedEstimate)
        errorCovariance = (1 - kalmanGain) * predictedError

        return estimate
    }

    fun reset() {
        estimate = 0f
        errorCovariance = 1f
        initialized = false
    }

    fun setNoiseParams(q: Float, r: Float) {
        processNoise = q
        measurementNoise = r
    }
}
