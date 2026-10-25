package com.tpl.hemen_lazim.model.enums

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.tpl.hemen_lazim.R

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

// Adlar strings.xml'de, telefonun dilinde
@StringRes
fun Category.labelRes(): Int = when (this) {
    Category.YEMEK -> R.string.category_food
    Category.YEMEK_MALZEMESI -> R.string.category_groceries
    Category.HIJYEN -> R.string.category_hygiene
    Category.TEMIZLIK -> R.string.category_cleaning
    Category.GIYIM -> R.string.category_clothing
    Category.KOZMETIK -> R.string.category_cosmetics
    Category.ELEKTRONIK -> R.string.category_electronics
    Category.KIRTASIYE -> R.string.category_stationery
    Category.DIGER -> R.string.category_other
}

@Composable
@ReadOnlyComposable
fun Category.label(): String = stringResource(labelRes())
