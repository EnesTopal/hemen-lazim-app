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
import kotlin.math.roundToInt

private const val MAX_QUANTITY = 9999
private const val MAX_QUANTITY_DIGITS = 4

class MaterialRequestCreateViewModel(
    private val repo: RequestRepository = RequestRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(MaterialFormState())
    val ui = _ui.asStateFlow()

    fun onTitle(v: String) = _ui.update { it.copy(title = v, titleError = null) }
    fun onDescription(v: String) = _ui.update { it.copy(description = v) }
    fun onCategory(v: Category) = _ui.update { it.copy(category = v, categoryError = null) }
    // Yalnızca rakam, en fazla 4 hane
    fun onQuantity(v: String) {
        val digits = v.filter(Char::isDigit).take(MAX_QUANTITY_DIGITS)
        _ui.update { it.copy(quantity = digits, quantityError = null) }
    }

    fun incrementQuantity() = stepQuantity(+1)
    fun decrementQuantity() = stepQuantity(-1)

    private fun stepQuantity(delta: Int) = _ui.update {
        val current = it.quantity.toIntOrNull() ?: 0
        val next = (current + delta).coerceIn(1, MAX_QUANTITY)
        it.copy(quantity = next.toString(), quantityError = null)
    }
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
    fun onRadiusKm(v: Float) = _ui.update { it.copy(radiusKm = v) }
    fun onDuration(minutes: Int) = _ui.update { it.copy(durationMinutes = minutes) }
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
            radiusMeters = (s.radiusKm * 1000).roundToInt(),
            expiresInMinutes = s.durationMinutes
        )

        Log.d("MaterialRequestCreateViewModel", "Sending request: $body")
        val res = repo.create(body)
        if (res.isSuccess) {
            _ui.update {
                it.copy(
                    isSubmitting = false,
                    success = true,
                    toastMessage = "Talebin yayında"
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