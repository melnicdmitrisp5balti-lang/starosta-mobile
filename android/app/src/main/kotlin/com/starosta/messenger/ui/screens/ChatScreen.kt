package com.starosta.messenger.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.starosta.messenger.data.models.MessageWithDetails
import com.starosta.messenger.ui.theme.*
import com.starosta.messenger.viewmodel.AuthState
import com.starosta.messenger.viewmodel.AuthViewModel
import com.starosta.messenger.viewmodel.ChatViewModel
import com.starosta.messenger.viewmodel.MessageViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onBack: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    messageViewModel: MessageViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val messages by messageViewModel.messages.collectAsState()
    val isLoading by messageViewModel.isLoading.collectAsState()
    val typingUsers by messageViewModel.typingUsers.collectAsState()
    val replyToMessage by messageViewModel.replyToMessage.collectAsState()
    val editingMessage by messageViewModel.editingMessage.collectAsState()
    val selectedChat by chatViewModel.selectedChat.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    val currentUserId = (authState as? AuthState.Authenticated)?.user?.id

    var inputText by remember { mutableStateOf("") }
    var selectedMessage by remember { mutableStateOf<MessageWithDetails?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { messageViewModel.sendFile(chatId, it, context) }
    }

    LaunchedEffect(chatId) {
        messageViewModel.loadMessages(chatId)
        chatViewModel.selectChat(chatId)
    }

    // Scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(messages.size - 1) }
        }
    }

    // Populate input with editing message content
    LaunchedEffect(editingMessage) {
        inputText = editingMessage?.content ?: ""
    }

    // Mark visible messages as read
    LaunchedEffect(messages) {
        val unread = messages
            .filter { msg ->
                msg.senderId != currentUserId &&
                        msg.status != "READ" &&
                        !msg.isDeleted
            }
            .map { it.id }
        if (unread.isNotEmpty()) messageViewModel.markMessagesRead(unread)
    }

    val chatTitle = selectedChat?.let { chatDisplayName(it) } ?: "Чат"
    val isGroupChat = selectedChat?.type == "GROUP"

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = TextPrimary
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = chatTitle,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        AnimatedVisibility(visible = typingUsers.isNotEmpty()) {
                            Text(
                                text = buildString {
                                    val names = typingUsers.values.toList()
                                    append(names.take(2).joinToString(", "))
                                    append(if (names.size == 1) " печатает..." else " печатают...")
                                },
                                color = CyanPrimary,
                                fontSize = 12.sp
                            )
                        }
                        if (typingUsers.isEmpty()) {
                            val membersText = selectedChat?.members?.let { members ->
                                if (isGroupChat) "${members.size} участников"
                                else {
                                    val other = members.firstOrNull { it.userId != currentUserId }
                                    if (other?.user?.isOnline == true) "в сети" else "не в сети"
                                }
                            }
                            if (membersText != null) {
                                Text(
                                    text = membersText,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Pinned message banner
            selectedChat?.pinnedMessages?.lastOrNull()?.let { pinned ->
                pinned.message?.let { msg ->
                    PinnedMessageBanner(message = msg)
                }
            }

            // Messages list
            Box(modifier = Modifier.weight(1f)) {
                if (isLoading && messages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CyanPrimary)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBackground),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                isOwn = message.senderId == currentUserId,
                                isGroupChat = isGroupChat,
                                onLongClick = {
                                    selectedMessage = message
                                    showContextMenu = true
                                },
                                onReplyClick = { replyMsg ->
                                    replyMsg.replyTo?.let { /* scroll to it */ }
                                }
                            )
                        }
                    }
                }
            }

            // Reply / Edit preview bar
            AnimatedVisibility(
                visible = replyToMessage != null || editingMessage != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                val previewMessage = replyToMessage ?: editingMessage
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(CyanPrimary)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (editingMessage != null) "Редактирование" else "Ответ: ${previewMessage?.sender?.name ?: ""}",
                            color = CyanPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = previewMessage?.content ?: previewMessage?.fileName ?: "",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = {
                        messageViewModel.clearReplyTo()
                        messageViewModel.clearEditingMessage()
                        inputText = ""
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Отмена", tint = TextSecondary)
                    }
                }
            }

            // Input bar
            ChatInputBar(
                text = inputText,
                onTextChange = { newText ->
                    inputText = newText
                    if (newText.isNotEmpty()) messageViewModel.sendTypingStart(chatId)
                    else messageViewModel.sendTypingStop(chatId)
                },
                onSend = {
                    val text = inputText.trim()
                    if (text.isNotEmpty()) {
                        if (editingMessage != null) {
                            messageViewModel.editMessage(editingMessage!!.id, text)
                        } else {
                            messageViewModel.sendMessage(text)
                        }
                        inputText = ""
                        messageViewModel.sendTypingStop(chatId)
                    }
                },
                onAttachFile = { filePickerLauncher.launch("*/*") }
            )
        }
    }

    // Context menu
    if (showContextMenu && selectedMessage != null) {
        MessageContextMenu(
            message = selectedMessage!!,
            isOwn = selectedMessage!!.senderId == currentUserId,
            onDismiss = {
                showContextMenu = false
                selectedMessage = null
            },
            onReply = {
                messageViewModel.setReplyTo(selectedMessage!!)
                showContextMenu = false
                selectedMessage = null
            },
            onEdit = {
                if (selectedMessage!!.senderId == currentUserId) {
                    messageViewModel.setEditingMessage(selectedMessage!!)
                }
                showContextMenu = false
                selectedMessage = null
            },
            onDelete = {
                messageViewModel.deleteMessage(selectedMessage!!.id)
                showContextMenu = false
                selectedMessage = null
            },
            onPin = {
                messageViewModel.pinMessage(selectedMessage!!.id)
                showContextMenu = false
                selectedMessage = null
            }
        )
    }
}

