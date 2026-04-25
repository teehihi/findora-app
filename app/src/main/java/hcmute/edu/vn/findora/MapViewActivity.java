package hcmute.edu.vn.findora;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationPluginImplKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManagerKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.findora.model.Post;

/**
 * MapViewActivity - Hiển thị bản đồ với tất cả các vật dụng mất/tìm thấy
 * Giống app "Tìm" trên iPhone
 * 
 * CHỨC NĂNG:
 * - Hiển thị bản đồ toàn màn hình
 * - Marker cho mỗi bài đăng (lost = đỏ, found = xanh)
 * - Click marker để xem chi tiết
 * - Bottom navigation để chuyển màn hình
 */
public class MapViewActivity extends AppCompatActivity {

    private static final String TAG = "MapViewActivity";
    
    // UI
    private MapView mapView;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNav;
    
    // Mapbox
    private PointAnnotationManager pointAnnotationManager;
    
    // Firebase
    private FirebaseFirestore db;
    
    // Data
    private List<Post> allPosts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mapViewRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            
            // Give the BottomNav internal padding for the gesture bar handle
            bottomNav.setPadding(0, 0, 0, systemBars.bottom);
            
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        
        mapView = findViewById(R.id.mapView);
        progressBar = findViewById(R.id.progressBar);
        bottomNav = findViewById(R.id.bottomNav);
        
        FloatingActionButton fabCreatePost = findViewById(R.id.fabCreatePost);
        fabCreatePost.setOnClickListener(v -> {
            startActivity(new Intent(this, CreatePostActivity.class));
        });
        
        // Set selected item
        bottomNav.setSelectedItemId(R.id.nav_map);
        
