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

@Serializable
@CommonParcelize
@Entity(tableName = "filters")
data class Filter(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    @Transient
    val id: Long = 0,
    @Redacted
    @ColumnInfo(name = "title")
    val title: String? = null,
    @ColumnInfo(name = "sql")
    val sql: String? = null,
    @ColumnInfo(name = "values")
    val values: String? = null,
    @ColumnInfo(name = "criterion")
    val criterion: String? = null,
    @ColumnInfo(name = "f_color")
    val color: Int? = 0,
    @ColumnInfo(name = "f_icon")
    val icon: String? = null,
    @ColumnInfo(name = "f_order")
    val order: Int = NO_ORDER,
) : CommonParcelable
