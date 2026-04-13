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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

    private AutoCompleteTextView actvProvince, actvDistrict, actvWard;
    private TextInputEditText etStreetAddress;
    private Button btnViewOnMap;
    private FloatingActionButton fabCurrentLocation;
    private android.widget.ImageButton btnBack;
    
    private FusedLocationProviderClient fusedLocationClient;
    
    private List<Province> provinces = new ArrayList<>();
    private List<District> districts = new ArrayList<>();
    private List<Ward> wards = new ArrayList<>();
    
    private Province selectedProvince = null;
    private District selectedDistrict = null;
    private Ward selectedWard = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_picker);

        actvProvince = findViewById(R.id.actvProvince);
        actvDistrict = findViewById(R.id.actvDistrict);
        actvWard = findViewById(R.id.actvWard);
        etStreetAddress = findViewById(R.id.etStreetAddress);
        btnViewOnMap = findViewById(R.id.btnViewOnMap);
        fabCurrentLocation = findViewById(R.id.fabCurrentLocation);
        btnBack = findViewById(R.id.btnBack);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Disable district and ward initially
        actvDistrict.setEnabled(false);
        actvWard.setEnabled(false);

        loadProvinces();
        
        btnViewOnMap.setOnClickListener(v -> viewOnMap());
        fabCurrentLocation.setOnClickListener(v -> getCurrentLocation());
        btnBack.setOnClickListener(v -> finish());
    }

    // ===== API Models =====
    
    static class Province {
        int code;
        String name;
        
        Province(int code, String name) {
            this.code = code;
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    static class District {
        int code;
        String name;
        
        District(int code, String name) {
            this.code = code;
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    static class Ward {
        int code;
        String name;
        
        Ward(int code, String name) {
            this.code = code;
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }

    // ===== Load Provinces =====
    
    private void loadProvinces() {
        new Thread(() -> {
            try {
                String jsonResponse = fetchFromAPI("https://provinces.open-api.vn/api/p/");
                JSONArray jsonArray = new JSONArray(jsonResponse);
                
                List<Province> provinceList = new ArrayList<>();
                
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    int code = obj.getInt("code");
                    String name = obj.getString("name");
                    provinceList.add(new Province(code, name));
                }
                
                provinces = provinceList;
                
                runOnUiThread(() -> {
                    ArrayAdapter<Province> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line, provinces);
                    actvProvince.setAdapter(adapter);
                    actvProvince.setThreshold(1);
                    
                    // Show dropdown when clicked
                    actvProvince.setOnFocusChangeListener((v, hasFocus) -> {
                        if (hasFocus) {
                            actvProvince.showDropDown();
                        }
                    });
                    
                    actvProvince.setOnClickListener(v -> {
                        actvProvince.showDropDown();
                    });
                    
                    actvProvince.setOnItemClickListener((parent, view, position, id) -> {
                        Province selected = (Province) parent.getItemAtPosition(position);
                        selectedProvince = selected;
                        loadDistricts(selected.code);
                        
                        // Clear district and ward
                        actvDistrict.setText("");
                        actvWard.setText("");
                        selectedDistrict = null;
                        selectedWard = null;
                    });
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi tải danh sách tỉnh/thành phố", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // ===== Load Districts =====
    
    private void loadDistricts(int provinceCode) {
        new Thread(() -> {
            try {
                String jsonResponse = fetchFromAPI("https://provinces.open-api.vn/api/p/" + provinceCode + "?depth=2");
                JSONObject jsonObject = new JSONObject(jsonResponse);
                JSONArray districtsArray = jsonObject.getJSONArray("districts");
                
                List<District> districtList = new ArrayList<>();
                
                for (int i = 0; i < districtsArray.length(); i++) {
                    JSONObject obj = districtsArray.getJSONObject(i);
                    int code = obj.getInt("code");
                    String name = obj.getString("name");
                    districtList.add(new District(code, name));
                }
                
                districts = districtList;
                
                runOnUiThread(() -> {
                    ArrayAdapter<District> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line, districts);
                    actvDistrict.setAdapter(adapter);
                    actvDistrict.setThreshold(1);
                    actvDistrict.setEnabled(true);
                    actvWard.setEnabled(false);
                    
                    // Show dropdown when clicked
                    actvDistrict.setOnFocusChangeListener((v, hasFocus) -> {
                        if (hasFocus) {
                            actvDistrict.showDropDown();
                        }
                    });
                    
                    actvDistrict.setOnClickListener(v -> {
                        actvDistrict.showDropDown();
                    });
                    
                    actvDistrict.setOnItemClickListener((parent, view, position, id) -> {
                        District selected = (District) parent.getItemAtPosition(position);
                        selectedDistrict = selected;
                        loadWards(selected.code);
                        
                        // Clear ward
                        actvWard.setText("");
                        selectedWard = null;
                    });
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi tải danh sách quận/huyện", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // ===== Load Wards =====
    
    private void loadWards(int districtCode) {
        new Thread(() -> {
            try {
                String jsonResponse = fetchFromAPI("https://provinces.open-api.vn/api/d/" + districtCode + "?depth=2");
                JSONObject jsonObject = new JSONObject(jsonResponse);
                JSONArray wardsArray = jsonObject.getJSONArray("wards");
                
                List<Ward> wardList = new ArrayList<>();
                
                for (int i = 0; i < wardsArray.length(); i++) {
                    JSONObject obj = wardsArray.getJSONObject(i);
                    int code = obj.getInt("code");
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
                    
                    // Show dropdown when clicked
                    actvWard.setOnFocusChangeListener((v, hasFocus) -> {
                        if (hasFocus) {
                            actvWard.showDropDown();
                        }
                    });
                    
                    actvWard.setOnClickListener(v -> {
                        actvWard.showDropDown();
                    });
                    
                    actvWard.setOnItemClickListener((parent, view, position, id) -> {
                        Ward selected = (Ward) parent.getItemAtPosition(position);
                        selectedWard = selected;
                    });
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi tải danh sách phường/xã", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // ===== Fetch from API =====
    
    private String fetchFromAPI(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "FindoraApp/1.0 (hcmute.edu.vn.findora)");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        
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
                    String district = "";
                    String ward = "";
                    String street = "";
                    
                    if (addressObj.has("city")) {
                        province = addressObj.getString("city");
                    } else if (addressObj.has("state")) {
                        province = addressObj.getString("state");
                    }
                    
                    if (addressObj.has("city_district")) {
                        district = addressObj.getString("city_district");
                    } else if (addressObj.has("county")) {
                        district = addressObj.getString("county");
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
                    String finalDistrict = district;
                    String finalWard = ward;
                    String finalStreet = street;
                    
                    runOnUiThread(() -> {
                        if (!finalProvince.isEmpty()) actvProvince.setText(finalProvince);
                        if (!finalDistrict.isEmpty()) actvDistrict.setText(finalDistrict);
                        if (!finalWard.isEmpty()) actvWard.setText(finalWard);
                        if (!finalStreet.isEmpty()) etStreetAddress.setText(finalStreet);
                        
                        Toast.makeText(this, "Đã lấy vị trí hiện tại", Toast.LENGTH_SHORT).show();
                    });
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Không thể xác định địa chỉ", Toast.LENGTH_SHORT).show();
                });
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
        
        if (selectedProvince == null) {
            Toast.makeText(this, "Vui lòng chọn Tỉnh/Thành phố", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedDistrict == null) {
            Toast.makeText(this, "Vui lòng chọn Quận/Huyện", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedWard == null) {
            Toast.makeText(this, "Vui lòng chọn Phường/Xã", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Build full address
        StringBuilder fullAddress = new StringBuilder();
        if (!streetAddress.isEmpty()) {
            fullAddress.append(streetAddress).append(", ");
        }
        fullAddress.append(selectedWard.name).append(", ");
        fullAddress.append(selectedDistrict.name).append(", ");
        fullAddress.append(selectedProvince.name);
        
        // Geocode address to get lat/lng
        geocodeAndShowMap(fullAddress.toString());
    }

    private void geocodeAndShowMap(String address) {
        new Thread(() -> {
            try {
                // Use Nominatim API
                String encodedAddress = java.net.URLEncoder.encode(address, "UTF-8");
                String url = "https://nominatim.openstreetmap.org/search?q=" + encodedAddress 
                           + "&format=json&addressdetails=1&limit=1&countrycodes=vn&accept-language=vi";
                
                String response = fetchFromAPI(url);
                
                JSONArray jsonArray = new JSONArray(response);
                if (jsonArray.length() > 0) {
                    JSONObject place = jsonArray.getJSONObject(0);
                    double lat = place.getDouble("lat");
                    double lon = place.getDouble("lon");
                    
                    runOnUiThread(() -> {
                        Intent intent = new Intent(AddressPickerActivity.this, MapActivity.class);
                        intent.putExtra("latitude", lat);
                        intent.putExtra("longitude", lon);
                        intent.putExtra("address", address);
                        startActivityForResult(intent, 100);
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Không tìm thấy địa chỉ này trên bản đồ", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
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
