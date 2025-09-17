package com.tpl.hemen_lazim.uix.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpl.hemen_lazim.network.repositories.RequestRepository
import com.tpl.hemen_lazim.model.enums.Category
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestCreateDTO
import com.tpl.hemen_lazim.model.MaterialFormState
import com.tpl.hemen_lazim.model.enums.Units
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MaterialRequestCreateViewModel(
    private val repo: RequestRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(MaterialFormState())
    val ui = _ui.asStateFlow()

    fun onTitle(v: String) = _ui.update { it.copy(title = v, titleError = null) }
    fun onDescription(v: String) = _ui.update { it.copy(description = v) }
    fun onCategory(v: Category) = _ui.update { it.copy(category = v, categoryError = null) }
    fun onQuantity(v: String) = _ui.update { it.copy(quantity = v, quantityError = null) }
    fun onUnit(v: Units) = _ui.update { it.copy(units = v, unitError = null) }
    fun onLat(v: String) = _ui.update { it.copy(latitude = v, latError = null) }
    fun onLon(v: String) = _ui.update { it.copy(longitude = v, lonError = null) }
    
    // GPS location update
    fun updateLocation(latitude: Double, longitude: Double) {
        _ui.update { 
            it.copy(
                latitude = latitude.toString(),
                longitude = longitude.toString(),
                latError = null,
                lonError = null
            )
        }
    }
    fun onRadius(v: String) = _ui.update { it.copy(radiusMeters = v, radiusError = null) }
    fun onExpires(v: String) = _ui.update { it.copy(expiresAt = v, expiresError = null) }
    fun clearToast() = _ui.update { it.copy(toastMessage = null) }

    private fun validate(): Boolean {
        var ok = true
        val s = _ui.value

        if (s.title.isBlank()) {
            _ui.update { it.copy(titleError = "Başlık gerekli") }; ok = false
        }
        if (s.category == null) {
            _ui.update { it.copy(categoryError = "Kategori seçin") }; ok = false
        }

        // quantity opsiyonel ama 0 olamaz – eğer girildiyse
        if (s.quantity.isNotBlank()) {
            val q = s.quantity.toIntOrNull()
            if (q == null || q <= 0) {
                _ui.update { it.copy(quantityError = "Miktar pozitif olmalı") }; ok = false
            }
        }

        val lat = s.latitude.toDoubleOrNull()
        val lon = s.longitude.toDoubleOrNull()
        if (lat == null) {
            _ui.update { it.copy(latError = "Geçerli enlem girin") }; ok = false
        }
        if (lon == null) {
            _ui.update { it.copy(lonError = "Geçerli boylam girin") }; ok = false
        }

        if (s.radiusMeters.isNotBlank()) {
            val r = s.radiusMeters.toIntOrNull()
            if (r == null || r <= 0) {
                _ui.update { it.copy(radiusError = "Yarıçap pozitif olmalı") }; ok = false
            }
        }

        // expiresAt opsiyonel; formatı şimdilik serbest bırakıyoruz
        return ok
    }

    fun submit() = viewModelScope.launch {
        if (!validate()) return@launch
        val s = _ui.value

        _ui.update { it.copy(isSubmitting = true) }

        val body = MaterialRequestCreateDTO(
            title = s.title,
            description = s.description.ifBlank { null },
            category = s.category!!,
            quantity = s.quantity.toIntOrNull(),
            units = s.units,
            latitude = s.latitude.toDouble(),
            longitude = s.longitude.toDouble(),
            radiusMeters = s.radiusMeters.toIntOrNull(),
            expiresInHours = s.expiresAt.ifBlank { null }
        )

        Log.d("MaterialRequestCreateViewModel", "Sending request: $body")
        val res = repo.create(body)
        if (res.isSuccess) {
            _ui.update {
                it.copy(
                    isSubmitting = false,
                    success = true,
                    toastMessage = "İstek oluşturuldu"
                )
            }
        } else {
            _ui.update {
                it.copy(
                    isSubmitting = false,
                    toastMessage = res.exceptionOrNull()?.message ?: "İstek oluşturulamadı"
                )
            }
        }
    }
}