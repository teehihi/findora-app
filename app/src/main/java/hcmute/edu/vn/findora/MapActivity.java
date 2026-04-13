package hcmute.edu.vn.findora;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

/**
 * MapActivity - Hiển thị và xác nhận vị trí trên bản đồ
 * Chỉ nhận dữ liệu từ AddressPickerActivity và hiển thị
 */
public class MapActivity extends AppCompatActivity {
    
    private MapView mapView;
    private GeoPoint selectedLocation;
    private Button btnConfirmLocation;
    private TextView tvAddress;
    private Marker currentMarker;
    private String addressString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize osmdroid configuration
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(getPackageName());
        
        setContentView(R.layout.activity_map);

        mapView = findViewById(R.id.mapView);
        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        tvAddress = findViewById(R.id.tvAddress);

        // Setup map
        setupMap();
        
        // Get location from intent
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("latitude") && extras.containsKey("longitude")) {
            double lat = extras.getDouble("latitude");
            double lng = extras.getDouble("longitude");
            addressString = extras.getString("address", "");
            
            selectedLocation = new GeoPoint(lat, lng);
            
            // Display address
            tvAddress.setText(addressString);
            
            // Move to location and add marker
            mapView.getController().setCenter(selectedLocation);
            mapView.getController().setZoom(16.0);
            addMarker(selectedLocation);
            
            btnConfirmLocation.setEnabled(true);
        } else {
            Toast.makeText(this, "Không có dữ liệu vị trí", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnConfirmLocation.setOnClickListener(v -> confirmLocation());
    }

    private void setupMap() {
        // Set tile source (OpenStreetMap)
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        
        // Enable zoom controls
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        
        // Set default zoom level
        mapView.getController().setZoom(15.0);
        
        // Add map click listener to allow adjusting marker
        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                onMapClick(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(mapEventsReceiver);
        mapView.getOverlays().add(0, mapEventsOverlay);
    }

    private void onMapClick(GeoPoint point) {
        // Remove previous marker
        if (currentMarker != null) {
            mapView.getOverlays().remove(currentMarker);
        }
        
        // Add new marker
        addMarker(point);
        
        selectedLocation = point;
        btnConfirmLocation.setEnabled(true);
    }
    
    private void addMarker(GeoPoint point) {
        currentMarker = new Marker(mapView);
        currentMarker.setPosition(point);
        currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        currentMarker.setTitle("Vị trí đã chọn");
        mapView.getOverlays().add(currentMarker);
        mapView.invalidate();
    }

    private void confirmLocation() {
        if (selectedLocation == null) {
            Toast.makeText(this, "Vui lòng chọn vị trí trên bản đồ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Return data
        Intent resultIntent = new Intent();
        resultIntent.putExtra("latitude", selectedLocation.getLatitude());
        resultIntent.putExtra("longitude", selectedLocation.getLongitude());
        resultIntent.putExtra("address", addressString);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDetach();
    }
}
