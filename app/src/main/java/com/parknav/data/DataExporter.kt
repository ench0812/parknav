package com.parknav.data

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class DataExporter(private val context: Context) {

    private val sensorLog = mutableListOf<SensorReading>()

    fun logReading(reading: SensorReading) {
        sensorLog.add(reading)
    }

    fun exportTrajectory(points: List<TrajectoryPoint>): File? {
        return try {
            val file = createFile("trajectory")
            FileWriter(file).use { writer ->
                writer.write("timestamp,x,y,z,heading,source\n")
                for (p in points) {
                    writer.write("${p.timestamp},${p.position.x},${p.position.y},${p.position.z},${p.heading},${p.source}\n")
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportSensorData(): File? {
        return try {
            val file = createFile("sensors")
            FileWriter(file).use { writer ->
                writer.write("timestamp,accX,accY,accZ,gyroX,gyroY,gyroZ,magX,magY,magZ,posX,posY,posZ,heading,stepCount,source\n")
                for (r in sensorLog) {
                    writer.write("${r.timestamp},${r.accX},${r.accY},${r.accZ},${r.gyroX},${r.gyroY},${r.gyroZ},${r.magX},${r.magY},${r.magZ},${r.posX},${r.posY},${r.posZ},${r.heading},${r.stepCount},${r.source}\n")
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun clearLog() {
        sensorLog.clear()
    }

    private fun createFile(prefix: String): File {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        val dir = File(context.getExternalFilesDir(null), "parknav_exports")
        dir.mkdirs()
        return File(dir, "${prefix}_$timestamp.csv")
    }

    fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "匯出數據").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
