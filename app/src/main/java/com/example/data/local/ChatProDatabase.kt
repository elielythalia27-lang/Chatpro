package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MessageEntity::class, UserEntity::class, StatusEntity::class, PostEntity::class],
    version = 3,
    exportSchema = false
)
abstract class ChatProDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun userDao(): UserDao
    abstract fun statusDao(): StatusDao
    abstract fun postDao(): PostDao

    companion object {
        @Volatile
        private var INSTANCE: ChatProDatabase? = null

        fun getDatabase(context: Context): ChatProDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatProDatabase::class.java,
                    "chatpro_secure.db"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
