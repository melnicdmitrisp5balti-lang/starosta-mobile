package com.starosta.messenger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starosta.messenger.data.models.Result
import com.starosta.messenger.data.models.User
import com.starosta.messenger.repository.AuthRepository
import com.starosta.messenger.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            authRepository.userId.collect { userId ->
                userId?.let {
                    userRepository.getLocalUser(it).collect { user ->
                        _currentUser.value = user
                    }
                }
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = userRepository.getMe()) {
                is Result.Success -> {
                    _currentUser.value = result.data
                    _error.value = null
                }
                is Result.Error -> _error.value = result.message
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun updateProfile(name: String?, username: String?, status: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = userRepository.updateProfile(name, username, status)) {
                is Result.Success -> {
                    _currentUser.value = result.data
                    _updateSuccess.value = true
                    _error.value = null
                }
                is Result.Error -> _error.value = result.message
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = userRepository.searchUsers(query)) {
                is Result.Success -> {
                    _searchResults.value = result.data
                    _error.value = null
                }
                is Result.Error -> _error.value = result.message
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    fun clearError() {
        _error.value = null
    }

    fun resetUpdateSuccess() {
        _updateSuccess.value = false
    }
}
