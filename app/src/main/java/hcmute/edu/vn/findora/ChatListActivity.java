package hcmute.edu.vn.findora;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hcmute.edu.vn.findora.adapter.ChatListAdapter;

/**
 * Màn hình danh sách các cuộc trò chuyện.
 * Hiển thị tất cả chat mà user hiện tại tham gia.
 */
public class ChatListActivity extends AppCompatActivity {
    
    private static final String TAG = "ChatListActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private RecyclerView rvChatList;
    private LinearLayout layoutEmpty;
    private BottomNavigationView bottomNav;
    private FloatingActionButton fabCreatePost;
    private TextView tvLocation;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ChatListAdapter adapter;
    private List<DocumentSnapshot> chatDocs;
    private String currentUserId;
    
    private FusedLocationProviderClient fusedLocationClient;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        executorService = Executors.newSingleThreadExecutor();

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }
        currentUserId = auth.getCurrentUser().getUid();

        rvChatList = findViewById(R.id.rvChatList);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        bottomNav = findViewById(R.id.bottomNav);
        fabCreatePost = findViewById(R.id.fabCreatePost);
        tvLocation = findViewById(R.id.tvLocation);

        layoutEmpty = findViewById(R.id.layoutEmpty);
        bottomNav = findViewById(R.id.bottomNav);
        fabCreatePost = findViewById(R.id.fabCreatePost);
        tvLocation = findViewById(R.id.tvLocation);

        // Apply window insets for safe area
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(android.R.id.content), (v, insets) -> {
                androidx.core.graphics.Insets systemBars =
                    insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                v.setPadding(0, systemBars.top, 0, 0);
                bottomNav.setPadding(0, 0, 0, systemBars.bottom);
                return insets;
            });

        chatDocs = new ArrayList<>();
        adapter = new ChatListAdapter(this, chatDocs, currentUserId);
        rvChatList.setLayoutManager(new LinearLayoutManager(this));
        rvChatList.setAdapter(adapter);
        
        // Get current location
        getCurrentLocation();

        fabCreatePost.setOnClickListener(v -> {
            startActivity(new Intent(this, CreatePostActivity.class));
        });

        // Bottom Navigation
        bottomNav.setSelectedItemId(R.id.nav_chat);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                // Slide left (Home is to the left of Chat)
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (id == R.id.nav_map) {
                Intent intent = new Intent(this, MapViewActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                // Slide left (Map is to the left of Chat)
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                // Slide right (Profile is to the right of Chat)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else if (id == R.id.nav_chat) {
                // Already on chat
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        hcmute.edu.vn.findora.utils.PresenceManager.goOnline();
        bottomNav.setSelectedItemId(R.id.nav_chat);
        loadChats();
    }

    private void loadChats() {
        // Try without orderBy first to avoid index requirement
        db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        android.util.Log.e("ChatListActivity", "Error loading chats: " + error.getMessage(), error);
                        // Show error toast for debugging
                        android.widget.Toast.makeText(this, "Lỗi tải tin nhắn: " + error.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                        showEmpty();
                        return;
                    }
                    
                    if (snapshots == null) {
                        android.util.Log.d("ChatListActivity", "Snapshots is null");
                        showEmpty();
                        return;
                    }

                    android.util.Log.d("ChatListActivity", "Loaded " + snapshots.size() + " chats");
                    
                    chatDocs.clear();
                    
                    // Add all documents to list
                    List<DocumentSnapshot> tempList = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        tempList.add(doc);
                    }
                    
                    // Sort by lastTimestamp manually
                    tempList.sort((a, b) -> {
                        com.google.firebase.Timestamp tsA = a.getTimestamp("lastTimestamp");
                        com.google.firebase.Timestamp tsB = b.getTimestamp("lastTimestamp");
                        if (tsA == null && tsB == null) return 0;
                        if (tsA == null) return 1;
                        if (tsB == null) return -1;
                        return tsB.compareTo(tsA); // Descending order
                    });
                    
                    chatDocs.addAll(tempList);
                    adapter.notifyDataSetChanged();

                    if (chatDocs.isEmpty()) {
                        showEmpty();
                    } else {
                        rvChatList.setVisibility(View.VISIBLE);
                        layoutEmpty.setVisibility(View.GONE);
                    }
                });
    }

    private void showEmpty() {
        rvChatList.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }
    
    /**
     * Get current location and update UI
     */
    private void getCurrentLocation() {
        // Check permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        
        // Get last known location
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // Got location, do reverse geocoding
                        reverseGeocode(location.getLatitude(), location.getLongitude());
                    } else {
                        // Fallback to default
                        tvLocation.setText(R.string.location_hcmc);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get location", e);
                    tvLocation.setText(R.string.location_hcmc);
                });
    }
    
    /**
     * Reverse geocoding to get address from coordinates
     */
    private void reverseGeocode(double lat, double lng) {
        executorService.execute(() -> {
            try {
                // Use Nominatim for reverse geocoding (free, no API key needed)
                String urlString = String.format(
                        "https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f&zoom=18&addressdetails=1",
                        lat, lng
                );
                
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Findora-Android-App");
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // Parse JSON
                JSONObject json = new JSONObject(response.toString());
                JSONObject address = json.getJSONObject("address");
                
                // Build full address
                StringBuilder fullAddress = new StringBuilder();
                
                // Add road/street
                if (address.has("road")) {
                    fullAddress.append(address.getString("road"));
                }
                
                // Add suburb/neighbourhood
                if (address.has("suburb")) {
                    if (fullAddress.length() > 0) fullAddress.append(", ");
                    fullAddress.append(address.getString("suburb"));
                } else if (address.has("neighbourhood")) {
                    if (fullAddress.length() > 0) fullAddress.append(", ");
                    fullAddress.append(address.getString("neighbourhood"));
                }
                
                // Add city district
                if (address.has("city_district")) {
                    if (fullAddress.length() > 0) fullAddress.append(", ");
                    fullAddress.append(address.getString("city_district"));
                } else if (address.has("county")) {
                    if (fullAddress.length() > 0) fullAddress.append(", ");
                    fullAddress.append(address.getString("county"));
                }
                
                // Add city
                if (address.has("city")) {
                    if (fullAddress.length() > 0) fullAddress.append(", ");
                    fullAddress.append(address.getString("city"));
                } else if (address.has("town")) {
                    if (fullAddress.length() > 0) fullAddress.append(", ");
                    fullAddress.append(address.getString("town"));
                }
                
                // If no address found, use display_name
                String finalLocation;
                if (fullAddress.length() > 0) {
                    finalLocation = fullAddress.toString();
                } else if (json.has("display_name")) {
                    // Use display_name but shorten it
                    String displayName = json.getString("display_name");
                    String[] parts = displayName.split(",");
                    // Take first 3-4 parts
                    StringBuilder shortAddress = new StringBuilder();
                    for (int i = 0; i < Math.min(4, parts.length); i++) {
                        if (i > 0) shortAddress.append(",");
                        shortAddress.append(parts[i].trim());
                    }
                    finalLocation = shortAddress.toString();
                } else {
                    finalLocation = "Thành phố Hồ Chí Minh";
                }
                
                // Update UI on main thread
                runOnUiThread(() -> tvLocation.setText(finalLocation));
                
            } catch (Exception e) {
                Log.e(TAG, "Reverse geocoding failed", e);
                runOnUiThread(() -> tvLocation.setText(R.string.location_hcmc));
            }
        });
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get location
                getCurrentLocation();
            } else {
                // Permission denied, use default
                tvLocation.setText(R.string.location_hcmc);
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        hcmute.edu.vn.findora.utils.PresenceManager.goOffline();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
