package com.example.netsure.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import android.widget.Toast

/**
 * BroadcastReceiver that captures incoming SMS messages and forwards the
 * message body to a callback for parsing. This receiver is registered and
 * unregistered dynamically by [PaymentViewModel] so it only runs during
 * the 90-second detection window.
 */
class SmsReceiver(
    private val onSmsReceived: (String) -> Unit
) : BroadcastReceiver() {

    companion object {
        private const val TAG = "SMS_DEBUG"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, ">>> onReceive() TRIGGERED — action=${intent?.action}")

        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.w(TAG, "Ignoring intent with unexpected action: ${intent?.action}")
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        Log.d(TAG, "Messages from intent: count=${messages?.size ?: 0}")

        if (messages.isNullOrEmpty()) {
            Log.w(TAG, "No SmsMessage objects extracted from intent")
            return
        }

        // Combine all message parts into a single body
        val fullBody = messages.joinToString("") { it.messageBody ?: "" }
        val sender = messages.firstOrNull()?.originatingAddress ?: "unknown"
        Log.d(TAG, "SMS from=$sender, body=\"$fullBody\"")

        // Visual confirmation via Toast
        try {
            Toast.makeText(
                context,
                "SMS_DEBUG: received from $sender",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Log.w(TAG, "Could not show Toast: ${e.message}")
        }

        if (fullBody.isNotBlank()) {
            Log.d(TAG, "Forwarding SMS body to callback for parsing...")
            onSmsReceived(fullBody)
        } else {
            Log.w(TAG, "SMS body is blank — skipping")
        }
    }
}
