package com.starosta.messenger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starosta.messenger.data.models.*
import com.starosta.messenger.data.remote.SocketEvent
import com.starosta.messenger.data.remote.WebSocketService
import com.starosta.messenger.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val webSocketService: WebSocketService
) : ViewModel() {

    private val _chats = MutableStateFlow<List<ChatWithDetails>>(emptyList())
    val chats: StateFlow<List<ChatWithDetails>> = _chats

    private val _selectedChat = MutableStateFlow<ChatWithDetails?>(null)
    val selectedChat: StateFlow<ChatWithDetails?> = _selectedChat

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadChats()
        observeSocketEvents()
    }

    private fun observeSocketEvents() {
        viewModelScope.launch {
            webSocketService.events.collect { event ->
                when (event) {
                    is SocketEvent.ChatNew -> loadChats()
                    is SocketEvent.NewMessage -> {
                        // Update chat list to show latest message
                        refreshChatIfNeeded(event.message.chatId)
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadChats() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = chatRepository.getChats()) {
                is Result.Success -> {
                    _chats.value = result.data
                    _error.value = null
                }
                is Result.Error -> _error.value = result.message
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun selectChat(chatId: String) {
        viewModelScope.launch {
            when (val result = chatRepository.getChatById(chatId)) {
                is Result.Success -> {
                    _selectedChat.value = result.data
                    webSocketService.joinChat(chatId)
                }
                is Result.Error -> _error.value = result.message
                else -> {}
            }
        }
    }

    fun createPrivateChat(userId: String, onSuccess: (ChatWithDetails) -> Unit) {
        viewModelScope.launch {
            when (val result = chatRepository.createPrivateChat(userId)) {
                is Result.Success -> {
                    loadChats()
                    onSuccess(result.data)
                }
                is Result.Error -> _error.value = result.message
                else -> {}
            }
        }
    }

    fun createGroupChat(name: String, memberIds: List<String>, onSuccess: (ChatWithDetails) -> Unit) {
        viewModelScope.launch {
            when (val result = chatRepository.createGroupChat(name, memberIds)) {
                is Result.Success -> {
                    loadChats()
                    onSuccess(result.data)
                }
                is Result.Error -> _error.value = result.message
                else -> {}
            }
        }
    }

    fun updateGroupChat(chatId: String, name: String?, avatarUrl: String?) {
        viewModelScope.launch {
            when (val result = chatRepository.updateChat(chatId, name, avatarUrl)) {
                is Result.Success -> {
                    _selectedChat.value = result.data
                    loadChats()
                }
                is Result.Error -> _error.value = result.message
                else -> {}
            }
        }
    }

    fun addMember(chatId: String, userId: String) {
        viewModelScope.launch {
            when (val result = chatRepository.addMember(chatId, userId)) {
                is Result.Success -> selectChat(chatId)
                is Result.Error -> _error.value = result.message
                else -> {}
            }
        }
    }

    fun removeMember(chatId: String, userId: String) {
        viewModelScope.launch {
            when (val result = chatRepository.removeMember(chatId, userId)) {
                is Result.Success -> selectChat(chatId)
                is Result.Error -> _error.value = result.message
                else -> {}
            }
        }
    }

    fun leaveChat(chatId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val result = chatRepository.leaveChat(chatId)) {
                is Result.Success -> {
                    _selectedChat.value = null
                    loadChats()
                    webSocketService.leaveChat(chatId)
                    onSuccess()
                }
                is Result.Error -> _error.value = result.message
                else -> {}
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun refreshChatIfNeeded(chatId: String) {
        if (_chats.value.any { it.id == chatId }) {
            viewModelScope.launch {
                when (val result = chatRepository.getChatById(chatId)) {
                    is Result.Success -> {
                        val updated = result.data
                        _chats.update { current ->
                            current.map { if (it.id == chatId) updated else it }
                                .sortedByDescending { it.updatedAt }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}
