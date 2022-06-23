package dev.leonardini.rehcorder.db

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
    }

    @PrimaryKey(autoGenerate = true)
    var uid: Long = 0

    @ColumnInfo(name = "name")
    var name: String? = null

    @ColumnInfo(name = "status", defaultValue = CREATED.toString())
    var status: Int = CREATED
}
