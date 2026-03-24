package com.starosta.messenger.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey
    val id: String,
    val chatId: String,
    val senderId: String,
    val content: String? = null,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileType: String? = null,
    val fileSize: Int? = null,
    val status: String = "SENT",
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val replyToId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class MessageWithDetails(
    val id: String,
    val chatId: String,
    val senderId: String,
    val content: String? = null,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileType: String? = null,
    val fileSize: Int? = null,
    val status: String = "SENT",
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val replyToId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val sender: User? = null,
    val replyTo: MessageWithDetails? = null,
    val readBy: List<MessageRead> = emptyList()
)

@Serializable
data class MessageRead(
    val userId: String,
    val readAt: String? = null
)

@Serializable
data class MessagesResponse(
    val messages: List<MessageWithDetails>,
    val hasMore: Boolean,
    val nextCursor: String? = null
)

@Serializable
data class SendMessageRequest(
    val content: String? = null,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileType: String? = null,
    val fileSize: Int? = null,
    val replyToId: String? = null
)

@Serializable
data class EditMessageRequest(
    val content: String
)

@Serializable
data class MarkReadRequest(
    val messageIds: List<String>
)

@Serializable
data class PinnedMessage(
    val id: String,
    val chatId: String,
    val messageId: String,
    val pinnedById: String,
    val message: MessageWithDetails? = null,
    val createdAt: String? = null
)

@Serializable
data class FileUploadResponse(
    val url: String,
    val fileName: String,
    val fileType: String,
    val fileSize: Int
)
