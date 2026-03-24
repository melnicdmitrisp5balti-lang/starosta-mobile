package com.starosta.messenger.ui.navigation

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.navigation.*
import androidx.navigation.compose.*
import com.starosta.messenger.viewmodel.AuthState

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object PhoneAuth : Screen("phone_auth")
    object Main : Screen("main")
    object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
    object Search : Screen("search")
    object GroupCreate : Screen("group_create")
    object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
    object EditProfile : Screen("edit_profile")
}

sealed class BottomNavDestination(val route: String, val label: String, val icon: String) {
    object Chats : BottomNavDestination("chats", "Чаты", "chat")
    object Contacts : BottomNavDestination("contacts", "Контакты", "contacts")
    object Profile : BottomNavDestination("my_profile", "Профиль", "person")
}
