package com.tpl.hemen_lazim.model.enums

fun Category.toEmoji(): String = when (this) {
    Category.YEMEK -> "🍽️"
    Category.YEMEK_MALZEMESİ -> "🥕"
    Category.HİJYEN -> "🧼"
    Category.TEMİZLİK -> "🧹"
    Category.GİYİM -> "👕"
    Category.KOZMETİK -> "💄"
    Category.ELEKTRONİK -> "📱"
    Category.KIRTASİYE -> "✏️"
    Category.DİĞER -> "📦"
}

fun Category.toDisplayName(): String = when (this) {
    Category.YEMEK -> "Yemek"
    Category.YEMEK_MALZEMESİ -> "Yemek Malzemesi"
    Category.HİJYEN -> "Hijyen"
    Category.TEMİZLİK -> "Temizlik"
    Category.GİYİM -> "Giyim"
    Category.KOZMETİK -> "Kozmetik"
    Category.ELEKTRONİK -> "Elektronik"
    Category.KIRTASİYE -> "Kırtasiye"
    Category.DİĞER -> "Diğer"
}
