package hcmute.edu.vn.findora;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {
    
    private static final String TAG = "ProfileActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextView tvUserName, tvUserEmail, tvActivePosts, tvItemsReturned, tvLocation;
    private android.widget.ImageView ivAvatar;
    private BottomNavigationView bottomNav;
    private FloatingActionButton fabCreatePost;
    
    private FusedLocationProviderClient fusedLocationClient;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        executorService = Executors.newSingleThreadExecutor();
        
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvActivePosts = findViewById(R.id.tvActivePosts);
        tvItemsReturned = findViewById(R.id.tvItemsReturned);
        ivAvatar = findViewById(R.id.ivAvatar);
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
        
        // Get current location
        getCurrentLocation();

        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

        // Edit Profile
        TextView btnEditProfile = findViewById(R.id.btnEditProfile);
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                startActivity(new Intent(this, EditProfileActivity.class));
            });
        }

        // FAB
        fabCreatePost.setOnClickListener(v -> {
            startActivity(new Intent(this, CreatePostActivity.class));
        });

        // My Posts Menu
        View btnMyPosts = findViewById(R.id.btnMyPosts);
        if (btnMyPosts != null) {
            btnMyPosts.setOnClickListener(v -> {
                startActivity(new Intent(this, MyPostsActivity.class));
            });
        }

        // Bottom nav
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                // Slide left (going to left tab)
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (id == R.id.nav_map) {
                Intent intent = new Intent(this, MapViewActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                // Slide left (Map is to the left of Profile)
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (id == R.id.nav_chat) {
                Intent intent = new Intent(this, ChatListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                // Slide left (Chat is to the left of Profile)
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                // Already on profile
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserInfo();
        loadUserStats();
    }

    private void loadUserInfo() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            
            // Load from Firestore
            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        String email = documentSnapshot.getString("email");
                        String photoUrl = documentSnapshot.getString("photoUrl");
                        
                        tvUserName.setText(fullName != null ? fullName : "User");
                        tvUserEmail.setText(email != null ? email : "");

                        if (photoUrl != null && !photoUrl.isEmpty() && ivAvatar != null) {
                            com.bumptech.glide.Glide.with(this)
                                    .load(photoUrl)
                                    .transform(new com.bumptech.glide.load.resource.bitmap.CircleCrop())
                                    .placeholder(R.drawable.ic_person)
                                    .error(R.drawable.ic_person)
                                    .into(ivAvatar);
                        } else if (ivAvatar != null) {
                            // Clear
                            ivAvatar.setImageResource(R.drawable.ic_person);
                        }
                    } else {
                        // Fallback to Auth if Firestore doc doesn't exist
                        tvUserName.setText(user.getDisplayName() != null ? user.getDisplayName() : "User");
                        tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "");
                        if (ivAvatar != null) {
                            ivAvatar.setImageResource(R.drawable.ic_person);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileActivity", "Error loading user info", e);
                    // Fallback to Auth
                    tvUserName.setText(user.getDisplayName() != null ? user.getDisplayName() : "User");
                    tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "");
                    if (ivAvatar != null) {
                        ivAvatar.setImageResource(R.drawable.ic_person);
                    }
                });
        }
    }

    private void loadUserStats() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            
            // Count active posts
            db.collection("posts")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    tvActivePosts.setText(String.valueOf(count));
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileActivity", "Error loading posts count", e);
                    tvActivePosts.setText("0");
                });
        }
    }

    private void logout() {
        auth.signOut();
        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
                        if (tvLocation != null) {
                            tvLocation.setText(R.string.location_hcmc);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get location", e);
                    if (tvLocation != null) {
                        tvLocation.setText(R.string.location_hcmc);
                    }
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
                runOnUiThread(() -> {
                    if (tvLocation != null) {
                        tvLocation.setText(finalLocation);
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Reverse geocoding failed", e);
                runOnUiThread(() -> {
                    if (tvLocation != null) {
                        tvLocation.setText(R.string.location_hcmc);
                    }
                });
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
                if (tvLocation != null) {
                    tvLocation.setText(R.string.location_hcmc);
                }
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
