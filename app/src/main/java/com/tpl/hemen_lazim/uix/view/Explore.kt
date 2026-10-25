package com.tpl.hemen_lazim.uix.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.ui.LocalAppSnackbar
import com.tpl.hemen_lazim.ui.theme.Spacing
import com.tpl.hemen_lazim.uix.innerview.CreateRequestFab
import com.tpl.hemen_lazim.uix.innerview.RadiusSlider
import com.tpl.hemen_lazim.uix.innerview.RequestDetailDialog
import com.tpl.hemen_lazim.uix.innerview.RespondMapView
import com.tpl.hemen_lazim.uix.viewmodel.RespondViewModel
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider

/** Keşfet: çevredeki talepler haritada; buradan teklif verilir ya da yeni talep açılır. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Explore(
    onCreateRequest: () -> Unit,
    onOpenChat: (roomId: String, me: String, other: String) -> Unit,
    vm: RespondViewModel = viewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = LocalAppSnackbar.current
    // Kart içindeki haritaya dokunulduğu sürece sayfa kaymasın: dikey sürükleme haritaya gitsin
    var mapTouched by remember { mutableStateOf(false) }

    LaunchedEffect(ui.toastMessage) {
        ui.toastMessage?.let {
            snackbar.show(it)
            vm.clearToast()
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.tab_explore)) }) },
        floatingActionButton = { CreateRequestFab(onClick = onCreateRequest) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState(), enabled = !mapTouched)
                .padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.l)
        ) {
            RespondMapView(
                radiusKm = ui.radiusKm,
                nearbyRequests = ui.nearbyRequests,
                highlightedRequest = ui.highlightedRequest,
                onLocationReceived = { latLng -> vm.setUserLocation(latLng.latitude, latLng.longitude) },
                onRequestClick = { request -> vm.selectRequest(request) },
                onMapTouchChange = { mapTouched = it }
            )

            RadiusSlider(
                title = "Arama alanı",
                radiusKm = ui.radiusKm,
                onRadiusChange = { vm.setRadius(it) },
                onRadiusChangeFinished = { vm.fetchNearbyRequests() }
            )

            // Sayaç konum bilinir bilinmez hep görünür; boş sonuç da bir sonuçtur
            if (ui.userLatitude != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = when {
                            ui.isLoading -> "Talepler aranıyor…"
                            ui.nearbyRequests.isEmpty() -> "Bu alanda talep yok. Alanı büyütmeyi dene."
                            else -> "${ui.nearbyRequests.size} talep bulundu"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(Spacing.m),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        ui.selectedRequest?.let { request ->
            RequestDetailDialog(
                request = request,
                onDismiss = { vm.selectRequest(null) },
                onSupply = {
                    val requesterId = request.requesterId
                    if (requesterId == null) {
                        snackbar.show("Talep sahibi bilgisi bulunamadı")
                        return@RequestDetailDialog
                    }
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
            )
        }
    }
}
