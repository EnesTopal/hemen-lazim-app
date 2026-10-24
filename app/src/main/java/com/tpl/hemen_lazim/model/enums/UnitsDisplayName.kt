package com.tpl.hemen_lazim.model.enums

fun Units.toDisplayName(): String = when (this) {
    Units.PARCA -> "Parça"
    Units.ADET -> "Adet"
    Units.PAKET -> "Paket"
    Units.KG -> "Kg"
    Units.LITRE -> "Litre"
    Units.KUTU -> "Kutu"
    Units.DIGER -> "Diğer"
}
