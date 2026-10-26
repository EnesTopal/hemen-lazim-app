package com.tpl.hemen_lazim.uix.innerview.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.tpl.hemen_lazim.R
import kotlinx.coroutines.delay

private val MAP_HEIGHT = 320.dp
private val FIT_PADDING = 32.dp
private const val FIT_DEBOUNCE_MS = 250L
private const val FIT_ANIMATION_MS = 500

// İki ekranda aynı: Google'ın kendi zoom/konum butonları yok, yerine kartın kendi butonu var
private val MAP_UI_SETTINGS = MapUiSettings(
    compassEnabled = false,
    mapToolbarEnabled = false,
    myLocationButtonEnabled = false,
    rotationGesturesEnabled = false,
    tiltGesturesEnabled = false,
    zoomControlsEnabled = false
)

/**
 * İlan açma ve arama ekranlarının ortak harita kartı: aynı butonlar, aynı daire stili ve aynı
 * konum durumu ekranları. Haritanın yüksekliğini ekran seçer ([height]); konum/izin ekranı en az o
 * kadar, metni sığmazsa daha uzun olur. Ekrana özgü içerik (talepler) [content] ile verilir.
 *
 * Kart kaydırılabilir bir formun içinde durduğu için haritaya dokunulduğu sürece
 * [onTouchChange] ile haber verir; ekran bu sırada sayfa kaydırmasını kapatır, yoksa dikey
 * sürükleme haritayı değil sayfayı kaydırırdı.
 */
@Composable
fun HemenMapCard(
    location: LocationController,
    cameraPositionState: CameraPositionState,
    // Okuma harita içeriğinde yapılır: kamera her karede hareket ederken kart yeniden çizilmesin
    circleCenter: () -> LatLng?,
    radiusMeters: Double,
    // Değiştiğinde (kısa bir beklemeden sonra) kamera fitBounds()'a sığdırılır
    fitKey: Any?,
    fitBounds: () -> LatLngBounds?,
    onTouchChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = MAP_HEIGHT,
    showMap: Boolean = location.lastLocation != null,
    showCenterPin: Boolean = false,
    onRecenter: () -> Unit = location::refresh,
    onMapClick: (LatLng) -> Unit = {},
    placeholderAction: (@Composable () -> Unit)? = null,
    content: @Composable @GoogleMapComposable () -> Unit = {}
) {
    // Android Studio önizlemesinde (ve ekran görüntüsü testlerinde) GoogleMap çizilmiyor, konum da
    // istenmiyor: kart haritanın yerine sade bir çizimle, butonlarıyla birlikte görünür
    val inPreview = LocalInspectionMode.current
    val showingMap = showMap || inPreview
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (showingMap) Modifier.height(height) else Modifier.heightIn(min = height)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (showingMap) {
            MapContent(
                location = location,
                cameraPositionState = cameraPositionState,
                circleCenter = circleCenter,
                radiusMeters = radiusMeters,
                fitKey = fitKey,
                fitBounds = fitBounds,
                onTouchChange = onTouchChange,
                showCenterPin = showCenterPin,
                onRecenter = onRecenter,
                onMapClick = onMapClick,
                inPreview = inPreview,
                content = content
            )
        } else {
            LocationPlaceholder(
                status = location.status,
                onResolve = location::resolve,
                extraAction = placeholderAction
            )
        }
    }
}

