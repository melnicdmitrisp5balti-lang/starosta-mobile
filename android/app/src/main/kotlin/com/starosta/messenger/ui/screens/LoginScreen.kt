package com.starosta.messenger.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.starosta.messenger.BuildConfig
import com.starosta.messenger.ui.theme.*
import com.starosta.messenger.viewmodel.AuthUiState
import com.starosta.messenger.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToPhoneAuth: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            viewModel.resetUiState()
            onLoginSuccess()
        }
    }

    // Animated orb values
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val orbOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb1"
    )
    val orbOffset2 by infiniteTransition.animateFloat(
        initialValue = (Math.PI).toFloat(),
        targetValue = (3 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBackground, DarkSurface, DarkBackground)
                )
            )
    ) {
        // Animated orbs
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawAnimatedOrb(
                center = Offset(
                    x = size.width * 0.2f + sin(orbOffset1) * 60f,
                    y = size.height * 0.15f + sin(orbOffset1 * 0.7f) * 40f
                ),
                radius = 180f,
                color = GradientOrbBlue
            )
            drawAnimatedOrb(
                center = Offset(
                    x = size.width * 0.85f + sin(orbOffset2) * 50f,
                    y = size.height * 0.7f + sin(orbOffset2 * 0.6f) * 60f
                ),
                radius = 220f,
                color = GradientOrbPurple
            )
            drawAnimatedOrb(
                center = Offset(
                    x = size.width * 0.5f + sin(orbOffset1 * 0.5f) * 70f,
                    y = size.height * 0.5f + sin(orbOffset2 * 0.4f) * 50f
                ),
                radius = 140f,
                color = Color(0x0A00D4E8)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Logo / Brand
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(CyanPrimary, TealDark))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    color = DarkBackground,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Starosta",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Войдите в свой аккаунт",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null, tint = CyanPrimary)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                colors = authTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = CyanPrimary)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (email.isNotBlank() && password.isNotBlank()) {
                            viewModel.login(email.trim(), password)
                        }
                    }
                ),
                colors = authTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Error message
            if (uiState is AuthUiState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    color = ErrorRed,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.login(email.trim(), password)
                },
                enabled = email.isNotBlank() && password.isNotBlank() && uiState !is AuthUiState.Loading,
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
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = DarkBackground,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Войти", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = DarkBorder)
                Text(
                    text = "  или  ",
                    color = TextTertiary,
                    fontSize = 12.sp
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = DarkBorder)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Sign-In button
            OutlinedButton(
                onClick = {
                    scope.launch {
                        try {
                            val credentialManager = CredentialManager.create(context)
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                                .build()
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()
                            val result = credentialManager.getCredential(
                                request = request,
                                context = context
                            )
                            val credential = result.credential
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            viewModel.handleGoogleSignIn(googleIdTokenCredential.idToken)
                        } catch (_: GetCredentialException) {
                            // handled silently; user cancelled or no accounts available
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Text("G", color = CyanPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Войти через Google", fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Phone auth button
            OutlinedButton(
                onClick = onNavigateToPhoneAuth,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Войти по номеру телефона", fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Нет аккаунта? ", color = TextSecondary, fontSize = 14.sp)
                Text(
                    text = "Зарегистрироваться",
                    color = CyanPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun DrawScope.drawAnimatedOrb(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

@Composable
internal fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanPrimary,
    unfocusedBorderColor = DarkBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = CyanPrimary,
    focusedContainerColor = DarkCard,
    unfocusedContainerColor = DarkCard,
    focusedLabelColor = CyanPrimary,
    unfocusedLabelColor = TextSecondary
)
