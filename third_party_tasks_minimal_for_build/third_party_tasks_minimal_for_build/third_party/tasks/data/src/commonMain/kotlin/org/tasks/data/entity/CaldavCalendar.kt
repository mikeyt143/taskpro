package org.tasks.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.tasks.CommonParcelable
import org.tasks.CommonParcelize
import org.tasks.data.NO_ORDER
import org.tasks.data.Redacted
import org.tasks.data.db.Table

@Serializable
@CommonParcelize
@Entity(tableName = "caldav_lists")
data class CaldavCalendar(
    @PrimaryKey(autoGenerate = true)
    @Transient
    @ColumnInfo(name = "cdl_id") var id: Long = 0,
    @ColumnInfo(name = "cdl_account") val account: String? = Task.NO_UUID,
    @ColumnInfo(name = "cdl_uuid") var uuid: String? = Task.NO_UUID,
    @Redacted
    @ColumnInfo(name = "cdl_name") var name: String? = "",
    @ColumnInfo(name = "cdl_color") var color: Int = 0,
    @ColumnInfo(name = "cdl_ctag") var ctag: String? = null,
    @Redacted
    @ColumnInfo(name = "cdl_url") var url: String? = "",
    @ColumnInfo(name = "cdl_icon") val icon: String? = null,
    @ColumnInfo(name = "cdl_order") val order: Int = NO_ORDER,
    @ColumnInfo(name = "cdl_access") var access: Int = ACCESS_OWNER,
    @ColumnInfo(name = "cdl_last_sync") val lastSync: Long = 0,
) : CommonParcelable {
    companion object {
        const val ACCESS_UNKNOWN = -1
        const val ACCESS_OWNER = 0
        const val ACCESS_READ_WRITE = 1
        const val ACCESS_READ_ONLY = 2

        const val INVITE_UNKNOWN = -1
        const val INVITE_ACCEPTED = 0
        const val INVITE_NO_RESPONSE = 1
        const val INVITE_DECLINED = 2
        const val INVITE_INVALID = 3

        val TABLE = Table("caldav_lists")
        val ACCOUNT = TABLE.column("cdl_account")
        val UUID = TABLE.column("cdl_uuid")
        @JvmField val NAME = TABLE.column("cdl_name")
        @JvmField val ORDER = TABLE.column("cdl_order")
    }

    fun readOnly(): Boolean = access == ACCESS_READ_ONLY
}
