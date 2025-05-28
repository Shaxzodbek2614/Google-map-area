package com.example.googlemaparea.activities

import MixedAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.googlemaparea.R
import com.example.googlemaparea.databinding.ActivitySavedList2Binding
import com.example.googlemaparea.db.AppDatabase
import com.example.googlemaparea.model.DistanceEntity
import com.example.googlemaparea.model.PolygonEntity
import com.example.googlemaparea.utils.ListItem
import kotlinx.coroutines.launch
import kotlin.jvm.java

class SavedListActivity2 : AppCompatActivity() {
    lateinit var adapter: MixedAdapter
    private val binding by lazy { ActivitySavedList2Binding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        lifecycleScope.launch {
            val items = mutableListOf<ListItem>()
            val db = AppDatabase.getInstance(applicationContext)
            val polygonList = db.myDao().getAll()
            val distanceList = db.distanceDao().getAll()
            items.addAll(polygonList) // from database
            items.addAll(distanceList)
            adapter = MixedAdapter(items , object : MixedAdapter.Action {
                override fun onPolygonClick(polygon: PolygonEntity) {
                    val intent = Intent(this@SavedListActivity2, PolygonActivity::class.java)
                    intent.putExtra("polygon", polygon)
                    startActivity(intent)

                }

                override fun onDistanceClick(distance: DistanceEntity) {
                    val intent = Intent(this@SavedListActivity2, DistanceSaveActivity::class.java)
                    intent.putExtra("distance", distance)
                    startActivity(intent)
                }

            })
            binding.rv.adapter = adapter
        }
    }

    }
