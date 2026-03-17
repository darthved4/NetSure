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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.netsure.viewmodel.PaymentViewModel
import kotlinx.coroutines.launch

/**
 * Screen that shows the scanned UPI ID and lets the user trigger
 * the *99# USSD payment menu or cancel back to the home screen.
 */
@Composable
fun ConfirmPaymentScreen(
    paymentViewModel: PaymentViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val upiId by paymentViewModel.upiId.collectAsStateWithLifecycleCompat()
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

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasCallPermission = granted
            if (!granted) {
                scope.launch {
                    snackbarHostState.showSnackbar("CALL_PHONE permission denied.")
                }
            }
        }

    androidx.compose.material3.Scaffold(
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

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (!hasCallPermission) {
                        requestPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
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

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBack
            ) {
                Text("Cancel")
            }
        }
    }
}

/**
 * Small helper to collect StateFlow in a composable without pulling in extra dependencies.
 * Mirrors the behaviour of collectAsStateWithLifecycle for this simple case.
 */
@Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateWithLifecycleCompat(): androidx.compose.runtime.State<T> {
    // For this hackathon prototype we forward to collectAsState, which is lifecycle-aware enough
    // when the ViewModel is scoped to the activity / NavHost.
    return collectAsState(initial = value)
}
