package com.tpl.hemen_lazim.uix.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestDTO
import com.tpl.hemen_lazim.model.enums.toEmoji
import com.tpl.hemen_lazim.ui.theme.Spacing
import com.tpl.hemen_lazim.uix.innerview.CreateRequestFab
import com.tpl.hemen_lazim.uix.viewmodel.MyRequestsViewModel
import com.tpl.hemen_lazim.utils.formatRemaining

/** Kullanıcının kendi talepleri. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRequests(
    onCreateRequest: () -> Unit,
    vm: MyRequestsViewModel = viewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    // Sekmeye her girişte (ör. talep yayınlanıp buraya gelindiğinde) liste tazelenir
    LaunchedEffect(Unit) { vm.refresh() }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.tab_my_requests)) }) },
        floatingActionButton = { CreateRequestFab(onClick = onCreateRequest) }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                ui.requests.isEmpty() && ui.isLoading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                ui.requests.isEmpty() && ui.loadError != null ->
                    CenteredMessage(
                        title = stringResource(R.string.my_requests_load_failed),
                        body = ui.loadError,
                        action = stringResource(R.string.retry),
                        onAction = vm::refresh
                    )

                ui.requests.isEmpty() ->
                    CenteredMessage(
                        title = stringResource(R.string.my_requests_empty_title),
                        body = stringResource(R.string.my_requests_empty_body),
                        action = stringResource(R.string.create_request),
                        onAction = onCreateRequest
                    )

                else -> LazyColumn(
                    contentPadding = PaddingValues(Spacing.screen),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s)
                ) {
                    items(ui.requests, key = { it.id }) { request -> MyRequestRow(request) }
                }
            }
        }
    }
}

@Composable
private fun MyRequestRow(request: MaterialRequestDTO) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m)
        ) {
            Text(request.category.toEmoji(), fontSize = 28.sp)
            Column(Modifier.weight(1f)) {
                Text(
                    request.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = request.expiresAt?.let(::formatRemaining) ?: request.status.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Boş liste ve hata durumları için ortada başlık, açıklama ve tek eylem. */
@Composable
fun CenteredMessage(
    title: String,
    body: String?,
    action: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xxl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (body != null) {
            Spacer(Modifier.height(Spacing.s))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (action != null) {
            Spacer(Modifier.height(Spacing.l))
            Button(onClick = onAction) { Text(action) }
        }
    }
}
