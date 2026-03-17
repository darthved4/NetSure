package com.example.netsure.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.netsure.services.UssdCaller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel responsible for storing the scanned UPI ID and
 * coordinating the USSD payment trigger.
 */
class PaymentViewModel(application: Application) : AndroidViewModel(application) {

    private val ussdCaller = UssdCaller()

    private val _upiId = MutableStateFlow<String?>(null)
    val upiId: StateFlow<String?> = _upiId.asStateFlow()

    /**
     * Called by the QR scanner once a valid UPI ID has been extracted.
     */
    fun onUpiScanned(upiId: String) {
        _upiId.value = upiId
    }

    /**
     * Triggers the *99# USSD call through [UssdCaller].
     *
     * @return true if the call was started, false if CALL_PHONE permission was missing.
     */
    fun triggerUssdPayment(context: Context): Boolean {
        // For this prototype we always dial the standard *99# UPI menu.
        return ussdCaller.callUssd(context, "*99#")
    }
}

