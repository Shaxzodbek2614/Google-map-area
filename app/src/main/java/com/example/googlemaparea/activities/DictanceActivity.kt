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
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.googlemaparea.R
import com.example.googlemaparea.databinding.ActivityDictanceBinding
import com.example.googlemaparea.db.AppDatabase
import com.example.googlemaparea.model.DistanceEntity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class DistanceActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityDictanceBinding
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val pointList = ArrayList<LatLng>()
    private val distanceMarkers = ArrayList<Marker>()
    private var totalDistance = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDictanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment)
                as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.clear.setOnClickListener {
            map.clear()
            pointList.clear()
            distanceMarkers.clear()
            totalDistance = 0.0
            binding.tvArea.text = "0.0 m"
        }

        binding.back.setOnClickListener {
            if (pointList.size >= 2) {
                pointList.removeLast()
                map.clear()
                distanceMarkers.clear()
                totalDistance = 0.0
                redrawAll()
            }
        }

        binding.customButton.setOnClickListener {
            binding.customButton.setOnClickListener {
                if (pointList.isEmpty()) {
                    Toast.makeText(this, "Avval yo‘lni belgilang", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                showCustomSaveDialog(this) { name, note ->
                    val jsonArray = JSONArray()
                    pointList.forEach { latLng ->
                        val obj = JSONObject()
                        obj.put("lat", latLng.latitude)
                        obj.put("lng", latLng.longitude)
                        jsonArray.put(obj)
                    }

                    val distanceEntity = DistanceEntity(
                        name = name,
                        note = note,
                        pointsJson = jsonArray.toString(),
                        totalDistance = totalDistance
                    )

                    lifecycleScope.launch {
                        val db = AppDatabase.getInstance(applicationContext)
                        db.distanceDao().insert(distanceEntity)
                        Toast.makeText(this@DistanceActivity, "✅ Yo‘l saqlandi!", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }
        binding.searchButton.setOnClickListener {
            val locationName = binding.searchEditText.text.toString()
            if (locationName.isNotEmpty()) {
                searchLocation(locationName)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_HYBRID
        checkLocationPermission()

        map.setOnMapClickListener { point ->
            pointList.add(point)
            map.addMarker(
                MarkerOptions()
                    .position(point)
                    .icon(BitmapDescriptorFactory.fromBitmap(createCircleMarker(this)))
                    .anchor(0.5f, 0.5f)
            )
            if (pointList.size >= 2) {
                val p1 = pointList[pointList.size - 2]
                val p2 = pointList[pointList.size - 1]
                val polyline = map.addPolyline(
                    PolylineOptions()
                        .add(p1, p2)
                        .color(Color.parseColor("#AA00FF"))
                        .width(6f)
                )

                val distance = SphericalUtil.computeDistanceBetween(p1, p2)
                totalDistance += distance
                binding.tvArea.text = String.format("Jami: %.2f m", totalDistance)

                val center = LatLng((p1.latitude + p2.latitude) / 2, (p1.longitude + p2.longitude) / 2)
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(center)
                        .icon(BitmapDescriptorFactory.fromBitmap(createLabelMarker(this, String.format("%.2f m", distance))))
                        .anchor(0.5f, 1.0f)
                )
                distanceMarkers.add(marker!!)
            }
        }
    }

    private fun redrawAll() {
        totalDistance = 0.0
        binding.tvArea.text = "0.0 m"
        for (i in 0 until pointList.size) {
            map.addMarker(
                MarkerOptions()
                    .position(pointList[i])
                    .icon(BitmapDescriptorFactory.fromBitmap(createCircleMarker(this)))
                    .anchor(0.5f, 0.5f)
            )
            if (i >= 1) {
                val p1 = pointList[i - 1]
                val p2 = pointList[i]
                map.addPolyline(
                    PolylineOptions()
                        .add(p1, p2)
                        .color(Color.parseColor("#AA00FF"))
                        .width(6f)
                )
                val dist = SphericalUtil.computeDistanceBetween(p1, p2)
                totalDistance += dist
                binding.tvArea.text = String.format("Jami: %.2f m", totalDistance)
                val center = LatLng((p1.latitude + p2.latitude) / 2, (p1.longitude + p2.longitude) / 2)
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(center)
                        .icon(BitmapDescriptorFactory.fromBitmap(createLabelMarker(this, String.format("%.2f m", dist))))
                        .anchor(0.5f, 1.0f)
                )
                distanceMarkers.add(marker!!)
            }
        }
    }

    private fun searchLocation(name: String) {
        val geocoder = Geocoder(this)
        val result = geocoder.getFromLocationName(name, 1)
        if (!result.isNullOrEmpty()) {
            val location = LatLng(result[0].latitude, result[0].longitude)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 18f))
            map.addMarker(MarkerOptions().position(location).title(name))
        }
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        }
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
}
