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

        fun create(
            applicationContext: Context,
            permissionToGetCoarseLocationAccepted: Boolean,
            permissionToGetFineLocationAccepted: Boolean
        ): Pair<Long, String> {
            val timestamp = System.currentTimeMillis() / 1000
            val fileName = "$timestamp.m4a"

            val (externalStorage, baseDir) = Utils.getPreferredStorageLocation(applicationContext)

            // Get location
            val (info, location) = Utils.getLocation(
                applicationContext,
                permissionToGetFineLocationAccepted,
                permissionToGetCoarseLocationAccepted
            )
            val (hasLocationData, willComputeNewLocation) = info
            val (latitude, longitude, provider) = location

            // Create rehearsal
            val id = Database.getInstance(applicationContext).rehearsalDao().insert(
                Rehearsal(
                    date = timestamp,
                    fileName = fileName,
                    externalStorage = externalStorage,
                    hasLocationData = hasLocationData,
                    latitude = latitude,
                    longitude = longitude
                )
            )

            // Callback for updated location
            if (willComputeNewLocation) {
                Utils.getUpdatedLocation(applicationContext, provider!!) { location ->
                    if (location != null) {
                        Thread {
                            Database.getInstance(applicationContext).rehearsalDao()
                                .updateLocation(id, location.latitude, location.longitude)
                        }.start()
                    }
                }
            }

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
