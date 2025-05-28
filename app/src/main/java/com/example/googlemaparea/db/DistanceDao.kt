package com.example.googlemaparea.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.googlemaparea.model.DistanceEntity

@Dao
interface DistanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(distance: DistanceEntity)

    @Query("SELECT * FROM distance_table")
    suspend fun getAll(): List<DistanceEntity>
}
