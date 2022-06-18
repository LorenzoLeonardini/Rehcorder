package dev.leonardini.rehcorder.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DB_NAME = "RehcorderDatabase"
private const val DB_VERSION = 1
const val TABLE_REHEARSALS = "Rehearsals"

const val REHEARSALS_ID = "_ID"
const val REHEARSALS_NAME = "name"
const val REHEARSALS_DATE = "date"
const val REHEARSALS_SONGS_COUNT = "songsCount"
const val REHEARSALS_PROCESSED = "processed"
const val REHEARSALS_FILE_NAME = "fileName"
const val REHEARSALS_EXTERNAL_STORAGE = "externalStorage"

class Database(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase?) {
        db!!.apply {
            execSQL(
                "CREATE TABLE IF NOT EXISTS $TABLE_REHEARSALS (" +
                        "$REHEARSALS_ID INTEGER PRIMARY KEY, " +
                        "$REHEARSALS_NAME TEXT, " +
                        "$REHEARSALS_DATE INTEGER NOT NULL, " +
                        "$REHEARSALS_SONGS_COUNT INTEGER NOT NULL DEFAULT(0), " +
                        "$REHEARSALS_PROCESSED INTEGER NOT NULL DEFAULT(FALSE), " +
                        "$REHEARSALS_FILE_NAME TEXT, " +
                        "$REHEARSALS_EXTERNAL_STORAGE INTEGER NOT NULL" +
                        ")"
            )
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }
}