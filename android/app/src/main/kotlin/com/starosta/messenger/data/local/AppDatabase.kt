package com.starosta.messenger.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.starosta.messenger.data.models.Chat
import com.starosta.messenger.data.models.Message
import com.starosta.messenger.data.models.User

@Database(
    entities = [User::class, Chat::class, Message::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
}
