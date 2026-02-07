package com.parknav

import android.Manifest
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.parknav.ar.ARTracker
import com.parknav.data.*
import com.parknav.navigation.KalmanFilter
import com.parknav.navigation.PDREngine
import com.parknav.navigation.PathRecorder
import com.parknav.sensor.SensorCollector
import com.parknav.ui.MainScreen
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : ComponentActivity() {

    // Components
    private lateinit var sensorCollector: SensorCollector
    private val arTracker = ARTracker()
    private val pdrEngine = PDREngine()
    private val pathRecorder = PathRecorder()
    private lateinit var dataExporter: DataExporter
    private val kalmanX = KalmanFilter()
    private val kalmanZ = KalmanFilter()

    // State
    private val _fusedPosition = MutableStateFlow(Position(0f, 0f, 0f))
    val fusedPosition: StateFlow<Position> = _fusedPosition

    private val _parkingSpot = MutableStateFlow<ParkingSpot?>(null)
    val parkingSpot: StateFlow<ParkingSpot?> = _parkingSpot

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating

    private val _trajectory = MutableStateFlow<List<TrajectoryPoint>>(emptyList())
    val trajectory: StateFlow<List<TrajectoryPoint>> = _trajectory

    private val _navPath = MutableStateFlow<List<TrajectoryPoint>>(emptyList())
    val navPath: StateFlow<List<TrajectoryPoint>> = _navPath

    private val _navDistance = MutableStateFlow(0f)
    val navDistance: StateFlow<Float> = _navDistance

    private val _navHeading = MutableStateFlow(0f)
    val navHeading: StateFlow<Float> = _navHeading

    // GL surface for ARCore updates
    private var glSurfaceView: GLSurfaceView? = null
    private var arUpdateJob: Job? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) initAR()
        else Toast.makeText(this, "需要相機權限才能使用 ARCore", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorCollector = SensorCollector(this)
        dataExporter = DataExporter(this)

        // Setup GL surface for ARCore (hidden, only for session updates)
        glSurfaceView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setRenderer(object : GLSurfaceView.Renderer {
                override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {}
                override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {}
                override fun onDrawFrame(gl: GL10?) {
                    arTracker.update()
                }
            })
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val position by fusedPosition.collectAsState()
                    val sensor by sensorCollector.data.collectAsState()
                    val spot by parkingSpot.collectAsState()
                    val recording by isRecording.collectAsState()
                    val navigating by isNavigating.collectAsState()
                    val traj by trajectory.collectAsState()
                    val arState by arTracker.trackingState.collectAsState()
                    val navDist by navDistance.collectAsState()
                    val navHead by navHeading.collectAsState()
                    val navP by navPath.collectAsState()

                    MainScreen(
                        position = position,
                        sensorData = sensor,
                        arTrackingState = arState,
                        stepCount = pdrEngine.stepDetector.stepCount,
                        trajectory = traj,
                        parkingSpot = spot,
                        isRecording = recording,
                        isNavigating = navigating,
                        navDistance = navDist,
                        navHeading = navHead,
                        navPath = navP,
                        onMarkParking = ::markParkingSpot,
                        onStartRecording = ::startRecording,
                        onStopRecording = ::stopRecording,
                        onNavigateBack = ::startNavigation,
                        onStopNavigation = ::stopNavigation,
                        onExportData = ::exportData
                    )
                }
            }
        }

        // Request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            initAR()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun initAR() {
        arTracker.initialize(this)
    }

    override fun onResume() {
        super.onResume()
        sensorCollector.start()
        arTracker.resume()
        glSurfaceView?.onResume()
        startFusionLoop()
    }

    override fun onPause() {
        super.onPause()
        sensorCollector.stop()
        arTracker.pause()
        glSurfaceView?.onPause()
        arUpdateJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        arTracker.destroy()
    }

    private fun startFusionLoop() {
        arUpdateJob?.cancel()
        arUpdateJob = lifecycleScope.launch(Dispatchers.Default) {
            while (isActive) {
                val sensorData = sensorCollector.data.value
                pdrEngine.onSensorUpdate(sensorData)

                val arPos = arTracker.position.value
                val pdrPos = pdrEngine.position

                // Fuse: if ARCore is tracking, use Kalman filter to fuse; otherwise use PDR
                val fused = if (arTracker.trackingState.value == "TRACKING") {
                    Position(
                        x = kalmanX.update(arPos.x),
                        y = arPos.y,
                        z = kalmanZ.update(arPos.z)
                    )
                } else {
                    pdrPos
                }

                _fusedPosition.value = fused

                // Record trajectory
                if (pathRecorder.isRecording) {
                    pathRecorder.recordPoint(fused, pdrEngine.orientationEstimator.heading)
                    _trajectory.value = pathRecorder.points
                }

                // Navigation updates
                if (_isNavigating.value) {
                    updateNavigation(fused)
                }

                // Log sensor data
                if (pathRecorder.isRecording) {
                    dataExporter.logReading(SensorReading(
                        timestamp = System.currentTimeMillis(),
                        accX = sensorData.accX, accY = sensorData.accY, accZ = sensorData.accZ,
                        gyroX = sensorData.gyroX, gyroY = sensorData.gyroY, gyroZ = sensorData.gyroZ,
                        magX = sensorData.magX, magY = sensorData.magY, magZ = sensorData.magZ,
                        posX = fused.x, posY = fused.y, posZ = fused.z,
                        heading = pdrEngine.orientationEstimator.heading,
                        stepCount = pdrEngine.stepDetector.stepCount,
                        source = if (arTracker.trackingState.value == "TRACKING") "fused" else "pdr"
                    ))
                }

                delay(50) // ~20Hz update
            }
        }
    }

    private var navTargetIndex = 0

    private fun updateNavigation(currentPos: Position) {
        val path = _navPath.value
        if (path.isEmpty()) return

        // Find closest upcoming waypoint
        while (navTargetIndex < path.size - 1) {
            val dist = currentPos.distanceTo2D(path[navTargetIndex].position)
            if (dist < 1.5f) navTargetIndex++ else break
        }

        val target = path[navTargetIndex].position
        val dist = currentPos.distanceTo2D(target)
        _navDistance.value = dist

        // Compute heading to target
        val dx = target.x - currentPos.x
        val dz = target.z - currentPos.z
        _navHeading.value = kotlin.math.atan2(dx, dz)

        // Check if arrived
        val spot = _parkingSpot.value
        if (spot != null && currentPos.distanceTo2D(spot.position) < 2.0f) {
            Toast.makeText(this@MainActivity, "已到達車位附近！", Toast.LENGTH_LONG).show()
            stopNavigation()
        }
    }

    // --- Actions ---

    private fun markParkingSpot() {
        val pos = _fusedPosition.value
        _parkingSpot.value = ParkingSpot(
            position = pos,
            heading = pdrEngine.orientationEstimator.heading
        )
        Toast.makeText(this, "車位已標記：(${String.format("%.1f", pos.x)}, ${String.format("%.1f", pos.z)})", Toast.LENGTH_SHORT).show()
    }

    private fun startRecording() {
        pathRecorder.startRecording()
        dataExporter.clearLog()
        _isRecording.value = true
        _trajectory.value = emptyList()
    }

    private fun stopRecording() {
        pathRecorder.stopRecording()
        _isRecording.value = false
        _trajectory.value = pathRecorder.points
    }

    private fun startNavigation() {
        val spot = _parkingSpot.value
        if (spot == null) {
            Toast.makeText(this, "請先標記車位", Toast.LENGTH_SHORT).show()
            return
        }
        if (pathRecorder.points.isEmpty()) {
            Toast.makeText(this, "請先記錄路徑", Toast.LENGTH_SHORT).show()
            return
        }
        _navPath.value = pathRecorder.getReversePath()
        navTargetIndex = 0
        _isNavigating.value = true
    }

    private fun stopNavigation() {
        _isNavigating.value = false
        navTargetIndex = 0
    }

    private fun exportData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val trajFile = dataExporter.exportTrajectory(pathRecorder.points)
            val sensorFile = dataExporter.exportSensorData()

            withContext(Dispatchers.Main) {
                if (trajFile != null || sensorFile != null) {
                    val msg = buildString {
                        trajFile?.let { append("軌跡: ${it.name}\n") }
                        sensorFile?.let { append("感測器: ${it.name}") }
                    }
                    Toast.makeText(this@MainActivity, "已匯出:\n$msg", Toast.LENGTH_LONG).show()
                    // Share the trajectory file
                    trajFile?.let { dataExporter.shareFile(it) }
                } else {
                    Toast.makeText(this@MainActivity, "匯出失敗", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
