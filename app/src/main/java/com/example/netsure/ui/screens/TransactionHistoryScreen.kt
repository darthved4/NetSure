package com.example.netsure.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Transaction(
    val upiId: String,
    val date: String,
    val time: String,
    val to: String,
    val amount: String
)

@Composable
fun TransactionHistoryScreen() {

    val transactions = listOf(
        Transaction("UPI123456789", "17 Mar 2026", "10:45 AM", "Rahul Sharma", "-₹500"),
        Transaction("UPI987654321", "16 Mar 2026", "08:20 PM", "Amazon", "-₹1299"),
        Transaction("UPI567891234", "15 Mar 2026", "02:10 PM", "Swiggy", "-₹350")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {

        // 🔥 TOP SPACE (ADDED)
        Spacer(modifier = Modifier.weight(0.1f))

        Column {

            // HEADER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "NETSURE",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "When network fails, payments dont.",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TRANSACTION LIST TITLE
            Text(
                text = "TRANSACTIONS",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // LIST
            LazyColumn {
                items(transactions) { txn ->
                    TransactionItem(txn)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // 🔥 BOTTOM SPACE (ADDED)
        Spacer(modifier = Modifier.weight(0.1f))
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White)
            .padding(10.dp)
    ) {

        Text("UPI Ref ID: ${transaction.upiId}", color = Color.White, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(4.dp))

        Text("Date: ${transaction.date}", color = Color.White, fontSize = 12.sp)
        Text("Time: ${transaction.time}", color = Color.White, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(4.dp))

        Text("To: ${transaction.to}", color = Color.White, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = transaction.amount,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}