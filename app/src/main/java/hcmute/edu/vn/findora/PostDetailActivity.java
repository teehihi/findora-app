package hcmute.edu.vn.findora;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Intent;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Màn hình chi tiết bài đăng - Stitch Design Style.
 */
public class PostDetailActivity extends AppCompatActivity {

    private ImageView ivDetailImage, ivMapPlaceholder;
    private TextView tvDetailType, tvDetailTitle, tvDetailTimeHeader, tvDetailDescription, tvDetailLocation, tvUserName, tvUserStatus;
    private TextView tvInfoTime, tvInfoCategory;
    private ImageButton btnBack;
    private com.google.firebase.firestore.FirebaseFirestore db;
    private com.mapbox.maps.MapView mapViewDetail;
    private androidx.cardview.widget.CardView cvMapPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        // Edge-to-edge support 
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detailToolbar), (v, insets) -> {
            v.setPadding(v.getPaddingLeft(), dpToPx(16), v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        initViews();
        initializeMap();
        displayData();

        btnBack.setOnClickListener(v -> finish());
    }

    private ImageView ivUserAvatar;
    
    private void initViews() {
        ivDetailImage       = findViewById(R.id.ivDetailImage);
        tvDetailType        = findViewById(R.id.tvDetailType);
        tvDetailTitle       = findViewById(R.id.tvDetailTitle);
        tvDetailTimeHeader  = findViewById(R.id.tvDetailTimeHeader);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvDetailLocation    = findViewById(R.id.tvDetailLocation);
        tvUserName          = findViewById(R.id.tvUserName);
        tvUserStatus        = findViewById(R.id.tvUserStatus);
        tvInfoTime          = findViewById(R.id.tvInfoTime);
        tvInfoCategory      = findViewById(R.id.tvInfoCategory);
        btnBack             = findViewById(R.id.btnBack);
        mapViewDetail       = findViewById(R.id.mapViewDetail);
        ivMapPlaceholder    = findViewById(R.id.ivMapPlaceholder);
        cvMapPreview        = findViewById(R.id.cvMapPreview);
        ivUserAvatar        = findViewById(R.id.ivUserAvatar);
    }
    
    private void initializeMap() {
        // Mapbox will be initialized when displaying location
    }
    
    private void displayMapLocation(double lat, double lng, String address) {
        // Hide placeholder, show map
        ivMapPlaceholder.setVisibility(View.GONE);
        mapViewDetail.setVisibility(View.VISIBLE);
        
        // Load Mapbox style and setup map
        mapViewDetail.getMapboxMap().loadStyleUri(com.mapbox.maps.Style.MAPBOX_STREETS, style -> {
            // Move camera to location
            com.mapbox.geojson.Point point = com.mapbox.geojson.Point.fromLngLat(lng, lat);
            com.mapbox.maps.CameraOptions cameraOptions = new com.mapbox.maps.CameraOptions.Builder()
                    .center(point)
                    .zoom(15.0)
                    .build();
            mapViewDetail.getMapboxMap().setCamera(cameraOptions);
            
            // Add marker using helper
            MapboxHelper.addMarker(mapViewDetail, lat, lng);
        });
        
        // Update location text
        if (address != null && !address.isEmpty()) {
            tvDetailLocation.setText(address);
        } else {
            tvDetailLocation.setText(String.format(java.util.Locale.getDefault(), "%.6f, %.6f", lat, lng));
        }
    }
    
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void displayData() {
        Bundle extras = getIntent().getExtras();
        if (extras == null) return;

        String title = extras.getString("title", "No Title");
        String description = extras.getString("description", "No Description");
        String type = extras.getString("type", "lost");
        String userId = extras.getString("userId", "");
        long timestamp = extras.getLong("timestamp", 0);

        tvDetailTitle.setText(title);
        tvDetailDescription.setText(description);
        
        // Check for location data
        if (extras.containsKey("lat") && extras.containsKey("lng")) {
            double lat = extras.getDouble("lat");
            double lng = extras.getDouble("lng");
            String address = extras.getString("address", "");
            
            if (lat != 0 && lng != 0) {
                displayMapLocation(lat, lng, address);
            } else {
                // No location, show placeholder
                ivMapPlaceholder.setVisibility(View.VISIBLE);
                mapViewDetail.setVisibility(View.GONE);
                tvDetailLocation.setText("Đang cập nhật (Sắp ra mắt)");
            }
        } else {
            // No location, show placeholder
            ivMapPlaceholder.setVisibility(View.VISIBLE);
            mapViewDetail.setVisibility(View.GONE);
            tvDetailLocation.setText("Đang cập nhật (Sắp ra mắt)");
        }
        
        // Fetch real name from Firestore
        if (!userId.isEmpty()) {
            tvUserName.setText("Loading...");
            fetchPosterName(userId);
        } else {
            tvUserName.setText("Anonymous User");
        }

        // Badge: LOST (Red) / FOUND (Blue)
        if ("lost".equals(type)) {
            tvDetailType.setText("MẤT");
            tvDetailType.setBackgroundResource(R.drawable.bg_badge_lost);
            tvDetailType.setTextColor(android.graphics.Color.parseColor("#D32F2F"));
        } else {
            tvDetailType.setText("NHẶT ĐƯỢC");
            tvDetailType.setBackgroundResource(R.drawable.bg_badge_found);
            tvDetailType.setTextColor(android.graphics.Color.parseColor("#1976D2"));
        }

        // Format times
        if (timestamp > 0) {
            Date date = new Date(timestamp);
            
            SimpleDateFormat relativeSdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            tvDetailTimeHeader.setText("Đăng lúc " + relativeSdf.format(date));
            
            SimpleDateFormat infoSdf = new SimpleDateFormat("HH:mm, dd/MM/yyyy", new Locale("vi", "VN"));
            tvInfoTime.setText(infoSdf.format(date));
        } else {
            tvDetailTimeHeader.setText("Vừa đăng");
            tvInfoTime.setText("N/A");
        }

        tvInfoCategory.setText("Đồ cá nhân");
        tvUserStatus.setText("Vừa truy cập • Đã xác thực");

        // Load image using Glide if available
        String imageUrl = getIntent().getStringExtra("imageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.bg_img_placeholder)
                .error(R.drawable.recent_activity_bg)
                .centerCrop()
                .into(ivDetailImage);
        } else {
            ivDetailImage.setImageResource(R.drawable.recent_activity_bg);
        }
        
        setupOwnerActions(getIntent().getExtras(), title, description, type, imageUrl, userId);
    }

    private void setupOwnerActions(Bundle extras, String title, String description, String type, String imageUrl, String postUserId) {
        TextView btnContact = findViewById(R.id.btnContact);
        TextView btnChat = findViewById(R.id.btnChat);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        
        if (auth.getCurrentUser() != null && auth.getCurrentUser().getUid().equals(postUserId)) {
            btnChat.setText("Chỉnh sửa");
            btnChat.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_edit_ios, 0, 0, 0);
            btnChat.setOnClickListener(v -> {
                Intent editIntent = new Intent(this, CreatePostActivity.class);
                editIntent.putExtra("editPostId", extras.getString("postId", ""));
                editIntent.putExtra("title", title);
                editIntent.putExtra("description", description);
                editIntent.putExtra("type", type);
                editIntent.putExtra("imageUrl", imageUrl);
                
                // Pass location data
                if (extras.containsKey("lat") && extras.containsKey("lng")) {
                    editIntent.putExtra("lat", extras.getDouble("lat"));
                    editIntent.putExtra("lng", extras.getDouble("lng"));
                    editIntent.putExtra("address", extras.getString("address", ""));
                }
                
                startActivity(editIntent);
                finish();
            });

            btnContact.setText("Xóa bài");
            int iosRed = android.graphics.Color.parseColor("#FF3B30");
            btnContact.setTextColor(iosRed);
            
            android.graphics.drawable.Drawable trashIcon = ContextCompat.getDrawable(this, R.drawable.ic_trash_ios);
            if (trashIcon != null) {
                androidx.core.graphics.drawable.DrawableCompat.setTint(
                        androidx.core.graphics.drawable.DrawableCompat.wrap(trashIcon).mutate(), iosRed);
                btnContact.setCompoundDrawablesWithIntrinsicBounds(trashIcon, null, null, null);
                btnContact.getCompoundDrawables()[0].setTintList(null);
            }
            
            btnContact.setOnClickListener(v -> {
                String postId = extras.getString("postId", "");
                if (postId != null && !postId.isEmpty()) {
                    db.collection("posts").document(postId).delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Đã xóa bài đăng thành công", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
        } else if (auth.getCurrentUser() != null) {
            // Người xem (không phải chủ bài) → hiện nút Nhắn tin + Gọi điện
            btnChat.setText("Nhắn tin");
            btnChat.setOnClickListener(v -> {
                String postId = extras.getString("postId", "");
                Intent chatIntent = new Intent(this, ChatActivity.class);
                chatIntent.putExtra("otherUserId", postUserId);
                chatIntent.putExtra("postId", postId);
                chatIntent.putExtra("postTitle", title);
                startActivity(chatIntent);
            });

            // Nút Liên hệ → gọi điện cho chủ bài
            btnContact.setText("Gọi điện");
            btnContact.setOnClickListener(v -> {
                // Lấy SĐT từ Firestore rồi mở dialer
                db.collection("users").document(postUserId).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String phone = doc.getString("phone");
                                if (phone != null && !phone.isEmpty()) {
                                    Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                                    dialIntent.setData(android.net.Uri.parse("tel:" + phone));
                                    startActivity(dialIntent);
                                } else {
                                    Toast.makeText(this, "Người đăng chưa cung cấp số điện thoại", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Không thể tải thông tin liên hệ", Toast.LENGTH_SHORT).show();
                        });
            });
        }
    }

    private void fetchPosterName(String userId) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String fullName = documentSnapshot.getString("fullName");
                    if (fullName != null && !fullName.isEmpty()) {
                        tvUserName.setText(fullName);
                    } else {
                        tvUserName.setText("Unknown User");
                    }
                    
                    // Load avatar if available
                    String photoUrl = documentSnapshot.getString("photoUrl");
                    if (photoUrl != null && !photoUrl.isEmpty() && ivUserAvatar != null) {
                        // Clear tint before loading image
                        ivUserAvatar.setImageTintList(null);
                        com.bumptech.glide.Glide.with(PostDetailActivity.this)
                            .load(photoUrl)
                            .circleCrop()
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(ivUserAvatar);
                    } else {
                        // Set tint back for placeholder icon
                        ivUserAvatar.setImageTintList(android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.primary, null)));
                    }
                } else {
                    tvUserName.setText("User Not Found");
                }
            })
            .addOnFailureListener(e -> {
                tvUserName.setText("Error loading name");
            });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (mapViewDetail != null) {
            mapViewDetail.onStart();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (mapViewDetail != null) {
            mapViewDetail.onStop();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapViewDetail != null) {
            mapViewDetail.onDestroy();
        }
    }
}
