package com.aaron.grainchaintest.models

import androidx.lifecycle.LiveData
import androidx.room.*

@Entity
data class Route(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var alias: String? = "",
    var distance: Double? = null,
    var time: Int? = null //seconds
)

@Dao
interface RouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRoute(route:Route): Long

    @Delete
    suspend fun deleteRoute(route: Route)

    @Query("SELECT * FROM Route ORDER BY id DESC")
    fun getAllRoutes(): LiveData<List<Route>>
}