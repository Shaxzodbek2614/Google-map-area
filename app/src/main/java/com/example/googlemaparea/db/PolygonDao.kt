package com.example.googlemaparea.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.googlemaparea.model.PolygonEntity

@Dao
interface PolygonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(polygon: PolygonEntity)

    @Query("SELECT * FROM polygon_table")
    suspend fun getAll(): List<PolygonEntity>
}
