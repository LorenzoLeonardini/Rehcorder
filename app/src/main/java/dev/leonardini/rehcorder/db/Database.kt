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

    // Location branch, this migration will be deleted in the future
    private val MIGRATION_2_1 = object : Migration(2, 1) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE rehearsal RENAME TO rehearsal_old")
            database.execSQL(
                "CREATE TABLE rehearsal (" +
                        "uid INTEGER NOT NULL PRIMARY KEY, name TEXT, status INTEGER NOT NULL DEFAULT(${Rehearsal.CREATED}), " +
                        "date INTEGER NOT NULL, file_name TEXT NOT NULL, external_storage INTEGER NOT NULL" +
                        ")"
            )
            database.execSQL(
                "INSERT INTO rehearsal (uid, name, status, date, file_name, external_storage) " +
                        "SELECT uid, name, status, date, file_name, external_storage FROM rehearsal_old"
            )
            database.execSQL("DROP TABLE rehearsal_old")
        }
    }

    fun getInstance(applicationContext: Context): AppDatabase {
        if (!::database.isInitialized) {
            database = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "rehearsal-database"
            ).addMigrations(MIGRATION_2_1).build()
        }
        return database
    }
}