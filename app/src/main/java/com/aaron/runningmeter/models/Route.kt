package com.aaron.runningmeter.models

import androidx.lifecycle.LiveData
import androidx.room.*

@Entity
data class Route(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var alias: String? = "",
    var distance: Double? = null,
    var time: Int? = null, //seconds
    var date: String? = null,
    var speed: Double? = null
)

@Dao
interface RouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRoute(route:Route): Long

    @Delete
    suspend fun deleteRoute(route: Route)

    @Update
    suspend fun updateRoute(route: Route)

    @Query("SELECT * FROM Route ORDER BY id DESC")
    fun getAllRoutes(): LiveData<List<Route>>
}