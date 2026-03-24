package com.starosta.messenger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.starosta.messenger.ui.theme.*
import com.starosta.messenger.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: UserViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val updateSuccess by viewModel.updateSuccess.collectAsState()
    val error by viewModel.error.collectAsState()
    val focusManager = LocalFocusManager.current

    var name by remember(currentUser) { mutableStateOf(currentUser?.name ?: "") }
    var username by remember(currentUser) { mutableStateOf(currentUser?.username ?: "") }
    var status by remember(currentUser) { mutableStateOf(currentUser?.status ?: "") }

    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            viewModel.resetUpdateSuccess()
            onBack()
        }
    }

    LaunchedEffect(currentUser) {
        currentUser?.let {
            if (name.isEmpty()) name = it.name
            if (username.isEmpty()) username = it.username
            if (status.isEmpty()) status = it.status ?: ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
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
                Text(
                    "Редактировать профиль",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Имя", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CyanPrimary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                colors = authTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Username field
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Имя пользователя", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = CyanPrimary) },
                prefix = { Text("@", color = TextTertiary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                colors = authTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Status field
            OutlinedTextField(
                value = status,
                onValueChange = { if (it.length <= 150) status = it },
                label = { Text("Статус", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = CyanPrimary) },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                supportingText = { Text("${status.length}/150", color = TextTertiary, fontSize = 11.sp) },
                colors = authTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error!!,
                    color = ErrorRed,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.updateProfile(
                        name = name.trim().takeIf { it.isNotBlank() },
                        username = username.trim().takeIf { it.isNotBlank() },
                        status = status.trim()
                    )
                },
                enabled = name.isNotBlank() && username.isNotBlank() && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = DarkBackground,
                    disabledContainerColor = CyanPrimary.copy(alpha = 0.4f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = DarkBackground,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Сохранить", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
