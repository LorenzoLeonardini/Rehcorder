package dev.leonardini.rehcorder.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Rehearsal::class, Song::class, SongRecording::class], version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rehearsalDao(): RehearsalDao
    abstract fun songDao(): SongDao
    abstract fun songRecordingDao(): SongRecordingDao
}