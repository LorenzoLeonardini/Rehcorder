package dev.leonardini.rehcorder.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_recording")
data class SongRecording(
    @ColumnInfo(name = "song_id") val songId: Long,
    @ColumnInfo(name = "recording_id") val recordingId: Long,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "external_storage") val externalStorage: Boolean
) {
    @PrimaryKey(autoGenerate = true)
    var uid: Long = 0

    @ColumnInfo
    var version: Int = 0
}
