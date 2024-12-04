package com.example.calpal.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.calpal.data.dao.*
import com.example.calpal.data.entities.*

/**
 * App's Room database configuration for storing application data.
 * Defines database schema and provides access to Data Access Objects (DAOs).
 *
 * Includes entities: Event, Note, UserLogin, and UserSignUp
 * Database version: 1
 */
@Database(
    entities = [
        Event::class,
        Note::class,
        UserLogin::class,
        UserSignUp::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Abstract methods to provide DAOs for different entities
    abstract fun eventDao(): EventDao
    abstract fun noteDao(): NoteDao
    abstract fun userLoginDao(): UserLoginDao
    abstract fun userSignUpDao(): UserSignUpDao

    companion object {
        // Volatile ensures visibility of INSTANCE across threads
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Provides singleton database instance.
         * Creates database if not already existing.
         *
         * @param context Application context for database creation
         * @return Singleton AppDatabase instance
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calpal_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}