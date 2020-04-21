package com.aaron.grainchaintest.models

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Route::class, Locations::class], version = 1, exportSchema = false)
abstract class GCTestDB: RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun locationsDao(): LocationsDao

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