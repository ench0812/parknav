package com.parknav.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.parknav.data.Position
import com.parknav.data.ParkingSpot
import com.parknav.data.TrajectoryPoint
import kotlin.math.max
import kotlin.math.min

@Composable
fun TrajectoryCanvas(
    trajectory: List<TrajectoryPoint>,
    currentPosition: Position,
    parkingSpot: ParkingSpot?,
    navPath: List<TrajectoryPoint> = emptyList(),
    navHeading: Float = 0f,
    isNavigating: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E))
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        // Compute scale from all points
        val allPoints = trajectory.map { it.position } +
                listOfNotNull(parkingSpot?.position) +
                listOf(currentPosition)

        val scale = if (allPoints.size > 1) {
            val xs = allPoints.map { it.x }
            val zs = allPoints.map { it.z }
            val rangeX = max(xs.max() - xs.min(), 1f)
            val rangeZ = max(zs.max() - zs.min(), 1f)
            val maxRange = max(rangeX, rangeZ)
            min(size.width, size.height) * 0.4f / maxRange
        } else {
            50f // default pixels per meter
        }

        // Offset so current position is centered
        val offsetX = centerX - currentPosition.x * scale
        val offsetZ = centerY - currentPosition.z * scale

        fun toScreen(pos: Position): Offset =
            Offset(pos.x * scale + offsetX, pos.z * scale + offsetZ)

        // Draw grid
        drawGrid(centerX, centerY, size.width, size.height)

        // Draw recorded trajectory
        if (trajectory.size > 1) {
            val path = Path()
            val first = toScreen(trajectory.first().position)
            path.moveTo(first.x, first.y)
            trajectory.drop(1).forEach { point ->
                val p = toScreen(point.position)
                path.lineTo(p.x, p.y)
            }
            drawPath(path, Color(0xFF00D2FF), style = Stroke(width = 3f))
        }

        // Draw nav path
        if (isNavigating && navPath.size > 1) {
            val path = Path()
            val first = toScreen(navPath.first().position)
            path.moveTo(first.x, first.y)
            navPath.drop(1).forEach { point ->
                val p = toScreen(point.position)
                path.lineTo(p.x, p.y)
            }
            drawPath(path, Color(0xFFFF6B35).copy(alpha = 0.5f), style = Stroke(width = 5f))
        }

        // Draw parking spot
        parkingSpot?.let {
            val p = toScreen(it.position)
            drawCircle(Color(0xFFFFD700), radius = 12f, center = p)
            drawCircle(Color(0xFFFF6B35), radius = 8f, center = p)
            // P label - draw a small square
            drawRect(Color(0xFFFFD700), topLeft = Offset(p.x - 6f, p.y - 14f),
                size = androidx.compose.ui.geometry.Size(12f, 4f))
        }

        // Draw current position (blue dot with direction indicator)
        val cur = toScreen(currentPosition)
        drawCircle(Color(0xFF4FC3F7), radius = 10f, center = cur)
        drawCircle(Color.White, radius = 5f, center = cur)

        // Draw direction arrow when navigating
        if (isNavigating) {
            rotate(Math.toDegrees(navHeading.toDouble()).toFloat(), pivot = cur) {
                val arrowPath = Path().apply {
                    moveTo(cur.x, cur.y - 25f)
                    lineTo(cur.x - 8f, cur.y - 10f)
                    lineTo(cur.x + 8f, cur.y - 10f)
                    close()
                }
                drawPath(arrowPath, Color(0xFFFF6B35))
            }
        }
    }
}

private fun DrawScope.drawGrid(cx: Float, cy: Float, w: Float, h: Float) {
    val gridColor = Color(0xFF2A2A4A)
    val spacing = 50f
    var x = cx % spacing
    while (x < w) {
        drawLine(gridColor, Offset(x, 0f), Offset(x, h))
        x += spacing
    }
    var y = cy % spacing
    while (y < h) {
        drawLine(gridColor, Offset(0f, y), Offset(w, y))
        y += spacing
    }
}
