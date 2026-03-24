package com.starosta.messenger.repository

import com.starosta.messenger.data.local.MessageDao
import com.starosta.messenger.data.models.*
import com.starosta.messenger.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val apiService: ApiService,
    private val messageDao: MessageDao
) {
    fun getLocalMessages(chatId: String): Flow<List<Message>> =
        messageDao.getMessagesByChatId(chatId)

    suspend fun getMessages(chatId: String, cursor: String? = null): Result<MessagesResponse> {
        return try {
            val response = apiService.getMessages(chatId, cursor)
            if (response.isSuccessful) {
                val body = response.body()!!
                messageDao.insertMessages(body.messages.map { it.toMessage() })
                Result.Success(body)
            } else {
                Result.Error("Failed to load messages", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun sendMessage(
        chatId: String,
        content: String? = null,
        fileUrl: String? = null,
        fileName: String? = null,
        fileType: String? = null,
        fileSize: Int? = null,
        replyToId: String? = null
    ): Result<MessageWithDetails> {
        return try {
            val response = apiService.sendMessage(
                chatId,
                SendMessageRequest(content, fileUrl, fileName, fileType, fileSize, replyToId)
            )
            if (response.isSuccessful) {
                val message = response.body()!!
                messageDao.insertMessage(message.toMessage())
                Result.Success(message)
            } else {
                Result.Error("Failed to send message", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun editMessage(chatId: String, messageId: String, content: String): Result<MessageWithDetails> {
        return try {
            val response = apiService.editMessage(chatId, messageId, EditMessageRequest(content))
            if (response.isSuccessful) {
                val message = response.body()!!
                messageDao.editMessage(messageId, content)
                Result.Success(message)
            } else {
                Result.Error("Failed to edit message", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun deleteMessage(chatId: String, messageId: String): Result<Unit> {
        return try {
            val response = apiService.deleteMessage(chatId, messageId)
            if (response.isSuccessful) {
                messageDao.softDeleteMessage(messageId)
                Result.Success(Unit)
            } else {
                Result.Error("Failed to delete message", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun pinMessage(chatId: String, messageId: String): Result<PinnedMessage> {
        return try {
            val response = apiService.pinMessage(chatId, messageId)
            if (response.isSuccessful) Result.Success(response.body()!!)
            else Result.Error("Failed to pin message", response.code())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun unpinMessage(chatId: String, messageId: String): Result<Unit> {
        return try {
            val response = apiService.unpinMessage(chatId, messageId)
            if (response.isSuccessful) Result.Success(Unit)
            else Result.Error("Failed to unpin message", response.code())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun markMessagesRead(chatId: String, messageIds: List<String>): Result<Unit> {
        return try {
            val response = apiService.markMessagesRead(chatId, MarkReadRequest(messageIds))
            if (response.isSuccessful) Result.Success(Unit)
            else Result.Error("Failed to mark as read", response.code())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun uploadFile(file: MultipartBody.Part): Result<FileUploadResponse> {
        return try {
            val response = apiService.uploadFile(file)
            if (response.isSuccessful) Result.Success(response.body()!!)
            else Result.Error("Failed to upload file", response.code())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    private fun MessageWithDetails.toMessage(): Message = Message(
        id = id,
        chatId = chatId,
        senderId = senderId,
        content = content,
        fileUrl = fileUrl,
        fileName = fileName,
        fileType = fileType,
        fileSize = fileSize,
        status = status,
        isEdited = isEdited,
        isDeleted = isDeleted,
        replyToId = replyToId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
