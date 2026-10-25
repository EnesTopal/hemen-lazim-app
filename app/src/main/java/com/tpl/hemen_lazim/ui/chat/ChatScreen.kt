package com.tpl.hemen_lazim.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.model.Message
import com.tpl.hemen_lazim.model.MessageStatus
import com.tpl.hemen_lazim.model.uiKey
import com.tpl.hemen_lazim.ui.theme.Spacing
import com.tpl.hemen_lazim.uix.innerview.Avatar
import com.tpl.hemen_lazim.uix.preview.PreviewData
import com.tpl.hemen_lazim.uix.preview.PreviewScreen
import com.tpl.hemen_lazim.uix.preview.ScreenPreviews
import com.tpl.hemen_lazim.ui.clockText
import com.tpl.hemen_lazim.ui.dayLabelText
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Emniyet yoklaması: soket kopukken sık, bağlıyken seyrek (soket sessizce düşebiliyor)
private const val DISCONNECTED_SYNC_INTERVAL_MS = 3_000L
private const val CONNECTED_SYNC_INTERVAL_MS = 30_000L

// Sunucudaki messages.content sütunu (ChatService.MAX_CONTENT_LENGTH); daha uzunu reddediliyor
private const val MAX_MESSAGE_LENGTH = 255
private const val COUNTER_VISIBLE_FROM = MAX_MESSAGE_LENGTH - 30

/** Listede ya gün ayracı ya mesaj var; ayraçlar mesajlardan türetiliyor. */
private sealed interface ChatRow {
    val key: String
}

// Gün metni ("Bugün", "23 Ekim") ekranda, telefonun dilinde kurulur
private data class DayRow(val day: LocalDate) : ChatRow {
    override val key: String get() = "day-$day"
}

private data class MessageRow(val message: Message) : ChatRow {
    override val key: String get() = message.uiKey
}

private fun buildRows(messages: List<Message>, zone: ZoneId = ZoneId.systemDefault()): List<ChatRow> {
    val rows = ArrayList<ChatRow>(messages.size + 4)
    var lastDay: LocalDate? = null
    messages.forEach { message ->
        val day = Instant.ofEpochMilli(message.serverTimestamp).atZone(zone).toLocalDate()
        if (day != lastDay) {
            rows += DayRow(day)
            lastDay = day
        }
        rows += MessageRow(message)
    }
    return rows
}

@Composable
fun ChatScreen(
        navController: NavController,
        roomId: String,
        userId: String,
        recipientId: String,
        viewModel: ChatViewModel = viewModel()
) {
    LaunchedEffect(roomId) { viewModel.initialize(roomId, userId, recipientId) }

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val otherUserName by viewModel.otherUserName.collectAsStateWithLifecycle()
    val rows = remember(messages) { buildRows(messages) }
    val listState = rememberLazyListState()

    // Emniyet ağı: canlı teslimat WebSocket'e bağlı ve soket sessizce düşebiliyor. Ekran
    // öndeyken eksik mesaj var mı diye sorulur: soket kopukken 3 sn'de bir, bağlıyken 30 sn'de
    // bir (eskiden her durumda 3 sn). Soket sağlamken bu istekler boş döner.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, roomId) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(
                        if (viewModel.isConnected.value) CONNECTED_SYNC_INTERVAL_MS
                        else DISCONNECTED_SYNC_INTERVAL_MS
                )
                viewModel.resync()
            }
        }
    }

    // Kullanıcı listenin başına yaklaşınca bir önceki sayfayı yükle.
    // Guard'lar bilinçli olarak loadMoreHistory() içinde: burada okunsalardı
    // collect lambda'sı ilk kompozisyondaki bayat değerleri yakalardı.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
                .collect { firstVisible ->
                    if (firstVisible <= 2) viewModel.loadMoreHistory()
                }
    }

    // En alta yalnızca listenin SONUNA bir şey eklendiğinde kaydır. messages.size'ı
    // dinlemek yanlış olurdu: yukarı kaydırıp geçmiş yüklendiğinde de boyut büyür ve
    // kullanıcı okuduğu yerden en alta fırlatılırdı.
    val lastMessageKey = messages.lastOrNull()?.uiKey
    LaunchedEffect(lastMessageKey) {
        if (rows.isNotEmpty()) {
            // Gün ayraçları ve "daha fazla yükleniyor" satırı da listede
            listState.animateScrollToItem(rows.lastIndex + if (isLoadingMore) 1 else 0)
        }
    }

    ChatContent(
            otherUserName = otherUserName,
            rows = rows,
            isLoadingMore = isLoadingMore,
            userId = userId,
            listState = listState,
            onBack = { navController.popBackStack() },
            onSend = viewModel::sendMessage,
            onRetry = viewModel::retrySend
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatContent(
        otherUserName: String,
        rows: List<ChatRow>,
        isLoadingMore: Boolean,
        userId: String,
        listState: LazyListState,
        onBack: () -> Unit,
        onSend: (String) -> Unit,
        onRetry: (Message) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Scaffold(
            // Alt kenar (gezinme çubuğu, klavye) mesaj kutusunda ele alınıyor
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (otherUserName.isNotBlank()) {
                                    Avatar(otherUserName, size = 32.dp)
                                    Spacer(Modifier.width(Spacing.m))
                                }
                                Text(
                                        text = otherUserName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back)
                                )
                            }
                        }
                )
            }
    ) { scaffoldPadding ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(scaffoldPadding)
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (rows.isEmpty()) {
                    Text(
                            stringResource(R.string.chat_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(Spacing.xxl)
                    )
                }
                LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = Spacing.m, vertical = Spacing.s),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    if (isLoadingMore) {
                        item(key = "loading_more") {
                            Box(
                                    modifier = Modifier.fillMaxWidth().padding(Spacing.s),
                                    contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                    items(rows, key = { it.key }) { row ->
                        when (row) {
                            is DayRow -> DaySeparator(dayLabelText(row.day))
                            is MessageRow ->
                                    MessageItem(
                                            message = row.message,
                                            isOwnMessage = row.message.senderId == userId,
                                            onRetry = { onRetry(row.message) }
                                    )
                        }
                    }
                }
            }

            MessageInput(
                    text = inputText,
                    onTextChange = { if (it.length <= MAX_MESSAGE_LENGTH) inputText = it },
                    onSend = {
                        if (inputText.isNotBlank()) {
                            onSend(inputText)
                            inputText = ""
                        }
                    }
            )
        }
    }
}

