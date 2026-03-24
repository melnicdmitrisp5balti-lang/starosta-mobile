package com.starosta.messenger.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.*
import androidx.navigation.compose.*
import com.starosta.messenger.ui.screens.*
import com.starosta.messenger.viewmodel.AuthState

@Composable
fun AppNavigation(
    navController: NavHostController,
    authState: AuthState
) {
    val startDestination = when (authState) {
        is AuthState.Loading -> Screen.Login.route
        is AuthState.Unauthenticated -> Screen.Login.route
        is AuthState.Authenticated -> Screen.Main.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToPhoneAuth = { navController.navigate(Screen.PhoneAuth.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PhoneAuth.route) {
            PhoneAuthScreen(
                onBack = { navController.popBackStack() },
                onAuthSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToChat = { chatId ->
                    navController.navigate(Screen.Chat.createRoute(chatId))
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToGroupCreate = {
                    navController.navigate(Screen.GroupCreate.route)
                },
                onNavigateToEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            Screen.Chat.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStack ->
            val chatId = backStack.arguments?.getString("chatId") ?: return@composable
            ChatScreen(
                chatId = chatId,
                onBack = { navController.popBackStack() },
                onNavigateToUserProfile = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onNavigateToChat = { chatId ->
                    navController.navigate(Screen.Chat.createRoute(chatId)) {
                        popUpTo(Screen.Search.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.GroupCreate.route) {
            GroupCreateScreen(
                onBack = { navController.popBackStack() },
                onGroupCreated = { chatId ->
                    navController.navigate(Screen.Chat.createRoute(chatId)) {
                        popUpTo(Screen.GroupCreate.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            Screen.Profile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStack ->
            val userId = backStack.arguments?.getString("userId") ?: return@composable
            UserProfileScreen(
                userId = userId,
                onBack = { navController.popBackStack() },
                onStartChat = { chatId ->
                    navController.navigate(Screen.Chat.createRoute(chatId))
                }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
