package com.tpl.hemen_lazim.uix.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.model.Message
import com.tpl.hemen_lazim.navigation.Routes
import com.tpl.hemen_lazim.network.repositories.ChatUnreadStore
import com.tpl.hemen_lazim.ui.theme.Spacing
import com.tpl.hemen_lazim.uix.innerview.Avatar
import com.tpl.hemen_lazim.uix.preview.PreviewData
import com.tpl.hemen_lazim.uix.preview.PreviewScreen
import com.tpl.hemen_lazim.uix.preview.ScreenPreviews
import com.tpl.hemen_lazim.uix.viewmodel.ChatsUiState
import com.tpl.hemen_lazim.uix.viewmodel.ChatsViewModel
import com.tpl.hemen_lazim.utils.formatChatListTime

@Composable
fun Chats(navController: NavController, viewModel: ChatsViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val unreadByRoom by ChatUnreadStore.unreadByRoom.collectAsStateWithLifecycle()

    // Sekmeye her girişte tazele
    LaunchedEffect(Unit) { viewModel.refresh() }

    ChatsContent(
            ui = ui,
            unreadByRoom = unreadByRoom,
            onRefresh = viewModel::refresh,
            onOpenChat = { roomId, otherUserId ->
                navController.navigate(Routes.chat(roomId, ui.currentUserId, otherUserId))
            }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatsContent(
        ui: ChatsUiState,
        unreadByRoom: Map<String, Int>,
        onRefresh: () -> Unit,
        onOpenChat: (roomId: String, otherUserId: String) -> Unit
) {
    // Yalnızca kullanıcı aşağı çektiğinde gösterge; yeni mesajla gelen tazelemeler sessiz
    var pulling by remember { mutableStateOf(false) }
    LaunchedEffect(ui.isLoading) { if (!ui.isLoading) pulling = false }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.tab_chats)) }) }) {
            padding ->
        PullToRefreshBox(
                isRefreshing = pulling,
                onRefresh = {
                    pulling = true
                    onRefresh()
                },
                modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when {
                ui.chats.isEmpty() && ui.isLoading ->
                        Box(Modifier.fillMaxSize()) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }
                ui.chats.isEmpty() && ui.error != null ->
                        CenteredMessage(
                                title = stringResource(R.string.chats_load_failed),
                                body = ui.error,
                                action = stringResource(R.string.retry),
                                onAction = onRefresh
                        )
                ui.chats.isEmpty() ->
                        CenteredMessage(
                                title = stringResource(R.string.chats_empty_title),
                                body = stringResource(R.string.chats_empty_body),
                                action = null,
                                onAction = {}
                        )
                else ->
                        LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = Spacing.s)
                        ) {
                            items(ui.chats, key = { it.roomId }) { message ->
                                val otherUserId =
                                        if (message.senderId == ui.currentUserId) message.recipientId
                                        else message.senderId
                                ChatItem(
                                        message = message,
                                        isOwn = message.senderId == ui.currentUserId,
                                        otherUserName = ui.userNames[otherUserId],
                                        unread = unreadByRoom[message.roomId] ?: 0,
                                        onClick = { onOpenChat(message.roomId, otherUserId) }
                                )
                            }
                        }
            }
        }
    }
}

@Composable
private fun ChatItem(
        message: Message,
        isOwn: Boolean,
        otherUserName: String?,
        unread: Int,
        onClick: () -> Unit
) {
    val hasUnread = unread > 0
    val preview = if (isOwn) stringResource(R.string.chats_you_prefix, message.content) else message.content
    ListItem(
            modifier = Modifier.clickable(onClick = onClick),
            leadingContent = { Avatar(otherUserName ?: "", size = 48.dp) },
            headlineContent = {
                Text(
                        // İsim gelene kadar kimlik yerine boşluk: UUID göstermek anlamsız
                        text = otherUserName ?: "…",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Text(
                        text = preview,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                        color =
                                if (hasUnread) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                            text = formatChatListTime(message.serverTimestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                    if (hasUnread) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (hasUnread) {
                        Spacer(Modifier.height(Spacing.xs))
                        Badge { Text(if (unread > 99) "99+" else unread.toString()) }
                    }
                }
            }
    )
}

@ScreenPreviews
@Composable
internal fun ChatsPreview() {
    PreviewScreen(tab = Routes.CHATS) {
        ChatsContent(
                ui = PreviewData.chats,
                unreadByRoom = PreviewData.unreadByRoom,
                onRefresh = {},
                onOpenChat = { _, _ -> }
        )
    }
}
