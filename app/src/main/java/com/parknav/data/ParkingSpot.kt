package com.parknav.data

data class Position(val x: Float, val y: Float, val z: Float) {
    fun distanceTo(other: Position): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun distanceTo2D(other: Position): Float {
        val dx = x - other.x
        val dz = z - other.z
        return kotlin.math.sqrt(dx * dx + dz * dz)
    }
}

data class TrajectoryPoint(
    val position: Position,
    val timestamp: Long = System.currentTimeMillis(),
    val heading: Float = 0f, // radians
    val source: String = "fused" // "arcore", "pdr", "fused"
)

data class ParkingSpot(
    val position: Position,
    val heading: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val label: String = "我的車位"
)

data class SensorReading(
    val timestamp: Long,
    val accX: Float, val accY: Float, val accZ: Float,
    val gyroX: Float, val gyroY: Float, val gyroZ: Float,
    val magX: Float, val magY: Float, val magZ: Float,
    val posX: Float, val posY: Float, val posZ: Float,
    val heading: Float,
    val stepCount: Int,
    val source: String
)
