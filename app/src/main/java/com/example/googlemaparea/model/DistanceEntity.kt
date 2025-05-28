package com.example.googlemaparea.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.googlemaparea.utils.ListItem
import java.io.Serializable

@Entity(tableName = "distance_table")
data class DistanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val note: String,
    val pointsJson: String, // marshrut nuqtalari JSON ko‘rinishida
    val totalDistance: Double
) : Serializable, ListItem
