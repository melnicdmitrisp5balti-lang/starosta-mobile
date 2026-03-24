package com.starosta.messenger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.starosta.messenger.ui.navigation.BottomNavDestination
import com.starosta.messenger.ui.theme.*
import com.starosta.messenger.viewmodel.AuthViewModel
import com.starosta.messenger.viewmodel.ChatViewModel
import com.starosta.messenger.viewmodel.UserViewModel

data class BottomNavItem(
    val destination: BottomNavDestination,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MainScreen(
    onNavigateToChat: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToGroupCreate: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onLogout: () -> Unit,
    chatViewModel: ChatViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var selectedDestination by remember { mutableStateOf<BottomNavDestination>(BottomNavDestination.Chats) }

    val navItems = remember {
        listOf(
            BottomNavItem(BottomNavDestination.Chats, Icons.Filled.Chat, Icons.Outlined.Chat),
            BottomNavItem(BottomNavDestination.Contacts, Icons.Filled.Search, Icons.Outlined.Search),
            BottomNavItem(BottomNavDestination.Profile, Icons.Filled.Person, Icons.Outlined.Person)
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                contentColor = TextPrimary,
                tonalElevation = 0.dp
            ) {
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = selectedDestination.route == item.destination.route,
                        onClick = { selectedDestination = item.destination },
                        icon = {
                            Icon(
                                imageVector = if (selectedDestination.route == item.destination.route)
                                    item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.destination.label
                            )
                        },
                        label = { Text(item.destination.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyanPrimary,
                            selectedTextColor = CyanPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = DarkCard
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedDestination is BottomNavDestination.Chats) {
                FloatingActionButton(
                    onClick = onNavigateToGroupCreate,
                    containerColor = CyanPrimary,
                    contentColor = DarkBackground,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Новый чат")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground)
        ) {
            when (selectedDestination) {
                is BottomNavDestination.Chats -> ChatListScreen(
                    onNavigateToChat = onNavigateToChat,
                    viewModel = chatViewModel
                )
                is BottomNavDestination.Contacts -> SearchScreen(
                    onBack = {},
                    onNavigateToChat = onNavigateToChat,
                    showBackButton = false,
                    userViewModel = userViewModel,
                    chatViewModel = chatViewModel
                )
                is BottomNavDestination.Profile -> ProfileScreen(
                    onNavigateToEditProfile = onNavigateToEditProfile,
                    onLogout = {
                        authViewModel.logout()
                        onLogout()
                    },
                    userViewModel = userViewModel
                )
            }
        }
    }
}
