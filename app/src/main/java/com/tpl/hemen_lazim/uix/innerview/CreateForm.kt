package com.tpl.hemen_lazim.uix.innerview

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.model.MaterialFormState
import com.tpl.hemen_lazim.model.enums.Category
import com.tpl.hemen_lazim.model.enums.Units
import com.tpl.hemen_lazim.model.enums.label
import com.tpl.hemen_lazim.model.enums.toEmoji
import com.tpl.hemen_lazim.ui.theme.Spacing

// Sunucudaki MaterialRequestCreateDTO sınırları
private const val TITLE_MAX = 120
private const val DESCRIPTION_MAX = 1000
// Miktar alanı dört haneye yetiyor; dar tutuldu ki yanında birim menüsüne yer kalsın
private val QUANTITY_FIELD_WIDTH = 72.dp
// Adımlayıcı (48 + 4 + 72 + 4 + 48 = 176 dp), aradaki 8 dp ve birim menüsünün en dar hali
// (112 dp: en uzun birim adı ve açma oku). Daha dar yerde menü alt satıra iner.
private val QUANTITY_ROW_MIN_WIDTH = 296.dp

/** Formdaki girdiler: ViewModel uygular, önizleme boş bir uygulama verir. */
interface CreateFormActions {
    fun onTitle(v: String)
    fun onDescription(v: String)
    fun onCategory(v: Category)
    fun onQuantity(v: String)
    fun incrementQuantity()
    fun decrementQuantity()
    fun onUnit(v: Units)
    fun updateLocation(latitude: Double, longitude: Double)
    fun onRadiusKm(v: Float)
    fun onDuration(minutes: Int)
}

/**
 * Talep formu: üç numaralı kart. "Hemen lazım" anında hızlı doldurulsun diye tek sayfa;
 * kategori ve birim çiplerle, miktar düğmelerle seçilir.
 */
@Composable
fun CreateForm(
    ui: MaterialFormState,
    actions: CreateFormActions,
    onMapTouchChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
        FormSection(number = 1, title = stringResource(R.string.form_section_what)) {
            WhatSection(ui, actions)
        }
        FormSection(number = 2, title = stringResource(R.string.form_section_where)) {
            val selected = ui.latitude.toDoubleOrNull()?.let { lat ->
                ui.longitude.toDoubleOrNull()?.let { lng -> LatLng(lat, lng) }
            }
            LocationMapComponent(
                selected = selected,
                radiusKm = ui.radiusKm,
                onLocationSelected = actions::updateLocation,
                onMapTouchChange = onMapTouchChange,
                modifier = Modifier.fillMaxWidth()
            )
            // Arama ekranıyla aynı kaydırıcı; eskiden metre cinsinden serbest metindi ve "2" yazan
            // 2 km değil 2 m kaydediyordu
            RadiusSlider(
                title = stringResource(R.string.form_visible_area),
                radiusKm = ui.radiusKm,
                onRadiusChange = actions::onRadiusKm
            )
        }
        FormSection(number = 3, title = stringResource(R.string.form_section_how_long)) {
            DurationPicker(minutes = ui.durationMinutes, onMinutesChange = actions::onDuration)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.WhatSection(ui: MaterialFormState, actions: CreateFormActions) {
    OutlinedTextField(
        value = ui.title,
        onValueChange = { if (it.length <= TITLE_MAX) actions.onTitle(it) },
        label = { Text(stringResource(R.string.form_title_label)) },
        placeholder = { Text(stringResource(R.string.form_title_placeholder)) },
        isError = ui.titleError != null,
        supportingText = {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    ui.titleError?.let { stringResource(it) } ?: "",
                    Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.error
                )
                Text(stringResource(R.string.form_counter, ui.title.length, TITLE_MAX))
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier.fillMaxWidth()
    )

    FieldLabel(stringResource(R.string.form_category), error = ui.categoryError)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Category.entries.forEach { category ->
            FilterChip(
                selected = ui.category == category,
                onClick = { actions.onCategory(category) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Emoji görsel; ekran okuyucu yalnızca kategori adını okusun
                        Text(category.toEmoji(), modifier = Modifier.clearAndSetSemantics {})
                        Spacer(Modifier.width(Spacing.xs))
                        Text(category.label())
                    }
                }
            )
        }
    }

    FieldLabel(stringResource(R.string.form_quantity), error = ui.quantityError)
    // "− 2 + [Adet ▾]" tek satırda; birim menüsüne yer kalmayan çok dar ekranda (ya da büyük
    // ekran ölçeğinde) menü alta, tam genişliğe iner
    BoxWithConstraints {
        val stepper = @Composable {
            QuantityStepper(
                value = ui.quantity,
                onValueChange = actions::onQuantity,
                onDecrease = actions::decrementQuantity,
                onIncrease = actions::incrementQuantity
            )
        }
        if (maxWidth >= QUANTITY_ROW_MIN_WIDTH) {
            // Alt kenara hizalı: menünün etiketi kenarlığın üstüne taştığı için kutular ancak böyle hizalanıyor
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                stepper()
                UnitMenu(selected = ui.units, onSelect = actions::onUnit, modifier = Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                stepper()
                UnitMenu(selected = ui.units, onSelect = actions::onUnit, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    OutlinedTextField(
        value = ui.description,
        onValueChange = { if (it.length <= DESCRIPTION_MAX) actions.onDescription(it) },
        label = { Text(stringResource(R.string.form_description_label)) },
        minLines = 2,
        supportingText = {
            // Sınıra yaklaşınca sayaç; boş açıklama sade dursun
            if (ui.description.length > DESCRIPTION_MAX - 100) {
                Text(
                    stringResource(R.string.form_counter, ui.description.length, DESCRIPTION_MAX),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth()
    )
}

/** − [2] + : miktar düğmelerle değişir, ortadaki alana elle de yazılabilir. */
@Composable
private fun QuantityStepper(
    value: String,
    onValueChange: (String) -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val current = value.toIntOrNull()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        FilledTonalIconButton(onClick = onDecrease, enabled = current == null || current > 1) {
            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.form_quantity_decrease))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.width(QUANTITY_FIELD_WIDTH)
        )
        FilledTonalIconButton(onClick = onIncrease) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.form_quantity_increase))
        }
    }
}

/** Birim seçimi açılır menüde: yedi birim çipi iki satır yer kaplıyordu ve miktardan ayrı duruyordu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitMenu(selected: Units, onSelect: (Units) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected.label(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.form_unit)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Units.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.label()) },
                    onClick = {
                        onSelect(unit)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String, @StringRes error: Int?) {
    Column {
        Text(text, style = MaterialTheme.typography.labelLarge)
        if (error != null) {
            Text(stringResource(error), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

/** Numaralı bölüm kartı: başlık okuyucular için "başlık" olarak işaretli. */
@Composable
private fun FormSection(number: Int, title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                modifier = Modifier.semantics(mergeDescendants = true) { heading() }
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$number",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}
