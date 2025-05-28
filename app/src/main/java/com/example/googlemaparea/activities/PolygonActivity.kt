package com.example.googlemaparea.activities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.googlemaparea.R
import com.example.googlemaparea.databinding.ActivityPolygonBinding
import com.example.googlemaparea.model.PolygonEntity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import org.json.JSONArray
import java.text.DecimalFormat

class PolygonActivity : AppCompatActivity(), OnMapReadyCallback {
    lateinit var binding: ActivityPolygonBinding
    private lateinit var map: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPolygonBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val polugon = intent.getSerializableExtra("polygon") as PolygonEntity
        supportActionBar?.title = polugon.name

        mapFragment =supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mapFragment.getMapAsync {
            map = it
            drawPolygon(polugon.pointsJson,polugon.area)
        }


    }
    private fun drawPolygon(json: String, area: Double) {
        val points = decodeLatLngFromJson(json)

        if (points.isNotEmpty()) {
            map.addPolygon(
                PolygonOptions()
                    .addAll(points)
                    .fillColor(Color.argb(100, 0, 0, 255))
                    .strokeColor(Color.BLUE)
                    .strokeWidth(4f)
            )

            // Tomonlar orasidagi masofalarni ko‘rsatish
            for (i in 0 until points.size - 1) {
                drawEdgeDistance(points[i], points[i + 1])
            }
            // Oxirgi va birinchi nuqta orasidagi masofa (poligon yopilishi uchun)
            drawEdgeDistance(points.last(), points.first())

            // Poligon markazi
            val lat = points.map { it.latitude }.average()
            val lng = points.map { it.longitude }.average()
            val center = LatLng(lat, lng)

            val df = DecimalFormat("#.##")
            map.addMarker(
                MarkerOptions()
                    .position(center)
                    .title("Maydon: ${df.format(area)} m²")
            )

            val cameraPosition = CameraPosition.Builder()
                .target(center)
                .zoom(18f)
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }


    private fun decodeLatLngFromJson(json: String): List<LatLng> {
        val array = JSONArray(json)
        val result = mutableListOf<LatLng>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val lat = obj.getDouble("lat")
            val lng = obj.getDouble("lng")
            result.add(LatLng(lat, lng))
        }
        return result
    }

    override fun onMapReady(p0: GoogleMap) {
        map = p0
        map.mapType = GoogleMap.MAP_TYPE_HYBRID

    }
    private fun drawEdgeDistance(p1: LatLng, p2: LatLng) {
        val center = LatLng(
            (p1.latitude + p2.latitude) / 2,
            (p1.longitude + p2.longitude) / 2
        )

        val distance = com.google.maps.android.SphericalUtil.computeDistanceBetween(p1, p2)
        val distanceText = String.format("%.2f m", distance)

        map.addMarker(
            MarkerOptions()
                .position(center)
                .icon(BitmapDescriptorFactory.fromBitmap(createLabelMarker(this, distanceText)))
                .anchor(0.5f, 1.0f)
                .zIndex(1f)
        )
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

}