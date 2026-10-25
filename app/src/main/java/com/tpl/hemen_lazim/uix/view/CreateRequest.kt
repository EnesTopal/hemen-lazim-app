package com.tpl.hemen_lazim.uix.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.ui.LocalAppSnackbar
import com.tpl.hemen_lazim.ui.theme.Spacing
import com.tpl.hemen_lazim.uix.innerview.CreateForm
import com.tpl.hemen_lazim.uix.viewmodel.MaterialRequestCreateViewModel

/** Tam ekran talep formu; yayınlanınca kapanır ve İlanlarım açılır. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRequest(
    onBack: () -> Unit,
    onPublished: () -> Unit,
    vm: MaterialRequestCreateViewModel = viewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = LocalAppSnackbar.current
    val currentOnPublished by rememberUpdatedState(onPublished)
    var mapTouched by remember { mutableStateOf(false) }

    LaunchedEffect(ui.toastMessage) {
        ui.toastMessage?.let {
            snackbar.show(it)
            vm.clearToast()
        }
    }
    LaunchedEffect(ui.success) {
        if (ui.success) currentOnPublished()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_request_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState(), enabled = !mapTouched)
                .padding(Spacing.screen)
        ) {
            CreateForm(ui = ui, vm = vm, onMapTouchChange = { mapTouched = it })

            Spacer(Modifier.height(Spacing.m))
            Button(
                onClick = { vm.submit() },
                enabled = ui.canSubmit && !ui.isSubmitting && !ui.success,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (ui.isSubmitting) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(Spacing.s))
                }
                Text(stringResource(R.string.publish))
            }
        }
    }
}
