package dev.leonardini.rehcorder.db

import android.content.Context
import android.os.Environment
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rehearsal")
data class Rehearsal(
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "external_storage") val externalStorage: Boolean,
    @ColumnInfo(name = "has_location_data", defaultValue = "FALSE") val hasLocationData: Boolean,
    @ColumnInfo(name = "latitude") val latitude: Double?,
    @ColumnInfo(name = "longitude") val longitude: Double?,
) {
    companion object {
        const val CREATED = 1
        const val RECORDED = 2
        const val NORMALIZED = 3
        const val PROCESSING = 4
        const val PROCESSED = 5

        fun create(applicationContext: Context): Pair<Long, String> {
            val timestamp = System.currentTimeMillis() / 1000
            val fileName = "$timestamp.m4a"

            val externalStorage =
                Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED && applicationContext.getExternalFilesDir(
                    null
                ) != null
            val baseDir =
                applicationContext.getExternalFilesDir(null) ?: applicationContext.filesDir

            val id = Database.getInstance(applicationContext).rehearsalDao().insert(
                Rehearsal(
                    date = timestamp,
                    fileName = fileName,
                    externalStorage = externalStorage
                )
            )

            return Pair(id, "${baseDir.absolutePath}/recordings/$fileName")
        }
    }

    @PrimaryKey(autoGenerate = true)
    var uid: Long = 0

    @ColumnInfo(name = "name")
    var name: String? = null

    @ColumnInfo(name = "status", defaultValue = CREATED.toString())
    var status: Int = CREATED
}