@Composable
private fun PinnedMessageBanner(message: MessageWithDetails) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.PushPin, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Закреплённое сообщение", color = CyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = message.content ?: message.fileName ?: "",
                color = TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
}

@Composable
private fun MessageBubble(
    message: MessageWithDetails,
    isOwn: Boolean,
    isGroupChat: Boolean,
    onLongClick: () -> Unit,
    onReplyClick: (MessageWithDetails) -> Unit
) {
    val bubbleColor = if (isOwn) MessageBubbleSent else MessageBubbleReceived
    val alignment = if (isOwn) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        if (isGroupChat && !isOwn) {
            Text(
                text = message.sender?.name ?: "",
                color = CyanPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 46.dp, bottom = 2.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isOwn && isGroupChat) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(TealSecondary, TealDark))),
                    contentAlignment = Alignment.Center
                ) {
                    if (message.sender?.avatarUrl != null) {
                        AsyncImage(
                            model = message.sender.avatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = message.sender?.name?.take(1)?.uppercase() ?: "?",
                            color = DarkBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onLongClick
                    )
            ) {
                if (message.isDeleted) {
                    Surface(
                        color = DarkCard,
                        shape = RoundedCornerShape(
                            topStart = 16.dp, topEnd = 16.dp,
                            bottomStart = if (isOwn) 16.dp else 4.dp,
                            bottomEnd = if (isOwn) 4.dp else 16.dp
                        )
                    ) {
                        Text(
                            text = "Сообщение удалено",
                            color = TextTertiary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    Surface(
                        color = bubbleColor,
                        shape = RoundedCornerShape(
                            topStart = 16.dp, topEnd = 16.dp,
                            bottomStart = if (isOwn) 16.dp else 4.dp,
                            bottomEnd = if (isOwn) 4.dp else 16.dp
                        )
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            // Reply preview
                            message.replyTo?.let { reply ->
                                Surface(
                                    color = DarkBorder.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .padding(bottom = 6.dp)
                                        .clickable { onReplyClick(reply) }
                                ) {
                                    Row(modifier = Modifier.padding(6.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .fillMaxHeight()
                                                .background(CyanPrimary)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = reply.sender?.name ?: "",
                                                color = CyanPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = reply.content ?: reply.fileName ?: "",
                                                color = TextSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            // File attachment
                            if (message.fileUrl != null) {
                                Row(
                                    modifier = Modifier.padding(bottom = if (message.content != null) 4.dp else 0.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AttachFile,
                                        contentDescription = null,
                                        tint = CyanPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = message.fileName ?: "Файл",
                                        color = CyanPrimary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Message content
                            if (message.content != null) {
                                Text(
                                    text = message.content,
                                    color = TextPrimary,
                                    fontSize = 15.sp
                                )
                            }

                            // Time + edited indicator
                            Row(
                                modifier = Modifier.align(Alignment.End),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (message.isEdited) {
                                    Text("ред.", color = TextTertiary, fontSize = 10.sp)
                                }
                                Text(
                                    text = formatMessageTime(message.createdAt),
                                    color = TextTertiary,
                                    fontSize = 10.sp
                                )
                                if (isOwn) {
                                    Icon(
                                        imageVector = when (message.status) {
                                            "READ" -> Icons.Default.DoneAll
                                            "DELIVERED" -> Icons.Default.DoneAll
                                            else -> Icons.Default.Done
                                        },
                                        contentDescription = null,
                                        tint = if (message.status == "READ") CyanPrimary else TextTertiary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachFile: () -> Unit
) {
    Surface(
        color = DarkSurface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = onAttachFile) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = "Прикрепить файл",
                    tint = TextSecondary
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Сообщение...", color = TextTertiary) },
                modifier = Modifier.weight(1f),
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary.copy(alpha = 0.5f),
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CyanPrimary,
                    focusedContainerColor = DarkCard,
                    unfocusedContainerColor = DarkCard
                ),
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilledIconButton(
                onClick = onSend,
                enabled = text.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = CyanPrimary,
                    contentColor = DarkBackground,
                    disabledContainerColor = CyanPrimary.copy(alpha = 0.3f)
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
            }
        }
    }
}

@Composable
private fun MessageContextMenu(
    message: MessageWithDetails,
    isOwn: Boolean,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp),
        text = {
            Column {
                ContextMenuItem(
                    icon = Icons.Default.Reply,
                    text = "Ответить",
                    onClick = onReply
                )
                if (isOwn && !message.isDeleted) {
                    ContextMenuItem(
                        icon = Icons.Default.Edit,
                        text = "Редактировать",
                        onClick = onEdit
                    )
                }
                ContextMenuItem(
                    icon = Icons.Default.PushPin,
                    text = "Закрепить",
                    onClick = onPin
                )
                if (isOwn && !message.isDeleted) {
                    ContextMenuItem(
                        icon = Icons.Default.Delete,
                        text = "Удалить",
                        tint = ErrorRed,
                        onClick = onDelete
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun ContextMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(text, color = tint, fontSize = 15.sp)
    }
}
