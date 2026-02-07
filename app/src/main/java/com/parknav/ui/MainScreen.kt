package com.parknav.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parknav.data.Position
import com.parknav.data.ParkingSpot
import com.parknav.data.TrajectoryPoint
import com.parknav.sensor.SensorData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    position: Position,
    sensorData: SensorData,
    arTrackingState: String,
    stepCount: Int,
    trajectory: List<TrajectoryPoint>,
    parkingSpot: ParkingSpot?,
    isRecording: Boolean,
    isNavigating: Boolean,
    navDistance: Float,
    navHeading: Float,
    navPath: List<TrajectoryPoint>,
    onMarkParking: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onNavigateBack: () -> Unit,
    onStopNavigation: () -> Unit,
    onExportData: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top: Position display
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📍 當前位置", style = MaterialTheme.typography.labelMedium)
                Text(
                    "X: ${fmt(position.x)}  Y: ${fmt(position.y)}  Z: ${fmt(position.z)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                if (parkingSpot != null) {
                    val dist = position.distanceTo2D(parkingSpot.position)
                    Text("🅿️ 車位距離: ${String.format("%.1f", dist)}m", fontSize = 12.sp)
                }
            }
        }

        // Navigation overlay
        if (isNavigating) {
            NavigationOverlay(
                navDistance = navDistance,
                navHeading = navHeading,
                currentHeading = 0f, // simplified
                onStop = onStopNavigation
            )
        }

        // Middle: Trajectory map
        TrajectoryCanvas(
            trajectory = trajectory,
            currentPosition = position,
            parkingSpot = parkingSpot,
            navPath = navPath,
            navHeading = navHeading,
            isNavigating = isNavigating,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        // Sensor panel
        SensorPanel(
            sensorData = sensorData,
            arTrackingState = arTrackingState,
            stepCount = stepCount,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        // Bottom: Action buttons
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onMarkParking,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Text("🅿️ 標記車位", fontSize = 13.sp)
                    }

                    if (!isRecording) {
                        Button(
                            onClick = onStartRecording,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("⏺ 開始記錄", fontSize = 13.sp)
                        }
                    } else {
                        Button(
                            onClick = onStopRecording,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("⏹ 停止記錄", fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isNavigating) {
                        Button(
                            onClick = onNavigateBack,
                            modifier = Modifier.weight(1f),
                            enabled = parkingSpot != null && trajectory.isNotEmpty()
                        ) {
                            Text("🧭 導航回車", fontSize = 13.sp)
                        }
                    } else {
                        Button(
                            onClick = onStopNavigation,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("⏹ 停止導航", fontSize = 13.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onExportData,
                        modifier = Modifier.weight(1f),
                        enabled = trajectory.isNotEmpty()
                    ) {
                        Text("📊 匯出數據", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun fmt(v: Float): String = String.format("%+.2f", v)
