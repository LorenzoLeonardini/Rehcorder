package dev.leonardini.rehcorder.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DB_NAME = "RehcorderDatabase"
private const val DB_VERSION = 1
const val TABLE_REHEARSALS = "Rehearsals"

class Database(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase?) {
        db!!.apply {
            execSQL("CREATE TABLE IF NOT EXISTS $TABLE_REHEARSALS (_ID INTEGER PRIMARY KEY, name TEXT, date INTEGER NOT NULL, songsCount INTEGER NOT NULL DEFAULT(0), processed INTEGER NOT NULL DEFAULT(FALSE))")
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }
}