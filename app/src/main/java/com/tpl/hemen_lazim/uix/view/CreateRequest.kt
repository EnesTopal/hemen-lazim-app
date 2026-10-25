package com.tpl.hemen_lazim.uix.view

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.model.MaterialFormState
import com.tpl.hemen_lazim.ui.LocalAppSnackbar
import com.tpl.hemen_lazim.ui.theme.Spacing
import com.tpl.hemen_lazim.uix.innerview.CreateForm
import com.tpl.hemen_lazim.uix.innerview.CreateFormActions
import com.tpl.hemen_lazim.uix.preview.PreviewData
import com.tpl.hemen_lazim.uix.preview.PreviewScreen
import com.tpl.hemen_lazim.uix.preview.ScreenPreviews
import com.tpl.hemen_lazim.uix.viewmodel.MaterialRequestCreateViewModel
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider
import com.tpl.hemen_lazim.utils.formatDuration
import com.tpl.hemen_lazim.utils.formatEndTime
import com.tpl.hemen_lazim.utils.formatRadiusKm

/**
 * Tam ekran talep formu. Yayınlanınca kapanır ve İlanlarım açılır; ilk yayında bildirim izni
 * sorulur (açılışta değil: kullanıcı neden istendiğini ancak burada anlıyor).
 */
@Composable
fun CreateRequest(
    onBack: () -> Unit,
    onPublished: () -> Unit,
    vm: MaterialRequestCreateViewModel = viewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = LocalAppSnackbar.current
    val currentOnPublished by rememberUpdatedState(onPublished)
    var askNotifications by remember { mutableStateOf(false) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { currentOnPublished() }

    LaunchedEffect(ui.toastMessage) {
        ui.toastMessage?.let {
            snackbar.show(it)
            vm.clearToast()
        }
    }
    LaunchedEffect(ui.success) {
        if (!ui.success) return@LaunchedEffect
        if (shouldAskForNotifications(context)) {
            SharedPreferencesProvider.markNotificationPromptShown()
            askNotifications = true
        } else {
            currentOnPublished()
        }
    }

    CreateRequestContent(ui = ui, actions = vm, onBack = onBack, onPublish = { vm.submit() })

    if (askNotifications) {
        AlertDialog(
            onDismissRequest = {
                askNotifications = false
                currentOnPublished()
            },
            icon = { Icon(Icons.Outlined.NotificationsActive, contentDescription = null) },
            title = { Text(stringResource(R.string.notification_prompt_title)) },
            text = { Text(stringResource(R.string.notification_prompt_body)) },
            confirmButton = {
                TextButton(onClick = {
                    askNotifications = false
                    // Sonuç ne olursa olsun İlanlarım'a geçilir (launcher geri çağrısı)
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text(stringResource(R.string.notification_prompt_allow)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    askNotifications = false
                    currentOnPublished()
                }) { Text(stringResource(R.string.notification_prompt_later)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateRequestContent(
    ui: MaterialFormState,
    actions: CreateFormActions,
    onBack: () -> Unit,
    onPublish: () -> Unit
) {
    var mapTouched by remember { mutableStateOf(false) }

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
        },
        bottomBar = { PublishBar(ui = ui, onPublish = onPublish) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState(), enabled = !mapTouched)
                .padding(Spacing.screen)
        ) {
            CreateForm(ui = ui, actions = actions, onMapTouchChange = { mapTouched = it })
        }
    }
}

/** Alttaki sabit çubuk: seçilen alan, süre ve bitişin özeti ile Yayınla. */
@Composable
private fun PublishBar(ui: MaterialFormState, onPublish: () -> Unit) {
    Surface(tonalElevation = 3.dp, shadowElevation = 3.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                // Klavye açıkken de üstte kalsın; içerik de onun üstünde kayar
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Spacing.screen, vertical = Spacing.m)
        ) {
            Text(
                text = stringResource(
                    R.string.form_summary,
                    formatRadiusKm(ui.radiusKm),
                    formatDuration(ui.durationMinutes),
                    formatEndTime(ui.durationMinutes)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.size(Spacing.s))
            Button(
                onClick = onPublish,
                // Başarıdan sonra da kapalı: aynı talep iki kez gönderilemesin
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

/** Android 13+ ve izin yoksa, bir kez sorulur; reddedeni her talepte rahatsız etmeyiz. */
private fun shouldAskForNotifications(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED &&
        !SharedPreferencesProvider.wasNotificationPromptShown()

@ScreenPreviews
@Composable
internal fun CreateRequestPreview() {
    PreviewScreen {
        CreateRequestContent(ui = PreviewData.createForm, actions = PreviewData.formActions, onBack = {}, onPublish = {})
    }
}
