package com.tpl.hemen_lazim.uix.innerview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tpl.hemen_lazim.model.enums.Category
import com.tpl.hemen_lazim.model.enums.toEmoji

/**
 * Kategori emojisi açık marka renginde bir dairede: liste, detay ve İlanlarım'da aynı görünüm.
 * Kategorinin adı her kullanıldığı yerde yanında yazıyor; rozet yalnızca görsel.
 */
@Composable
fun CategoryBadge(category: Category, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    // Emoji dairenin yarısı kadar; dp'den çevrildiği için yazı ölçeği büyüse de daireden taşmaz
    val emojiSize = with(LocalDensity.current) { (size * 0.5f).toSp() }
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            // Ekran okuyucu emojinin adını ("havuç") okumasın
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center
    ) {
        Text(category.toEmoji(), fontSize = emojiSize)
    }
}
