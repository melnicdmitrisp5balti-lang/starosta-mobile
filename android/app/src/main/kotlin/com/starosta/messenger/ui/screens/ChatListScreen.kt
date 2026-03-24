package com.starosta.messenger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.starosta.messenger.data.models.ChatWithDetails
import com.starosta.messenger.ui.theme.*
import com.starosta.messenger.viewmodel.ChatViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onNavigateToChat: (String) -> Unit,
    viewModel: ChatViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val chats by viewModel.chats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadChats()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = {
                Text(
                    "Сообщения",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkBackground
            )
        )

        if (isLoading && chats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanPrimary)
            }
        } else if (chats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Нет чатов", color = TextSecondary, fontSize = 18.sp)
                    Text(
                        "Найдите людей и начните общаться",
                        color = TextTertiary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(chats, key = { it.id }) { chat ->
                    ChatListItem(
                        chat = chat,
                        onClick = { onNavigateToChat(chat.id) }
                    )
                    HorizontalDivider(
                        color = DarkBorder.copy(alpha = 0.4f),
                        modifier = Modifier.padding(start = 76.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatListItem(
    chat: ChatWithDetails,
    onClick: () -> Unit
) {
    val lastMessage = chat.messages.lastOrNull()
    val unreadCount = chat._count?.messages ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        ChatAvatar(chat = chat, modifier = Modifier.size(52.dp))

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chatDisplayName(chat),
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatChatTimestamp(chat.updatedAt),
                    color = if (unreadCount > 0) CyanPrimary else TextTertiary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (lastMessage?.isDeleted == true) "Сообщение удалено"
                    else lastMessage?.content ?: if (lastMessage?.fileName != null) "📎 ${lastMessage.fileName}" else "Нет сообщений",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (unreadCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(CyanPrimary)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            color = DarkBackground,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChatAvatar(chat: ChatWithDetails, modifier: Modifier = Modifier) {
    val isGroup = chat.type == "GROUP"
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.radialGradient(listOf(TealSecondary, TealDark))
            ),
        contentAlignment = Alignment.Center
    ) {
        if (chat.avatarUrl != null) {
            AsyncImage(
                model = chat.avatarUrl,
                contentDescription = chatDisplayName(chat),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else if (isGroup) {
            Icon(
                Icons.Default.Group,
                contentDescription = null,
                tint = DarkBackground,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = chatDisplayName(chat).take(1).uppercase(),
                color = DarkBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}

internal fun chatDisplayName(chat: ChatWithDetails): String {
    return when (chat.type) {
        "GROUP" -> chat.name ?: "Группа"
        else -> chat.name ?: chat.members.firstOrNull()?.user?.name ?: "Чат"
    }
}

internal fun formatChatTimestamp(updatedAt: String?): String {
    if (updatedAt == null) return ""
    return try {
        val instant = Instant.parse(updatedAt)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val today = LocalDate.now(ZoneId.systemDefault())
        when {
            dateTime.toLocalDate() == today ->
                dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            dateTime.toLocalDate().year == today.year ->
                dateTime.format(DateTimeFormatter.ofPattern("d MMM"))
            else ->
                dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yy"))
        }
    } catch (_: Exception) {
        ""
    }
}

internal fun formatMessageTime(createdAt: String?): String {
    if (createdAt == null) return ""
    return try {
        val instant = Instant.parse(createdAt)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) {
        ""
    }
}
