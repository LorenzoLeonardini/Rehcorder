package dev.leonardini.rehcorder.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_recording")
data class SongRecording(
    @PrimaryKey(autoGenerate = true) val uid: Long,
    @ColumnInfo(name = "song_id") val songId: Long,
    @ColumnInfo(name = "recording_id") val recordingId: Long,
)