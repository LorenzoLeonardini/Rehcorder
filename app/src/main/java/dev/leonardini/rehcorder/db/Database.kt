package dev.leonardini.rehcorder.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Singleton to retrieve AppDatabase instance
 */
object Database {
    private lateinit var database: AppDatabase

    fun getInstance(applicationContext: Context): AppDatabase {
        if (!::database.isInitialized) {
            database = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "rehearsal-database"
            ).build()
        }
        return database
    }
}