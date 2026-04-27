package hcmute.edu.vn.findora;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
    private TextView tvUserName, tvActivePosts, tvItemsReturned, tvLocation;
    private TextView tvLevelBadge, tvFindoPoints, tvProgressPercent, tvLevelUpDesc;
    private android.widget.ProgressBar circularProgress;
    private android.view.View btnUpgrade;
    private android.widget.ImageView ivAvatar, ivLevelIcon;
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
        tvActivePosts = findViewById(R.id.tvActivePosts);
        tvItemsReturned = findViewById(R.id.tvItemsReturned);
        tvLevelBadge = findViewById(R.id.tvLevelBadge);
        tvFindoPoints = findViewById(R.id.tvFindoPoints);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        tvLevelUpDesc = findViewById(R.id.tvLevelUpDesc);
        circularProgress = findViewById(R.id.circularProgress);
        btnUpgrade = findViewById(R.id.btnUpgrade);
        ivAvatar = findViewById(R.id.ivAvatar);
        ivLevelIcon = findViewById(R.id.ivLevelIcon);

        // Long press badge to show detail modal
        findViewById(R.id.ivLevelIcon).setOnLongClickListener(v -> {
            showLevelBadgeDialog();
            return true;
        });
        tvLevelBadge = findViewById(R.id.tvLevelBadge);
        tvLevelBadge.setOnLongClickListener(v -> {
            showLevelBadgeDialog();
            return true;
        });
        bottomNav = findViewById(R.id.bottomNav);
        fabCreatePost = findViewById(R.id.fabCreatePost);
        tvLocation = findViewById(R.id.tvLocation);

        // Utility buttons
        findViewById(R.id.btnFindoraMap).setOnClickListener(v ->
            startActivity(new Intent(this, MapViewActivity.class)));
        findViewById(R.id.btnLeaderboard).setOnClickListener(v ->
            startActivity(new Intent(this, LeaderboardActivity.class)));
        findViewById(R.id.btnWallet).setOnClickListener(v ->
            startActivity(new Intent(this, WalletActivity.class)));
        findViewById(R.id.btnVoucherMarket).setOnClickListener(v ->
            startActivity(new Intent(this, VoucherMarketActivity.class)));

        // Pull to refresh
        androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(() -> {
            loadUserInfo();
            loadUserStats();
            swipeRefresh.setRefreshing(false);
        });

        // Apply window insets for safe area
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(android.R.id.content), (v, insets) -> {
                androidx.core.graphics.Insets systemBars =
                    insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                v.setPadding(0, systemBars.top, 0, 0);
                return insets;
            });
        // Prevent BottomNavigationView from consuming window insets
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            return androidx.core.view.WindowInsetsCompat.CONSUMED;
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
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_map) {
                Intent intent = new Intent(this, MapViewActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                // Slide left (Map is to the left of Profile)
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_chat) {
                Intent intent = new Intent(this, ChatListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                // Slide left (Chat is to the left of Profile)
                overridePendingTransition(0, 0);
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
                        // tvUserEmail replaced by level badge - no longer used

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
                        // tvUserEmail replaced by level badge
                        if (ivAvatar != null) {
                            ivAvatar.setImageResource(R.drawable.ic_person);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileActivity", "Error loading user info", e);
                    // Fallback to Auth
                    tvUserName.setText(user.getDisplayName() != null ? user.getDisplayName() : "User");
                    // tvUserEmail replaced by level badge
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

            // Load points & level from Firestore
            db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    long points = doc.contains("points") ? doc.getLong("points") : 0;
                    long returned = doc.contains("totalReturned") ? doc.getLong("totalReturned") : 0;

                    tvFindoPoints.setText(String.valueOf(points));
                    tvItemsReturned.setText(String.valueOf(returned));

                    // Update level badge
                    updateLevelBadge(points);

                    // Animate circular progress (next level = 100 pts per tier)
                    long nextLevel = ((points / 100) + 1) * 100;
                    long currentTierPoints = points % 100;
                    int progressPercent = (int) ((currentTierPoints * 100) / 100);

                    animateProgress(progressPercent);
                    tvProgressPercent.setText(progressPercent + "%");
                    tvLevelUpDesc.setText("Đạt " + nextLevel + " FP (FindoPoint) để lên cấp tiếp theo");

                    // Pulse animation on upgrade button if close to next level (>= 80%)
                    if (progressPercent >= 80) {
                        startPulseAnimation(btnUpgrade);
                    }
                });
        }
    }

    private long currentPoints = 0;

    private void updateLevelBadge(long points) {
        String name;
        int iconRes;
        if (points >= 1000) {
            name = "Huyền thoại";
            iconRes = R.drawable.ic_legendary;
        } else if (points >= 500) {
            name = "Thiên thần";
            iconRes = R.drawable.ic_angel;
        } else if (points >= 100) {
            name = "Người tốt";
            iconRes = R.drawable.ic_good;
        } else {
            name = "Người mới";
            iconRes = R.drawable.ic_newbie;
        }
        tvLevelBadge.setText(name);
        if (ivLevelIcon != null) ivLevelIcon.setImageResource(iconRes);
        currentPoints = points;
    }

    private void showLevelBadgeDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_level_badge);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(0xCC000000));
            dialog.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.85),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // Set content based on current level
        android.widget.ImageView ivBadge = dialog.findViewById(R.id.ivBadgeLarge);
        android.widget.TextView tvName = dialog.findViewById(R.id.tvBadgeName);
        android.widget.TextView tvRange = dialog.findViewById(R.id.tvBadgeRange);
        android.widget.TextView tvDesc = dialog.findViewById(R.id.tvBadgeDesc);
        android.widget.TextView tvPoints = dialog.findViewById(R.id.tvCurrentPoints);

        if (currentPoints >= 1000) {
            ivBadge.setImageResource(R.drawable.ic_legendary);
            tvName.setText("Huyền thoại");
            tvRange.setText("1000+ FindoPoint");
            tvDesc.setText("Bạn là huyền thoại của cộng đồng Findora! Cảm ơn vì những đóng góp tuyệt vời.");
        } else if (currentPoints >= 500) {
            ivBadge.setImageResource(R.drawable.ic_angel);
            tvName.setText("Thiên thần");
            tvRange.setText("500 – 999 FindoPoint");
            tvDesc.setText("Bạn đã trở thành thiên thần của cộng đồng, luôn sẵn sàng giúp đỡ mọi người!");
        } else if (currentPoints >= 100) {
            ivBadge.setImageResource(R.drawable.ic_good);
            tvName.setText("Người tốt");
            tvRange.setText("100 – 499 FindoPoint");
            tvDesc.setText("Bạn đã chứng minh mình là người tốt bụng và đáng tin cậy trong cộng đồng.");
        } else {
            ivBadge.setImageResource(R.drawable.ic_newbie);
            tvName.setText("Người mới");
            tvRange.setText("0 – 99 FindoPoint");
            tvDesc.setText("Bạn đang bắt đầu hành trình tìm kiếm và trả lại đồ vật!");
        }

        tvPoints.setText(currentPoints + " FP hiện tại");

        // Scale animation
        dialog.getWindow().getDecorView().setScaleX(0.8f);
        dialog.getWindow().getDecorView().setScaleY(0.8f);
        dialog.getWindow().getDecorView().setAlpha(0f);
        dialog.getWindow().getDecorView().animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(200)
            .setInterpolator(new android.view.animation.DecelerateInterpolator())
            .start();

        dialog.show();
    }

    /**
     * TASK 4: Animate ProgressBar from 0 to target value smoothly
     */
    private void animateProgress(int targetProgress) {
        ObjectAnimator animator = ObjectAnimator.ofInt(circularProgress, "progress", 0, targetProgress);
        animator.setDuration(1200);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.start();
    }

    /**
     * TASK 4: Continuous pulse/breathing animation on upgrade button
     */
    private void startPulseAnimation(android.view.View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 1.05f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 1.05f, 1.0f);
        scaleX.setDuration(1000);
        scaleY.setDuration(1000);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        scaleX.start();
        scaleY.start();
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
