package com.parknav.sensor

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Peak-detection based step detector with Weinberg step length model.
 * step_length = K * (a_max - a_min)^0.25
 */
class StepDetector(
    private val weinbergK: Float = 0.5f,
    private val peakThreshold: Float = 1.2f, // m/s² above gravity
    private val minStepInterval: Long = 300_000_000L // 300ms in nanoseconds
) {
    var stepCount: Int = 0
        private set

    private var lastStepTime: Long = 0L
    private var accWindow = mutableListOf<Float>()
    private val windowSize = 30 // ~0.5s at GAME rate

    data class StepEvent(val stepLength: Float, val timestamp: Long)

    private var listener: ((StepEvent) -> Unit)? = null

    fun setOnStepListener(l: (StepEvent) -> Unit) { listener = l }

    fun onAccelerometerUpdate(x: Float, y: Float, z: Float, timestamp: Long) {
        val magnitude = sqrt(x * x + y * y + z * z)
        accWindow.add(magnitude)
        if (accWindow.size > windowSize) accWindow.removeAt(0)

        if (accWindow.size < windowSize) return

        val aMax = accWindow.max()
        val aMin = accWindow.min()
        val mid = accWindow.size / 2
        val midVal = accWindow[mid]

        // Peak at center of window
        val isPeak = midVal == aMax && (aMax - 9.81f) > peakThreshold

        if (isPeak && (timestamp - lastStepTime) > minStepInterval) {
            lastStepTime = timestamp
            stepCount++
            val stepLength = weinbergK * (aMax - aMin).toDouble().pow(0.25).toFloat()
            listener?.invoke(StepEvent(stepLength, timestamp))
        }
    }

    fun reset() {
        stepCount = 0
        accWindow.clear()
        lastStepTime = 0L
    }
}
