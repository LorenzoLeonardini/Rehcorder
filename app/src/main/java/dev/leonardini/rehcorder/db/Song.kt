package dev.leonardini.rehcorder.db

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "song")
data class Song(
    @ColumnInfo(name = "name") var name: String
) : Parcelable {
    @PrimaryKey(autoGenerate = true)
    var uid: Long = 0

    override fun toString(): String {
        return name
    }
}
