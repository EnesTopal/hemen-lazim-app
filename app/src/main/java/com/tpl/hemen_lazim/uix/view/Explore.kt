package com.tpl.hemen_lazim.uix.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.LatLng
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestDTO
import com.tpl.hemen_lazim.model.RequestLimits
import com.tpl.hemen_lazim.navigation.Routes
import com.tpl.hemen_lazim.ui.LocalAppSnackbar
import com.tpl.hemen_lazim.ui.theme.Spacing
import com.tpl.hemen_lazim.uix.innerview.CreateRequestFab
import com.tpl.hemen_lazim.uix.innerview.NearbyRequestCard
import com.tpl.hemen_lazim.uix.innerview.RadiusSlider
import com.tpl.hemen_lazim.uix.innerview.RequestDetailSheet
import com.tpl.hemen_lazim.uix.innerview.RespondMapView
import com.tpl.hemen_lazim.uix.preview.PreviewData
import com.tpl.hemen_lazim.uix.preview.PreviewScreen
import com.tpl.hemen_lazim.uix.preview.ScreenPreviews
import com.tpl.hemen_lazim.uix.viewmodel.RespondUiState
import com.tpl.hemen_lazim.uix.viewmodel.RespondViewModel
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider

/**
 * Keşfet: çevredeki talepler haritada ve altında yakından uzağa liste. Buradan teklif verilir
 * ya da yeni talep açılır.
 */
@Composable
fun Explore(
    onCreateRequest: () -> Unit,
    onOpenChat: (roomId: String, me: String, other: String) -> Unit,
    vm: RespondViewModel = viewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = LocalAppSnackbar.current
    val missingRequester = stringResource(R.string.explore_missing_requester)

    LaunchedEffect(ui.toastMessage) {
        ui.toastMessage?.let {
            snackbar.show(it)
            vm.clearToast()
        }
    }
    // Sekmeye dönünce sonuçlar tazelensin (konum biliniyorsa; bilinmiyorsa konum gelince aranır)
    LaunchedEffect(Unit) { vm.fetchNearbyRequests() }

    ExploreContent(
        ui = ui,
        onRefresh = vm::fetchNearbyRequests,
        onCreateRequest = onCreateRequest,
        onLocationReceived = { latLng -> vm.setUserLocation(latLng.latitude, latLng.longitude) },
        onSelectRequest = vm::selectRequest,
        onRadiusChange = vm::setRadius,
        onExpandRadius = vm::expandRadius,
        onSupply = { request ->
            val requesterId = request.requesterId
            if (requesterId == null) {
                snackbar.show(missingRequester)
            } else {
                vm.sendSupplyOfferNotification(
                    requestId = request.id,
                    requesterId = requesterId,
                    onSuccess = {
                        val me = SharedPreferencesProvider.getUserId()
                        if (me != null) {
                            vm.createChatRoom(me, requesterId) { roomId ->
                                vm.selectRequest(null)
                                onOpenChat(roomId, me, requesterId)
                            }
                        } else {
                            vm.selectRequest(null)
                        }
                    }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreContent(
    ui: RespondUiState,
    onRefresh: () -> Unit,
    onCreateRequest: () -> Unit,
    onLocationReceived: (LatLng) -> Unit,
    onSelectRequest: (MaterialRequestDTO?) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onExpandRadius: () -> Unit,
    onSupply: (MaterialRequestDTO) -> Unit
) {
    // Kart içindeki haritaya dokunulduğu sürece sayfa kaymasın: dikey sürükleme haritaya gitsin
    var mapTouched by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.tab_explore)) },
                actions = {
                    IconButton(onClick = onRefresh, enabled = ui.userLatitude != null) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                }
            )
        },
        floatingActionButton = { CreateRequestFab(onClick = onCreateRequest) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState(), enabled = !mapTouched)
                .padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.m)
        ) {
            RespondMapView(
                radiusKm = ui.radiusKm,
                nearbyRequests = ui.nearbyRequests,
                highlightedRequest = ui.highlightedRequest,
                onLocationReceived = onLocationReceived,
                onRequestClick = onSelectRequest,
                onMapTouchChange = { mapTouched = it }
            )

            RadiusSlider(
                title = stringResource(R.string.explore_search_area),
                radiusKm = ui.radiusKm,
                onRadiusChange = onRadiusChange,
                onRadiusChangeFinished = onRefresh,
                // Seçili değer başlığın yanında yazıyor; uç etiketlerinin satırı listeye kalsın
                showBounds = false
            )

            // Konum bilinene kadar liste yok; harita kartı izin/konum durumunu zaten gösteriyor
            if (ui.userLatitude != null) {
                Results(
                    ui = ui,
                    onRequestClick = onSelectRequest,
                    onRetry = onRefresh,
                    onExpand = onExpandRadius
                )
            }

            // FAB son kartın üstünü örtmesin
            Spacer(Modifier.height(FAB_CLEARANCE))
        }

        ui.selectedRequest?.let { request ->
            RequestDetailSheet(
                request = request,
                isSending = ui.isSendingNotification,
                onDismiss = { onSelectRequest(null) },
                onSupply = { onSupply(request) }
            )
        }
    }
}

