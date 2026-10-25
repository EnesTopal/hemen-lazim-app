package com.tpl.hemen_lazim.model

import com.tpl.hemen_lazim.model.enums.Category
import com.tpl.hemen_lazim.model.enums.Units

data class MaterialFormState(
    val title: String = "",
    val description: String = "",
    val category: Category? = null,
    // Çoğu talepte bir miktar var; − / + ile değişir, istenirse silinip boş bırakılabilir
    val quantity: String = "1",
    val units: Units = Units.PARCA,
    val latitude: String = "",
    val longitude: String = "",
    val radiusKm: Float = RequestLimits.DEFAULT_REQUEST_RADIUS_KM,
    val durationMinutes: Int = RequestLimits.DEFAULT_DURATION_MIN,
    val titleError: String? = null,
    val categoryError: String? = null,
    val quantityError: String? = null,
    val unitError: String? = null,
    val latError: String? = null,
    val lonError: String? = null,
    // UI
    val isSubmitting: Boolean = false,
    val toastMessage: String? = null,
    val success: Boolean = false
) {
    val canSubmit: Boolean
        get() = title.isNotBlank()
                && category != null
                && units != null
                && latitude.toDoubleOrNull() != null
                && longitude.toDoubleOrNull() != null
}