        // Bottom navigation
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_map) {
                // Already on map
                return true;
            } else if (id == R.id.nav_chat) {
                Intent intent = new Intent(this, ChatListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
        
        // Adjust bottom nav icon positions
        bottomNav.post(() -> {
            try {
                View menuView = bottomNav.getChildAt(0);
                if (menuView instanceof android.view.ViewGroup) {
                    android.view.ViewGroup menuViewGroup = (android.view.ViewGroup) menuView;
                    for (int i = 0; i < menuViewGroup.getChildCount(); i++) {
                        View itemView = menuViewGroup.getChildAt(i);
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
        
        // Initialize map
        initializeMap();
    }
    
    /**
     * Khởi tạo bản đồ và load dữ liệu
     */
    private void initializeMap() {
        progressBar.setVisibility(View.VISIBLE);
        
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
            // Set camera to Ho Chi Minh City
            CameraOptions cameraOptions = new CameraOptions.Builder()
                    .center(Point.fromLngLat(106.7717, 10.8505)) // Thủ Đức
                    .zoom(11.0)
                    .build();
            mapView.getMapboxMap().setCamera(cameraOptions);
            
            // Initialize annotation manager
            AnnotationPlugin annotationPlugin = AnnotationPluginImplKt.getAnnotations(mapView);
            pointAnnotationManager = PointAnnotationManagerKt.createPointAnnotationManager(annotationPlugin, mapView);
            
            // Load posts from Firestore
            loadPosts();
        });
    }
    
    /**
     * Load tất cả bài đăng từ Firestore
     */
    private void loadPosts() {
        db.collection("posts")
                .addSnapshotListener((snapshots, error) -> {
                    progressBar.setVisibility(View.GONE);
                    
                    if (error != null) {
                        Log.e(TAG, "Error loading posts", error);
                        Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (snapshots == null) return;
                    
                    allPosts.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Post post = doc.toObject(Post.class);
                        post.setId(doc.getId());
                        
                        // Chỉ thêm bài đăng có location
                        if (post.getLat() != null && post.getLng() != null) {
                            allPosts.add(post);
                        }
                    }
                    
                    Log.d(TAG, "Loaded " + allPosts.size() + " posts with location");
                    
                    // Hiển thị markers
                    displayMarkers();
                });
    }
    
    /**
     * Hiển thị markers cho tất cả bài đăng
     */
    private void displayMarkers() {
        if (pointAnnotationManager == null) return;
        
        // Clear old markers
        pointAnnotationManager.deleteAll();
        
        for (Post post : allPosts) {
            addMarker(post);
        }
        
        Log.d(TAG, "Displayed " + allPosts.size() + " markers");
    }
    
    /**
     * Thêm marker cho 1 bài đăng với ảnh thumbnail
     * 
     * @param post Bài đăng cần thêm marker
     */
    private void addMarker(Post post) {
        if (post.getLat() == null || post.getLng() == null) return;
        
        // Nếu có ảnh, load ảnh và tạo marker với ảnh
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            loadImageMarker(post);
        } else {
            // Không có ảnh, dùng icon mặc định
            addDefaultMarker(post);
        }
    }
    
    /**
     * Load ảnh từ URL và tạo marker với ảnh
     */
    private void loadImageMarker(Post post) {
        // Chọn màu viền dựa trên loại
        int borderColor = "lost".equals(post.getType()) 
                ? 0xFFFF3B30  // Đỏ
                : 0xFF34C759; // Xanh
        
        // Load ảnh bằng Glide
        Glide.with(this)
                .asBitmap()
                .load(post.getImageUrl())
                .circleCrop()
                .into(new CustomTarget<Bitmap>(120, 120) {
                    @Override
                    public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                        // Tạo marker với ảnh và viền màu
                        Bitmap markerBitmap = createImageMarker(bitmap, borderColor, post.getType());
                        addMarkerToMap(post, markerBitmap);
                    }
                    
                    @Override
                    public void onLoadCleared(Drawable placeholder) {
                        // Fallback to default marker
                        addDefaultMarker(post);
                    }
                    
                    @Override
                    public void onLoadFailed(Drawable errorDrawable) {
                        // Fallback to default marker
                        addDefaultMarker(post);
                    }
                });
    }
    
    /**
     * Tạo bitmap marker với ảnh và viền màu
     * Giống app "Tìm" trên iPhone
     */
    private Bitmap createImageMarker(Bitmap image, int borderColor, String type) {
        int size = 120; // Kích thước marker
        int borderWidth = 8; // Độ dày viền
        
        Bitmap output = Bitmap.createBitmap(size, size + 40, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        
        // 1. Vẽ viền màu (circle lớn)
        Paint borderPaint = new Paint();
        borderPaint.setAntiAlias(true);
        borderPaint.setColor(borderColor);
        borderPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, borderPaint);
        
        // 2. Vẽ viền trắng (circle trung bình)
        Paint whitePaint = new Paint();
        whitePaint.setAntiAlias(true);
        whitePaint.setColor(0xFFFFFFFF);
        whitePaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - borderWidth, whitePaint);
        
        // 3. Vẽ ảnh (circle nhỏ)
        Paint imagePaint = new Paint();
        imagePaint.setAntiAlias(true);
        
        // Tạo circular clip
        int imageSize = size - borderWidth * 2 - 4;
        Bitmap circularImage = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888);
        Canvas imageCanvas = new Canvas(circularImage);
        
        Paint clipPaint = new Paint();
        clipPaint.setAntiAlias(true);
        imageCanvas.drawCircle(imageSize / 2f, imageSize / 2f, imageSize / 2f, clipPaint);
        clipPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        
        // Scale và vẽ ảnh
        Rect srcRect = new Rect(0, 0, image.getWidth(), image.getHeight());
        Rect dstRect = new Rect(0, 0, imageSize, imageSize);
        imageCanvas.drawBitmap(image, srcRect, dstRect, clipPaint);
        
        // Vẽ circular image lên canvas chính
        canvas.drawBitmap(circularImage, borderWidth + 2, borderWidth + 2, imagePaint);
        
        // 4. Vẽ pin (pointer) ở dưới
        Paint pinPaint = new Paint();
        pinPaint.setAntiAlias(true);
        pinPaint.setColor(borderColor);
        pinPaint.setStyle(Paint.Style.FILL);
        
        // Triangle path
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(size / 2f, size + 35); // Bottom point
        path.lineTo(size / 2f - 15, size); // Left point
        path.lineTo(size / 2f + 15, size); // Right point
        path.close();
        canvas.drawPath(path, pinPaint);
        
        // 5. Draw corner icon (? for lost, checkmark for found)
        drawCornerIcon(canvas, type, size, borderColor);
        
        return output;
    }
    
    /**
     * Draw small icon at marker corner (? for lost, checkmark for found)
     */
    private void drawCornerIcon(Canvas canvas, String type, int size, int color) {
        int iconSize = 32;
        int iconX = size - iconSize - 4;
        int iconY = size - iconSize - 4;
        
        // Vẽ background trắng
        Paint bgPaint = new Paint();
        bgPaint.setAntiAlias(true);
        bgPaint.setColor(0xFFFFFFFF);
        bgPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(iconX + iconSize / 2f, iconY + iconSize / 2f, iconSize / 2f, bgPaint);
        
        // Vẽ icon
        Paint iconPaint = new Paint();
        iconPaint.setAntiAlias(true);
        iconPaint.setColor(color);
        iconPaint.setStyle(Paint.Style.FILL);
        iconPaint.setTextSize(20);
        iconPaint.setTextAlign(Paint.Align.CENTER);
        iconPaint.setFakeBoldText(true);
        
        String icon = "lost".equals(type) ? "?" : "✓";
        canvas.drawText(icon, iconX + iconSize / 2f, iconY + iconSize / 2f + 7, iconPaint);
    }
    
    /**
     * Thêm marker mặc định (không có ảnh)
     */
    private void addDefaultMarker(Post post) {
        int iconRes = "lost".equals(post.getType()) 
                ? R.drawable.ic_marker_lost 
                : R.drawable.ic_marker_found;
        
        Bitmap iconBitmap = drawableToBitmap(ContextCompat.getDrawable(this, iconRes));
        if (iconBitmap != null) {
            addMarkerToMap(post, iconBitmap);
        }
    }
    
    /**
     * Thêm marker lên bản đồ
     */
    private void addMarkerToMap(Post post, Bitmap iconBitmap) {
        if (pointAnnotationManager == null || iconBitmap == null) return;
        
        Point point = Point.fromLngLat(post.getLng(), post.getLat());
        PointAnnotationOptions pointAnnotationOptions = new PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(iconBitmap);
        
        pointAnnotationManager.create(pointAnnotationOptions);
    }
    
    /**
     * Convert Drawable to Bitmap
     */
    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) return null;
        
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888
        );
        
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        
        return bitmap;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        bottomNav.setSelectedItemId(R.id.nav_map);
    }
}
