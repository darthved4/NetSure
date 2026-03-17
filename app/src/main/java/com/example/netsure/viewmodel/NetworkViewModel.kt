package com.example.netsure.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.netsure.services.NetworkAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for exposing high-level network information to the UI.
 *
 * It uses [NetworkAnalyzer] to poll connectivity APIs and transforms the raw
 * information into a simple UI state model collected by composables.
 */
class NetworkViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Public UI model consumed by composables.
     */
    data class NetworkUiState(
        val typeLabel: String = "Unknown",
        val signalLevel: Int = 0, // 0..4
        val qualityScore: Int = 0 // 0..100
    )

    private val analyzer = NetworkAnalyzer(application.applicationContext)

    private val _uiState = MutableStateFlow(NetworkUiState())
    val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

    init {
        // Start observing network changes when the ViewModel is created.
        viewModelScope.launch {
            analyzer.observeNetworkInfo().collect { info ->
                _uiState.value = NetworkUiState(
                    typeLabel = when (info.type) {
                        NetworkAnalyzer.NetworkType.WIFI -> "Wi‑Fi"
                        NetworkAnalyzer.NetworkType.CELLULAR -> "Cellular"
                        NetworkAnalyzer.NetworkType.NONE -> "Offline"
                        NetworkAnalyzer.NetworkType.OTHER -> "Other"
                    },
                    signalLevel = info.signalLevel,
                    qualityScore = info.qualityScore
                )
            }
        }
    }
}

