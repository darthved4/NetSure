package com.example.netsure.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.netsure.viewmodel.NetworkViewModel

/**
 * Home screen of the prototype:
 *  - Shows basic app title and description
 *  - Hosts the "Scan QR" button
 *  - Displays the [NetworkAnalyzerWidget] with live network information
 */
@Composable
fun HomeScreen(
    networkViewModel: NetworkViewModel,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val networkState by networkViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "NetSure UPI Offline",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Prototype app to demonstrate offline UPI payments using the *99# USSD flow.",
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = onScanClick,
            modifier = Modifier
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Text(text = "Scan QR")
        }

        Spacer(modifier = Modifier.height(8.dp))

        NetworkAnalyzerWidget(
            typeLabel = networkState.typeLabel,
            signalLevel = networkState.signalLevel,
            qualityScore = networkState.qualityScore,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Stateless widget that visualizes basic network information such as:
 *  - current network type (Wi‑Fi / Cellular / Offline)
 *  - an approximate signal level bar
 *  - a simple numeric quality score
 */


@Composable
fun NetworkAnalyzerWidget(
    typeLabel: String,
    signalLevel: Int,
    qualityScore: Int,
    modifier: Modifier = Modifier
) {
    // Decide status based on quality
    val isStable = qualityScore >= 40

    val statusText = if (isStable) "Stable" else "Unstable"

    val statusColor = if (isStable) {
        androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
    } else {
        androidx.compose.ui.graphics.Color(0xFFEA1F10) // Red
    }

    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(50) // pill shape
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        // Status Dot
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    color = statusColor,
                    shape = RoundedCornerShape(50)
                )
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Text
        Text(
            text = "Network: $statusText",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
@Composable
private fun SignalBarRow(level: Int) {
    val clamped = level.coerceIn(0, 4)
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(5) { index ->
            val isActive = index <= clamped
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .height(((index + 1) * 6).dp)
                    .weight(1f)
                    .padding(vertical = 2.dp)
                    .then(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
            )
        }
    }
}

