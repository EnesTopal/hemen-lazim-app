package com.tpl.hemen_lazim.uix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestDTO
import com.tpl.hemen_lazim.network.repositories.RequestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RespondUiState(
    val radiusKm: Float = 0.5f,
    val userLatitude: Double? = null,
    val userLongitude: Double? = null,
    val nearbyRequests: List<MaterialRequestDTO> = emptyList(),
    val isLoading: Boolean = false,
    val toastMessage: String? = null,
    val selectedRequest: MaterialRequestDTO? = null
)

class RespondViewModel(
    private val repo: RequestRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(RespondUiState())
    val ui = _ui.asStateFlow()

    fun setUserLocation(latitude: Double, longitude: Double) {
        _ui.update { it.copy(userLatitude = latitude, userLongitude = longitude) }
        fetchNearbyRequests()
    }

    fun setRadius(radiusKm: Float) {
        _ui.update { it.copy(radiusKm = radiusKm) }
        // Auto-fetch when radius changes
        fetchNearbyRequests()
    }

    fun selectRequest(request: MaterialRequestDTO?) {
        _ui.update { it.copy(selectedRequest = request) }
    }

    fun clearToast() {
        _ui.update { it.copy(toastMessage = null) }
    }

    fun fetchNearbyRequests() = viewModelScope.launch {
        val state = _ui.value
        if (state.userLatitude == null || state.userLongitude == null) {
            return@launch
        }

        _ui.update { it.copy(isLoading = true) }

        val radiusMeters = (state.radiusKm * 1000).toInt()
        val result = repo.listNearby(
            lat = state.userLatitude,
            lng = state.userLongitude,
            radius = radiusMeters,
            category = null // Search all categories
        )

        if (result.isSuccess) {
            _ui.update {
                it.copy(
                    nearbyRequests = result.getOrNull() ?: emptyList(),
                    isLoading = false
                )
            }
        } else {
            _ui.update {
                it.copy(
                    isLoading = false,
                    toastMessage = result.exceptionOrNull()?.message ?: "İstekler yüklenemedi"
                )
            }
        }
    }
}
