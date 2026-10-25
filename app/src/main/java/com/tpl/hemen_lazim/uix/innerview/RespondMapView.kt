package com.tpl.hemen_lazim.uix.innerview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.tpl.hemen_lazim.R
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.rememberCameraPositionState
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestDTO
import com.tpl.hemen_lazim.uix.innerview.map.HemenMapCard
import com.tpl.hemen_lazim.uix.innerview.map.RequestMarkers
import com.tpl.hemen_lazim.uix.innerview.map.boundsAround
import com.tpl.hemen_lazim.uix.innerview.map.rememberLocationController

/**
 * Yakındaki talepler: ilan açma ekranıyla aynı harita kartı. Arama dairesi kullanıcının
 * konumunda; son dokunulan talebin kendi dairesi de soluk çizilir ki iki dairenin kesiştiği
 * (talebin merkezi arama dairesinin dışında olsa bile neden listelendiği) görülsün.
 */
@Composable
fun RespondMapView(
    radiusKm: Float,
    nearbyRequests: List<MaterialRequestDTO>,
    highlightedRequest: MaterialRequestDTO?,
    onLocationReceived: (LatLng) -> Unit,
    onRequestClick: (MaterialRequestDTO) -> Unit,
    onMapTouchChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val location = rememberLocationController()
    val cameraPositionState = rememberCameraPositionState()
    val currentOnLocationReceived by rememberUpdatedState(onLocationReceived)
    var cameraPlaced by remember { mutableStateOf(false) }
    var sameSpotRequests by remember { mutableStateOf<List<MaterialRequestDTO>>(emptyList()) }
    val highlightColor = MaterialTheme.colorScheme.tertiary
    val radiusMeters = radiusKm * 1000.0
    val requestPoints = remember(nearbyRequests) { nearbyRequests.map { LatLng(it.latitude, it.longitude) } }

    LaunchedEffect(location.locationVersion) {
        val here = location.lastLocation ?: return@LaunchedEffect
        // İlk konumda kamera doğrudan oraya; sonraki değişiklikler sığdırma ile yumuşak geçer
        if (!cameraPlaced) {
            cameraPlaced = true
            cameraPositionState.position = CameraPosition.fromLatLngZoom(here, 14f)
        }
        currentOnLocationReceived(here)
    }

    Column(modifier) {
        HemenMapCard(
            location = location,
            cameraPositionState = cameraPositionState,
            circleCenter = { location.lastLocation },
            radiusMeters = radiusMeters,
            // Talebin merkezi arama dairesinin dışında olabilir (daireler kesişiyorsa listelenir);
            // kamera arama alanını ve bulunan tüm talepleri birlikte göstersin
            fitKey = Triple(location.locationVersion, radiusKm, requestPoints),
            fitBounds = { location.lastLocation?.let { boundsAround(it, radiusMeters, requestPoints) } },
            onTouchChange = onMapTouchChange
        ) {
            highlightedRequest?.radiusMeters?.let { requestRadius ->
                Circle(
                    center = LatLng(highlightedRequest.latitude, highlightedRequest.longitude),
                    radius = requestRadius.toDouble(),
                    strokeColor = highlightColor,
                    strokeWidth = 3f,
                    fillColor = highlightColor.copy(alpha = 0.08f)
                )
            }
            RequestMarkers(
                requests = nearbyRequests,
                cameraPositionState = cameraPositionState,
                onRequestClick = onRequestClick,
                onSameSpotClick = { sameSpotRequests = it }
            )
        }
        Text(
            text = stringResource(R.string.explore_map_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    if (sameSpotRequests.isNotEmpty()) {
        SameSpotRequestsSheet(
            requests = sameSpotRequests,
            onSelect = { request ->
                sameSpotRequests = emptyList()
                onRequestClick(request)
            },
            onDismiss = { sameSpotRequests = emptyList() }
        )
    }
}
