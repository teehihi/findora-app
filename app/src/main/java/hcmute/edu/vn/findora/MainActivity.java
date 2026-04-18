package hcmute.edu.vn.findora;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hcmute.edu.vn.findora.adapter.PostAdapter;
import hcmute.edu.vn.findora.model.Post;
import hcmute.edu.vn.findora.utils.NotificationHelper;
import hcmute.edu.vn.findora.utils.WorkManagerHelper;

/**
 * Màn hình chính - theo Stitch Design 2024.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // UI
    private RecyclerView         recyclerView;
    private ProgressBar          progressBar;
    private LinearLayout         layoutEmpty;
    private BottomNavigationView bottomNav;
    private FloatingActionButton fabCreatePost;
    private TextView             tvHeading;
    private android.widget.EditText etSearch;
    private TextView             tvLocation;
    private LinearLayout         layoutAICard;
    private TextView             tvAITitle;
    private TextView             tvAIDescription;

    // Filter chips
    private TextView chipAll, chipLost, chipFound;
    private String currentFilter = "all";
    private String currentSearchQuery = "";

    // Adapter & data
    private PostAdapter adapter;
    private List<Post>  postList;
    private List<Post>  allPosts;

    // Firebase
    private FirebaseFirestore db;
    
    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Map padding: Top should apply to root. Bottom should NOT apply to root (so BottomNav stays at bottom).
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);

            // Give the BottomNav internal padding for the gesture bar handle
            bottomNav.setPadding(0, 0, 0, systemBars.bottom);
            // Ensure the main scrollable area doesn't get hidden behind the BottomNav
            findViewById(R.id.recyclerView).setPadding(
                findViewById(R.id.recyclerView).getPaddingLeft(),
                findViewById(R.id.recyclerView).getPaddingTop(),
                findViewById(R.id.recyclerView).getPaddingRight(),
                (int) (90 * getResources().getDisplayMetrics().density) // Height of bottom nav area
            );
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        executorService = Executors.newSingleThreadExecutor();

        recyclerView  = findViewById(R.id.recyclerView);
        progressBar   = findViewById(R.id.progressBar);
        layoutEmpty   = findViewById(R.id.layoutEmpty);
        bottomNav     = findViewById(R.id.bottomNav);
        fabCreatePost = findViewById(R.id.fabCreatePost);
        tvHeading     = findViewById(R.id.tvHeading);
        chipAll       = findViewById(R.id.chipAll);
        chipLost      = findViewById(R.id.chipLost);
        chipFound     = findViewById(R.id.chipFound);
        etSearch      = findViewById(R.id.etSearch);
        tvLocation    = findViewById(R.id.tvLocation);
        layoutAICard  = findViewById(R.id.layoutAICard);
        tvAITitle     = findViewById(R.id.tvAITitle);
        tvAIDescription = findViewById(R.id.tvAIDescription);
        
        ImageButton btnRefreshMatches = findViewById(R.id.btnRefreshMatches);
        btnRefreshMatches.setOnClickListener(v -> {
            android.widget.Toast.makeText(this, "Đang tìm matches mới...", android.widget.Toast.LENGTH_SHORT).show();
            WorkManagerHelper.runAIMatchingNow(this);
        });

        // Heading: "What are we finding today?" -> "finding" is blue italic
        setStylizedHeading();

        allPosts = new ArrayList<>();
        postList = new ArrayList<>();
        adapter  = new PostAdapter(this, postList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        // Setup search functionality
        setupSearch();
        
        // Get current location
        getCurrentLocation();
        
        // Load unread notification count
        loadUnreadNotificationCount();
        
        // Schedule periodic AI matching (mỗi 6 giờ)
        WorkManagerHelper.schedulePeriodicAIMatching(this);
        Log.d(TAG, "Periodic AI matching scheduled");

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Already on home, do nothing
                return true;
            } else if (id == R.id.nav_map) {
                Intent intent = new Intent(this, MapViewActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                // Slide right (Map is to the right of Home)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            } else if (id == R.id.nav_chat) {
                Intent intent = new Intent(this, ChatListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                // Slide right (Chat is to the right of Home)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                // Slide right (Profile is to the right of Home)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            return false;
        });

        // Adjust bottom nav icon positions to align with FAB
        bottomNav.post(() -> {
            try {
                // Get the BottomNavigationMenuView
                View menuView = bottomNav.getChildAt(0);
                if (menuView instanceof android.view.ViewGroup) {
                    android.view.ViewGroup menuViewGroup = (android.view.ViewGroup) menuView;
                    // Iterate through each menu item view
                    for (int i = 0; i < menuViewGroup.getChildCount(); i++) {
                        View itemView = menuViewGroup.getChildAt(i);
                        // Add top padding to push icons down
                        itemView.setPadding(
                            itemView.getPaddingLeft(),
                            itemView.getPaddingTop() + (int)(8 * getResources().getDisplayMetrics().density),
                            itemView.getPaddingRight(),
                            itemView.getPaddingBottom()
                        );
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        fabCreatePost.setOnClickListener(v -> {
            startActivity(new Intent(this, CreatePostActivity.class));
        });

        chipAll.setOnClickListener(v   -> applyFilter("all"));
        chipLost.setOnClickListener(v  -> applyFilter("lost"));
        chipFound.setOnClickListener(v -> applyFilter("found"));
        
        // AI Card click listener
        layoutAICard.setOnClickListener(v -> {
            // TODO: Navigate to AI matches screen or show dialog
            android.widget.Toast.makeText(this, "Xem chi tiết gợi ý AI", android.widget.Toast.LENGTH_SHORT).show();
        });

        loadPosts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set bottom nav to home when returning to this activity
        bottomNav.setSelectedItemId(R.id.nav_home);
        // Reload notification count
        loadUnreadNotificationCount();
    }

    private void setStylizedHeading() {
        String fullText = getString(R.string.home_heading);
        SpannableString spannable = new SpannableString(fullText);
        
        String findingWord = getString(R.string.home_heading_finding);
        int start = fullText.indexOf(findingWord);
        int end = start + findingWord.length();
        
        if (start != -1) {
            // Blue color
            spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.primary)), 
                    start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            // Bold Italic
            spannable.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), 
                    start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tvHeading.setText(spannable);
    }

    private void loadPosts() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);

        db.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (error != null || snapshots == null) {
                        showEmptyOrList();
                        return;
                    }

                    allPosts.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Post post = doc.toObject(Post.class);
                        post.setId(doc.getId());
                        allPosts.add(post);
                    }
                    applyFilter(currentFilter);
                    updateAIBanner();
                });
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        updateChipUI(filter);

        postList.clear();
        for (Post post : allPosts) {
            // Apply type filter
            boolean matchesType = "all".equals(filter) || filter.equals(post.getType());
            
            // Apply search filter
            boolean matchesSearch = currentSearchQuery.isEmpty() || matchesSearchQuery(post, currentSearchQuery);
            
            if (matchesType && matchesSearch) {
                postList.add(post);
            }
        }
        adapter.notifyDataSetChanged();
        showEmptyOrList();
    }
    
    /**
     * Setup search functionality
     */
    private void setupSearch() {
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                applyFilter(currentFilter);
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }
    
    /**
     * Check if post matches search query
     * Search in: title, description, address
     */
    private boolean matchesSearchQuery(Post post, String query) {
        if (query.isEmpty()) return true;
        
        // Search in title
        if (post.getTitle() != null && post.getTitle().toLowerCase().contains(query)) {
            return true;
        }
        
        // Search in description
        if (post.getDescription() != null && post.getDescription().toLowerCase().contains(query)) {
            return true;
        }
        
        // Search in address
        if (post.getAddress() != null && post.getAddress().toLowerCase().contains(query)) {
            return true;
        }
        
        // Search in image label (AI detected)
        if (post.getImageLabel() != null && post.getImageLabel().toLowerCase().contains(query)) {
            return true;
        }
        
        return false;
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
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private void updateChipUI(String activeFilter) {
        // Reset all to Inactive (Blue Outline)
        resetChip(chipAll, getString(R.string.chip_all));
        resetChip(chipLost, getString(R.string.chip_lost));
        resetChip(chipFound, getString(R.string.chip_found));

        // Set Active with specific color for each filter
        TextView activeChip;
        int activeBackground;
        switch (activeFilter) {
            case "lost":
                activeChip = chipLost;
                activeBackground = R.drawable.bg_chip_active_lost;
                break;
            case "found":
                activeChip = chipFound;
                activeBackground = R.drawable.bg_chip_active_found;
                break;
            default:
                activeChip = chipAll;
                activeBackground = R.drawable.bg_chip_active_all;
                break;
        }
        activeChip.setBackgroundResource(activeBackground);
        activeChip.setTextColor(ContextCompat.getColor(this, R.color.white));
        activeChip.setTypeface(null, Typeface.BOLD);
    }

    private void resetChip(TextView chip, String text) {
        chip.setBackgroundResource(R.drawable.bg_chip_inactive);
        chip.setTextColor(ContextCompat.getColor(this, R.color.primary));
        chip.setTypeface(null, Typeface.NORMAL);
        chip.setText(text);
    }

    private void showEmptyOrList() {
        if (postList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }
    
    /**
     * Cập nhật AI banner với gợi ý match thông minh
     * 
     * CHỨC NĂNG:
     * - Hiển thị gợi ý bài đăng phù hợp cho TỪNG USER
     * - Nếu user có bài đăng: Tìm matches cho bài đăng của họ
     * - Nếu user chưa có bài: Hiển thị các bài đăng gần vị trí hiện tại
     * 
     * LOGIC:
     * 1. Kiểm tra user có bài đăng không
     * 2. Nếu có: Tìm matches cho bài đăng của user (lost ↔ found)
     * 3. Nếu không: Hiển thị các bài đăng gần vị trí user (dựa trên location)
     * 4. Hiển thị tổng số gợi ý và bài phù hợp nhất
     */
    private void updateAIBanner() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null 
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() 
            : null;
        
        if (currentUserId == null || allPosts.isEmpty()) {
            // Không có user hoặc không có posts → hiển thị default
            tvAITitle.setText("Đăng bài để nhận gợi ý AI");
            tvAIDescription.setText("AI sẽ tự động tìm kiếm và gợi ý các bài đăng phù hợp với bạn");
            return;
        }
        
        // Tìm các bài đăng của user hiện tại
        List<Post> userPosts = new ArrayList<>();
        for (Post post : allPosts) {
            if (post.getUserId().equals(currentUserId)) {
                userPosts.add(post);
            }
        }
        
        if (userPosts.isEmpty()) {
            // User chưa có bài đăng → Hiển thị các bài gần vị trí hiện tại
            showNearbyPostsSuggestion();
            return;
        }
        
        // User có bài đăng → Tìm matches cho bài đăng của user
        int totalMatches = 0;
        AIMatchingHelper.MatchResult bestMatch = null;
        Post bestUserPost = null;
        
        for (Post userPost : userPosts) {
            List<AIMatchingHelper.MatchResult> matches = AIMatchingHelper.findMatches(userPost, allPosts);
            totalMatches += matches.size();
            
            // Tìm match tốt nhất
            if (!matches.isEmpty()) {
                AIMatchingHelper.MatchResult topMatch = matches.get(0);
                if (bestMatch == null || topMatch.score > bestMatch.score) {
                    bestMatch = topMatch;
                    bestUserPost = userPost;
                }
            }
        }
        
        // Cập nhật UI
        if (totalMatches == 0) {
            tvAITitle.setText("Chưa tìm thấy gợi ý phù hợp");
            tvAIDescription.setText("AI đang phân tích... Hãy thử thêm thông tin chi tiết hơn vào bài đăng");
        } else {
            // Có matches
            tvAITitle.setText(String.format("🔥 Tìm thấy %d gợi ý phù hợp", totalMatches));
            
            if (bestMatch != null && bestUserPost != null) {
                // Hiển thị: "Bài của bạn (lost/found) match với bài X"
                String userPostType = bestUserPost.getType().equals("lost") ? "mất" : "tìm thấy";
                String matchType = bestMatch.post.getType().equals("lost") ? "Mất" : "Tìm thấy";
                
                tvAIDescription.setText(String.format(
                    "%s: %s - Độ phù hợp %d%%",
                    matchType,
                    bestMatch.post.getTitle(),
                    bestMatch.getScorePercentage()
                ));
            } else {
                tvAIDescription.setText("Nhấn để xem chi tiết các gợi ý");
            }
        }
    }
    
    /**
     * Hiển thị gợi ý các bài đăng gần vị trí user (cho user chưa có bài đăng)
     */
    private void showNearbyPostsSuggestion() {
        // Lấy vị trí hiện tại của user (từ tvLocation hoặc fusedLocationClient)
        // Tạm thời hiển thị các bài đăng mới nhất
        if (allPosts.size() >= 2) {
            Post topPost = allPosts.get(0);
            String postType = topPost.getType().equals("lost") ? "Mất" : "Tìm thấy";
            
            tvAITitle.setText(String.format("📍 Có %d bài đăng gần bạn", Math.min(allPosts.size(), 10)));
            tvAIDescription.setText(String.format(
                "%s: %s",
                postType,
                topPost.getTitle()
            ));
        } else {
            tvAITitle.setText("Chưa có bài đăng nào");
            tvAIDescription.setText("Hãy là người đầu tiên đăng bài!");
        }
    }
    
    /**
     * Load số lượng thông báo chưa đọc và hiển thị badge
     */
    private void loadUnreadNotificationCount() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null 
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() 
            : null;
        
        if (currentUserId == null) return;
        
        NotificationHelper.getUnreadCount(currentUserId, count -> {
            // Hiển thị badge trên notification icon
            if (count > 0) {
                // TODO: Hiển thị badge số (có thể dùng BadgeDrawable)
                Log.d(TAG, "Unread notifications: " + count);
            }
        });
    }
}