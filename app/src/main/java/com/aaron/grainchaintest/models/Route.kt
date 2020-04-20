package com.aaron.grainchaintest.models

import android.location.Location
import androidx.room.*

@Entity
data class Route(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var alias: String? = "",
    var distance: Double? = null,
    var time: Int? = null, //seconds
    var locations: ArrayList<Location>? = null,
    var initialLocation: Location? = null,
    var lastLocation: Location? = null
)

@Dao
interface RouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRoute(route:Route)

    @Delete
    suspend fun deleteRoute(route: Route)

    @Query("SELECT * FROM Route ORDER BY id DESC")
    suspend fun getAllRoutes(): List<Route>
}