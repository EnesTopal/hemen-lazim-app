package com.tpl.hemen_lazim.model.enums

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.tpl.hemen_lazim.R

// Adlar strings.xml'de, telefonun dilinde
@StringRes
fun Units.labelRes(): Int = when (this) {
    Units.PARCA -> R.string.unit_piece
    Units.ADET -> R.string.unit_count
    Units.PAKET -> R.string.unit_pack
    Units.KG -> R.string.unit_kg
    Units.LITRE -> R.string.unit_litre
    Units.KUTU -> R.string.unit_box
    Units.DIGER -> R.string.unit_other
}

@Composable
@ReadOnlyComposable
fun Units.label(): String = stringResource(labelRes())
