package com.example.googlemaparea.activities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.googlemaparea.R
import com.example.googlemaparea.databinding.ActivityMainBinding
import com.example.googlemaparea.db.AppDatabase
import com.example.googlemaparea.model.PolygonEntity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    lateinit var map: GoogleMap
    var latLng: LatLng? = null
    lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var locationRequast: LocationRequest
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Xarita tayyor bo'lganda hozirgi joylashuvni olish
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

        binding.customButton.setOnClickListener {
            if (list.isEmpty()) {
                Toast.makeText(this, "Avval poligon chizing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showCustomSaveDialog(this) { name, note ->
                val jsonArray = JSONArray()
                list.forEach { latLng ->
                    val obj = JSONObject()
                    obj.put("lat", latLng.latitude)
                    obj.put("lng", latLng.longitude)
                    jsonArray.put(obj)
                }

                val area = calculatePolygonArea(list)


                val polygonEntity = PolygonEntity(
                    name = name,
                    note = note,
                    pointsJson = jsonArray.toString(),
                    area = area
                )

                lifecycleScope.launch {
                    val db = AppDatabase.Companion.getInstance(applicationContext)
                    db.myDao().insert(polygonEntity)
                    Toast.makeText(this@MainActivity, "✅ Saqlandi!", Toast.LENGTH_SHORT).show()
                }
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
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))

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
    var markerList = ArrayList<com.google.android.gms.maps.model.Marker>()
    val distanceMarkers = ArrayList<Marker>()
    var  polyGone: Polygon? = null
    override fun onMapReady(p0: GoogleMap) {
        map = p0
        map.mapType = GoogleMap.MAP_TYPE_HYBRID
        checkLocationPermission()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        }

        // Kamerani boshlang'ich nuqtaga olib borish yoki qidiruv funksiyalarini chaqirish
        getCurrentLocation()


        map.setOnMapClickListener {
            list.add(it)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(it)
                    .icon(BitmapDescriptorFactory.fromBitmap(createCircleMarker(this))) // kichik moviy marke
                    .anchor(0.5f,1.0f) // markazda turishi uchun
                    .title("Nuqta ${list.size}")
            )
            marker?.let { markerList.add(it) }

            // 3. Poligonni qayta chiz
            if (polyGone == null) {
                polyGone = map.addPolygon(
                    PolygonOptions().addAll(list).clickable(false)
                    .fillColor(Color.argb(128, 0, 0, 255))
                    .strokeColor(Color.BLUE))
            }else{
                polyGone!!.points = list
            }

            distanceMarkers.forEach { it.remove() }
            distanceMarkers.clear()
            // ✅ Har bir chiziq ustiga masofa markerlarini qo‘shish
            for (i in 0 until list.size - 1) {
                val p1 = list[i]
                val p2 = list[i + 1]
                val center = LatLng(
                    (p1.latitude + p2.latitude) / 2,
                    (p1.longitude + p2.longitude) / 2
                )
                val distance = SphericalUtil.computeDistanceBetween(p1, p2)
                val distanceStr = String.format("%.2f Meter", distance)

                val distMarker = map.addMarker(
                    MarkerOptions()
                        .position(center)
                        .icon(BitmapDescriptorFactory.fromBitmap(createLabelMarker(this, distanceStr)))
                        .anchor(0.5f, 1.0f)
                )
                distanceMarkers.add(distMarker!!)
            }

            // ✅ Poligon yopilgan bo‘lsa, oxirgi–birinchi nuqtani hisoblash
            if (list.size >= 3) {
                val p1 = list.last()
                val p2 = list.first()
                val center = LatLng(
                    (p1.latitude + p2.latitude) / 2,
                    (p1.longitude + p2.longitude) / 2
                )
                val distance = SphericalUtil.computeDistanceBetween(p1, p2)
                val distanceStr = String.format("%.2f Meter", distance)

                val marker = map.addMarker(
                    MarkerOptions()
                        .position(center)
                        .icon(BitmapDescriptorFactory.fromBitmap(createLabelMarker(this, distanceStr)))
                        .anchor(0.5f, 1.0f)
                )
                distanceMarkers.add(marker!!)
            }
            val area = calculatePolygonArea(list)
            val areaString = String.format("%.2f", area)
            binding.tvArea.text = "Area: $areaString m^2"
        }
        binding.clear.setOnClickListener {
            list.clear()
            polyGone = null
            map.clear()
            distanceMarkers.clear()
            binding.tvArea.text = "Area: 0"
        }
        binding.back.setOnClickListener {
            if (list.size > 0) {
                list.removeAt(list.size - 1)
                polyGone!!.points = list

                if (markerList.isNotEmpty()) {
                    val lastMarker = markerList.removeLast() // oxirgi marker
                    lastMarker.remove() // xaritadan o‘chirish
                }

                for (it in distanceMarkers) {
                    it.remove()
                }

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
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Joylashuvni olish
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)

                    // Kamerani foydalanuvchi joylashuviga o‘tkazish
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f))

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
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
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
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // If permission granted, get location
                getLastLocation()
            }
            else -> {
                // Request permission
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
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
    fun showCustomSaveDialog(context: Context, onSave: (String, String) -> Unit) {
        val layout = LayoutInflater.from(context).inflate(R.layout.dialog_save_polygon, null)
        val dialog = AlertDialog.Builder(context).setView(layout).create()

        val nameInput = layout.findViewById<EditText>(R.id.etName)
        val noteInput = layout.findViewById<EditText>(R.id.etNote)
        val btnSave = layout.findViewById<Button>(R.id.btnSavePolygon)

        btnSave.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val note = noteInput.text.toString().trim()

            if (name.isEmpty()) {
                nameInput.error = "Hudud nomi kerak"
                return@setOnClickListener
            }

            onSave(name, note)
            dialog.dismiss()
        }

        dialog.show()
    }
    private fun createLabelMarker(context: Context, text: String): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = 30f
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        val padding = 20f

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        backgroundPaint.color = Color.rgb(0, 102, 204) // Ko‘k rang fon

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)

        val width = bounds.width() + (padding * 2).toInt()
        val height = bounds.height() + (padding * 2).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Oval pill background
        val rectF = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rectF, 50f, 50f, backgroundPaint)

        // Draw text
        canvas.drawText(
            text,
            width / 2f,
            height / 2f - (paint.descent() + paint.ascent()) / 2,
            paint
        )

        return bitmap
    }

    private fun createCircleMarker(context: Context): Bitmap {
        val size = 24 // px – kichik marker
        val strokeWidth = 4f

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE // markaz rangi
            style = Paint.Style.FILL
        }

        val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#800080FF") // ko‘k chegarasi
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
        }

        val radius = size / 2f - strokeWidth
        val center = size / 2f

        canvas.drawCircle(center, center, radius, paintFill)
        canvas.drawCircle(center, center, radius, paintStroke)

        return bitmap
    }



}