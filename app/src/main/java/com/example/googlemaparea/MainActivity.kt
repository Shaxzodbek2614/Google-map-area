package com.example.googlemaparea

import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.googlemaparea.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.SphericalUtil
import java.util.Locale
import java.util.jar.Manifest

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    lateinit var map:GoogleMap
    var latLng: LatLng? = null
    lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var locationRequast: LocationRequest
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Xarita tayyor bo'lganda hozirgi joylashuvni oling
        getCurrentLocation()


        val mapFragment =supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        binding.searchButton.setOnClickListener {
            val locationName = binding.searchEditText.text.toString()
            if (locationName.isNotEmpty()) {
                searchLocation(locationName)
            } else {
                Toast.makeText(this, "Iltimos, joylashuvni kiriting", Toast.LENGTH_SHORT).show()
            }
        }

    }
    private fun searchLocation(locationName: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addressList = geocoder.getFromLocationName(locationName, 1)
            if (addressList != null && addressList.isNotEmpty()) {
                val address = addressList[0]
                val latLng = LatLng(address.latitude, address.longitude)

                // Xarita pozitsiyasini o'zgartirish
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                // Marker qo'shish
                map.addMarker(MarkerOptions().position(latLng).title(address.featureName))

                Toast.makeText(this, "Lokatsiya topildi: ${address.featureName}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Lokatsiya topilmadi", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Xatolik: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    var list = ArrayList<LatLng>()
    var  polyGone:Polygon? = null
    override fun onMapReady(p0: GoogleMap) {
        map = p0
        map.mapType = GoogleMap.MAP_TYPE_HYBRID
        checkLocationPermission()
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        }

        // Kamerani boshlang'ich nuqtaga olib borish yoki qidiruv funksiyalarini chaqirish
        getCurrentLocation()

        map.setOnMapClickListener {
            list.add(it)
            if (polyGone == null) {
                polyGone = map.addPolygon(PolygonOptions().addAll(list).clickable(false)
                    .fillColor(Color.TRANSPARENT))
            }else{
                polyGone!!.points = list
            }
            val area = calculatePolygonArea(list)
            val areaString = String.format("%.2f", area)
            binding.tvArea.text = "Area: $areaString m^2"
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
                val areaString = String.format("%.2f", area)
                binding.tvArea.text = "Area: $areaString m^2"
            }
        }


    }
    private fun getCurrentLocation() {
        // Foydalanuvchi ruxsat berganini tekshirish
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Joylashuvni olish
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)

                    // Kamerani foydalanuvchi joylashuviga o‘tkazish
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                    // Markerni qo‘shish
                    map.addMarker(MarkerOptions().position(currentLatLng).title("You are here"))
                } else {
                    Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Agar ruxsat berilmagan bo‘lsa, so‘rash
            requestLocationPermission()
        }
    }
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            100
        )
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkLocationPermission() {
        when {
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // If permission granted, get location
                getLastLocation()
            }
            else -> {
                // Request permission
                requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // If permission granted, get location
            getLastLocation()
        } else {
            // Permission denied, handle accordingly
        }
    }
    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    latLng = LatLng(location.latitude, location.longitude)
                    if (::map.isInitialized) {
                        updateMapLocation()

                    }
                }
            }
    }
    private fun updateMapLocation() {
        latLng?.let {
            val cameraPosition = CameraPosition.Builder()
                .target(it)
                .zoom(18f)
                .tilt(45f)
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }
    private fun calculatePolygonArea(polygonPoints: List<LatLng>): Double {
        return SphericalUtil.computeArea(polygonPoints)
    }
}