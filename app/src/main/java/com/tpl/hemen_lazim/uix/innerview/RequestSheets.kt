package com.tpl.hemen_lazim.uix.innerview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestDTO
import com.tpl.hemen_lazim.model.enums.label
import com.tpl.hemen_lazim.ui.distanceText
import com.tpl.hemen_lazim.ui.remainingText
import com.tpl.hemen_lazim.ui.theme.Spacing
import com.tpl.hemen_lazim.uix.preview.ComponentPreviews
import com.tpl.hemen_lazim.uix.preview.PreviewData
import com.tpl.hemen_lazim.uix.preview.PreviewSurface

/**
 * Talep ayrıntısı alt sayfada: harita ve liste arkada görünür kalır, tek elle kapatılır.
 * Ana eylem "Bende var, verebilirim": talep sahibine haber verir ve sohbeti açar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailSheet(
    request: MaterialRequestDTO,
    isSending: Boolean,
    onDismiss: () -> Unit,
    onSupply: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        RequestDetailContent(request = request, isSending = isSending, onSupply = onSupply)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RequestDetailContent(request: MaterialRequestDTO, isSending: Boolean, onSupply: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl)
            .padding(bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.l)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            CategoryBadge(request.category, size = 56.dp)
            Column(Modifier.weight(1f)) {
                Text(request.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    request.category.label(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.s)
        ) {
            request.quantity?.let { quantity ->
                InfoPill(Icons.Outlined.Inventory2, listOfNotNull("$quantity", request.units?.label()).joinToString(" "))
            }
            request.distanceMeters?.let { meters ->
                InfoPill(Icons.Outlined.NearMe, stringResource(R.string.distance_away, distanceText(meters)))
            }
            request.expiresAt?.let { remainingText(it) }?.let { remaining ->
                InfoPill(Icons.Outlined.Schedule, stringResource(R.string.time_left, remaining))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.PersonOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(Spacing.s))
            Text(
                stringResource(
                    R.string.request_requester,
                    request.requesterName ?: stringResource(R.string.request_requester_unknown)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!request.description.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    request.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(Spacing.m)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onSupply,
                enabled = !isSending,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(Spacing.s))
                }
                Text(stringResource(R.string.request_supply))
            }
            Spacer(Modifier.size(Spacing.xs))
            Text(
                stringResource(R.string.request_supply_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Haritada aynı noktadaki birden çok talep: alt sayfada liste, dokununca ayrıntısı açılır. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SameSpotRequestsSheet(
    requests: List<MaterialRequestDTO>,
    onSelect: (MaterialRequestDTO) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = Spacing.l)) {
            Text(
                pluralStringResource(R.plurals.same_spot_title, requests.size, requests.size),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                stringResource(R.string.same_spot_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(Spacing.m))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
                modifier = Modifier.padding(bottom = Spacing.xl)
            ) {
                items(requests, key = { it.id }) { request ->
                    NearbyRequestCard(request = request, highlighted = false, onClick = { onSelect(request) })
                }
            }
        }
    }
}

/** Ayrıntıdaki küçük bilgi hapı: simge + kısa metin (tıklanmaz). */
@Composable
private fun InfoPill(icon: ImageVector, text: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

// Alt sayfanın kendisi önizlemede açılmıyor; içeriği tek başına gösteriliyor
@ComponentPreviews
@Composable
internal fun RequestDetailPreview() {
    PreviewSurface {
        RequestDetailContent(request = PreviewData.nearbyRequests.first(), isSending = false, onSupply = {})
    }
}
