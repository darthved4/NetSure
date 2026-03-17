package com.example.netsure.services

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat

/**
 * Small service class that knows how to dial a USSD code such as *99#.
 *
 * The actual permission request for CALL_PHONE should be handled by the UI layer.
 * This class simply checks whether the permission is already granted and, if so,
 * starts an ACTION_CALL intent.
 */
class UssdCaller {

    /**
     * Triggers a phone call to the given USSD code (e.g., "*99#") using ACTION_CALL.
     *
     * @param context A context used to start the call activity.
     * @param ussdCode The raw USSD string (e.g., "*99#").
     * @return true if the call intent was started, false if permission was missing.
     */
    fun callUssd(context: Context, ussdCode: String): Boolean {
        // Ensure CALL_PHONE permission has already been granted by the UI.
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCallPermission) {
            // The caller (e.g., ConfirmPaymentScreen) should request permission first.
            return false
        }

        // Encode the USSD string into a tel: URI (e.g., tel:*99%23).
        val encodedUssd = Uri.encode(ussdCode)
        val uri = Uri.parse("tel:$encodedUssd")

        val intent = Intent(Intent.ACTION_CALL, uri).apply {
            // Ensure this starts in a new task if needed.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        return true
    }
}

