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

        LaunchedEffect(ui.toastMessage) {
            ui.toastMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                vm.clearToast()
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
                    // Placeholder for respond functionality
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "İstek Cevaplama",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Bu özellik yakında aktif olacak. Yakındaki talepleri görebilecek ve cevap verebileceksiniz.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Placeholder for future features
                            OutlinedButton(
                                onClick = { /* TODO: Implement nearby requests */ },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false
                            ) {
                                Text("Yakındaki Talepleri Göster")
                            }
                        }
                    }
                }
            }
        }
    }
}