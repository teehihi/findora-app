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
import java.util.Date;
import java.util.Locale;

import android.content.Intent;
import android.widget.Toast;
import android.widget.ProgressBar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import hcmute.edu.vn.findora.adapter.CommentAdapter;
import hcmute.edu.vn.findora.adapter.PostAdapter;
import hcmute.edu.vn.findora.model.Post;
import hcmute.edu.vn.findora.MapboxHelper;

/**
 * Màn hình chi tiết bài đăng - Stitch Design Style.
 */
public class PostDetailActivity extends AppCompatActivity {

    private ImageView ivDetailImage, ivMapPlaceholder;
    private TextView tvDetailType, tvDetailTitle, tvDetailTimeHeader, tvDetailDescription, tvDetailLocation, tvUserName, tvUserStatus;
    private TextView tvInfoTime, tvInfoCategory;
    private ImageButton btnBack, btnLike;
    private TextView tvLikeCount, tvCommentCount;
    private com.google.firebase.firestore.FirebaseFirestore db;
    private com.mapbox.maps.MapView mapViewDetail;
    private androidx.cardview.widget.CardView cvMapPreview;
    
    // Smart Matches components
    private RecyclerView rvSmartMatches;
    private ProgressBar pbSmartMatches;
    private TextView tvNoSmartMatches;
    private PostAdapter smartMatchesAdapter;
    private List<Post> smartMatchesList;
    
    // Comment components
    private RecyclerView rvComments;
    private com.google.android.material.textfield.TextInputEditText etComment;
    private ImageButton btnSendComment;
    private CommentAdapter commentAdapter;
    private List<hcmute.edu.vn.findora.model.Comment> commentsList;
    
    // Current post data
    private String currentPostId;
    private String currentPostOwnerId;
    private String currentPostTitle;
    private boolean isLiked = false;
    private List<String> likesList;

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
    private androidx.core.widget.NestedScrollView scrollView;
    
    private void initViews() {
        scrollView          = findViewById(R.id.scrollViewDetail);
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
        
        rvSmartMatches      = findViewById(R.id.rvSmartMatches);
        pbSmartMatches      = findViewById(R.id.pbSmartMatches);
        tvNoSmartMatches    = findViewById(R.id.tvNoSmartMatches);
        
        // Like & Comment components
        btnLike             = findViewById(R.id.btnLike);
        tvLikeCount         = findViewById(R.id.tvLikeCount);
        tvCommentCount      = findViewById(R.id.tvCommentCount);
        rvComments          = findViewById(R.id.rvComments);
        etComment           = findViewById(R.id.etComment);
        btnSendComment      = findViewById(R.id.btnSendComment);
        
        smartMatchesList = new ArrayList<>();
        smartMatchesAdapter = new PostAdapter(this, smartMatchesList);
        rvSmartMatches.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rvSmartMatches.setAdapter(smartMatchesAdapter);
        
        // Setup comments
        commentsList = new ArrayList<>();
        commentAdapter = new CommentAdapter(this, commentsList);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);
        rvComments.setNestedScrollingEnabled(false);
        
        likesList = new ArrayList<>();
        
        // Like button click
        btnLike.setOnClickListener(v -> toggleLike());
        
        // Send comment button click
        btnSendComment.setOnClickListener(v -> sendComment());
        
