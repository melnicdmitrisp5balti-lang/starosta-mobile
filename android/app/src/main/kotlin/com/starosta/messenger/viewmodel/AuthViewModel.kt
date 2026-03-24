package com.starosta.messenger.viewmodel

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.starosta.messenger.BuildConfig
import com.starosta.messenger.data.models.AuthResponse
import com.starosta.messenger.data.models.Result
import com.starosta.messenger.data.models.User
import com.starosta.messenger.data.remote.WebSocketService
import com.starosta.messenger.repository.AuthRepository
import com.starosta.messenger.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    object Success : AuthUiState()
    object OtpSent : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val webSocketService: WebSocketService
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { isLoggedIn ->
                if (isLoggedIn) {
                    loadCurrentUser()
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            }
        }
    }

    private suspend fun loadCurrentUser() {
        authRepository.userId.first()?.let { userId ->
            userRepository.getLocalUser(userId).first()?.let { user ->
                _authState.value = AuthState.Authenticated(user)
                connectWebSocket()
            } ?: run {
                when (val result = userRepository.getMe()) {
                    is Result.Success -> {
                        _authState.value = AuthState.Authenticated(result.data)
                        connectWebSocket()
                    }
                    else -> _authState.value = AuthState.Unauthenticated
                }
            }
        } ?: run {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.register(email, password, name)) {
                is Result.Success -> {
                    _authState.value = AuthState.Authenticated(result.data.user)
                    _uiState.value = AuthUiState.Success
                    connectWebSocket()
                }
                is Result.Error -> _uiState.value = AuthUiState.Error(result.message)
                else -> {}
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.login(email, password)) {
                is Result.Success -> {
                    _authState.value = AuthState.Authenticated(result.data.user)
                    _uiState.value = AuthUiState.Success
                    connectWebSocket()
                }
                is Result.Error -> _uiState.value = AuthUiState.Error(result.message)
                else -> {}
            }
        }
    }

    fun sendOtp(phone: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.sendOtp(phone)) {
                is Result.Success -> _uiState.value = AuthUiState.OtpSent
                is Result.Error -> _uiState.value = AuthUiState.Error(result.message)
                else -> {}
            }
        }
    }

    fun verifyOtp(phone: String, code: String, name: String? = null) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.verifyOtp(phone, code, name)) {
                is Result.Success -> {
                    _authState.value = AuthState.Authenticated(result.data.user)
                    _uiState.value = AuthUiState.Success
                    connectWebSocket()
                }
                is Result.Error -> _uiState.value = AuthUiState.Error(result.message)
                else -> {}
            }
        }
    }

    fun handleGoogleSignIn(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.googleSignIn(idToken)) {
                is Result.Success -> {
                    _authState.value = AuthState.Authenticated(result.data.user)
                    _uiState.value = AuthUiState.Success
                    connectWebSocket()
                }
                is Result.Error -> _uiState.value = AuthUiState.Error(result.message)
                else -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = AuthState.Unauthenticated
            _uiState.value = AuthUiState.Idle
        }
    }

    fun updateFcmToken(token: String) {
        viewModelScope.launch {
            authRepository.updateFcmToken(token)
        }
    }

    fun resetUiState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun connectWebSocket() {
        viewModelScope.launch {
            authRepository.accessToken.first()?.let { token ->
                if (!webSocketService.isConnected()) {
                    webSocketService.connect(BuildConfig.WS_URL, token)
                }
            }
        }
    }
}
