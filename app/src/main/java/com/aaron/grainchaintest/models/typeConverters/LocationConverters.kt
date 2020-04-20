package com.aaron.grainchaintest.models.typeConverters

import android.location.Location
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocationConverters {

    companion object {
        private val gson = Gson()
        @TypeConverter
        @JvmStatic
        fun fromString(value: String): Location? {
            return gson.fromJson(value,Location::class.java)
        }

        @TypeConverter
        @JvmStatic
        fun fromLocation(value: Location): String? {
            return gson.toJson(value)
        }

        @TypeConverter
        @JvmStatic
        fun fromLocationList(value: ArrayList<Location>): String? {
            return gson.toJson(value)
        }

        @TypeConverter
        @JvmStatic
        fun fromStringToList(value: String): ArrayList<Location>? {
            val listType = object : TypeToken<ArrayList<Location>>() {}.type
            return gson.fromJson(value, listType)
        }
    }
}