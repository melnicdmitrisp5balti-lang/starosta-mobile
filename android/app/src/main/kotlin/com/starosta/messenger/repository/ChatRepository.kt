package com.starosta.messenger.repository

import com.starosta.messenger.data.local.ChatDao
import com.starosta.messenger.data.models.*
import com.starosta.messenger.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val apiService: ApiService,
    private val chatDao: ChatDao
) {
    fun getLocalChats(): Flow<List<Chat>> = chatDao.getAllChats()

    suspend fun getChats(): Result<List<ChatWithDetails>> {
        return try {
            val response = apiService.getChats()
            if (response.isSuccessful) {
                val chats = response.body()!!
                chatDao.insertChats(chats.map { it.toChat() })
                Result.Success(chats)
            } else {
                Result.Error("Failed to load chats", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getChatById(chatId: String): Result<ChatWithDetails> {
        return try {
            val response = apiService.getChatById(chatId)
            if (response.isSuccessful) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("Chat not found", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun createPrivateChat(userId: String): Result<ChatWithDetails> {
        return try {
            val response = apiService.createPrivateChat(CreatePrivateChatRequest(userId))
            if (response.isSuccessful) {
                val chat = response.body()!!
                chatDao.insertChat(chat.toChat())
                Result.Success(chat)
            } else {
                Result.Error("Failed to create chat", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun createGroupChat(name: String, memberIds: List<String>): Result<ChatWithDetails> {
        return try {
            val response = apiService.createGroupChat(CreateGroupChatRequest(name, memberIds))
            if (response.isSuccessful) {
                val chat = response.body()!!
                chatDao.insertChat(chat.toChat())
                Result.Success(chat)
            } else {
                Result.Error("Failed to create group", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun updateChat(chatId: String, name: String?, avatarUrl: String?): Result<ChatWithDetails> {
        return try {
            val response = apiService.updateChat(chatId, UpdateChatRequest(name, avatarUrl))
            if (response.isSuccessful) {
                val chat = response.body()!!
                chatDao.insertChat(chat.toChat())
                Result.Success(chat)
            } else {
                Result.Error("Failed to update chat", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun addMember(chatId: String, userId: String): Result<Unit> {
        return try {
            val response = apiService.addMember(chatId, AddMemberRequest(userId))
            if (response.isSuccessful) Result.Success(Unit)
            else Result.Error("Failed to add member", response.code())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun removeMember(chatId: String, userId: String): Result<Unit> {
        return try {
            val response = apiService.removeMember(chatId, userId)
            if (response.isSuccessful) Result.Success(Unit)
            else Result.Error("Failed to remove member", response.code())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun leaveChat(chatId: String): Result<Unit> {
        return try {
            val response = apiService.leaveChat(chatId)
            if (response.isSuccessful) {
                chatDao.deleteChatById(chatId)
                Result.Success(Unit)
            } else {
                Result.Error("Failed to leave chat", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    private fun ChatWithDetails.toChat(): Chat = Chat(
        id = id,
        type = type,
        name = name,
        avatarUrl = avatarUrl,
        createdById = createdById,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
