package com.example.googlemaparea.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.googlemaparea.R
import com.example.googlemaparea.databinding.ActivityPickBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class PickActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityPickBinding

    private lateinit var map: GoogleMap
    private lateinit var tvLat: TextView
    private lateinit var tvLng: TextView
    private var marker: Marker? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPickBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tvLat = binding.tvLat
        tvLng = binding.tvLng

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_HYBRID

        // Lokatsiya ruxsatini tekshirish
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
            showCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        }

        // Marker drag va click funksiyasi
        map.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(p0: Marker) {}
            override fun onMarkerDrag(p0: Marker) {}
            override fun onMarkerDragEnd(p0: Marker) {
                updateLatLngText(p0.position)
            }
        })

        map.setOnMapClickListener { latLng ->
            marker?.position = latLng
            updateLatLngText(latLng)
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun showCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)

                marker = map.addMarker(
                    MarkerOptions()
                        .position(currentLatLng)
                        .title("Tanlangan joy")
                        .draggable(true)
                )

                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
                updateLatLngText(currentLatLng)
            } ?: Toast.makeText(this, "Joylashuv aniqlanmadi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLatLngText(latLng: LatLng) {
        tvLat.text = "Latitude: %.6f".format(latLng.latitude)
        tvLng.text = "Longitude: %.6f".format(latLng.longitude)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
            showCurrentLocation()
        } else {
            Toast.makeText(this, "Joylashuvga ruxsat berilmadi", Toast.LENGTH_SHORT).show()
        }
    }
}
