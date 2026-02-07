package com.parknav.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager as AndroidSensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SensorData(
    val accX: Float = 0f, val accY: Float = 0f, val accZ: Float = 0f,
    val gyroX: Float = 0f, val gyroY: Float = 0f, val gyroZ: Float = 0f,
    val magX: Float = 0f, val magY: Float = 0f, val magZ: Float = 0f,
    val timestamp: Long = 0L
)

class SensorCollector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as AndroidSensorManager

    private val _data = MutableStateFlow(SensorData())
    val data: StateFlow<SensorData> = _data

    private var acc = floatArrayOf(0f, 0f, 0f)
    private var gyro = floatArrayOf(0f, 0f, 0f)
    private var mag = floatArrayOf(0f, 0f, 0f)

    fun start() {
        val delay = AndroidSensorManager.SENSOR_DELAY_GAME
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, delay)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
            sensorManager.registerListener(this, it, delay)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
            sensorManager.registerListener(this, it, delay)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                acc = event.values.copyOf()
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyro = event.values.copyOf()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                mag = event.values.copyOf()
            }
        }
        _data.value = SensorData(
            accX = acc[0], accY = acc[1], accZ = acc[2],
            gyroX = gyro[0], gyroY = gyro[1], gyroZ = gyro[2],
            magX = mag[0], magY = mag[1], magZ = mag[2],
            timestamp = event.timestamp
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
