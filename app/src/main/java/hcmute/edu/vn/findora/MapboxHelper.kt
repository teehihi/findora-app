package hcmute.edu.vn.findora

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures

/**
 * Helper class to bridge Mapbox Kotlin API to Java
 */
object MapboxHelper {
    
    private val managerMap = mutableMapOf<MapView, PointAnnotationManager>()
    
    @JvmStatic
    fun addMarker(mapView: MapView, latitude: Double, longitude: Double) {
        Log.d("MapboxHelper", "addMarker called: $latitude, $longitude")
        
        // Get or create manager for this MapView
        val manager = managerMap.getOrPut(mapView) {
            Log.d("MapboxHelper", "Creating new PointAnnotationManager")
            mapView.annotations.createPointAnnotationManager(AnnotationConfig())
        }
        
        // Clear existing markers first
        val existingCount = manager.annotations.size
        if (existingCount > 0) {
            Log.d("MapboxHelper", "Clearing $existingCount existing markers")
            manager.deleteAll()
        }
        
        // Use location pin for generic marker (no type)
        val context = mapView.context
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_location_pin)
        val bitmap = drawable?.let { drawableToBitmap(it) }
        
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(Point.fromLngLat(longitude, latitude))
        
        if (bitmap != null) {
            pointAnnotationOptions.withIconImage(bitmap)
        }
        
        manager.create(pointAnnotationOptions)
        Log.d("MapboxHelper", "Marker added. Total markers: ${manager.annotations.size}")
    }

    @JvmStatic
    fun addMarker(mapView: MapView, latitude: Double, longitude: Double, type: String?) {
        Log.d("MapboxHelper", "addMarker called: $latitude, $longitude, type=$type")
        
        // Get or create manager for this MapView
        val manager = managerMap.getOrPut(mapView) {
            Log.d("MapboxHelper", "Creating new PointAnnotationManager")
            mapView.annotations.createPointAnnotationManager(AnnotationConfig())
        }
        
        // Clear existing markers first
        val existingCount = manager.annotations.size
        if (existingCount > 0) {
            Log.d("MapboxHelper", "Clearing $existingCount existing markers")
            manager.deleteAll()
        }
        
        // Choose icon based on type
        val context = mapView.context
        val iconRes = when (type) {
            "lost" -> R.drawable.ic_marker_lost
            "found" -> R.drawable.ic_marker_found
            else -> R.drawable.ic_marker_lost  // default red
        }
        val drawable = ContextCompat.getDrawable(context, iconRes)
        val bitmap = drawable?.let { drawableToBitmap(it) }
        
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(Point.fromLngLat(longitude, latitude))
        
        if (bitmap != null) {
            pointAnnotationOptions.withIconImage(bitmap)
        }
        
        manager.create(pointAnnotationOptions)
        Log.d("MapboxHelper", "Marker added. Total markers: ${manager.annotations.size}")
    }
    
    @JvmStatic
    fun clearMarkers(mapView: MapView) {
        val manager = managerMap[mapView]
        if (manager != null) {
            val count = manager.annotations.size
            manager.deleteAll()
            Log.d("MapboxHelper", "Cleared $count markers")
        } else {
            Log.d("MapboxHelper", "No manager found for this MapView")
        }
    }
    
    @JvmStatic
    fun setOnMapClickListener(mapView: MapView, callback: MapClickCallback) {
        mapView.gestures.addOnMapClickListener { point ->
            Log.d("MapboxHelper", "Map clicked: ${point.latitude()}, ${point.longitude()}")
            callback.onMapClick(point.latitude(), point.longitude())
            true
        }
    }
    
    @JvmStatic
    fun cleanup(mapView: MapView) {
        managerMap.remove(mapView)
        Log.d("MapboxHelper", "Cleaned up manager for MapView")
    }
    
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
    
    interface MapClickCallback {
        fun onMapClick(latitude: Double, longitude: Double)
    }
}