@Composable
private fun Results(
    ui: RespondUiState,
    onRequestClick: (MaterialRequestDTO) -> Unit,
    onRetry: () -> Unit,
    onExpand: () -> Unit
) {
    val requests = ui.nearbyRequests
    when {
        // İlk arama sürüyor
        !ui.hasLoaded && ui.loadError == null -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.l),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.size(Spacing.m))
            Text(stringResource(R.string.explore_searching), style = MaterialTheme.typography.bodyMedium)
        }

        requests.isEmpty() && ui.loadError != null -> StateMessage(
            icon = Icons.Outlined.ErrorOutline,
            title = stringResource(R.string.explore_load_failed),
            body = ui.loadError?.let { stringResource(it) },
            action = stringResource(R.string.retry),
            onAction = onRetry
        )

        requests.isEmpty() -> StateMessage(
            icon = Icons.Outlined.SearchOff,
            title = stringResource(R.string.explore_empty_title),
            body = stringResource(R.string.explore_empty_body),
            // En geniş alandaysa genişletecek yer yok
            action = stringResource(R.string.explore_expand_area)
                .takeIf { ui.radiusKm < RequestLimits.MAX_RADIUS_KM },
            onAction = onExpand
        )

        else -> {
            Text(
                text = pluralStringResource(R.plurals.explore_results, requests.size, requests.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Sonuçlar ekrandayken yeniden arama: liste yerinde kalsın, ince bir çizgi yeter
            if (ui.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            requests.forEach { request ->
                NearbyRequestCard(
                    request = request,
                    highlighted = request.id == ui.highlightedRequest?.id,
                    onClick = { onRequestClick(request) }
                )
            }
        }
    }
}

/** Liste yerine gösterilen boş/hata durumu: simge, başlık, açıklama ve en fazla bir eylem. */
@Composable
private fun StateMessage(
    icon: ImageVector,
    title: String,
    body: String?,
    action: String?,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xl, horizontal = Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(Spacing.s))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (body != null) {
            Spacer(Modifier.height(Spacing.xs))
            Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (action != null) {
            Spacer(Modifier.height(Spacing.m))
            OutlinedButton(onClick = onAction) { Text(action) }
        }
    }
}

private val FAB_CLEARANCE = 72.dp

@ScreenPreviews
@Composable
internal fun ExplorePreview() {
    PreviewScreen(tab = Routes.EXPLORE) {
        ExploreContent(
            ui = PreviewData.explore,
            onRefresh = {},
            onCreateRequest = {},
            onLocationReceived = {},
            onSelectRequest = {},
            onRadiusChange = {},
            onExpandRadius = {},
            onSupply = {}
        )
    }
}
