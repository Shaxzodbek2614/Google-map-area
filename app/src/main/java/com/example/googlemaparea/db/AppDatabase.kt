package com.example.googlemaparea.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.googlemaparea.model.DistanceEntity
import com.example.googlemaparea.model.PolygonEntity

@Database(entities = [PolygonEntity::class,DistanceEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun myDao(): PolygonDao
    abstract fun distanceDao(): DistanceDao

    companion object {
        var instance: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase {
            if (instance==null){
                instance = Room.databaseBuilder(context,AppDatabase::class.java,"db_room")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
            }
            return instance!!
        }
    }
}
