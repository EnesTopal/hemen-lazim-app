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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestDTO
import com.tpl.hemen_lazim.model.RequestLimits
import com.tpl.hemen_lazim.ui.LocalAppSnackbar
import com.tpl.hemen_lazim.ui.theme.Spacing
import com.tpl.hemen_lazim.uix.innerview.CategoryBadge
import com.tpl.hemen_lazim.uix.innerview.CreateRequestFab
import com.tpl.hemen_lazim.uix.innerview.DurationPicker
import com.tpl.hemen_lazim.uix.innerview.quantityAndCategory
import com.tpl.hemen_lazim.uix.viewmodel.MyRequestsViewModel
import com.tpl.hemen_lazim.utils.DisplayStatus
import com.tpl.hemen_lazim.utils.displayStatus
import com.tpl.hemen_lazim.utils.extendedEnd
import com.tpl.hemen_lazim.utils.formatEndTime
import com.tpl.hemen_lazim.utils.formatRemaining
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

/** Kullanıcının kendi talepleri: açıklar üstte (geri sayımlı, eylemli), geçmiş altta. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRequests(
    onCreateRequest: () -> Unit,
    vm: MyRequestsViewModel = viewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = LocalAppSnackbar.current
    var cancelTarget by remember { mutableStateOf<MaterialRequestDTO?>(null) }
    var extendTarget by remember { mutableStateOf<MaterialRequestDTO?>(null) }
    // Kalan süreler dakikada bir güncellensin
    val now by produceState(Instant.now()) {
        while (true) {
            delay(COUNTDOWN_TICK_MS)
            value = Instant.now()
        }
    }

    // Sekmeye her girişte (ör. talep yayınlanıp buraya gelindiğinde) liste tazelenir
    LaunchedEffect(Unit) { vm.refresh() }
    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbar.show(it)
            vm.clearMessage()
        }
    }

    val (open, past) = remember(ui.requests, now) {
        ui.requests.partition { it.displayStatus(now) == DisplayStatus.OPEN }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.tab_my_requests)) }) },
        floatingActionButton = { CreateRequestFab(onClick = onCreateRequest) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = ui.isLoading && ui.requests.isNotEmpty(),
            onRefresh = vm::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                ui.requests.isEmpty() && ui.isLoading ->
                    Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }

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
                    contentPadding = PaddingValues(
                        start = Spacing.screen,
                        end = Spacing.screen,
                        top = Spacing.s,
                        // FAB son kartın eylemlerini örtmesin
                        bottom = FAB_CLEARANCE
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (open.isNotEmpty()) {
                        item(key = "open-header") { SectionHeader(stringResource(R.string.my_requests_section_open)) }
                        items(open, key = { it.id }) { request ->
                            MyRequestCard(
                                request = request,
                                now = now,
                                busy = request.id in ui.busyIds,
                                onComplete = { vm.complete(request.id) },
                                onCancel = { cancelTarget = request },
                                onExtend = { extendTarget = request }
                            )
                        }
                    }
                    if (past.isNotEmpty()) {
                        item(key = "past-header") { SectionHeader(stringResource(R.string.my_requests_section_past)) }
                        items(past, key = { it.id }) { request ->
                            MyRequestCard(request = request, now = now, busy = false)
                        }
                    }
                }
            }
        }
    }

    cancelTarget?.let { request ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text(stringResource(R.string.cancel_confirm_title)) },
            text = { Text(stringResource(R.string.cancel_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    cancelTarget = null
                    vm.cancel(request.id)
                }) { Text(stringResource(R.string.cancel_confirm_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { cancelTarget = null }) { Text(stringResource(R.string.cancel_confirm_no)) }
            }
        )
    }

    extendTarget?.let { request ->
        ExtendSheet(
            request = request,
            onDismiss = { extendTarget = null },
            onConfirm = { minutes ->
                extendTarget = null
                vm.extend(request.id, minutes)
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(top = Spacing.m, bottom = Spacing.xs)
            .semantics { heading() }
    )
}

@Composable
private fun MyRequestCard(
    request: MaterialRequestDTO,
    now: Instant,
    busy: Boolean,
    onComplete: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onExtend: (() -> Unit)? = null
) {
    val status = request.displayStatus(now)
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.l)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                CategoryBadge(request.category, size = 44.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        request.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        quantityAndCategory(request),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            Spacer(Modifier.height(Spacing.s))
            StatusBadge(status = status, remaining = request.expiresAt?.let { formatRemaining(it, now) })

            if (status == DisplayStatus.OPEN && onComplete != null && onCancel != null && onExtend != null) {
                Spacer(Modifier.height(Spacing.s))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel, enabled = !busy) { Text(stringResource(R.string.action_cancel)) }
                    TextButton(onClick = onExtend, enabled = !busy) {
                        Icon(Icons.Outlined.MoreTime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.action_extend))
                    }
                    FilledTonalButton(onClick = onComplete, enabled = !busy) {
                        Icon(Icons.Outlined.TaskAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.action_complete))
                    }
                }
            }
        }
    }
}

/** Durum hapı: açık talepte kalan süreyle birlikte, renkle de ayrışır. */
@Composable
private fun StatusBadge(status: DisplayStatus, remaining: String?) {
    val scheme = MaterialTheme.colorScheme
    val (text, container, content) = when (status) {
        DisplayStatus.OPEN -> Triple(
            remaining?.let { stringResource(R.string.status_open_with_time, it) } ?: stringResource(R.string.status_open),
            scheme.primaryContainer,
            scheme.onPrimaryContainer
        )
        DisplayStatus.EXPIRED -> Triple(stringResource(R.string.status_expired), scheme.surfaceContainerHighest, scheme.onSurfaceVariant)
        DisplayStatus.COMPLETED -> Triple(stringResource(R.string.status_completed), scheme.tertiaryContainer, scheme.onTertiaryContainer)
        DisplayStatus.CANCELLED -> Triple(stringResource(R.string.status_cancelled), scheme.surfaceContainerHighest, scheme.onSurfaceVariant)
        DisplayStatus.MATCHED -> Triple(stringResource(R.string.status_matched), scheme.tertiaryContainer, scheme.onTertiaryContainer)
    }
    Pill(text, container, content)
}

@Composable
private fun Pill(text: String, container: Color, content: Color) {
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs)
        )
    }
}

/** Süre uzatma: oluştururken kullanılan aynı süre seçici ve yeni bitişin önizlemesi. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtendSheet(
    request: MaterialRequestDTO,
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int) -> Unit
) {
    var minutes by remember { mutableIntStateOf(RequestLimits.DEFAULT_DURATION_MIN) }
    val now = Instant.now()
    val newEnd = extendedEnd(request.expiresAt, minutes, now)
    val capped = newEnd == now.plus(Duration.ofMinutes(RequestLimits.MAX_DURATION_MIN.toLong()))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.m)
        ) {
            Text(stringResource(R.string.extend_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.extend_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DurationPicker(minutes = minutes, onMinutesChange = { minutes = it })
            Text(
                stringResource(R.string.extend_new_end, formatEndTime(Duration.between(now, newEnd).toMinutes().toInt())),
                style = MaterialTheme.typography.titleSmall
            )
            if (capped) {
                Text(
                    stringResource(R.string.extend_capped),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )
            }
            Button(onClick = { onConfirm(minutes) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.extend_confirm))
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

private const val COUNTDOWN_TICK_MS = 60_000L
private val FAB_CLEARANCE = 88.dp