        // Scroll to comment input when focused
        etComment.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && scrollView != null) {
                v.postDelayed(() -> {
                    scrollView.smoothScrollTo(0, scrollView.getBottom());
                }, 300);
            }
        });
    }
    
    private void initializeMap() {
        // Mapbox will be initialized when displaying location
    }
    
    private void displayMapLocation(double lat, double lng, String address, String type) {
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
            
            // Add marker using helper (generic blue pin)
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
        String imageUrl = extras.getString("imageUrl");
        
        // Save current post info
        currentPostId = extras.getString("postId", "");
        currentPostOwnerId = userId;
        currentPostTitle = title;
        
        // Build current Post for matching
        Post currentPost = new Post();
        currentPost.setId(currentPostId);
        currentPost.setTitle(title);
        currentPost.setDescription(description);
        currentPost.setType(type);
        currentPost.setImageUrl(imageUrl);
        
        tvDetailTitle.setText(title);
        tvDetailDescription.setText(description);
        
        // Check for location data
        if (extras.containsKey("lat") && extras.containsKey("lng")) {
            double lat = extras.getDouble("lat");
            double lng = extras.getDouble("lng");
            String address = extras.getString("address", "");
            
            currentPost.setLat(lat);
            currentPost.setLng(lng);
            
            if (lat != 0 && lng != 0) {
                displayMapLocation(lat, lng, address, type);
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

        // Badge: LOST (Red) / FOUND (Green)
        if ("lost".equals(type)) {
            tvDetailType.setText("THẤT LẠC");
            tvDetailType.setBackgroundResource(R.drawable.bg_badge_lost);
            tvDetailType.setTextColor(android.graphics.Color.WHITE);
        } else {
            tvDetailType.setText("TÌM THẤY");
            tvDetailType.setBackgroundResource(R.drawable.bg_badge_found);
            tvDetailType.setTextColor(android.graphics.Color.WHITE);
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
        
        // Fetch full post to get AI details and then trigger Smart Matches
        fetchFullCurrentPostAndMatch(currentPost);
        
        // Load likes and comments
        loadLikesAndComments();
    }
    
    private void fetchFullCurrentPostAndMatch(Post memoryPost) {
        if (memoryPost.getId() == null || memoryPost.getId().isEmpty()) {
            pbSmartMatches.setVisibility(View.GONE);
            tvNoSmartMatches.setVisibility(View.VISIBLE);
            return;
        }
        
        db.collection("posts").document(memoryPost.getId()).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Post fullPost = doc.toObject(Post.class);
                    if (fullPost != null) {
                        fullPost.setId(doc.getId());
                        findSmartMatches(fullPost);
                    }
                } else {
                    pbSmartMatches.setVisibility(View.GONE);
                    tvNoSmartMatches.setVisibility(View.VISIBLE);
                }
            })
            .addOnFailureListener(e -> {
                pbSmartMatches.setVisibility(View.GONE);
                tvNoSmartMatches.setVisibility(View.VISIBLE);
            });
    }
    
    private void findSmartMatches(Post currentPost) {
        db.collection("posts")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                pbSmartMatches.setVisibility(View.GONE);
                
                // Chuyển đổi tất cả posts từ Firestore
                List<Post> allPosts = new ArrayList<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Post post = doc.toObject(Post.class);
                    post.setId(doc.getId());
                    allPosts.add(post);
                }
                
                // Sử dụng AIMatchingHelper với logic mới (Title 50% + Location 30% + Time 20%)
                List<AIMatchingHelper.MatchResult> matches = AIMatchingHelper.findMatches(currentPost, allPosts);
                
                if (matches.isEmpty()) {
                    tvNoSmartMatches.setVisibility(View.VISIBLE);
                    rvSmartMatches.setVisibility(View.GONE);
                } else {
                    tvNoSmartMatches.setVisibility(View.GONE);
                    rvSmartMatches.setVisibility(View.VISIBLE);
                    
                    smartMatchesList.clear();
                    // Lấy top 5 bài đăng match tốt nhất
                    int limit = Math.min(matches.size(), 5);
                    for (int i = 0; i < limit; i++) {
                        smartMatchesList.add(matches.get(i).post);
                    }
                    smartMatchesAdapter.notifyDataSetChanged();
                }
            })
            .addOnFailureListener(e -> {
                pbSmartMatches.setVisibility(View.GONE);
                tvNoSmartMatches.setVisibility(View.VISIBLE);
            });
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
    
    /**
     * Load likes và comments từ Firestore
     * 
     * CHỨC NĂNG:
     * - Load danh sách user IDs đã like bài đăng
     * - Load tất cả comments realtime (auto-update khi có comment mới)
     * - Update UI (số likes, số comments)
     * 
     * FIRESTORE STRUCTURE:
     * - Likes: posts/{postId}.likes (Array<String>)
     * - Comments: posts/{postId}/comments/{commentId}
     * 
     * ĐƯỢC GỌI TỪ:
     * - displayData() - Khi mở bài đăng
     * 
     * LƯU Ý:
     * - Comments dùng addSnapshotListener để realtime update
     * - Likes chỉ load 1 lần (update khi user like/unlike)
     */
    private void loadLikesAndComments() {
        if (currentPostId == null || currentPostId.isEmpty()) return;
        
        // Load likes
        db.collection("posts").document(currentPostId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    @SuppressWarnings("unchecked")
                    List<String> likes = (List<String>) doc.get("likes");
                    if (likes != null) {
                        likesList = likes;
                        updateLikeUI();
                    }
                }
            });
        
        // Load comments realtime
        db.collection("posts").document(currentPostId)
            .collection("comments")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null || snapshots == null) return;
                
                commentsList.clear();
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshots) {
                    hcmute.edu.vn.findora.model.Comment comment = doc.toObject(hcmute.edu.vn.findora.model.Comment.class);
                    comment.setId(doc.getId());
                    commentsList.add(comment);
                }
                commentAdapter.notifyDataSetChanged();
                updateCommentCount();
            });
    }
    
    /**
     * Toggle like/unlike bài đăng
     * 
     * CHỨC NĂNG:
     * - Nếu chưa like: Thêm user ID vào danh sách likes
     * - Nếu đã like: Xóa user ID khỏi danh sách likes
     * - Update Firestore
     * - Gửi notification cho chủ bài (nếu không phải chính mình)
     * - Update UI (icon đổi màu, số likes)
     * 
     * LOGIC:
     * 1. Kiểm tra user đã login chưa
     * 2. Check isLiked (dựa vào likesList.contains(currentUserId))
     * 3. Add/Remove user ID từ likesList
     * 4. Update Firestore: posts/{postId}.likes
     * 5. Gửi notification (nếu cần)
     * 6. Update UI
     * 
     * ĐƯỢC GỌI TỪ:
     * - btnLike.setOnClickListener() - Khi user click icon trái tim
     * 
     * VÍ DỤ:
     * User A like bài của User B:
     * - likesList: [] → ["userA_id"]
     * - Icon: outline → filled red
     * - User B nhận notification: "User A đã thích bài đăng"
     */
    private void toggleLike() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String currentUserId = auth.getCurrentUser().getUid();
        
        if (isLiked) {
            // Unlike
            likesList.remove(currentUserId);
        } else {
            // Like
            likesList.add(currentUserId);
            
            // Gửi notification nếu không phải chủ bài
            if (!currentUserId.equals(currentPostOwnerId)) {
                sendLikeNotification();
            }
        }
        
        // Update Firestore
        db.collection("posts").document(currentPostId)
            .update("likes", likesList)
            .addOnSuccessListener(aVoid -> {
                isLiked = !isLiked;
                updateLikeUI();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    /**
     * Update UI của like button và like count
     * 
     * CHỨC NĂNG:
     * - Kiểm tra user hiện tại đã like chưa
     * - Update icon: outline (chưa like) hoặc filled red (đã like)
     * - Update số lượng likes
     * - Show/hide like count
     * 
     * LOGIC:
     * 1. Check isLiked = likesList.contains(currentUserId)
     * 2. Update icon:
     *    - Đã like: ic_favorite_filled (màu đỏ)
     *    - Chưa like: ic_favorite (màu xám)
     * 3. Update count:
     *    - count > 0: Hiển thị số
     *    - count = 0: Ẩn
     * 
     * ĐƯỢC GỌI TỪ:
     * - loadLikesAndComments() - Khi load bài đăng
     * - toggleLike() - Sau khi like/unlike thành công
     * 
     * VÍ DỤ:
     * - 0 likes: Icon xám, không hiển thị số
     * - 5 likes (user đã like): Icon đỏ, hiển thị "5"
     * - 10 likes (user chưa like): Icon xám, hiển thị "10"
     */
    private void updateLikeUI() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String currentUserId = auth.getCurrentUser().getUid();
            isLiked = likesList.contains(currentUserId);
        }
        
        // Update icon
        if (isLiked) {
            btnLike.setImageResource(R.drawable.ic_favorite_filled);
            btnLike.setColorFilter(ContextCompat.getColor(this, R.color.error));
        } else {
            btnLike.setImageResource(R.drawable.ic_favorite);
            btnLike.setColorFilter(ContextCompat.getColor(this, R.color.text_secondary));
        }
        
        // Update count
        int count = likesList.size();
        if (count > 0) {
            tvLikeCount.setText(String.valueOf(count));
            tvLikeCount.setVisibility(View.VISIBLE);
        } else {
            tvLikeCount.setVisibility(View.GONE);
        }
    }
    
    /**
     * Update số lượng comments
     * 
     * CHỨC NĂNG:
     * - Đếm số comments trong commentsList
     * - Update TextView hiển thị số comments
     * - Show/hide comment count
     * 
     * LOGIC:
     * - count > 0: Hiển thị số (VD: "5")
     * - count = 0: Ẩn
     * 
     * ĐƯỢC GỌI TỪ:
     * - loadLikesAndComments() - Khi có comment mới (realtime listener)
     * 
     * VÍ DỤ:
     * - 0 comments: Không hiển thị số
     * - 3 comments: Hiển thị "3"
     */
    private void updateCommentCount() {
        int count = commentsList.size();
        if (count > 0) {
            tvCommentCount.setText(String.valueOf(count));
            tvCommentCount.setVisibility(View.VISIBLE);
        } else {
            tvCommentCount.setVisibility(View.GONE);
        }
    }
    
    /**
     * Gửi comment mới
     * 
     * CHỨC NĂNG:
     * - Lấy text từ EditText
     * - Validate (không empty)
     * - Lấy thông tin user hiện tại (name, avatar)
     * - Tạo Comment object
     * - Lưu vào Firestore subcollection
     * - Gửi notification cho chủ bài (nếu không phải chính mình)
     * - Clear EditText
     * 
     * LOGIC:
     * 1. Kiểm tra user đã login chưa
     * 2. Validate text không empty
     * 3. Get user info từ Firestore users/{userId}
     * 4. Create Comment(userId, userName, userAvatar, text)
     * 5. Save to posts/{postId}/comments
     * 6. Gửi notification (nếu cần)
     * 7. Clear input
     * 
     * FIRESTORE STRUCTURE:
     * posts/{postId}/comments/{commentId}
     *   ├─ userId: "abc123"
     *   ├─ userName: "Nguyễn Văn A"
     *   ├─ userAvatar: "https://..."
     *   ├─ text: "Bạn tìm thấy ở đâu?"
     *   └─ timestamp: Timestamp.now()
     * 
     * ĐƯỢC GỌI TỪ:
     * - btnSendComment.setOnClickListener() - Khi user click nút gửi
     * 
     * VÍ DỤ:
     * User A comment "Bạn tìm thấy ở đâu?" vào bài của User B:
     * - Comment được lưu vào Firestore
     * - Comment hiển thị ngay lập tức (realtime listener)
     * - User B nhận notification: "User A đã bình luận: Bạn tìm thấy ở đâu?"
     */
    private void sendComment() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String text = etComment.getText() != null ? etComment.getText().toString().trim() : "";
        if (text.isEmpty()) return;
        
        String currentUserId = auth.getCurrentUser().getUid();
        
        // Get current user info
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String userName = doc.getString("fullName");
                    String userAvatar = doc.getString("photoUrl");
                    
                    // Create comment
                    hcmute.edu.vn.findora.model.Comment comment = new hcmute.edu.vn.findora.model.Comment(
                        currentUserId,
                        userName != null ? userName : "User",
                        userAvatar,
                        text
                    );
                    
                    // Save to Firestore
                    db.collection("posts").document(currentPostId)
                        .collection("comments")
                        .add(comment)
                        .addOnSuccessListener(documentReference -> {
                            etComment.setText("");
                            
                            // Gửi notification nếu không phải chủ bài
                            if (!currentUserId.equals(currentPostOwnerId)) {
                                sendCommentNotification(userName, userAvatar, text);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                }
            });
    }
    
    /**
     * Gửi notification khi user like bài đăng
     * 
     * CHỨC NĂNG:
     * - Lấy thông tin user hiện tại (name, avatar)
     * - Gọi NotificationHelper.sendLikeNotification()
     * - Lưu notification vào Firestore
     * - Trigger FCM push notification (nếu có)
     * 
     * LOGIC:
     * 1. Get user info từ Firestore users/{currentUserId}
     * 2. Call NotificationHelper.sendLikeNotification(
     *      postOwnerId,    // Người nhận notification
     *      postId,         // Bài đăng
     *      postTitle,      // Tiêu đề bài đăng
     *      likerId,        // Người like
     *      likerName,      // Tên người like
     *      likerAvatar     // Avatar người like
     *    )
     * 
     * NOTIFICATION:
     * - Type: "like"
     * - Title: "User A đã thích bài đăng"
     * - Body: "Mất mèo vàng"
     * - Icon: Heart icon (red heart)
     * - Action: Mở PostDetailActivity
     * 
     * ĐƯỢC GỌI TỪ:
     * - toggleLike() - Khi user like (không phải chủ bài)
     * 
     * LƯU Ý:
     * - Chỉ gửi khi currentUserId != postOwnerId (không gửi cho chính mình)
     */
    private void sendLikeNotification() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        
        String currentUserId = auth.getCurrentUser().getUid();
        
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String likerName = doc.getString("fullName");
                    String likerAvatar = doc.getString("photoUrl");
                    
                    hcmute.edu.vn.findora.utils.NotificationHelper.sendLikeNotification(
                        currentPostOwnerId,
                        currentPostId,
                        currentPostTitle,
                        currentUserId,
                        likerName != null ? likerName : "Người dùng",
                        likerAvatar
                    );
                }
            });
    }
    
    /**
     * Gửi notification khi user comment bài đăng
     * 
     * CHỨC NĂNG:
     * - Gọi NotificationHelper.sendCommentNotification()
     * - Lưu notification vào Firestore
     * - Trigger FCM push notification (nếu có)
     * 
     * PARAMETERS:
     * @param commenterName Tên người comment
     * @param commenterAvatar Avatar người comment
     * @param commentText Nội dung comment
     * 
     * LOGIC:
     * Call NotificationHelper.sendCommentNotification(
     *   postOwnerId,      // Người nhận notification
     *   postId,           // Bài đăng
     *   postTitle,        // Tiêu đề bài đăng
     *   commenterId,      // Người comment
     *   commenterName,    // Tên người comment
     *   commenterAvatar,  // Avatar người comment
     *   commentText       // Nội dung comment
     * )
     * 
     * NOTIFICATION:
     * - Type: "comment"
     * - Title: "User A đã bình luận"
     * - Body: "Bạn tìm thấy ở đâu?"
     * - Icon: Comment icon (comment bubble)
     * - Action: Mở PostDetailActivity
     * 
     * ĐƯỢC GỌI TỪ:
     * - sendComment() - Sau khi lưu comment thành công
     * 
     * LƯU Ý:
     * - Chỉ gửi khi currentUserId != postOwnerId (không gửi cho chính mình)
     * - Comment text được hiển thị trong notification body
     */
    private void sendCommentNotification(String commenterName, String commenterAvatar, String commentText) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        
        String currentUserId = auth.getCurrentUser().getUid();
        
        hcmute.edu.vn.findora.utils.NotificationHelper.sendCommentNotification(
            currentPostOwnerId,
            currentPostId,
            currentPostTitle,
            currentUserId,
            commenterName,
            commenterAvatar,
            commentText
        );
    }
}
