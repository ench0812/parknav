package com.parknav.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parknav.data.Position
import com.parknav.data.ParkingSpot
import com.parknav.data.TrajectoryPoint

@Composable
fun NavigationOverlay(
    navDistance: Float,
    navHeading: Float,
    currentHeading: Float,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🚗 導航中", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // Direction indicator
            val relativeAngle = navHeading - currentHeading
            val direction = when {
                relativeAngle > -0.4f && relativeAngle < 0.4f -> "⬆️ 直走"
                relativeAngle >= 0.4f && relativeAngle < 1.5f -> "↗️ 右前方"
                relativeAngle >= 1.5f -> "➡️ 右轉"
                relativeAngle <= -0.4f && relativeAngle > -1.5f -> "↖️ 左前方"
                else -> "⬅️ 左轉"
            }
            Text(direction, fontSize = 32.sp)

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "距離: ${String.format("%.1f", navDistance)} 公尺",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("停止導航")
            }
        }
    }
}
