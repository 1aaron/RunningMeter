package com.aaron.grainchaintest.models

import androidx.lifecycle.LiveData
import androidx.room.*
import java.io.Serializable

@Entity
data class Locations (
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ForeignKey(entity = Route::class,parentColumns = ["id"],childColumns = ["routeId"])
    var routeId: Long,
    val latitude: Double,
    val longitude: Double
): Serializable

@Dao
interface LocationsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addLocation(locations: Locations): Long

    @Delete
    suspend fun deleteLocations(location: Locations)

    @Query("SELECT * FROM Locations WHERE routeId = :id")
    suspend fun getLocationsForRoute(id: Long): List<Locations>

    @Query("DELETE FROM Locations WHERE routeId = :id")
    suspend fun removeLocationsForRoute(id: Long)
}

