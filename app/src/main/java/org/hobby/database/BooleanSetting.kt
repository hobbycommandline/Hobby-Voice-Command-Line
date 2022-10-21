package org.hobby.database

//import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * This table is for storing settings
 *
 * path is going to be like action/timer/doQuitAfter
 * or like app/doAnnounceInput
 */
@Entity(tableName = "boolean_setting")
data class BooleanSetting(
    @PrimaryKey val path: String,
    val value: Boolean?
)
