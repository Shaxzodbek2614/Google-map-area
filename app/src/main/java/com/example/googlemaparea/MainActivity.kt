package com.example.googlemaparea

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.googlemaparea.databinding.ActivityMainBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.SphericalUtil

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    lateinit var map:GoogleMap
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val mapFragment =supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    var list = ArrayList<LatLng>()
    var  polyGone:Polygon? = null
    override fun onMapReady(p0: GoogleMap) {
        map = p0
        val cameraPosition = CameraPosition.Builder()
        cameraPosition.zoom(18f)
        cameraPosition.tilt(45f)
        cameraPosition.target(LatLng(40.382999588904006, 71.78275311066025))
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition.build()))

        map.mapType = GoogleMap.MAP_TYPE_HYBRID
        map.setOnMapClickListener {
            list.add(it)
            if (polyGone == null) {
                polyGone = map.addPolygon(PolygonOptions().addAll(list).clickable(false)
                    .fillColor(Color.GRAY))
            }else{
                polyGone!!.points = list
            }
            val area = calculatePolygonArea(list)
            binding.tvArea.text = "Area: $area"
        }
        binding.clear.setOnClickListener {
            list.clear()
            polyGone = null
            map.clear()
            binding.tvArea.text = "Area: 0"
        }
        binding.back.setOnClickListener {
            if (list.size > 0) {
                list.removeAt(list.size - 1)
                polyGone!!.points = list
                val area = calculatePolygonArea(list)
                binding.tvArea.text = "Area: $area"
            }
        }


    }
    private fun calculatePolygonArea(polygonPoints: List<LatLng>): Double {
        return SphericalUtil.computeArea(polygonPoints)
    }
}