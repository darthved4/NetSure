package com.example.netsure.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.netsure.viewmodel.NetworkViewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable

@Composable
fun HomeScreen(
    networkViewModel: NetworkViewModel,
    onScanClick: () -> Unit,
    onTransactionHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val networkState by networkViewModel.uiState.collectAsStateWithLifecycle()
    val isStable = networkState.qualityScore >= 40
    val statusText = if (isStable) "Stable" else "Unstable"
    val statusColor = if (isStable) Color.Green else Color.Red

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(12.dp)
    ) {

        // 🔥 TOP SPACE
        Spacer(modifier = Modifier.weight(0.15f))

        Column {

            // 🔷 TOP CARD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "NETSURE",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "When network fails, payments dont.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .background(Color.LightGray, RoundedCornerShape(50))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(statusColor, CircleShape)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "Network : $statusText",
                            color = Color.Black,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔷 MAIN MENU
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White)
            ) {
                SectionHeader("MAIN MENU")

                MenuItem("1. Send Money") { onScanClick() }
                MenuItem("2. Receive Money") {}
                MenuItem("3. Check Balance") {}
                MenuItem("4. Transaction History") {
                    onTransactionHistoryClick()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔷 BANK DETAILS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White)
            ) {
                SectionHeader("BANK DETAILS")

                Column(modifier = Modifier.padding(8.dp)) {

                    BankDetailRow("BANK NAME:", "Bank of India")
                    BankDetailRow("ACCOUNT NO:", "**** **** 9378")
                    BankDetailRow("AVAILABLE BALANCE :", "₹ **,***.**")

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "[CHECK BALANCE]",
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 🔥 BOTTOM SPACE
        Spacer(modifier = Modifier.weight(0.15f))
    }
}

@Composable
fun SectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.LightGray)
            .padding(8.dp)
    ) {
        Text(
            text = title,
            color = Color.Black,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun MenuItem(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(1.dp, Color.White)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun BankDetailRow(label: String, value: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = label,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                color = Color.White
            )
        }
    }
}