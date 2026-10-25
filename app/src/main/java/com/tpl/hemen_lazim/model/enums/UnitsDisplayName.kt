package com.tpl.hemen_lazim.model.enums

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.tpl.hemen_lazim.R

// Adlar strings.xml'de, telefonun dilinde
@StringRes
fun Units.labelRes(): Int = when (this) {
    Units.PARCA -> R.string.unit_piece
    Units.ADET -> R.string.unit_item
    Units.PAKET -> R.string.unit_pack
    Units.KG -> R.string.unit_kg
    Units.LITRE -> R.string.unit_litre
    Units.KUTU -> R.string.unit_box
    Units.DIGER -> R.string.unit_other
}

@Composable
@ReadOnlyComposable
fun Units.label(): String = stringResource(labelRes())

// Miktarla birlikte ayrı metin: İngilizcede birim çoğul çekiliyor ("2 packs")
@PluralsRes
fun Units.quantityRes(): Int = when (this) {
    Units.PARCA -> R.plurals.quantity_piece
    Units.ADET -> R.plurals.quantity_item
    Units.PAKET -> R.plurals.quantity_pack
    Units.KG -> R.plurals.quantity_kg
    Units.LITRE -> R.plurals.quantity_litre
    Units.KUTU -> R.plurals.quantity_box
    Units.DIGER -> R.plurals.quantity_other
}

/** "2 Paket" / "2 packs" */
@Composable
@ReadOnlyComposable
fun Units.quantity(count: Int): String = pluralStringResource(quantityRes(), count, count)
