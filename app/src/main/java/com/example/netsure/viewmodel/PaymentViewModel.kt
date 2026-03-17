package com.example.netsure.viewmodel

import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.provider.Telephony
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.netsure.data.Transaction
import com.example.netsure.data.TransactionDbHelper
import com.example.netsure.services.SmsReceiver
import com.example.netsure.services.UssdCaller
import com.example.netsure.utils.UpiSmsParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sealed hierarchy representing the SMS-detection lifecycle.
 */
sealed class PaymentStatus {
    data object Idle : PaymentStatus()
    data class Detecting(val secondsLeft: Int) : PaymentStatus()
    data class Success(val transaction: Transaction) : PaymentStatus()
    data object Failure : PaymentStatus()
}

/**
 * ViewModel responsible for storing the scanned UPI ID,
 * coordinating the USSD payment trigger, and managing the
 * 90-second SMS detection window.
 */
class PaymentViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SMS_DEBUG"
        private const val DETECTION_WINDOW_SECONDS = 90
    }

    private val ussdCaller = UssdCaller()
    private val dbHelper = TransactionDbHelper(application)

    // --- UPI ID from QR scan ---
    private val _upiId = MutableStateFlow<String?>(null)
    val upiId: StateFlow<String?> = _upiId.asStateFlow()

    // --- Payment / detection status ---
    private val _paymentStatus = MutableStateFlow<PaymentStatus>(PaymentStatus.Idle)
    val paymentStatus: StateFlow<PaymentStatus> = _paymentStatus.asStateFlow()

    // Internal state
    private var smsReceiver: SmsReceiver? = null
    private var countdownJob: Job? = null
    private var scanStartTimestamp: Long = 0L

    /**
     * Called by the QR scanner once a valid UPI ID has been extracted.
     */
    fun onUpiScanned(upiId: String) {
        _upiId.value = upiId
    }

    /**
     * Triggers the *99# USSD call through [UssdCaller] **and** starts the
     * 90-second SMS detection window in parallel.
     *
     * @return true if the USSD call was started, false if CALL_PHONE permission was missing.
     */
    fun triggerUssdPayment(context: Context): Boolean {
        Log.d(TAG, "triggerUssdPayment() called")
        val started = ussdCaller.callUssd(context, "*99#")
        Log.d(TAG, "USSD call started=$started")
        if (started) {
            startSmsDetection(context)
        }
        return started
    }

    // ------------------------------------------------------------------ //
    //  SMS Detection Window
    // ------------------------------------------------------------------ //

    private fun startSmsDetection(context: Context) {
        // Prevent duplicate detection windows
        if (_paymentStatus.value is PaymentStatus.Detecting) {
            Log.w(TAG, "startSmsDetection() skipped — already detecting")
            return
        }

        scanStartTimestamp = System.currentTimeMillis()
        _paymentStatus.value = PaymentStatus.Detecting(DETECTION_WINDOW_SECONDS)
        Log.d(TAG, "=== SMS DETECTION STARTED at $scanStartTimestamp ===")

        // 1. Register a dynamic BroadcastReceiver
        smsReceiver = SmsReceiver { smsBody -> onSmsBodyReceived(smsBody, context) }
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
            priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        }

        // CRITICAL: SMS_RECEIVED_ACTION is a system broadcast, so on Android 13+ we
        // MUST use RECEIVER_EXPORTED, otherwise the OS silently blocks delivery.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(smsReceiver, filter, Context.RECEIVER_EXPORTED)
            Log.d(TAG, "Receiver registered with RECEIVER_EXPORTED (API ${Build.VERSION.SDK_INT})")
        } else {
            context.registerReceiver(smsReceiver, filter)
            Log.d(TAG, "Receiver registered (pre-Tiramisu, API ${Build.VERSION.SDK_INT})")
        }

        // Visual confirmation
        try {
            Toast.makeText(context, "SMS listener active (90s)", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {}

        // 2. Start countdown coroutine
        countdownJob = viewModelScope.launch {
            for (tick in DETECTION_WINDOW_SECONDS downTo 1) {
                // If another path already resolved the status, stop ticking
                if (_paymentStatus.value is PaymentStatus.Success) {
                    Log.d(TAG, "Countdown stopped early — success detected")
                    return@launch
                }
                _paymentStatus.value = PaymentStatus.Detecting(tick)
                if (tick % 10 == 0) {
                    Log.d(TAG, "Countdown: ${tick}s remaining...")
                }
                delay(1000)
            }
            // Time is up without a valid SMS
            Log.d(TAG, "=== 90-SECOND WINDOW EXPIRED ===")
            if (_paymentStatus.value !is PaymentStatus.Success) {
                _paymentStatus.value = PaymentStatus.Failure
            }
            stopSmsDetection(context)
        }
    }

    /**
     * Called by [SmsReceiver] on every incoming SMS during the window.
     */
    private fun onSmsBodyReceived(body: String, context: Context) {
        Log.d(TAG, "onSmsBodyReceived() — body length=${body.length}")
        Log.d(TAG, "SMS body: \"$body\"")

        // Ignore if we already succeeded
        if (_paymentStatus.value is PaymentStatus.Success) {
            Log.d(TAG, "Already in Success state — ignoring this SMS")
            return
        }

        val txn = UpiSmsParser.parse(body)
        if (txn == null) {
            Log.d(TAG, "UpiSmsParser.parse() returned null — SMS is not a UPI debit confirmation")
            return
        }

        Log.d(TAG, "✅ PARSED TRANSACTION: amount=${txn.amount}, ref=${txn.referenceNumber}, target=${txn.target}")

        // Prevent duplicate entries
        if (dbHelper.existsByRef(txn.referenceNumber)) {
            Log.d(TAG, "Duplicate ref ${txn.referenceNumber} — ignoring.")
            return
        }

        // Persist to SQLite
        val rowId = dbHelper.insertTransaction(txn)
        if (rowId == -1L) {
            Log.w(TAG, "DB insert failed (likely duplicate) for ref=${txn.referenceNumber}")
            return
        }

        Log.d(TAG, "Transaction saved to DB with rowId=$rowId")

        _paymentStatus.value = PaymentStatus.Success(txn)
        stopSmsDetection(context)

        try {
            Toast.makeText(context, "✅ Transaction detected!", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {}
    }

    private fun stopSmsDetection(context: Context) {
        Log.d(TAG, "stopSmsDetection() — cleaning up")
        countdownJob?.cancel()
        countdownJob = null
        try {
            smsReceiver?.let {
                context.unregisterReceiver(it)
                Log.d(TAG, "Receiver unregistered successfully")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Receiver unregister failed: ${e.message}")
        }
        smsReceiver = null
    }

    /**
     * Call from the UI to reset back to Idle so the user can try again.
     */
    fun resetPaymentStatus() {
        _paymentStatus.value = PaymentStatus.Idle
        Log.d(TAG, "Payment status reset to Idle")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "PaymentViewModel.onCleared()")
        countdownJob?.cancel()
        // Cannot unregister here because we need a Context; the DisposableEffect
        // in the composable handles cleanup.
    }
}
