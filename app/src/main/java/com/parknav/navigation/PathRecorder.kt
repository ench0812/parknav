package com.parknav.navigation

import com.parknav.data.Position
import com.parknav.data.TrajectoryPoint

class PathRecorder {

    private val _points = mutableListOf<TrajectoryPoint>()
    val points: List<TrajectoryPoint> get() = _points.toList()

    var isRecording = false
        private set

    private var lastRecordTime = 0L
    private val minIntervalMs = 200L // record at most 5 points/sec

    fun startRecording() {
        _points.clear()
        isRecording = true
        lastRecordTime = 0L
    }

    fun stopRecording() {
        isRecording = false
    }

    fun recordPoint(position: Position, heading: Float, source: String = "fused") {
        if (!isRecording) return
        val now = System.currentTimeMillis()
        if (now - lastRecordTime < minIntervalMs) return
        lastRecordTime = now
        _points.add(TrajectoryPoint(position, now, heading, source))
    }

    /**
     * Returns points in reverse order for back-navigation.
     */
    fun getReversePath(): List<TrajectoryPoint> = _points.reversed()

    fun clear() {
        _points.clear()
        isRecording = false
    }
}
