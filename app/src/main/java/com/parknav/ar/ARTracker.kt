package com.parknav.ar

import android.app.Activity
import com.google.ar.core.*
import com.parknav.data.Position
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Wraps ARCore Session to provide VIO-based position tracking.
 */
class ARTracker {

    private var session: Session? = null
    private var isTracking = false

    private val _position = MutableStateFlow(Position(0f, 0f, 0f))
    val position: StateFlow<Position> = _position

    private val _trackingState = MutableStateFlow("NOT_INITIALIZED")
    val trackingState: StateFlow<String> = _trackingState

    fun initialize(activity: Activity) {
        try {
            session = Session(activity).apply {
                val config = Config(this).apply {
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    planeFindingMode = Config.PlaneFindingMode.DISABLED
                    lightEstimationMode = Config.LightEstimationMode.DISABLED
                    focusMode = Config.FocusMode.AUTO
                }
                configure(config)
            }
            _trackingState.value = "INITIALIZED"
        } catch (e: Exception) {
            _trackingState.value = "ERROR: ${e.message}"
        }
    }

    fun resume() {
        try {
            session?.resume()
            isTracking = true
        } catch (e: Exception) {
            _trackingState.value = "ERROR: ${e.message}"
        }
    }

    fun pause() {
        session?.pause()
        isTracking = false
    }

    /**
     * Call from GL thread or dedicated update loop.
     * Returns current position or null if not tracking.
     */
    fun update(): Position? {
        val s = session ?: return null
        return try {
            val frame = s.update()
            val camera = frame.camera

            when (camera.trackingState) {
                TrackingState.TRACKING -> {
                    _trackingState.value = "TRACKING"
                    val pose = camera.pose
                    val pos = Position(pose.tx(), pose.ty(), pose.tz())
                    _position.value = pos
                    pos
                }
                TrackingState.PAUSED -> {
                    _trackingState.value = "PAUSED"
                    null
                }
                TrackingState.STOPPED -> {
                    _trackingState.value = "STOPPED"
                    null
                }
                else -> null
            }
        } catch (e: Exception) {
            _trackingState.value = "ERROR: ${e.message}"
            null
        }
    }

    fun destroy() {
        session?.close()
        session = null
        isTracking = false
        _trackingState.value = "DESTROYED"
    }

    fun isAvailable(): Boolean = session != null
}
