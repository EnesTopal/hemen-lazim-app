package com.tpl.hemen_lazim.uix.innerview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tpl.hemen_lazim.model.MaterialFormState
import com.tpl.hemen_lazim.model.enums.Category
import com.tpl.hemen_lazim.model.enums.Units
import com.tpl.hemen_lazim.uix.viewmodel.MaterialRequestCreateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateForm(ui: MaterialFormState, vm: MaterialRequestCreateViewModel) {

    OutlinedTextField(
        value = ui.title,
        onValueChange = vm::onTitle,
        label = { Text("Başlık *") },
        isError = ui.titleError != null,
        supportingText = {
            ui.titleError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(10.dp))

    OutlinedTextField(
        value = ui.description,
        onValueChange = vm::onDescription,
        label = { Text("Açıklama") },
        minLines = 3,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(10.dp))

    EnumDropdown(
        label = "Kategori *",
        values = Category.entries.toTypedArray(),
        selected = ui.category,
        onSelect = { vm.onCategory(it) },
        error = ui.categoryError
    )
    Spacer(Modifier.height(10.dp))

    OutlinedTextField(
        value = ui.quantity,
        onValueChange = vm::onQuantity,
        label = { Text("Miktar") },
        isError = ui.quantityError != null,
        supportingText = {
            ui.quantityError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(10.dp))

    EnumDropdown(
        label = "Birim *",
        values = Units.entries.toTypedArray(),
        selected = ui.units,
        onSelect = { vm.onUnit(it) },
        error = ui.unitError
    )
    Spacer(Modifier.height(10.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = ui.latitude,
            onValueChange = vm::onLat,
            label = { Text("Enlem *") },
            isError = ui.latError != null,
            supportingText = {
                ui.latError?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = ui.longitude,
            onValueChange = vm::onLon,
            label = { Text("Boylam *") },
            isError = ui.lonError != null,
            supportingText = {
                ui.lonError?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(10.dp))

    OutlinedTextField(
        value = ui.radiusMeters,
        onValueChange = vm::onRadius,
        label = { Text("Yarıçap (metre)") },
        isError = ui.radiusError != null,
        supportingText = {
            ui.radiusError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(10.dp))

    OutlinedTextField(
        value = ui.expiresAt,
        onValueChange = vm::onExpires,
        label = { Text("Bitiş (ISO8601)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

