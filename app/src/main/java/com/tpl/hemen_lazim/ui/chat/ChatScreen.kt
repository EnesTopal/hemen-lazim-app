package com.tpl.hemen_lazim.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tpl.hemen_lazim.model.Message
import com.tpl.hemen_lazim.model.MessageStatus
import com.tpl.hemen_lazim.model.uiKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
        navController: NavController,
        roomId: String,
        userId: String,
        recipientId: String,
        viewModel: ChatViewModel = viewModel()
) {
    LaunchedEffect(roomId) { viewModel.initialize(roomId, userId, recipientId) }

    val messages by viewModel.messages.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val otherUserName by viewModel.otherUserName.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

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
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = {
                            Text(
                                    text = otherUserName,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Geri"
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
                            .background(Color(0xFFF5F5F5))
    ) {
        LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            if (isLoadingMore) {
                item(key = "loading_more") {
                    Box(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            }
            items(messages, key = { it.uiKey }) { message ->
                MessageItem(
                        message = message,
                        isOwnMessage = message.senderId == userId,
                        onRetry = { viewModel.retrySend(message) }
                )
            }
        }

        // Input Area
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(8.dp)
                                .background(Color.White, RoundedCornerShape(24.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Mesaj yazın...") },
                    colors =
                            TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                            )
            )

            IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    }
            ) {
                Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    }
}

@Composable
fun MessageItem(message: Message, isOwnMessage: Boolean, onRetry: () -> Unit) {
    val isFailed = isOwnMessage && message.status == MessageStatus.FAILED

    Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isFailed) {
                IconButton(onClick = onRetry, modifier = Modifier.size(28.dp)) {
                    Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Tekrar gönder",
                            tint = Color.Red
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Card(
                    shape =
                            RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isOwnMessage) 16.dp else 0.dp,
                                    bottomEnd = if (isOwnMessage) 0.dp else 16.dp
                            ),
                    colors =
                            CardDefaults.cardColors(
                                    containerColor =
                                            when {
                                                isFailed -> Color(0xFFFFCDD2)
                                                isOwnMessage -> MaterialTheme.colorScheme.primary
                                                else -> Color.White
                                            }
                            ),
                    modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                        text = message.content,
                        modifier = Modifier.padding(12.dp),
                        color = if (isOwnMessage && !isFailed) Color.White else Color.Black
                )
            }
        }

        if (isOwnMessage && message.status == MessageStatus.SENDING) {
            Text(
                    text = "gönderiliyor...",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp, end = 4.dp)
            )
        } else if (isFailed) {
            Text(
                    text = "gönderilemedi, tekrar dene",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 2.dp, end = 4.dp)
            )
        }
    }
}
