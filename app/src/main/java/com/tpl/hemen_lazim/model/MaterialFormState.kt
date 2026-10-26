package com.tpl.hemen_lazim.model

import androidx.annotation.StringRes
import com.tpl.hemen_lazim.model.enums.Category
import com.tpl.hemen_lazim.model.enums.Units

data class MaterialFormState(
    val title: String = "",
    val description: String = "",
    val category: Category? = null,
    // Çoğu talepte bir miktar var; − / + ile değişir, istenirse silinip boş bırakılabilir
    val quantity: String = "1",
    // Varsayılan Adet: sayılabilen çoğu şey için doğru, birim menüsünü çoğu zaman açmak gerekmez
    val units: Units = Units.ADET,
    val latitude: String = "",
    val longitude: String = "",
    val radiusKm: Float = RequestLimits.DEFAULT_REQUEST_RADIUS_KM,
    val durationMinutes: Int = RequestLimits.DEFAULT_DURATION_MIN,
    // Hata ve mesajlar strings.xml kimliği: metin telefonun dilinde ekranda çözülür
    @StringRes val titleError: Int? = null,
    @StringRes val categoryError: Int? = null,
    @StringRes val quantityError: Int? = null,
    @StringRes val latError: Int? = null,
    @StringRes val lonError: Int? = null,
    // UI
    val isSubmitting: Boolean = false,
    @StringRes val toastMessage: Int? = null,
    val success: Boolean = false
) {
    val canSubmit: Boolean
        get() = title.isNotBlank()
                && category != null
                && latitude.toDoubleOrNull() != null
                && longitude.toDoubleOrNull() != null
}
