package com.example.googlemaparea.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.googlemaparea.activities.MainActivity
import com.example.googlemaparea.R
import com.example.googlemaparea.databinding.ActivityKirishBinding

class KirishActivity : AppCompatActivity() {
    lateinit var binding: ActivityKirishBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityKirishBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        Glide.with(this)
            .asGif()
            .load(R.drawable.stars)
            .into(binding.appbar)

        binding.area.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        binding.savedList.setOnClickListener {
            val intent = Intent(this, SavedListActivity2::class.java)
            startActivity(intent)
        }
        binding.distance.setOnClickListener {
            val intent = Intent(this, DistanceActivity::class.java)
            startActivity(intent)
        }
        binding.location.setOnClickListener {
            val intent = Intent(this, PickActivity::class.java)
            startActivity(intent)
        }


    }
}