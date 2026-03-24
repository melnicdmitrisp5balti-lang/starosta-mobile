package com.starosta.messenger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.starosta.messenger.ui.theme.*
import com.starosta.messenger.viewmodel.ChatViewModel
import com.starosta.messenger.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onStartChat: (String) -> Unit,
    userViewModel: UserViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val searchResults by userViewModel.searchResults.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    val chatError by chatViewModel.error.collectAsState()

    val user = searchResults.firstOrNull { it.id == userId }

    LaunchedEffect(userId) {
        userViewModel.searchUsers(userId)
    }

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = TextPrimary)
                }
            },
            title = { Text("Профиль", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
        )

        if (isLoading && user == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanPrimary)
            }
        } else if (user == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Пользователь не найден", color = TextSecondary, fontSize = 16.sp)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hero
                Box(
                    modifier = Modifier.fillMaxWidth().height(220.dp).background(Brush.verticalGradient(listOf(DarkSurface, DarkBackground))),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(96.dp).clip(CircleShape).border(2.dp, if (user.isOnline) OnlineGreen else DarkBorder, CircleShape).background(Brush.radialGradient(listOf(TealSecondary, TealDark))),
                            contentAlignment = Alignment.Center
                        ) {
                            if (user.avatarUrl != null) {
                                AsyncImage(model = user.avatarUrl, contentDescription = user.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                            } else {
                                Text(user.name.take(1).uppercase(), color = DarkBackground, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(user.name, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            if (user.isVerified) Icon(Icons.Default.Verified, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                        }
                        Text("@${user.username}", color = CyanPrimary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (user.isOnline) OnlineGreen else TextTertiary))
                            Text(if (user.isOnline) "В сети" else "Не в сети", color = if (user.isOnline) OnlineGreen else TextTertiary, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (user.status != null) {
                    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), color = DarkCard, shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Статус", color = TextTertiary, fontSize = 12.sp)
                                Text(user.status, color = TextPrimary, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }

                if (chatError != null) {
                    Text(chatError!!, color = ErrorRed, fontSize = 13.sp, modifier = Modifier.padding(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        chatViewModel.createPrivateChat(user.id) { chat -> onStartChat(chat.id) }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DarkBackground)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Написать сообщение", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