@Composable
private fun MapContent(
    location: LocationController,
    cameraPositionState: CameraPositionState,
    circleCenter: () -> LatLng?,
    radiusMeters: Double,
    fitKey: Any?,
    fitBounds: () -> LatLngBounds?,
    onTouchChange: (Boolean) -> Unit,
    showCenterPin: Boolean,
    onRecenter: () -> Unit,
    onMapClick: (LatLng) -> Unit,
    inPreview: Boolean,
    content: @Composable @GoogleMapComposable () -> Unit
) {
    val context = LocalContext.current
    var mapLoaded by remember { mutableStateOf(false) }
    val currentFitBounds by rememberUpdatedState(fitBounds)
    // pointerInput bir kez kurulur; her zaman en güncel geri çağrıyı kullansın
    val currentOnTouchChange by rememberUpdatedState(onTouchChange)
    val fitPaddingPx = with(LocalDensity.current) { FIT_PADDING.roundToPx() }
    val circleColor = MaterialTheme.colorScheme.primary
    // İzin yokken mavi noktayı açmak SecurityException fırlatır
    val showMyLocation = remember(location.status) { !inPreview && hasLocationPermission(context) }

    LaunchedEffect(fitKey, mapLoaded) {
        if (!mapLoaded) return@LaunchedEffect
        // Kaydırıcı sürüklenirken her adımda değil, durunca sığdır
        delay(FIT_DEBOUNCE_MS)
        val bounds = currentFitBounds() ?: return@LaunchedEffect
        cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, fitPaddingPx), FIT_ANIMATION_MS)
    }

    Box(
        Modifier
            .fillMaxSize()
            .reportTouches { touched -> currentOnTouchChange(touched) }
    ) {
        if (inPreview) {
            MapPreviewBackdrop(circleColor)
        } else {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = showMyLocation),
                uiSettings = MAP_UI_SETTINGS,
                onMapClick = onMapClick,
                onMapLoaded = { mapLoaded = true }
            ) {
                circleCenter()?.let { center ->
                    Circle(
                        center = center,
                        radius = radiusMeters,
                        strokeColor = circleColor,
                        strokeWidth = 4f,
                        fillColor = circleColor.copy(alpha = 0.15f)
                    )
                }
                content()
            }
        }

        if (showCenterPin) {
            // İğnenin ucu tam merkezde dursun diye yarı boyu kadar yukarıda
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = stringResource(R.string.map_selected_location),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .offset(y = (-20).dp)
            )
        }

        SmallFloatingActionButton(
            onClick = onRecenter,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            if (location.status == LocationStatus.Loading && !inPreview) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.map_my_location))
            }
        }
    }
}

/** Önizlemede haritanın yerine: birkaç sokak çizgisi ve haritadakiyle aynı renkte alan dairesi. */
@Composable
private fun MapPreviewBackdrop(circleColor: Color) {
    // Koyu temada outlineVariant zeminle neredeyse aynı; yazı renginin soluğu iki temada da seçiliyor
    val roadColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
    Canvas(Modifier.fillMaxSize()) {
        val road = 6.dp.toPx()
        listOf(0.18f, 0.55f, 0.84f).forEach { y ->
            drawLine(roadColor, Offset(0f, size.height * y), Offset(size.width, size.height * (y + 0.08f)), road)
        }
        listOf(0.22f, 0.68f).forEach { x ->
            drawLine(roadColor, Offset(size.width * x, 0f), Offset(size.width * (x - 0.06f), size.height), road)
        }
        val radius = size.minDimension * 0.32f
        drawCircle(circleColor.copy(alpha = 0.15f), radius)
        drawCircle(circleColor, radius, style = Stroke(width = 4f))
    }
}

@Composable
private fun LocationPlaceholder(
    status: LocationStatus,
    onResolve: () -> Unit,
    extraAction: (@Composable () -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (status == LocationStatus.Loading || status == LocationStatus.Ready) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.location_loading), style = MaterialTheme.typography.bodyMedium)
        } else {
            val (title, body, action) = when (status) {
                is LocationStatus.PermissionDenied -> Triple(
                    R.string.location_permission_title,
                    R.string.location_permission_body,
                    if (status.canAskAgain) R.string.location_permission_allow else R.string.location_open_settings
                )
                LocationStatus.LocationOff -> Triple(
                    R.string.location_off_title,
                    R.string.location_off_body,
                    R.string.location_turn_on
                )
                else -> Triple(R.string.location_unavailable_title, R.string.location_unavailable_body, R.string.retry)
            }
            Icon(
                imageVector = Icons.Filled.LocationOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onResolve) { Text(stringResource(action)) }
        }
        extraAction?.invoke()
    }
}

/**
 * Dokunuşun başını ve sonunu olayları tüketmeden izler (Initial geçişi: harita olayları yine
 * alır). maps-compose örneğindeki desen kaydırmayı yalnızca kamera durunca geri açıyordu;
 * haritaya dokunup bırakmak sayfayı kilitli bırakırdı.
 */
private fun Modifier.reportTouches(onTouchChange: (Boolean) -> Unit): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            onTouchChange(true)
            try {
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                } while (event.changes.any { it.pressed })
            } finally {
                onTouchChange(false)
            }
        }
    }
)
