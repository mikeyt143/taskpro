package org.tasks.data.entity


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.tasks.CommonParcelable
import org.tasks.CommonParcelize
import org.tasks.data.NO_ORDER
import org.tasks.data.Redacted
import org.tasks.data.UUIDHelper
import org.tasks.data.db.Table
import org.tasks.formatCoordinates
import java.util.regex.Pattern

@Serializable
@CommonParcelize
@Entity(
    tableName = Place.TABLE_NAME,
    indices = [
        Index(name = "place_uid", value = ["uid"], unique = true)
    ],
)
data class Place(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "place_id")
    @Transient
    val id: Long = 0,
    @ColumnInfo(name = "uid")
    val uid: String? = UUIDHelper.newUUID(),
    @Redacted
    @ColumnInfo(name = "name")
    val name: String? = null,
    @Redacted
    @ColumnInfo(name = "address")
    val address: String? = null,
    @Redacted
    @ColumnInfo(name = "phone")
    val phone: String? = null,
    @Redacted
    @ColumnInfo(name = "url")
    val url: String? = null,
    @Redacted
    @ColumnInfo(name = "latitude")
    val latitude: Double = 0.0,
    @Redacted
    @ColumnInfo(name = "longitude")
    val longitude: Double = 0.0,
    @ColumnInfo(name = "place_color")
    val color: Int = 0,
    @ColumnInfo(name = "place_icon")
    val icon: String? = null,
    @ColumnInfo(name = "place_order")
    val order: Int = NO_ORDER,
    @ColumnInfo(name = "radius", defaultValue = "250")
    val radius: Int = 250,
) : java.io.Serializable, CommonParcelable {
    val displayAddress: String?
        get() = if (address.isNullOrEmpty()) null else address.replace("$name, ", "")

    val displayName: String
        get() {
            if (!name.isNullOrEmpty() && !COORDS.matcher(name).matches()) {
                return name
            }
            return if (!address.isNullOrEmpty()) {
                address
            } else {
                "${formatCoordinates(latitude, true)} ${formatCoordinates(longitude, false)}"
            }
        }

    companion object {
        private val COORDS = Pattern.compile("^\\d+°\\d+'\\d+\\.\\d+\"[NS] \\d+°\\d+'\\d+\\.\\d+\"[EW]$")
        const val KEY = "place"
        const val TABLE_NAME = "places"
        @JvmField val TABLE = Table(TABLE_NAME)
        @JvmField val UID = TABLE.column("uid")
        @JvmField val NAME = TABLE.column("name")
        @JvmField val ADDRESS = TABLE.column("address")
    }
}
