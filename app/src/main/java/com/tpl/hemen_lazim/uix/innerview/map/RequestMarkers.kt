package com.tpl.hemen_lazim.uix.innerview.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.clustering.Clustering
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestDTO
import com.tpl.hemen_lazim.model.enums.toEmoji
import com.tpl.hemen_lazim.uix.innerview.RequestClusterItem
import kotlinx.coroutines.launch

// Bu kadar yakın talepler yakınlaştırmakla ayrılmaz; tek yer sayılıp liste gösterilir
private const val SAME_SPOT_METERS = 10.0
private const val CLUSTER_PADDING_PX = 120

/**
 * Talepleri emoji işaretleri olarak gösterir, yakın olanları kümeler. Kümeye dokunmak
 * yakınlaştırır; aynı noktadaki talepler (yakınlaştırınca ayrılmayanlar) için [onSameSpotClick]
 * çağrılır ki hepsi listelenebilsin.
 */
@OptIn(MapsComposeExperimentalApi::class)
@Composable
@GoogleMapComposable
fun RequestMarkers(
    requests: List<MaterialRequestDTO>,
    cameraPositionState: CameraPositionState,
    onRequestClick: (MaterialRequestDTO) -> Unit,
    onSameSpotClick: (List<MaterialRequestDTO>) -> Unit
) {
    val scope = rememberCoroutineScope()
    val items = remember(requests) { requests.map(::RequestClusterItem) }

    Clustering(
        items = items,
        onClusterClick = { cluster ->
            val positions = cluster.items.map { it.position }
            if (positions.allWithin(SAME_SPOT_METERS)) {
                onSameSpotClick(cluster.items.map { it.request })
            } else {
                val bounds = LatLngBounds.builder().apply { positions.forEach(::include) }.build()
                scope.launch {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, CLUSTER_PADDING_PX), 500)
                }
            }
            true
        },
        onClusterItemClick = { item ->
            // Küme en az birkaç talepten oluşur; aynı noktadaki 2-3 talep üst üste tek işaret
            // gibi görünür, dokununca hepsi gösterilsin. Dinleyici her çizimde güncellenir, liste
            // güncel kalır.
            val here = LatLng(item.request.latitude, item.request.longitude)
            val sameSpot = requests.filter {
                listOf(here, LatLng(it.latitude, it.longitude)).allWithin(SAME_SPOT_METERS)
            }
            if (sameSpot.size > 1) onSameSpotClick(sameSpot) else onRequestClick(item.request)
            true
        },
        clusterContent = { cluster -> ClusterBadge(cluster.size) },
        clusterItemContent = { item -> EmojiMarker(item.request.category.toEmoji()) }
    )
}

@Composable
private fun EmojiMarker(emoji: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        shadowElevation = 4.dp
    ) {
        Text(text = emoji, fontSize = 22.sp, modifier = Modifier.padding(6.dp))
    }
}

@Composable
private fun ClusterBadge(count: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Text(
            text = "$count",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
