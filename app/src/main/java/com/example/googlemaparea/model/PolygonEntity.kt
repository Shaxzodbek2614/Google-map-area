package com.example.googlemaparea.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.googlemaparea.utils.ListItem
import java.io.Serializable

@Entity(tableName = "polygon_table")
data class PolygonEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val note: String,
    val pointsJson: String,
    val area: Double
): Serializable, ListItem