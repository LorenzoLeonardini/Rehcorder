package dev.leonardini.rehcorder.db

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.leonardini.rehcorder.Utils

@Entity(tableName = "rehearsal")
data class Rehearsal(
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "external_storage") val externalStorage: Boolean
) {
    companion object {
        const val CREATED = 1
        const val RECORDED = 2
        const val NORMALIZED = 3
        const val PROCESSING = 4
        const val PROCESSED = 5

        suspend fun create(applicationContext: Context): Pair<Long, String> {
            val timestamp = System.currentTimeMillis() / 1000
            val fileName = "$timestamp.m4a"

            val (externalStorage, baseDir) = Utils.getPreferredStorageLocation(applicationContext)

            val id = Database.getInstance(applicationContext).rehearsalDao().insert(
                Rehearsal(
                    date = timestamp,
                    fileName = fileName,
                    externalStorage = externalStorage
                )
            )

            return Pair(id, Utils.getRecordingPath(baseDir, fileName))
        }
    }

    @PrimaryKey(autoGenerate = true)
    var uid: Long = 0

    @ColumnInfo(name = "name")
    var name: String? = null

    @ColumnInfo(name = "status", defaultValue = CREATED.toString())
    var status: Int = CREATED
}
