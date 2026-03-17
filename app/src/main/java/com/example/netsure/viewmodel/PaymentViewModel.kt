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
 *
 * Uses [Application.getApplicationContext] (safe in AndroidViewModel)
 * so that cleanup can happen from any call-site without a UI Context.
 */
class PaymentViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SMS_DEBUG"
        private const val DETECTION_WINDOW_SECONDS = 90
    }

    private val ussdCaller = UssdCaller()
    private val dbHelper = TransactionDbHelper(application)

    /**
     * Application context stored once — this is SAFE in AndroidViewModel
     * because it outlives all Activities and never leaks.
     */
    private val appContext: Context = application.applicationContext

    // --- UPI ID from QR scan ---
    private val _upiId = MutableStateFlow<String?>(null)
    val upiId: StateFlow<String?> = _upiId.asStateFlow()

    // --- Payment / detection status ---
    private val _paymentStatus = MutableStateFlow<PaymentStatus>(PaymentStatus.Idle)
    val paymentStatus: StateFlow<PaymentStatus> = _paymentStatus.asStateFlow()

    // Internal state — guarded by @Volatile for thread-safety with BroadcastReceiver callbacks
    @Volatile private var smsReceiver: SmsReceiver? = null
    @Volatile private var countdownJob: Job? = null
    @Volatile private var scanStartTimestamp: Long = 0L

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
            startSmsDetection()
        }
        return started
    }

    // ================================================================== //
    //  SMS Detection Window
    // ================================================================== //

    private fun startSmsDetection() {
        // *** ALWAYS clean up first — this is the key fix ***
        Log.d(TAG, "startSmsDetection() — cleaning up any previous session first")
        cleanupDetection()

        scanStartTimestamp = System.currentTimeMillis()
        _paymentStatus.value = PaymentStatus.Detecting(DETECTION_WINDOW_SECONDS)
        Log.d(TAG, "=== SMS DETECTION STARTED at $scanStartTimestamp ===")

        // 1. Register a dynamic BroadcastReceiver
        val receiver = SmsReceiver { smsBody -> onSmsBodyReceived(smsBody) }
        smsReceiver = receiver

        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
            priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        }

        // CRITICAL: SMS_RECEIVED_ACTION is a system broadcast, so on Android 13+ we
        // MUST use RECEIVER_EXPORTED, otherwise the OS silently blocks delivery.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            Log.d(TAG, "Receiver registered with RECEIVER_EXPORTED (API ${Build.VERSION.SDK_INT})")
        } else {
            appContext.registerReceiver(receiver, filter)
            Log.d(TAG, "Receiver registered (pre-Tiramisu, API ${Build.VERSION.SDK_INT})")
        }

        // Visual confirmation
        try {
            Toast.makeText(appContext, "SMS listener active (90s)", Toast.LENGTH_SHORT).show()
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
                cleanupDetection()
            }
        }

        Log.d(TAG, "countdownJob launched: ${countdownJob?.hashCode()}")
    }

    /**
     * Called by [SmsReceiver] on every incoming SMS during the window.
     */
    private fun onSmsBodyReceived(body: String) {
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
        cleanupDetection()

        try {
            Toast.makeText(appContext, "✅ Transaction detected!", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {}
    }

    // ================================================================== //
    //  Cleanup — THE single source of truth for tearing down detection
    // ================================================================== //

    /**
     * Cancels the countdown coroutine, unregisters the SMS receiver, and
     * nulls out all references. Safe to call multiple times or from any thread.
     */
    private fun cleanupDetection() {
        Log.d(TAG, "cleanupDetection() — job=${countdownJob?.hashCode()}, receiver=${smsReceiver?.hashCode()}")

        // 1. Cancel coroutine
        countdownJob?.cancel()
        countdownJob = null
        Log.d(TAG, "  countdownJob cancelled and nulled")

        // 2. Unregister receiver
        val receiver = smsReceiver
        smsReceiver = null
        if (receiver != null) {
            try {
                appContext.unregisterReceiver(receiver)
                Log.d(TAG, "  Receiver unregistered successfully")
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "  Receiver was already unregistered: ${e.message}")
            }
        } else {
            Log.d(TAG, "  No receiver to unregister")
        }

        // 3. Reset timestamp
        scanStartTimestamp = 0L
        Log.d(TAG, "cleanupDetection() — DONE")
    }

    /**
     * Resets the ViewModel to Idle state and cleans up all running processes.
     * Called by the UI when: leaving screen, pressing "Try Again", pressing "Cancel".
     */
    fun resetPaymentStatus() {
        Log.d(TAG, "resetPaymentStatus() — current status: ${_paymentStatus.value}")
        cleanupDetection()
        _paymentStatus.value = PaymentStatus.Idle
        Log.d(TAG, "Payment status reset to Idle — all processes stopped")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "PaymentViewModel.onCleared() — final cleanup")
        cleanupDetection()
    }
}
