package com.starosta.messenger.data.local

import androidx.room.*
import com.starosta.messenger.data.models.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt ASC")
    fun getMessagesByChatId(chatId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): Message?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<Message>)

    @Update
    suspend fun updateMessage(message: Message)

    @Delete
    suspend fun deleteMessage(message: Message)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesByChatId(chatId: String)

    @Query("UPDATE messages SET isDeleted = 1 WHERE id = :messageId")
    suspend fun softDeleteMessage(messageId: String)

    @Query("UPDATE messages SET content = :content, isEdited = 1 WHERE id = :messageId")
    suspend fun editMessage(messageId: String, content: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
}
