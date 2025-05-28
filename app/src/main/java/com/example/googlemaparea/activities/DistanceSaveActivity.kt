package com.example.googlemaparea.activities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.googlemaparea.R
import com.example.googlemaparea.databinding.ActivityDistanceSaveBinding
import com.example.googlemaparea.model.DistanceEntity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.SphericalUtil
import org.json.JSONArray

class DistanceSaveActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private lateinit var distanceEntity: DistanceEntity
    private val pointList = ArrayList<LatLng>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityDistanceSaveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        distanceEntity = intent.getSerializableExtra("distance") as DistanceEntity

        // JSON’dan nuqtalarni olish
        val jsonArray = JSONArray(distanceEntity.pointsJson)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val lat = obj.getDouble("lat")
            val lng = obj.getDouble("lng")
            pointList.add(LatLng(lat, lng))
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment)
                as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_HYBRID

        if (pointList.isNotEmpty()) {
            pointList.forEach {
                map.addMarker(
                    MarkerOptions()
                        .position(it)
                        .icon(BitmapDescriptorFactory.fromBitmap(createCircleMarker(this)))
                        .anchor(0.5f, 0.5f)
                )
            }

            var totalDistance = 0.0
            for (i in 0 until pointList.size - 1) {
                val p1 = pointList[i]
                val p2 = pointList[i + 1]

                map.addPolyline(
                    PolylineOptions()
                        .add(p1, p2)
                        .color(Color.parseColor("#AA00FF"))
                        .width(6f)
                )

                val dist = SphericalUtil.computeDistanceBetween(p1, p2)
                totalDistance += dist

                val center = LatLng((p1.latitude + p2.latitude) / 2, (p1.longitude + p2.longitude) / 2)
                map.addMarker(
                    MarkerOptions()
                        .position(center)
                        .icon(BitmapDescriptorFactory.fromBitmap(createLabelMarker(this, "%.2f m".format(dist))))
                        .anchor(0.5f, 1.0f)
                )
            }

            // Xarita pozitsiyasini markazga olib borish
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(pointList[0], 16f))
        }
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
