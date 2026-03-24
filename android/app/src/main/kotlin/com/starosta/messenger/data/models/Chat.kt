package com.starosta.messenger.data.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey
    val id: String,
    val type: String = "PRIVATE",
    val name: String? = null,
    val avatarUrl: String? = null,
    val createdById: String,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class ChatMember(
    val id: String,
    val chatId: String,
    val userId: String,
    val user: User,
    val isAdmin: Boolean = false,
    val joinedAt: String? = null
)

@Serializable
data class ChatWithDetails(
    val id: String,
    val type: String = "PRIVATE",
    val name: String? = null,
    val avatarUrl: String? = null,
    val createdById: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val members: List<ChatMember> = emptyList(),
    val messages: List<Message> = emptyList(),
    val pinnedMessages: List<PinnedMessage> = emptyList(),
    val _count: MessageCount? = null
)

@Serializable
data class MessageCount(
    val messages: Int = 0
)

@Serializable
data class CreatePrivateChatRequest(
    val userId: String
)

@Serializable
data class CreateGroupChatRequest(
    val name: String,
    val memberIds: List<String>
)

@Serializable
data class UpdateChatRequest(
    val name: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class AddMemberRequest(
    val userId: String
)
