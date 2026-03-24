package com.starosta.messenger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import coil.compose.AsyncImage
import com.starosta.messenger.ui.theme.*
import com.starosta.messenger.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToEditProfile: () -> Unit,
    onLogout: () -> Unit,
    userViewModel: UserViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        userViewModel.refreshProfile()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = {
                Text("Профиль", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            },
            actions = {
                IconButton(onClick = onNavigateToEditProfile) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = CyanPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Hero section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(listOf(DarkSurface, DarkBackground))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .border(2.dp, CyanPrimary, CircleShape)
                            .background(Brush.radialGradient(listOf(TealSecondary, TealDark))),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentUser?.avatarUrl != null) {
                            AsyncImage(
                                model = currentUser!!.avatarUrl,
                                contentDescription = "Аватар",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = currentUser?.name?.take(1)?.uppercase() ?: "?",
                                color = DarkBackground,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = currentUser?.name ?: "",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (currentUser?.username != null) {
                        Text(
                            text = "@${currentUser!!.username}",
                            color = CyanPrimary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status
            if (currentUser?.status != null) {
                ProfileInfoCard(
                    icon = Icons.Default.Info,
                    label = "Статус",
                    value = currentUser!!.status!!
                )
            }

            // Email
            if (currentUser?.email != null) {
                ProfileInfoCard(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = currentUser!!.email!!
                )
            }

            // Phone
            if (currentUser?.phone != null) {
                ProfileInfoCard(
                    icon = Icons.Default.Phone,
                    label = "Телефон",
                    value = currentUser!!.phone!!
                )
            }

            // Verified badge
            if (currentUser?.isVerified == true) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Подтверждённый аккаунт", color = CyanPrimary, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Edit profile button
            Button(
                onClick = onNavigateToEditProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkCard,
                    contentColor = TextPrimary
                )
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Редактировать профиль")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Logout button
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Выйти из аккаунта")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Выход", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text = { Text("Вы уверены, что хотите выйти из аккаунта?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text("Выйти", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Отмена", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ProfileInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = DarkCard,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(label, color = TextTertiary, fontSize = 12.sp)
                Text(value, color = TextPrimary, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
