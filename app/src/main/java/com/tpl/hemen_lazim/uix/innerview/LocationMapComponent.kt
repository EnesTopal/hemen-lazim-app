package com.tpl.hemen_lazim.uix.innerview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState
import com.tpl.hemen_lazim.uix.innerview.map.HemenMapCard
import com.tpl.hemen_lazim.uix.innerview.map.boundsAround
import com.tpl.hemen_lazim.uix.innerview.map.rememberLocationController
import kotlinx.coroutines.launch

// Konum alınamayıp kullanıcı elle seçmek istediğinde haritanın açılacağı yer
private val FALLBACK_CENTER = LatLng(41.0082, 28.9784)
private const val PICK_ZOOM = 15f

/**
 * İlan açarken konum ve görünürlük alanı: arama ekranıyla aynı harita kartı. Pin kartın
 * ortasında sabit, haritayı kaydırmak konumu değiştirir; başka talepler gösterilmez.
 */
@Composable
fun LocationMapComponent(
    selected: LatLng?,
    radiusKm: Float,
    onLocationSelected: (latitude: Double, longitude: Double) -> Unit,
    onMapTouchChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val location = rememberLocationController()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(selected ?: FALLBACK_CENTER, PICK_ZOOM)
    }
    val scope = rememberCoroutineScope()
    val currentOnLocationSelected by rememberUpdatedState(onLocationSelected)
    var manualPick by rememberSaveable { mutableStateOf(false) }
    // Pin yalnızca henüz seçim yokken ya da "Konumuma git" ile kullanıcının konumuna taşınır:
    // sekmeye dönünce yeniden alınan konum, elle seçilmiş yeri ezmesin
    var followNextLocation by remember { mutableStateOf(selected == null) }
    val radiusMeters = radiusKm * 1000.0

    LaunchedEffect(location.locationVersion) {
        val here = location.lastLocation ?: return@LaunchedEffect
        if (followNextLocation) {
            followNextLocation = false
            cameraPositionState.position = CameraPosition.fromLatLngZoom(here, PICK_ZOOM)
            currentOnLocationSelected(here.latitude, here.longitude)
        }
    }

    LaunchedEffect(manualPick) {
        if (manualPick && selected == null) {
            currentOnLocationSelected(FALLBACK_CENTER.latitude, FALLBACK_CENTER.longitude)
        }
    }

    // Pin = kamera merkezi: kullanıcı haritayı kaydırıp bırakınca seçilen konum güncellenir
    LaunchedEffect(cameraPositionState.isMoving) {
        val mapShown = location.lastLocation != null || manualPick || selected != null
        if (!cameraPositionState.isMoving && mapShown) {
            val target = cameraPositionState.position.target
            currentOnLocationSelected(target.latitude, target.longitude)
        }
    }

    Column(modifier) {
        HemenMapCard(
            location = location,
            cameraPositionState = cameraPositionState,
            circleCenter = { cameraPositionState.position.target },
            radiusMeters = radiusMeters,
            fitKey = location.locationVersion to radiusKm,
            fitBounds = { boundsAround(cameraPositionState.position.target, radiusMeters) },
            onTouchChange = onMapTouchChange,
            showMap = location.lastLocation != null || manualPick || selected != null,
            showCenterPin = true,
            onRecenter = {
                followNextLocation = true
                location.refresh()
            },
            onMapClick = { tapped ->
                scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLng(tapped), 300) }
            },
            placeholderAction = {
                TextButton(onClick = { manualPick = true }) { Text("Haritadan kendim seçeceğim") }
            }
        )
        Text(
            text = "Haritayı kaydırarak pini talebin olduğu yere getir.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
