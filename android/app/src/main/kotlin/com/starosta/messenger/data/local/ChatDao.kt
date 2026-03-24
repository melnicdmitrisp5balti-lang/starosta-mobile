package com.starosta.messenger.data.local

import androidx.room.*
import com.starosta.messenger.data.models.Chat
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    fun getAllChats(): Flow<List<Chat>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: String): Chat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: Chat)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<Chat>)

    @Update
    suspend fun updateChat(chat: Chat)

    @Delete
    suspend fun deleteChat(chat: Chat)

    @Query("DELETE FROM chats")
    suspend fun deleteAllChats()

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChatById(chatId: String)
}
