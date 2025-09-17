package com.tpl.hemen_lazim.model

import com.tpl.hemen_lazim.model.enums.Category
import com.tpl.hemen_lazim.model.enums.Units

data class MaterialFormState(
    val title: String = "",
    val description: String = "",
    val category: Category? = null,
    val quantity: String = "",
    val units: Units = Units.PARCA,
    val latitude: String = "",
    val longitude: String = "",
    val radiusMeters: String = "",
    val expiresAt: String = "",
    val titleError: String? = null,
    val categoryError: String? = null,
    val quantityError: String? = null,
    val unitError: String? = null,
    val latError: String? = null,
    val lonError: String? = null,
    val radiusError: String? = null,
    val expiresError: String? = null,
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
