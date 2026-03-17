package com.example.netsure.utils

import android.util.Log
import com.example.netsure.data.Transaction

/**
 * Regex-based parser that extracts UPI transaction details from bank SMS messages.
 *
 * Handles common SMS formats such as:
 *  - "Rs. 250.00 debited from a/c **1234 on 01-10-23 to VPA someone@upi Ref No 12345678901"
 *  - "INR 500 debited from A/C XX5678. UPI Ref: 98765432109. Payee: merchant@bank"
 *  - "Your a/c X1234 debited by Rs.100.00 on 17-Mar-2026. UPI Ref 12345678901."
 */
object UpiSmsParser {

    private const val TAG = "SMS_DEBUG"

    // --- Amount patterns ---
    private val amountPatterns = listOf(Regex("""(?:₹|Rs\.?|INR)\s?([\d,]+(?:\.\d{1,2})?)"""))

    // --- UPI Reference number (typically 11-12 digits) ---
    private val refPatterns = listOf(
        Regex("""UPI Ref(?: No)?[:\s]\s*(\d{11,12})""")
    )

    // --- Account / target patterns ---
    private val accountPatterns = listOf(

        // 1️ Merchant name (BEST match)
        Regex("""credited to\s+([A-Za-z0-9\s]+?)\s+via UPI""", RegexOption.IGNORE_CASE),

        // 2️ Fallback: UPI ID (if present)
        Regex("""([a-zA-Z0-9.\-_]+@[a-zA-Z]+)"""),

        // 3️ Last fallback: account number
        Regex("""[Aa]/[Cc]\s*(?:XX|X)?(\d{4})""")
    )

    // --- Date patterns ---
    private val datePatterns = listOf(
        Regex("""(\d{1,2}[-/]\d{1,2}[-/]\d{2,4})"""),
        Regex("""(\d{1,2}-[A-Za-z]{3}-\d{2,4})""")
    )

    // --- Debit keywords to filter only debit SMS ---
    private val debitKeywords = Regex(
        """debit|debited|withdrawn|sent|transferred|paid""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Try to parse a UPI debit confirmation from [smsBody].
     * Returns a [Transaction] if all key fields (amount + reference) are found, else null.
     */
    fun parse(smsBody: String): Transaction? {
        Log.d(TAG, "--- UpiSmsParser.parse() START ---")
        Log.d(TAG, "Input: \"$smsBody\"")

        // Quick gate: must look like a debit SMS
        if (!debitKeywords.containsMatchIn(smsBody)) {
            Log.d(TAG, "❌ FAILED: no debit keyword found (debit/debited/withdrawn/sent/transferred/paid)")
            return null
        }
        Log.d(TAG, "✓ Debit keyword matched")

        val amount = extractFirst(amountPatterns, smsBody, "amount")
        if (amount == null) {
            Log.d(TAG, "❌ FAILED: could not extract amount (pattern: ₹/Rs./INR followed by number)")
            return null
        }
        Log.d(TAG, "✓ Amount extracted: $amount")

        val refNumber = extractFirst(refPatterns, smsBody, "refNumber")
        if (refNumber == null) {
            Log.d(TAG, "❌ FAILED: could not extract UPI ref number (pattern: 'UPI Ref' followed by 11-12 digits)")
            return null
        }
        Log.d(TAG, "✓ Reference number extracted: $refNumber")

        val target = extractFirst(accountPatterns, smsBody, "account") ?: "Unknown"
        Log.d(TAG, "✓ Target/account: $target")

        val dateStr = extractFirst(datePatterns, smsBody, "date")
        Log.d(TAG, "✓ Date: ${dateStr ?: "(not found, using current time)"}")

        val txn = Transaction(
            amount = amount.replace(",", ""),
            referenceNumber = refNumber,
            target = target,
            timestampMs = System.currentTimeMillis()
        )
        Log.d(TAG, "✅ PARSE SUCCESS: $txn")
        return txn
    }

    /** Returns the first capture-group match across a list of patterns. */
    private fun extractFirst(patterns: List<Regex>, text: String, label: String): String? {
        for ((i, pattern) in patterns.withIndex()) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                val value = match.groupValues[1].trim()
                Log.d(TAG, "  [$label] pattern #$i matched: \"$value\" (pattern: ${pattern.pattern})")
                return value
            } else {
                Log.d(TAG, "  [$label] pattern #$i no match (pattern: ${pattern.pattern})")
            }
        }
        return null
    }
}
