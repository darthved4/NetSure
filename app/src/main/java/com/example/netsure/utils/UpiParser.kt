package com.example.netsure.utils

import android.net.Uri

/**
 * Utility responsible for parsing UPI QR payloads and extracting the UPI ID (PA parameter).
 *
 * Example payload:
 *  upi://pay?pa=merchant@upi&pn=MerchantName&am=100&cu=INR
 */
object UpiParser {

    /**
     * Extracts the UPI ID (value of "pa" query parameter) from a UPI payload string.
     *
     * @param payload The raw QR code string content.
     * @return The UPI ID (e.g., "merchant@upi") or null if it cannot be parsed.
     */
    fun extractUpiIdFromPayload(payload: String): String? {
        // Basic validation to ensure this looks like a UPI payment URI.
        if (!payload.startsWith(prefix = "upi://pay", ignoreCase = true)) {
            return null
        }

        return try {
            val uri = Uri.parse(payload)
            // "pa" stands for "payment address" – the UPI ID we care about.
            uri.getQueryParameter("pa")
        } catch (e: Exception) {
            null
        }
    }
}

