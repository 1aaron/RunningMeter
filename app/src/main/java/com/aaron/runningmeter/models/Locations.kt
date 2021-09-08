package com.aaron.runningmeter.models

import androidx.lifecycle.LiveData
import androidx.room.*
import java.io.Serializable

@Entity(foreignKeys = [ForeignKey(entity = Route::class,parentColumns = ["id"],childColumns = ["routeId"])])
data class Locations (
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    var routeId: Long,
    val latitude: Double,
    val longitude: Double
): Serializable

@Dao
interface LocationsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addLocation(location: Locations): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addLocations(locations: ArrayList<Locations>)

    @Delete
    suspend fun deleteLocations(location: Locations)

    @Query("SELECT * FROM Locations WHERE routeId = :id")
    suspend fun getLocationsForRoute(id: Long): List<Locations>

    @Query("SELECT * FROM Locations WHERE routeId = :id")
    fun getLiveLocationsForRoute(id: Long): LiveData<List<Locations>>

    @Query("DELETE FROM Locations WHERE routeId = :id")
    suspend fun removeLocationsForRoute(id: Long)
}

