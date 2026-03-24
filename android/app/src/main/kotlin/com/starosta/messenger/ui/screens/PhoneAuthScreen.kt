package com.starosta.messenger.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.starosta.messenger.ui.theme.*
import com.starosta.messenger.viewmodel.AuthUiState
import com.starosta.messenger.viewmodel.AuthViewModel
import kotlin.math.sin

@Composable
fun PhoneAuthScreen(
    onBack: () -> Unit,
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) } // 1 = phone, 2 = otp

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.OtpSent -> {
                step = 2
                viewModel.resetUiState()
            }
            is AuthUiState.Success -> {
                viewModel.resetUiState()
                onAuthSuccess()
            }
            else -> {}
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val orbOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBackground, DarkSurface, DarkBackground)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawPhoneOrb(
                center = Offset(
                    x = size.width * 0.15f + sin(orbOffset) * 50f,
                    y = size.height * 0.2f + sin(orbOffset * 0.7f) * 40f
                ),
                radius = 170f,
                color = GradientOrbBlue
            )
            drawPhoneOrb(
                center = Offset(
                    x = size.width * 0.9f + sin(orbOffset * 1.2f) * 45f,
                    y = size.height * 0.65f + sin(orbOffset * 0.5f) * 55f
                ),
                radius = 210f,
                color = GradientOrbPurple
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(52.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (step == 2) {
                        step = 1
                        otp = ""
                        viewModel.resetUiState()
                    } else {
                        onBack()
                    }
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(CyanPrimary, TealDark))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = null,
                    tint = DarkBackground,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                },
                label = "step_content"
            ) { currentStep ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (currentStep == 1) {
                        Text(
                            "Вход по телефону",
                            color = TextPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Введите номер телефона для получения кода",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp, bottom = 32.dp)
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Номер телефона", color = TextSecondary) },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = CyanPrimary)
                            },
                            placeholder = { Text("+7 900 000 00 00", color = TextTertiary) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (phone.isNotBlank()) viewModel.sendOtp(phone.trim())
                                }
                            ),
                            colors = authTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

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

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.sendOtp(phone.trim())
                            },
                            enabled = phone.isNotBlank() && uiState !is AuthUiState.Loading,
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
                                Text("Получить код", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    } else {
                        Text(
                            "Введите код",
                            color = TextPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Код отправлен на номер $phone",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp, bottom = 32.dp)
                        )

                        OtpInputField(
                            otp = otp,
                            onOtpChange = { otp = it }
                        )

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

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.verifyOtp(phone.trim(), otp.trim())
                            },
                            enabled = otp.length >= 4 && uiState !is AuthUiState.Loading,
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
                                Text("Подтвердить", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(onClick = {
                            otp = ""
                            viewModel.sendOtp(phone.trim())
                        }) {
                            Text("Отправить код повторно", color = CyanPrimary, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OtpInputField(
    otp: String,
    onOtpChange: (String) -> Unit
) {
    OutlinedTextField(
        value = otp,
        onValueChange = { if (it.length <= 6) onOtpChange(it) },
        label = { Text("Код подтверждения", color = TextSecondary) },
        placeholder = { Text("_ _ _ _ _ _", color = TextTertiary, letterSpacing = 6.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done
        ),
        colors = authTextFieldColors(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun DrawScope.drawPhoneOrb(center: Offset, radius: Float, color: Color) {
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
