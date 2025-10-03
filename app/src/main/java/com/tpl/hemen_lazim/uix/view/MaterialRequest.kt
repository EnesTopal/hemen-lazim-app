// uix/view/MaterialRequest.kt
package com.tpl.hemen_lazim.uix.view

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tpl.hemen_lazim.uix.innerview.ScreenScaffold
import com.tpl.hemen_lazim.network.repositories.RequestRepository
import com.tpl.hemen_lazim.uix.viewmodel.MaterialRequestCreateViewModel
import com.tpl.hemen_lazim.model.MaterialFormState
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.tpl.hemen_lazim.model.enums.Category
import com.tpl.hemen_lazim.model.enums.Units
import com.tpl.hemen_lazim.network.RetrofitClient
import com.tpl.hemen_lazim.network.services.RequestService
import com.tpl.hemen_lazim.uix.innerview.CreateForm
import com.tpl.hemen_lazim.uix.innerview.EnumDropdown
import com.tpl.hemen_lazim.uix.innerview.SegButton
import com.tpl.hemen_lazim.uix.innerview.RadiusSlider
import com.tpl.hemen_lazim.uix.innerview.RespondMapView
import com.tpl.hemen_lazim.uix.innerview.RequestDetailDialog
import com.tpl.hemen_lazim.uix.viewmodel.RespondViewModel
import com.google.android.gms.maps.model.LatLng

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialRequest(navController: NavController) {
    // State to track which tab is selected
    var selectedTab by remember { mutableStateOf("create") }
    
    ScreenScaffold(
        navController, currentRoute = "MaterialRequest",
        topBar = { CenterAlignedTopAppBar(title = { Text("Malzeme Talepleri") }) }
    ) { padding ->

        val context = LocalContext.current
        val scrollState = rememberScrollState()

        val api = remember { RetrofitClient.retrofit.create(RequestService::class.java) }
        val repo = remember { RequestRepository(api) }
        val vm = remember { MaterialRequestCreateViewModel(repo) }
        val ui by vm.ui.collectAsState()
        
        // Respond tab ViewModel
        val respondVm = remember { RespondViewModel(repo) }
        val respondUi by respondVm.ui.collectAsState()

        LaunchedEffect(ui.toastMessage) {
            ui.toastMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                vm.clearToast()
            }
        }
        
        LaunchedEffect(respondUi.toastMessage) {
            respondUi.toastMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                respondVm.clearToast()
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SegButton(
                    text = "İstek Oluştur",
                    selected = selectedTab == "create",
                    onClick = { selectedTab = "create" }
                )
                SegButton(
                    text = "İstek Cevapla",
                    selected = selectedTab == "respond",
                    onClick = { selectedTab = "respond" },
                    enabled = true
                )
            }
            Spacer(Modifier.height(16.dp))

            when (selectedTab) {
                "create" -> {
                    CreateForm(ui = ui, vm = vm)
                    
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { vm.submit() },
                        enabled = ui.canSubmit && !ui.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (ui.isSubmitting) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Oluştur")
                    }
                }
                "respond" -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Map with nearby requests
                        RespondMapView(
                            userLocation = if (respondUi.userLatitude != null && respondUi.userLongitude != null)
                                LatLng(respondUi.userLatitude!!, respondUi.userLongitude!!)
                            else null,
                            radiusKm = respondUi.radiusKm,
                            nearbyRequests = respondUi.nearbyRequests,
                            onLocationReceived = { latLng ->
                                respondVm.setUserLocation(latLng.latitude, latLng.longitude)
                            },
                            onRequestClick = { request ->
                                respondVm.selectRequest(request)
                            }
                        )
                        
                        // Radius slider (below map)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            RadiusSlider(
                                radiusKm = respondUi.radiusKm,
                                onRadiusChange = { respondVm.setRadius(it) },
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        
                        // Requests count info
                        if (respondUi.nearbyRequests.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Text(
                                    text = "${respondUi.nearbyRequests.size} talep bulundu",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    
                    // Request detail dialog
                    respondUi.selectedRequest?.let { request ->
                        RequestDetailDialog(
                            request = request,
                            onDismiss = { respondVm.selectRequest(null) },
                            onSupply = {
                                // Send notification to requester
                                request.requesterId?.let { requesterId ->
                                    respondVm.sendSupplyOfferNotification(
                                        requestId = request.id,
                                        requesterId = requesterId,
                                        onSuccess = {
                                            // TODO: Navigate to chat with requester
                                            // For now just show success toast and close dialog
                                            Toast.makeText(
                                                context,
                                                "Talep sahibine bildirim gönderildi. Sohbet özelliği yakında eklenecek.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            respondVm.selectRequest(null)
                                        }
                                    )
                                } ?: run {
                                    Toast.makeText(
                                        context,
                                        "Talep sahibi bilgisi bulunamadı",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}