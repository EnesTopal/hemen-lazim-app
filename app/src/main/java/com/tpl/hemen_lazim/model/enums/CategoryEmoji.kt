package com.tpl.hemen_lazim.model.enums

fun Category.toEmoji(): String = when (this) {
    Category.YEMEK -> "🍽️"
    Category.YEMEK_MALZEMESI -> "🥕"
    Category.HIJYEN -> "🧼"
    Category.TEMIZLIK -> "🧹"
    Category.GIYIM -> "👕"
    Category.KOZMETIK -> "💄"
    Category.ELEKTRONIK -> "📱"
    Category.KIRTASIYE -> "✏️"
    Category.DIGER -> "📦"
}

fun Category.toDisplayName(): String = when (this) {
    Category.YEMEK -> "Yemek"
    Category.YEMEK_MALZEMESI -> "Yemek Malzemesi"
    Category.HIJYEN -> "Hijyen"
    Category.TEMIZLIK -> "Temizlik"
    Category.GIYIM -> "Giyim"
    Category.KOZMETIK -> "Kozmetik"
    Category.ELEKTRONIK -> "Elektronik"
    Category.KIRTASIYE -> "Kırtasiye"
    Category.DIGER -> "Diğer"
}
