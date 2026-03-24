package com.starosta.messenger.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starosta.messenger.data.models.*
import com.starosta.messenger.data.remote.SocketEvent
import com.starosta.messenger.data.remote.WebSocketService
import com.starosta.messenger.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val webSocketService: WebSocketService
) : ViewModel() {

    private val _messages = MutableStateFlow<List<MessageWithDetails>>(emptyList())
    val messages: StateFlow<List<MessageWithDetails>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _typingUsers = MutableStateFlow<Map<String, String>>(emptyMap())
    val typingUsers: StateFlow<Map<String, String>> = _typingUsers

    private val _replyToMessage = MutableStateFlow<MessageWithDetails?>(null)
    val replyToMessage: StateFlow<MessageWithDetails?> = _replyToMessage

    private val _editingMessage = MutableStateFlow<MessageWithDetails?>(null)
    val editingMessage: StateFlow<MessageWithDetails?> = _editingMessage

    private var currentChatId: String? = null
    private var nextCursor: String? = null
    private var typingJob: Job? = null

    init {
        observeSocketEvents()
    }

    private fun observeSocketEvents() {
        viewModelScope.launch {
            webSocketService.events.collect { event ->
                when (event) {
                    is SocketEvent.NewMessage -> {
                        if (event.message.chatId == currentChatId) {
                            _messages.update { current ->
                                if (current.any { it.id == event.message.id }) current
                                else current + event.message
                            }
                        }
                    }
                    is SocketEvent.MessageEdited -> {
                        if (event.message.chatId == currentChatId) {
                            _messages.update { current ->
                                current.map { if (it.id == event.message.id) event.message else it }
                            }
                        }
                    }
                    is SocketEvent.MessageDeleted -> {
                        if (event.chatId == currentChatId) {
                            _messages.update { current ->
                                current.map {
                                    if (it.id == event.messageId)
                                        it.copy(isDeleted = true, content = null)
                                    else it
                                }
                            }
                        }
                    }
                    is SocketEvent.TypingStart -> {
                        if (event.chatId == currentChatId) {
                            _typingUsers.update { it + (event.userId to event.userName) }
                        }
                    }
                    is SocketEvent.TypingStop -> {
                        if (event.chatId == currentChatId) {
                            _typingUsers.update { it - event.userId }
                        }
                    }
                    is SocketEvent.MessageRead -> {
                        if (event.chatId == currentChatId) {
                            _messages.update { current ->
                                current.map { msg ->
                                    if (event.messageIds.contains(msg.id)) {
                                        val updatedReadBy = msg.readBy + MessageRead(event.userId)
                                        msg.copy(
                                            readBy = updatedReadBy,
                                            status = "READ"
                                        )
                                    } else msg
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadMessages(chatId: String) {
        currentChatId = chatId
        _messages.value = emptyList()
        nextCursor = null
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = messageRepository.getMessages(chatId)) {
                is Result.Success -> {
                    _messages.value = result.data.messages
                    _hasMore.value = result.data.hasMore
                    nextCursor = result.data.nextCursor
                    _error.value = null
                }
                is Result.Error -> _error.value = result.message
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun loadMoreMessages() {
        val chatId = currentChatId ?: return
        if (!_hasMore.value || _isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            when (val result = messageRepository.getMessages(chatId, nextCursor)) {
                is Result.Success -> {
                    _messages.update { current -> result.data.messages + current }
                    _hasMore.value = result.data.hasMore
                    nextCursor = result.data.nextCursor
                }
                is Result.Error -> _error.value = result.message
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun sendMessage(content: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            val replyTo = _replyToMessage.value
            clearReplyTo()
            when (val result = messageRepository.sendMessage(
                chatId = chatId,
                content = content,
                replyToId = replyTo?.id
            )) {
                is Result.Error -> _error.value = result.message
                else -> {}
            }
        }
    }

    fun sendFile(chatId: String, fileUri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(fileUri) ?: "application/octet-stream"
                val inputStream = contentResolver.openInputStream(fileUri) ?: return@launch

                val tempFile = File.createTempFile("upload_", null, context.cacheDir)
                tempFile.outputStream().use { out -> inputStream.copyTo(out) }
                inputStream.close()

                val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", tempFile.name, requestBody)

                when (val uploadResult = messageRepository.uploadFile(part)) {
                    is Result.Success -> {
                        val file = uploadResult.data
                        messageRepository.sendMessage(
                            chatId = chatId,
                            fileUrl = file.url,
                            fileName = file.fileName,
                            fileType = file.fileType,
                            fileSize = file.fileSize
                        )
                    }
                    is Result.Error -> _error.value = uploadResult.message
                    else -> {}
                }

                tempFile.delete()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to send file"
            }
        }
    }

    fun editMessage(messageId: String, content: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            clearEditingMessage()
            when (val result = messageRepository.editMessage(chatId, messageId, content)) {
                is Result.Error -> _error.value = result.message
                else -> {}
            }
        }
    }

    fun deleteMessage(messageId: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            when (val result = messageRepository.deleteMessage(chatId, messageId)) {
                is Result.Error -> _error.value = result.message
                else -> {}
            }
        }
    }

    fun pinMessage(messageId: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            when (val result = messageRepository.pinMessage(chatId, messageId)) {
                is Result.Error -> _error.value = result.message
                else -> {}
            }
        }
    }

    fun unpinMessage(messageId: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            when (val result = messageRepository.unpinMessage(chatId, messageId)) {
                is Result.Error -> _error.value = result.message
                else -> {}
            }
        }
    }

    fun markMessagesRead(messageIds: List<String>) {
        val chatId = currentChatId ?: return
        if (messageIds.isEmpty()) return
        viewModelScope.launch {
            messageRepository.markMessagesRead(chatId, messageIds)
            webSocketService.markMessagesRead(chatId, messageIds)
        }
    }

    fun sendTypingStart(chatId: String) {
        webSocketService.sendTypingStart(chatId)
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(3000)
            webSocketService.sendTypingStop(chatId)
        }
    }

    fun sendTypingStop(chatId: String) {
        typingJob?.cancel()
        webSocketService.sendTypingStop(chatId)
    }

    fun setReplyTo(message: MessageWithDetails) {
        _replyToMessage.value = message
    }

    fun clearReplyTo() {
        _replyToMessage.value = null
    }

    fun setEditingMessage(message: MessageWithDetails) {
        _editingMessage.value = message
    }

    fun clearEditingMessage() {
        _editingMessage.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
