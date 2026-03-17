package com.example.netsure.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Service that periodically reads basic network information from Android's
 * connectivity APIs and exposes it as a simple data model.
 *
 * This is intentionally lightweight and polling-based for the hackathon prototype.
 */
class NetworkAnalyzer(private val context: Context) {

    /**
     * Simple snapshot of the current network state used by the UI.
     */
    data class NetworkInfo(
        val type: NetworkType,
        val signalLevel: Int,
        val qualityScore: Int
    )

    /**
     * High-level network type categories that the UI can render.
     */
    enum class NetworkType {
        WIFI,
        CELLULAR,
        NONE,
        OTHER
    }

    /**
     * Returns a cold [Flow] that emits [NetworkInfo] every [intervalMs] milliseconds.
     * The ViewModel collects this and exposes it as StateFlow to the UI.
     */
    fun observeNetworkInfo(intervalMs: Long = 3_000L): Flow<NetworkInfo> = flow {
        while (true) {
            emit(readCurrentNetworkInfo())
            delay(intervalMs)
        }
    }

    private fun readCurrentNetworkInfo(): NetworkInfo {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        val type = when {
            capabilities == null -> NetworkType.NONE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            else -> NetworkType.OTHER
        }

        // For this prototype we approximate signal level and quality score using simple heuristics.
        val signalLevel = when (type) {
            NetworkType.WIFI -> 4 // Assume good Wi-Fi for visualization.
            NetworkType.CELLULAR -> approximateCellSignalLevel()
            NetworkType.NONE -> 0
            NetworkType.OTHER -> 2
        }.coerceIn(0, 4)

        val qualityScore = when (type) {
            NetworkType.NONE -> 0
            NetworkType.WIFI -> 90
            NetworkType.CELLULAR -> 50 + signalLevel * 10
            NetworkType.OTHER -> 40
        }.coerceIn(0, 100)

        return NetworkInfo(
            type = type,
            signalLevel = signalLevel,
            qualityScore = qualityScore
        )
    }

    /**
     * Very rough cellular signal strength approximation.
     * In a production app we would use SignalStrength APIs and callbacks.
     */
    private fun approximateCellSignalLevel(): Int {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        // For simplicity we do not read actual SignalStrength here.
        // Returning a mid-level strength keeps the UI interesting without extra complexity.
        return if (telephonyManager.simState == TelephonyManager.SIM_STATE_READY) {
            3
        } else {
            1
        }
    }
}

