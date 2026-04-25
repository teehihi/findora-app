package hcmute.edu.vn.findora;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AddressPickerActivity - Chọn địa chỉ theo từng bước với filter
 * Sử dụng Vietnam Provinces API + AutoCompleteTextView
 */
public class AddressPickerActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private AutoCompleteTextView actvProvince, actvWard;
    private TextInputEditText etStreetAddress;
    private Button btnViewOnMap;
    private androidx.cardview.widget.CardView btnCurrentLocation;
    private android.widget.ImageButton btnBack;
    private android.view.View loadingOverlay;
    private android.widget.TextView tvLoadingMessage;
    
    private FusedLocationProviderClient fusedLocationClient;
    
    private List<Province> provinces = new ArrayList<>();
    private List<Ward> wards = new ArrayList<>();
    
    private Province selectedProvince = null;
    private Ward selectedWard = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_picker);

        actvProvince = findViewById(R.id.actvProvince);
        actvWard = findViewById(R.id.actvWard);
        etStreetAddress = findViewById(R.id.etStreetAddress);
        btnViewOnMap = findViewById(R.id.btnViewOnMap);
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation);
        btnBack = findViewById(R.id.btnBack);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        tvLoadingMessage = findViewById(R.id.tvLoadingMessage);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Disable ward initially
        actvWard.setEnabled(false);

        loadProvinces();
        
        btnViewOnMap.setOnClickListener(v -> viewOnMap());
        btnCurrentLocation.setOnClickListener(v -> getCurrentLocation());
        btnBack.setOnClickListener(v -> finish());
    }

    // ===== API Models =====
    
    static class Province {
        String code;
        String name;
        
        Province(String code, String name) {
            this.code = code;
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    static class Ward {
        String code;
        String name;
        
        Ward(String code, String name) {
            this.code = code;
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }

    private void showLoading(String message) {
        runOnUiThread(() -> {
            tvLoadingMessage.setText(message);
            loadingOverlay.setVisibility(android.view.View.VISIBLE);
        });
    }

    private void hideLoading() {
        runOnUiThread(() -> loadingOverlay.setVisibility(android.view.View.GONE));
    }

    // ===== Load Provinces (addresskit.cas.so) =====
    
    private void loadProvinces() {
        showLoading("Đang tải danh sách tỉnh/thành...");
        new Thread(() -> {
            try {
                String jsonResponse = fetchFromAPI("https://addresskit.cas.so/api/latest/provinces");
                JSONObject root = new JSONObject(jsonResponse);
                JSONArray jsonArray = root.getJSONArray("provinces");
                
                List<Province> provinceList = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String code = obj.getString("code");
                    String name = obj.getString("name");
                    provinceList.add(new Province(code, name));
                }
                
                provinces = provinceList;
                
                runOnUiThread(() -> {
                    ArrayAdapter<Province> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line, provinces);
                    actvProvince.setAdapter(adapter);
                    actvProvince.setThreshold(1);
                    
                    actvProvince.setOnFocusChangeListener((v, hasFocus) -> {
                        if (hasFocus) actvProvince.showDropDown();
                    });
                    actvProvince.setOnClickListener(v -> actvProvince.showDropDown());
                    
                    actvProvince.setOnItemClickListener((parent, view, position, id) -> {
                        Province selected = (Province) parent.getItemAtPosition(position);
                        selectedProvince = selected;
                        loadWards(selected.code);
                        actvWard.setText("");
                        selectedWard = null;
                    });
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                    Toast.makeText(this, "Lỗi tải danh sách tỉnh/thành phố", Toast.LENGTH_SHORT).show());
            } finally {
                hideLoading();
            }
        }).start();
    }

    // ===== Load Wards (addresskit.cas.so) =====

    private void loadWards(String provinceCode) {
        showLoading("Đang tải danh sách phường/xã...");
        new Thread(() -> {
            try {
                String jsonResponse = fetchFromAPI("https://addresskit.cas.so/api/latest/provinces/" + provinceCode + "/communes");
                JSONObject root = new JSONObject(jsonResponse);
                JSONArray jsonArray = root.getJSONArray("communes");
                
                List<Ward> wardList = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String code = obj.getString("code");
                    String name = obj.getString("name");
                    wardList.add(new Ward(code, name));
                }
                
                wards = wardList;
                
                runOnUiThread(() -> {
                    ArrayAdapter<Ward> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line, wards);
                    actvWard.setAdapter(adapter);
                    actvWard.setThreshold(1);
                    actvWard.setEnabled(true);
                    
                    actvWard.setOnFocusChangeListener((v, hasFocus) -> {
                        if (hasFocus) actvWard.showDropDown();
                    });
                    actvWard.setOnClickListener(v -> actvWard.showDropDown());
                    
                    actvWard.setOnItemClickListener((parent, view, position, id) -> {
                        Ward selected = (Ward) parent.getItemAtPosition(position);
                        selectedWard = selected;
                    });
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                    Toast.makeText(this, "Lỗi tải danh sách phường/xã", Toast.LENGTH_SHORT).show());
            } finally {
                hideLoading();
            }
        }).start();
    }

    // ===== Fetch from API =====
    
    private String fetchFromAPI(String urlString) throws IOException {
        android.util.Log.d("AddressPicker", "Fetching: " + urlString);
        
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "FindoraApp/1.0 (hcmute.edu.vn.findora; Android)");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Accept-Language", "vi,en");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        
        int responseCode = conn.getResponseCode();
        android.util.Log.d("AddressPicker", "Response code: " + responseCode);
        
        if (responseCode != 200) {
            android.util.Log.e("AddressPicker", "HTTP error: " + responseCode);
            throw new IOException("HTTP error code: " + responseCode);
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();
        
        return response.toString();
    }

    // ===== Get Current Location =====
    
    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null && location.getLatitude() != 0.0 && location.getLongitude() != 0.0) {
                        showLoading("Đang xác định địa chỉ...");
                        reverseGeocode(location.getLatitude(), location.getLongitude());
                    } else {
                        Toast.makeText(this, "Không thể lấy vị trí hiện tại", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void reverseGeocode(double lat, double lng) {
        new Thread(() -> {
            try {
                // Try Nominatim first
                String url = "https://nominatim.openstreetmap.org/reverse?lat=" + lat 
                           + "&lon=" + lng 
                           + "&format=json&addressdetails=1&accept-language=vi";
                
                String response = fetchFromAPI(url);
                JSONObject json = new JSONObject(response);
                
                if (json.has("address")) {
                    JSONObject addressObj = json.getJSONObject("address");
                    
                    String province = "";
                    String ward = "";
                    String street = "";
                    
                    if (addressObj.has("city")) {
                        province = addressObj.getString("city");
                    } else if (addressObj.has("state")) {
                        province = addressObj.getString("state");
                    }
                    
                    if (addressObj.has("suburb")) {
                        ward = addressObj.getString("suburb");
                    } else if (addressObj.has("quarter")) {
                        ward = addressObj.getString("quarter");
                    }
                    
                    if (addressObj.has("road")) {
                        street = addressObj.getString("road");
                    }
                    
                    String finalProvince = province;
                    String finalWard = ward;
                    String finalStreet = street;
                    
                    runOnUiThread(() -> {
                        if (!finalProvince.isEmpty()) {
                            actvProvince.setText(finalProvince);
                            // Match province in list to trigger loadWards
                            for (Province p : provinces) {
                                if (p.name.contains(finalProvince) || finalProvince.contains(p.name)) {
                                    selectedProvince = p;
                                    loadWards(p.code);
                                    break;
                                }
                            }
                        }
                        if (!finalWard.isEmpty()) {
                            // Set ward text after a short delay to let loadWards finish
                            actvWard.postDelayed(() -> {
                                actvWard.setText(finalWard);
                                // Try to match ward in list
                                for (Ward w : wards) {
                                    if (w.name.contains(finalWard) || finalWard.contains(w.name)) {
                                        selectedWard = w;
                                        break;
                                    }
                                }
                            }, 1500);
                        }
                        if (!finalStreet.isEmpty()) etStreetAddress.setText(finalStreet);
                        
                        Toast.makeText(this, "Đã lấy vị trí hiện tại", Toast.LENGTH_SHORT).show();
                    });
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Không thể xác định địa chỉ", Toast.LENGTH_SHORT).show();
                });
            } finally {
                hideLoading();
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Cần quyền truy cập vị trí", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ===== View on Map =====

    private void viewOnMap() {
        String streetAddress = etStreetAddress.getText().toString().trim();
        
        // Allow viewing map even without full address
        if (selectedProvince == null && selectedWard == null && streetAddress.isEmpty()) {
            // No address at all, open map at default location
            Intent intent = new Intent(AddressPickerActivity.this, MapActivity.class);
            intent.putExtra("latitude", 10.762622); // Default HCMC
            intent.putExtra("longitude", 106.660172);
            intent.putExtra("address", "");
            startActivityForResult(intent, 100);
            return;
        }
        
        // Build full address from available components
        StringBuilder fullAddress = new StringBuilder();
        
        // Add street address first (most specific)
        if (!streetAddress.isEmpty()) {
            fullAddress.append(streetAddress);
        }
        
        // Add ward
        if (selectedWard != null) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(selectedWard.name);
        }
        
        // Add province
        if (selectedProvince != null) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(selectedProvince.name);
        }
        
        // Always add Vietnam for better geocoding
        if (fullAddress.length() > 0) {
            fullAddress.append(", Vietnam");
        }
        
        android.util.Log.d("AddressPicker", "=== VIEW ON MAP CLICKED ===");
        android.util.Log.d("AddressPicker", "Street: " + streetAddress);
        android.util.Log.d("AddressPicker", "Ward: " + (selectedWard != null ? selectedWard.name : "null"));
        android.util.Log.d("AddressPicker", "Province: " + (selectedProvince != null ? selectedProvince.name : "null"));
        android.util.Log.d("AddressPicker", "Full address: " + fullAddress.toString());
        
        // Geocode address to get lat/lng
        if (fullAddress.length() > 0) {
            showLoading("Đang tìm vị trí...");
            geocodeAndShowMap(fullAddress.toString());
        } else {
            // Open map at default location
            Intent intent = new Intent(AddressPickerActivity.this, MapActivity.class);
            intent.putExtra("latitude", 10.762622);
            intent.putExtra("longitude", 106.660172);
            intent.putExtra("address", "");
            startActivityForResult(intent, 100);
        }
    }

    private void geocodeAndShowMap(String address) {
        android.util.Log.d("AddressPicker", "=== GEOCODING START ===");
        android.util.Log.d("AddressPicker", "Full address: " + address);
        
        new Thread(() -> {
            try {
                // Try multiple geocoding services for better results
                double lat = 0;
                double lon = 0;
                boolean found = false;
                
                // Method 1: Try Nominatim with full address
                android.util.Log.d("AddressPicker", "Method 1: Trying Nominatim with full address");
                try {
                    String encodedAddress = java.net.URLEncoder.encode(address, "UTF-8");
                    String url = "https://nominatim.openstreetmap.org/search?q=" + encodedAddress 
                               + "&format=json&addressdetails=1&limit=5&countrycodes=vn&accept-language=vi";
                    
                    android.util.Log.d("AddressPicker", "Nominatim URL: " + url);
                    
                    String response = fetchFromAPI(url);
                    android.util.Log.d("AddressPicker", "Nominatim response: " + response);
                    
                    JSONArray jsonArray = new JSONArray(response);
                    
                    if (jsonArray.length() > 0) {
                        // Get the best match (first result)
                        JSONObject place = jsonArray.getJSONObject(0);
                        lat = place.getDouble("lat");
                        lon = place.getDouble("lon");
                        found = true;
                        android.util.Log.d("AddressPicker", "[OK] Found via Nominatim: " + lat + ", " + lon);
                    } else {
                        android.util.Log.d("AddressPicker", "[FAIL] No results from Nominatim");
                    }
                } catch (Exception e) {
                    android.util.Log.e("AddressPicker", "[ERROR] Nominatim error: " + e.getMessage());
                    e.printStackTrace();
                }
                
                // Method 2: Try Mapbox Geocoding API (better for Vietnam)
                if (!found) {
                    android.util.Log.d("AddressPicker", "Method 2: Trying Mapbox Geocoding API");
                    try {
                        // Get token from BuildConfig (loaded from local.properties)
                        String mapboxToken = BuildConfig.MAPBOX_ACCESS_TOKEN;
                        
                        // Remove "Vietnam" suffix for Mapbox as we use country filter
                        String addressForMapbox = address.replace(", Vietnam", "");
                        String encodedAddress = java.net.URLEncoder.encode(addressForMapbox, "UTF-8");
                        
                        String url = "https://api.mapbox.com/geocoding/v5/mapbox.places/" + encodedAddress 
                                   + ".json?access_token=" + mapboxToken 
                                   + "&country=vn"
                                   + "&language=vi"
                                   + "&limit=5"
                                   + "&types=address,poi,place,locality";
                        
                        android.util.Log.d("AddressPicker", "Mapbox URL: " + url);
                        android.util.Log.d("AddressPicker", "Mapbox Token (first 20 chars): " + mapboxToken.substring(0, Math.min(20, mapboxToken.length())));
                        
                        String response = fetchFromAPI(url);
                        android.util.Log.d("AddressPicker", "Mapbox response length: " + response.length());
                        android.util.Log.d("AddressPicker", "Mapbox response: " + response.substring(0, Math.min(500, response.length())));
                        
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray features = jsonObject.getJSONArray("features");
                        
                        android.util.Log.d("AddressPicker", "Mapbox found " + features.length() + " results");
                        
                        if (features.length() > 0) {
                            JSONObject feature = features.getJSONObject(0);
                            JSONArray coordinates = feature.getJSONArray("center");
                            String placeName = feature.optString("place_name", "");
                            
                            lon = coordinates.getDouble(0);
                            lat = coordinates.getDouble(1);
                            found = true;
                            
                            android.util.Log.d("AddressPicker", "[OK] Found via Mapbox: " + lat + ", " + lon);
                            android.util.Log.d("AddressPicker", "  Place name: " + placeName);
                        } else {
                            android.util.Log.d("AddressPicker", "[FAIL] No results from Mapbox");
                        }
                    } catch (Exception e) {
                        android.util.Log.e("AddressPicker", "[ERROR] Mapbox error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                // Method 3: If not found, try with just ward + province
                if (!found && selectedWard != null && selectedProvince != null) {
                    android.util.Log.d("AddressPicker", "Method 3: Trying with ward + province only");
                    try {
                        String simpleAddress = selectedWard.name + ", " + selectedProvince.name + ", Vietnam";
                        String encodedAddress = java.net.URLEncoder.encode(simpleAddress, "UTF-8");
                        String url = "https://nominatim.openstreetmap.org/search?q=" + encodedAddress 
                                   + "&format=json&limit=1&countrycodes=vn";
                        
                        String response = fetchFromAPI(url);
                        JSONArray jsonArray = new JSONArray(response);
                        
                        if (jsonArray.length() > 0) {
                            JSONObject place = jsonArray.getJSONObject(0);
                            lat = place.getDouble("lat");
                            lon = place.getDouble("lon");
                            found = true;
                            android.util.Log.d("AddressPicker", "[OK] Found via fallback: " + lat + ", " + lon);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("AddressPicker", "[ERROR] Fallback error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                // Method 4: Fallback to province center
                if (!found && selectedProvince != null) {
                    android.util.Log.d("AddressPicker", "Method 4: Using province center");
                    // Default coordinates for major cities
                    if (selectedProvince.name.contains("Hồ Chí Minh")) {
                        lat = 10.762622;
                        lon = 106.660172;
                        found = true;
                        android.util.Log.d("AddressPicker", "[OK] Using HCMC center");
                    } else if (selectedProvince.name.contains("Hà Nội")) {
                        lat = 21.028511;
                        lon = 105.804817;
                        found = true;
                        android.util.Log.d("AddressPicker", "[OK] Using Hanoi center");
                    } else if (selectedProvince.name.contains("Đà Nẵng")) {
                        lat = 16.047079;
                        lon = 108.206230;
                        found = true;
                        android.util.Log.d("AddressPicker", "[OK] Using Da Nang center");
                    } else if (selectedProvince.name.contains("Cần Thơ")) {
                        lat = 10.045162;
                        lon = 105.746857;
                        found = true;
                        android.util.Log.d("AddressPicker", "[OK] Using Can Tho center");
                    }
                }
                
                android.util.Log.d("AddressPicker", "=== GEOCODING END ===");
                android.util.Log.d("AddressPicker", "Final result: " + (found ? "FOUND" : "NOT FOUND"));
                android.util.Log.d("AddressPicker", "Coordinates: " + lat + ", " + lon);
                
                if (found) {
                    double finalLat = lat;
                    double finalLon = lon;
                    String finalAddress = address;
                    
                    runOnUiThread(() -> {
                        hideLoading();
                        if (address.matches("^\\d+.*")) {
                            Toast.makeText(this, 
                                "Đã tìm thấy khu vực gần đúng.\nVui lòng click trên bản đồ để chọn vị trí chính xác.", 
                                Toast.LENGTH_LONG).show();
                        }
                        
                        Intent intent = new Intent(AddressPickerActivity.this, MapActivity.class);
                        intent.putExtra("latitude", finalLat);
                        intent.putExtra("longitude", finalLon);
                        intent.putExtra("address", finalAddress);
                        startActivityForResult(intent, 100);
                    });
                } else {
                    runOnUiThread(() -> {
                        hideLoading();
                        Toast.makeText(this, "Không tìm thấy địa chỉ này trên bản đồ.\nVui lòng chọn trực tiếp trên bản đồ.", Toast.LENGTH_LONG).show();
                        
                        // Still open map at province/district center
                        double defaultLat = 10.762622;
                        double defaultLon = 106.660172;
                        
                        if (selectedProvince != null) {
                            if (selectedProvince.name.contains("Hồ Chí Minh")) {
                                defaultLat = 10.762622;
                                defaultLon = 106.660172;
                            } else if (selectedProvince.name.contains("Hà Nội")) {
                                defaultLat = 21.028511;
                                defaultLon = 105.804817;
                            } else if (selectedProvince.name.contains("Đà Nẵng")) {
                                defaultLat = 16.047079;
                                defaultLon = 108.206230;
                            }
                        }
                        
                        Intent intent = new Intent(AddressPickerActivity.this, MapActivity.class);
                        intent.putExtra("latitude", defaultLat);
                        intent.putExtra("longitude", defaultLon);
                        intent.putExtra("address", address);
                        startActivityForResult(intent, 100);
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("AddressPicker", "[FATAL] Fatal error: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi tìm kiếm địa chỉ", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            // User confirmed location on map, return to CreatePostActivity
            setResult(RESULT_OK, data);
            finish();
        }
    }
}
