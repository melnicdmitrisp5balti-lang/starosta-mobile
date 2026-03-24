package com.starosta.messenger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.starosta.messenger.data.models.User
import com.starosta.messenger.ui.theme.*
import com.starosta.messenger.viewmodel.ChatViewModel
import com.starosta.messenger.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCreateScreen(
    onBack: () -> Unit,
    onGroupCreated: (String) -> Unit,
    userViewModel: UserViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val searchResults by userViewModel.searchResults.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    val chatError by chatViewModel.error.collectAsState()

    var groupName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    val selectedMembers = remember { mutableStateListOf<User>() }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) userViewModel.searchUsers(searchQuery)
        else userViewModel.clearSearch()
    }

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = TextPrimary)
                }
            },
            title = { Text("Новая группа", color = TextPrimary, fontWeight = FontWeight.Bold) },
            actions = {
                TextButton(
                    onClick = {
                        if (groupName.isNotBlank() && selectedMembers.isNotEmpty()) {
                            chatViewModel.createGroupChat(
                                name = groupName.trim(),
                                memberIds = selectedMembers.map { it.id }
                            ) { chat -> onGroupCreated(chat.id) }
                        }
                    },
                    enabled = groupName.isNotBlank() && selectedMembers.isNotEmpty()
                ) {
                    Text("Создать", color = if (groupName.isNotBlank() && selectedMembers.isNotEmpty()) CyanPrimary else TextTertiary, fontWeight = FontWeight.SemiBold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
        )

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            // Group name
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Название группы", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, tint = CyanPrimary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = authTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            // Selected members chips
            if (selectedMembers.isNotEmpty()) {
                Text("Участники (${selectedMembers.size})", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    items(selectedMembers, key = { it.id }) { member ->
                        InputChip(
                            selected = true,
                            onClick = { selectedMembers.remove(member) },
                            label = { Text(member.name, color = TextPrimary, fontSize = 13.sp) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary) },
                            colors = InputChipDefaults.inputChipColors(selectedContainerColor = DarkCard),
                            border = InputChipDefaults.inputChipBorder(enabled = true, selected = true, selectedBorderColor = CyanPrimary.copy(alpha = 0.5f), borderColor = DarkBorder)
                        )
                    }
                }
            }

            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Найти участников...", color = TextTertiary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
                singleLine = true,
                colors = authTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )

            if (chatError != null) {
                Text(chatError!!, color = ErrorRed, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CyanPrimary, modifier = Modifier.size(28.dp))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(searchResults, key = { it.id }) { user ->
                        val isSelected = selectedMembers.any { it.id == user.id }
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (isSelected) selectedMembers.removeAll { it.id == user.id }
                                else selectedMembers.add(user)
                            }.padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(Brush.radialGradient(listOf(TealSecondary, TealDark))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(user.name.take(1).uppercase(), color = DarkBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("@${user.username}", color = TextSecondary, fontSize = 13.sp)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(22.dp))
                            } else {
                                Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(22.dp))
                            }
                        }
                        HorizontalDivider(color = DarkBorder.copy(alpha = 0.3f), modifier = Modifier.padding(start = 60.dp))
                    }
                }
            }
        }
    }
}
