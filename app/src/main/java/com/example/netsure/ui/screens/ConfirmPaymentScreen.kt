package com.example.netsure.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.netsure.viewmodel.PaymentStatus
import com.example.netsure.viewmodel.PaymentViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen that shows the scanned UPI ID and lets the user trigger
 * the *99# USSD payment menu or cancel back to the home screen.
 * Now also shows a 90-second SMS detection indicator and transaction results.
 */
@Composable
fun ConfirmPaymentScreen(
    paymentViewModel: PaymentViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val upiId by paymentViewModel.upiId.collectAsStateWithLifecycle()
    val paymentStatus by paymentViewModel.paymentStatus.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Prevent repeated clipboard writes on recomposition.
    var lastCopiedUpiId by remember { mutableStateOf<String?>(null) }

    // When we receive a valid UPI ID, copy it to clipboard automatically.
    LaunchedEffect(upiId) {
        val value = upiId
        if (!value.isNullOrBlank() && value != lastCopiedUpiId) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("UPI ID", value))
            lastCopiedUpiId = value
        }
    }

    var hasCallPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECEIVE_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val requestCallPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasCallPermission = granted
            if (!granted) {
                scope.launch { snackbarHostState.showSnackbar("CALL_PHONE permission denied.") }
            }
        }

    val requestSmsPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasSmsPermission = granted
            if (!granted) {
                scope.launch { snackbarHostState.showSnackbar("RECEIVE_SMS permission denied. Transaction detection won't work.") }
            }
        }

    // Request SMS permission on first composition if not already granted
    LaunchedEffect(Unit) {
        if (!hasSmsPermission) {
            requestSmsPermission.launch(Manifest.permission.RECEIVE_SMS)
        }
    }

    // Reset payment status when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            paymentViewModel.resetPaymentStatus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Confirm Payment",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "UPI ID:",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = upiId ?: "(not available)",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "This will open the *99# UPI USSD menu in your phone app.",
                style = MaterialTheme.typography.bodySmall
            )

            // ---------- Action / status area ----------

            when (val status = paymentStatus) {

                is PaymentStatus.Idle -> {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (!hasCallPermission) {
                                requestCallPermission.launch(Manifest.permission.CALL_PHONE)
                            } else {
                                val started = paymentViewModel.triggerUssdPayment(context)
                                if (!started) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Unable to start USSD call. Check permissions."
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Call USSD (*99#)")
                    }
                }

                is PaymentStatus.Detecting -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Detecting transaction… ${status.secondsLeft}s remaining",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is PaymentStatus.Success -> {
                    val txn = status.transaction
                    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        .format(Date(txn.timestampMs))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "✅ Transaction Successful",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            DetailRow("Amount", "₹${txn.amount}")
                            DetailRow("Debited to", txn.target)
                            DetailRow("UPI Ref No.", txn.referenceNumber)
                            DetailRow("Date & Time", dateStr)
                        }
                    }
                }

                is PaymentStatus.Failure -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Transaction might have failed. Please wait for confirmation before trying again.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            paymentViewModel.resetPaymentStatus()
                        }
                    ) {
                        Text("Try Again")
                    }
                }
            }

            // Cancel / Back button (always visible)
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBack
            ) {
                Text(if (paymentStatus is PaymentStatus.Success) "Done" else "Cancel")
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}
