package org.hobby.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BooleanSetting::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun booleanSettingDao(): BooleanSettingDAO
}
