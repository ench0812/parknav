package com.parknav.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parknav.sensor.SensorData

@Composable
fun SensorPanel(
    sensorData: SensorData,
    arTrackingState: String,
    stepCount: Int,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📡 感測器面板", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (expanded) "▲ 收合" else "▼ 展開",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Always show summary
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("步數: $stepCount", fontSize = 12.sp)
                Text("ARCore: $arTrackingState", fontSize = 12.sp)
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    SensorRow("加速計", sensorData.accX, sensorData.accY, sensorData.accZ, "m/s²")
                    SensorRow("陀螺儀", sensorData.gyroX, sensorData.gyroY, sensorData.gyroZ, "rad/s")
                    SensorRow("磁力計", sensorData.magX, sensorData.magY, sensorData.magZ, "μT")
                }
            }
        }
    }
}

@Composable
private fun SensorRow(label: String, x: Float, y: Float, z: Float, unit: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
        Text(
            "X: ${fmt(x)}  Y: ${fmt(y)}  Z: ${fmt(z)}  $unit",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun fmt(v: Float): String = String.format("%+8.3f", v)
