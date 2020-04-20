package com.aaron.grainchaintest.models

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aaron.grainchaintest.models.typeConverters.LocationConverters

@Database(entities = [Route::class], version = 1, exportSchema = false)
@TypeConverters(LocationConverters::class)
abstract class GCTestDB: RoomDatabase() {
    abstract fun routeDao(): RouteDao

    companion object {
        private var INSTANCE: GCTestDB? = null
        private const val DATABASE_NAME = "GCTestDB"
        fun getAppDataBase(context: Context): GCTestDB =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, GCTestDB::class.java, DATABASE_NAME)
                    .build().also { INSTANCE = it }
            }
    }
}