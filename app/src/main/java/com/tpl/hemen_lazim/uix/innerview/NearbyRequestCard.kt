package com.tpl.hemen_lazim.uix.innerview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestDTO
import com.tpl.hemen_lazim.model.enums.label
import com.tpl.hemen_lazim.ui.distanceText
import com.tpl.hemen_lazim.ui.remainingText
import com.tpl.hemen_lazim.ui.theme.Spacing

/** Keşfet listesindeki talep: ne, ne kadar, ne uzaklıkta, ne kadar süre kaldı. */
@Composable
fun NearbyRequestCard(
    request: MaterialRequestDTO,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        // Haritada dairesi vurgulanan talep listede de belli olsun
        border = if (highlighted) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m)
        ) {
            CategoryBadge(request.category)
            Column(Modifier.weight(1f)) {
                Text(
                    request.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = quantityAndCategory(request),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                request.expiresAt?.let { remainingText(it) }?.let { remaining ->
                    Text(
                        text = stringResource(R.string.time_left, remaining),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            request.distanceMeters?.let { meters ->
                Text(
                    text = distanceText(meters),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** "2 Kg · Yemek Malzemesi"; miktar yoksa yalnızca kategori. */
@Composable
@ReadOnlyComposable
fun quantityAndCategory(request: MaterialRequestDTO): String {
    val unit = request.units?.label()
    val quantity = request.quantity?.let { q -> listOfNotNull("$q", unit).joinToString(" ") }
    return listOfNotNull(quantity, request.category.label()).joinToString(" · ")
}
