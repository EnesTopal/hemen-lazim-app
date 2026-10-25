package com.tpl.hemen_lazim.uix.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.model.Message
import com.tpl.hemen_lazim.network.repositories.ChatUnreadStore
import com.tpl.hemen_lazim.navigation.Routes
import com.tpl.hemen_lazim.uix.viewmodel.ChatsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Chats(navController: NavController, viewModel: ChatsViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsState()
    val unreadByRoom by ChatUnreadStore.unreadByRoom.collectAsState()

    // Refresh on resume
    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.tab_chats)) }) }) {
            padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (ui.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (ui.chats.isEmpty()) {
                Text(
                        "Henüz sohbetiniz yok.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ui.chats) { message ->
                        ChatItem(
                                message = message,
                                currentUserId = ui.currentUserId,
                                userNames = ui.userNames,
                                hasUnread = (unreadByRoom[message.roomId] ?: 0) > 0,
                                onClick = {
                                    val otherUserId =
                                            if (message.senderId == ui.currentUserId)
                                                    message.recipientId
                                            else message.senderId
                                    navController.navigate(
                                            Routes.chat(message.roomId, ui.currentUserId, otherUserId)
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatItem(
        message: Message,
        currentUserId: String,
        userNames: Map<String, String>,
        hasUnread: Boolean,
        onClick: () -> Unit
) {
    val otherUserId =
            if (message.senderId == currentUserId) message.recipientId else message.senderId
    val otherUserName = userNames[otherUserId] ?: otherUserId
    val date = Date(message.serverTimestamp)
    val format = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())

    Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                            text = otherUserName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (hasUnread) FontWeight.Bold else null
                    )
                    if (hasUnread) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                                modifier =
                                        Modifier.size(9.dp)
                                                .background(MaterialTheme.colorScheme.error, CircleShape)
                        )
                    }
                }
                Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                    text = format.format(date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