@Composable
private fun MessageInput(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Surface(tonalElevation = 2.dp) {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                // Gezinme çubuğu ve klavye: ikisi birden değil, büyüğü kadar
                                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                                .padding(horizontal = Spacing.s, vertical = Spacing.s),
                verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.chat_input_placeholder)) },
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    supportingText =
                            if (text.length >= COUNTER_VISIBLE_FROM) {
                                {
                                    Text(
                                            text = "${text.length}/$MAX_MESSAGE_LENGTH",
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.End,
                                            color =
                                                    if (text.length == MAX_MESSAGE_LENGTH)
                                                            MaterialTheme.colorScheme.error
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else null,
                    colors =
                            TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                            )
            )
            Spacer(Modifier.width(Spacing.s))
            FilledIconButton(onClick = onSend, enabled = text.isNotBlank()) {
                Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.chat_send)
                )
            }
        }
    }
}

@Composable
private fun DaySeparator(label: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = Spacing.s), contentAlignment = Alignment.Center) {
        Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.small
        ) {
            Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs)
            )
        }
    }
}

@Composable
fun MessageItem(message: Message, isOwnMessage: Boolean, onRetry: () -> Unit) {
    val isFailed = isOwnMessage && message.status == MessageStatus.FAILED
    val isSending = isOwnMessage && message.status == MessageStatus.SENDING
    val scheme = MaterialTheme.colorScheme
    val (container, content) =
            when {
                isFailed -> scheme.errorContainer to scheme.onErrorContainer
                isOwnMessage -> scheme.primary to scheme.onPrimary
                else -> scheme.surfaceContainerHighest to scheme.onSurface
            }

    Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isFailed) {
                IconButton(onClick = onRetry) {
                    Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.chat_retry),
                            tint = scheme.error
                    )
                }
            }

            Surface(
                    color = container,
                    contentColor = content,
                    shape =
                            RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                                    bottomEnd = if (isOwnMessage) 4.dp else 16.dp
                            ),
                    modifier =
                            Modifier.widthIn(max = 300.dp)
                                    .then(if (isFailed) Modifier.clickable(onClick = onRetry) else Modifier)
            ) {
                Column(Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s)) {
                    Text(text = message.content, style = MaterialTheme.typography.bodyLarge)
                    // Gönderim onayı gelene kadar saat yerine durum yazısı
                    if (!isSending && !isFailed) {
                        Text(
                                text = clockText(message.serverTimestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = content.copy(alpha = 0.7f),
                                modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }

        if (isSending) {
            Text(
                    text = stringResource(R.string.chat_sending),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, end = Spacing.xs)
            )
        } else if (isFailed) {
            Text(
                    text = stringResource(R.string.chat_failed),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.error,
                    modifier = Modifier.padding(top = 2.dp, end = Spacing.xs)
            )
        }
    }
}

@ScreenPreviews
@Composable
internal fun ChatPreview() {
    PreviewScreen {
        ChatContent(
                otherUserName = PreviewData.CHAT_PARTNER,
                rows = buildRows(PreviewData.chatMessages),
                isLoadingMore = false,
                userId = PreviewData.ME,
                listState = rememberLazyListState(),
                onBack = {},
                onSend = {},
                onRetry = {}
        )
    }
}
