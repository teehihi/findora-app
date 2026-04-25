package hcmute.edu.vn.findora;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * MapActivity - Hiển thị bản đồ Mapbox để chọn vị trí
 */
public class MapActivity extends AppCompatActivity {
    
    private MapView mapView;
    private Button btnConfirmLocation;
    private TextView tvAddress;
    
    private double selectedLat = 0;
    private double selectedLng = 0;
    private String addressString = "";
    private Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mapView = findViewById(R.id.mapView);
        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        tvAddress = findViewById(R.id.tvAddress);
        
        geocoder = new Geocoder(this, new Locale("vi", "VN"));

        // Get location from intent
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("latitude") && extras.containsKey("longitude")) {
            selectedLat = extras.getDouble("latitude");
            selectedLng = extras.getDouble("longitude");
            addressString = extras.getString("address", "");
            
            if (!addressString.isEmpty()) {
                tvAddress.setText(addressString);
                
                // Show hint if address has house number
                if (addressString.matches("^\\d+.*")) {
                    Toast.makeText(this, 
                        "Vị trí có thể chưa chính xác. Hãy click trên bản đồ để điều chỉnh!", 
                        Toast.LENGTH_LONG).show();
                }
            }
            btnConfirmLocation.setEnabled(true);
        } else {
            // Default to HCMC
            selectedLat = 10.762622;
            selectedLng = 106.660172;
            getAddressFromLocation(selectedLat, selectedLng);
            btnConfirmLocation.setEnabled(true);
        }

        // Initialize map
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
            // Move camera to initial position
            Point point = Point.fromLngLat(selectedLng, selectedLat);
            CameraOptions cameraOptions = new CameraOptions.Builder()
                    .center(point)
                    .zoom(15.0)
                    .build();
            mapView.getMapboxMap().setCamera(cameraOptions);
            
            // Add marker using helper
            MapboxHelper.addMarker(mapView, selectedLat, selectedLng);
            
            // Setup map click listener
            setupMapClickListener();
        });

        btnConfirmLocation.setOnClickListener(v -> confirmLocation());
    }

    private void setupMapClickListener() {
        MapboxHelper.setOnMapClickListener(mapView, new MapboxHelper.MapClickCallback() {
            @Override
            public void onMapClick(double latitude, double longitude) {
                selectedLat = latitude;
                selectedLng = longitude;
                
                // Enable confirm button
                btnConfirmLocation.setEnabled(true);
                
                // Get address from coordinates
                getAddressFromLocation(latitude, longitude);
                
                // Add new marker (will auto-clear old ones)
                MapboxHelper.addMarker(mapView, selectedLat, selectedLng);
            }
        });
    }
    
    private void getAddressFromLocation(double latitude, double longitude) {
        // Show loading state
        tvAddress.setText("Đang tải địa chỉ...");
        
        // Run geocoding in background thread
        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    
                    // Build detailed address string
                    StringBuilder addressBuilder = new StringBuilder();
                    
                    // Add house number if available
                    if (address.getSubThoroughfare() != null) {
                        addressBuilder.append(address.getSubThoroughfare()).append(" ");
                    }
                    
                    // Add street name
                    if (address.getThoroughfare() != null) {
                        addressBuilder.append(address.getThoroughfare());
                    }
                    
                    // Add ward/suburb
                    if (address.getSubLocality() != null) {
                        if (addressBuilder.length() > 0) addressBuilder.append(", ");
                        addressBuilder.append(address.getSubLocality());
                    }
                    
                    // Add district
                    if (address.getSubAdminArea() != null) {
                        if (addressBuilder.length() > 0) addressBuilder.append(", ");
                        addressBuilder.append(address.getSubAdminArea());
                    }
                    
                    // Add city/province
                    if (address.getLocality() != null) {
                        if (addressBuilder.length() > 0) addressBuilder.append(", ");
                        addressBuilder.append(address.getLocality());
                    } else if (address.getAdminArea() != null) {
                        if (addressBuilder.length() > 0) addressBuilder.append(", ");
                        addressBuilder.append(address.getAdminArea());
                    }
                    
                    // Fallback to full address line if components are empty
                    if (addressBuilder.length() == 0 && address.getMaxAddressLineIndex() >= 0) {
                        addressBuilder.append(address.getAddressLine(0));
                    }
                    
                    final String finalAddress = addressBuilder.length() > 0 
                        ? addressBuilder.toString() 
                        : String.format(Locale.getDefault(), "%.6f, %.6f", latitude, longitude);
                    
                    // Update UI on main thread
                    runOnUiThread(() -> {
                        addressString = finalAddress;
                        tvAddress.setText(addressString);
                    });
                } else {
                    // No address found, show coordinates
                    runOnUiThread(() -> {
                        addressString = String.format(Locale.getDefault(), "%.6f, %.6f", latitude, longitude);
                        tvAddress.setText(addressString);
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                // Error, show coordinates
                runOnUiThread(() -> {
                    addressString = String.format(Locale.getDefault(), "%.6f, %.6f", latitude, longitude);
                    tvAddress.setText(addressString);
                });
            }
        }).start();
    }

    private void confirmLocation() {
        if (selectedLat == 0 && selectedLng == 0) {
            Toast.makeText(this, "Vui lòng chọn vị trí trên bản đồ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Return data
        Intent resultIntent = new Intent();
        resultIntent.putExtra("latitude", selectedLat);
        resultIntent.putExtra("longitude", selectedLng);
        resultIntent.putExtra("address", addressString);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapView != null) {
            mapView.onStop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            MapboxHelper.cleanup(mapView);
            mapView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }
}
