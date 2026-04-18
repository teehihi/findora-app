package hcmute.edu.vn.findora;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import android.net.Uri;
import android.provider.MediaStore;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.cardview.widget.CardView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.os.Build;

import hcmute.edu.vn.findora.ml.ImageClassifier;
import hcmute.edu.vn.findora.model.Post;
import hcmute.edu.vn.findora.utils.NotificationHelper;

import java.util.ArrayList;
import java.util.List;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * Màn hình tạo bài đăng mới
 */
public class CreatePostActivity extends AppCompatActivity {

    // UI - Navigation
    private ImageButton btnBack;

    // UI - Type toggle (Stitch Modern Toggle)
    private LinearLayout btnLost, btnFound;
    private ImageView    iconLost, iconFound;
    private TextView     tvLost, tvFound;

    // UI - Inputs
    private EditText etTitle, etDescription;
    private android.widget.Button btnSubmit;
    private android.widget.Button btnSelectLocation;
    private TextView tvSelectedAddress;
    private com.mapbox.maps.MapView mapViewPreview;
    private androidx.cardview.widget.CardView cvMapPreview;
    private TextView tvAiAssisted;

    // State
    private String selectedType = "lost";
    private Uri selectedImageUri = null;
    private String editPostId = null;
    private String existingImageUrl = null;
    
    // Location data
    private Double selectedLat = null;
    private Double selectedLng = null;
    private String selectedAddress = null;
    
    // AI state
    private String predictedLabel = null;
    private Double predictedConfidence = null;
    private ImageClassifier imageClassifier;
    private ExecutorService executorService;
    
    private static final int MAP_REQUEST_CODE = 1002;

    // Firebase
    private FirebaseAuth      auth;
    private FirebaseFirestore db;
    private FirebaseStorage   storage;

    // UI - Upload
    private CardView cvImageUpload;
    private ImageView ivImagePreview;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.createPostRoot), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        btnBack      = findViewById(R.id.btnBack);
        btnLost      = findViewById(R.id.btnLost);
        btnFound     = findViewById(R.id.btnFound);
        iconLost     = findViewById(R.id.iconLost);
        iconFound    = findViewById(R.id.iconFound);
        tvLost       = findViewById(R.id.tvLost);
        tvFound      = findViewById(R.id.tvFound);
        etTitle      = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        btnSubmit    = findViewById(R.id.btnSubmit);
        cvImageUpload = findViewById(R.id.cvImageUpload);
        ivImagePreview = findViewById(R.id.ivImagePreview);
        btnSelectLocation = findViewById(R.id.btnSelectLocation);
        tvSelectedAddress = findViewById(R.id.tvSelectedAddress);
        mapViewPreview = findViewById(R.id.mapViewPreview);
        cvMapPreview = findViewById(R.id.cvMapPreview);
        tvAiAssisted = findViewById(R.id.tvAiAssisted);
        
        executorService = Executors.newSingleThreadExecutor();
        com.google.android.gms.tflite.java.TfLite.initialize(this)
            .addOnSuccessListener(aVoid -> {
                try {
                    imageClassifier = new ImageClassifier(CreatePostActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(CreatePostActivity.this, "Failed to load ML model", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(CreatePostActivity.this, "Failed to connect to ML Service", Toast.LENGTH_SHORT).show();
            });
        
        // Initialize map preview
        initializeMapPreview();
        
        cvImageUpload.setOnClickListener(v -> pickImage.launch("image/*"));
        btnSelectLocation.setOnClickListener(v -> openMapActivity());

        btnBack.setOnClickListener(v -> finish());
        
        // Handle Edit Mode
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("editPostId")) {
            editPostId = extras.getString("editPostId");
            etTitle.setText(extras.getString("title", ""));
            etDescription.setText(extras.getString("description", ""));
            setTypeSelected(extras.getString("type", "lost"));
            btnSubmit.setText("Cập nhật bài đăng");

            existingImageUrl = extras.getString("imageUrl", "");
            if (existingImageUrl != null && !existingImageUrl.isEmpty()) {
                com.bumptech.glide.Glide.with(this)
                        .load(existingImageUrl)
                        .centerCrop()
                        .into(ivImagePreview);
                ivImagePreview.setImageTintList(null);
            }
            
            // Load existing location if available
            if (extras.containsKey("lat") && extras.containsKey("lng")) {
                selectedLat = extras.getDouble("lat");
                selectedLng = extras.getDouble("lng");
                selectedAddress = extras.getString("address", "");
                if (selectedAddress != null && !selectedAddress.isEmpty()) {
                    tvSelectedAddress.setText(selectedAddress);
                    tvSelectedAddress.setVisibility(View.VISIBLE);
                }
                // Update map preview
                if (selectedLat != 0 && selectedLng != 0) {
                    updateMapPreview(selectedLat, selectedLng);
                }
            }
        } else {
            // Default state
            setTypeSelected("lost");
        }
        
        btnLost.setOnClickListener(v  -> setTypeSelected("lost"));
        btnFound.setOnClickListener(v -> setTypeSelected("found"));
        btnSubmit.setOnClickListener(v -> submitPost());
    }
    
    // ActivityResultLauncher for picking image
    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivImagePreview.setImageURI(selectedImageUri);
                    ivImagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ivImagePreview.setImageTintList(null); // Remove tint to show original image colors
                    
                    // Run AI classification
                    classifyImage(uri);
                }
            }
    );
    
    private void classifyImage(Uri uri) {
        if (imageClassifier == null) return;
        
        tvAiAssisted.setText("Analyzing image...");
        executorService.execute(() -> {
            try {
                Bitmap bitmap;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri));
                } else {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                }
                
                // Convert to ARGB_8888 if needed (TensorImage requires it)
                Bitmap softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                
                ImageClassifier.Result result = imageClassifier.classify(softwareBitmap);
                
                runOnUiThread(() -> {
                    if (result != null && result.confidence >= 0.5f) { // 50% threshold
                        predictedLabel = result.label;
                        predictedConfidence = (double) result.confidence;
                        int percentage = (int) (result.confidence * 100);
                        tvAiAssisted.setText("Detected: " + predictedLabel + " (" + percentage + "%)");
                    } else {
                        predictedLabel = null;
                        predictedConfidence = null;
                        tvAiAssisted.setText(R.string.post_ai_assisted);
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> tvAiAssisted.setText(R.string.post_ai_assisted));
            }
        });
    }

    /**
     * Cập nhật UI toggle button (Lost / Found).
     * On: bg_toggle_on (Blue outline) + Blue text
     * Off: bg_toggle_off (Gray bg) + Gray text
     */
    private void setTypeSelected(String type) {
        selectedType = type;

        int activeColor = ContextCompat.getColor(this, R.color.primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.outline);

        if ("lost".equals(type)) {
            // Lost is active
            btnLost.setBackgroundResource(R.drawable.bg_toggle_on);
            tvLost.setTextColor(activeColor);
            iconLost.setColorFilter(activeColor);

            btnFound.setBackgroundResource(R.drawable.bg_toggle_off);
            tvFound.setTextColor(inactiveColor);
            iconFound.setColorFilter(inactiveColor);
        } else {
            // Found is active
            btnFound.setBackgroundResource(R.drawable.bg_toggle_on);
            tvFound.setTextColor(activeColor);
            iconFound.setColorFilter(activeColor);

            btnLost.setBackgroundResource(R.drawable.bg_toggle_off);
            tvLost.setTextColor(inactiveColor);
            iconLost.setColorFilter(inactiveColor);
        }
    }

    private void submitPost() {
        String title       = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Item Name required");
            etTitle.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(description)) {
            etDescription.setError("Description required");
            etDescription.requestFocus();
            return;
        }

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> postData = new HashMap<>();
        postData.put("title",       title);
        postData.put("description", description);
        postData.put("type",        selectedType);
        postData.put("userId",      auth.getCurrentUser().getUid());
        postData.put("createdAt",   Timestamp.now());
        
        // Add location data if available
        if (selectedLat != null && selectedLng != null) {
            postData.put("lat", selectedLat);
            postData.put("lng", selectedLng);
            if (selectedAddress != null && !selectedAddress.isEmpty()) {
                postData.put("address", selectedAddress);
            }
        }
        
        // Add AI fields if confident
        if (predictedLabel != null && predictedConfidence != null) {
            postData.put("imageLabel", predictedLabel);
            postData.put("confidence", predictedConfidence);
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang xử lý...");

        if (selectedImageUri != null) {
            String filename = UUID.randomUUID().toString() + ".jpg";
            StorageReference storageRef = storage.getReference()
                    .child("images/" + auth.getCurrentUser().getUid() + "/" + filename);

            storageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        postData.put("imageUrl", uri.toString());
                        savePostToFirestore(postData);
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Image Upload Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText(editPostId != null ? "Cập nhật bài đăng" : "Đăng bài");
                    });
        } else {
            if (existingImageUrl != null && !existingImageUrl.isEmpty()) {
                postData.put("imageUrl", existingImageUrl); // keep old image if no new one
            }
            savePostToFirestore(postData);
        }
    }

    private void savePostToFirestore(Map<String, Object> postData) {
        if (editPostId != null && !editPostId.isEmpty()) {
            // Update
            // Bỏ createdAt vì đang cập nhật (hoặc giữ nguyên cũng được do Firestore merge)
            postData.remove("createdAt");
            db.collection("posts").document(editPostId)
                    .update(postData)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(this, "Đã cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Cập nhật bài đăng");
                    });
        } else {
            // Add new
            db.collection("posts")
                    .add(postData)
                    .addOnSuccessListener(ref -> {
                        String newPostId = ref.getId();
                        Toast.makeText(this, "Posted successfully!", Toast.LENGTH_SHORT).show();
                        
                        // Tự động tìm matches và gửi thông báo AI
                        findMatchesForNewPost(newPostId, postData);
                        
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Đăng bài");
                    });
        }
    }
    
    /**
     * Tự động tìm matches cho bài đăng mới và gửi thông báo
     * 
     * CHỨC NĂNG:
     * - Load tất cả bài đăng từ Firestore
     * - Sử dụng AIMatchingHelper để tìm matches
     * - Gửi thông báo cho users có bài match >= 70%
     * 
     * @param newPostId ID của bài đăng mới
     * @param postData Dữ liệu bài đăng mới
     */
    private void findMatchesForNewPost(String newPostId, Map<String, Object> postData) {
        // Tạo Post object từ postData
        Post newPost = new Post();
        newPost.setId(newPostId);
        newPost.setTitle((String) postData.get("title"));
        newPost.setDescription((String) postData.get("description"));
        newPost.setType((String) postData.get("type"));
        newPost.setUserId((String) postData.get("userId"));
        newPost.setCreatedAt((Timestamp) postData.get("createdAt"));
        
        if (postData.containsKey("lat") && postData.containsKey("lng")) {
            newPost.setLat((Double) postData.get("lat"));
            newPost.setLng((Double) postData.get("lng"));
        }
        if (postData.containsKey("address")) {
            newPost.setAddress((String) postData.get("address"));
        }
        if (postData.containsKey("imageLabel")) {
            newPost.setImageLabel((String) postData.get("imageLabel"));
        }
        
        // Load tất cả bài đăng
        db.collection("posts").get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Post> allPosts = new ArrayList<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Post p = doc.toObject(Post.class);
                    p.setId(doc.getId());
                    allPosts.add(p);
                }
                
                // Tìm matches
                List<AIMatchingHelper.MatchResult> matches = 
                    AIMatchingHelper.findMatches(newPost, allPosts);
                
                // Gửi thông báo cho các user có bài match >= 70%
                for (AIMatchingHelper.MatchResult match : matches) {
                    if (match.getScorePercentage() >= 70) {
                        // Gửi thông báo AI match
                        NotificationHelper.sendAIMatchNotification(
                            match.post.getUserId(),
                            newPostId,
                            newPost.getTitle(),
                            match.getScorePercentage()
                        );
                        
                        android.util.Log.d("CreatePost", String.format(
                            "Sent AI match notification to user %s: %s (%d%%)",
                            match.post.getUserId(),
                            match.post.getTitle(),
                            match.getScorePercentage()
                        ));
                    }
                }
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("CreatePost", "Failed to find matches", e);
            });
    }
    
    private void openMapActivity() {
        Intent intent = new Intent(this, AddressPickerActivity.class);
        startActivityForResult(intent, MAP_REQUEST_CODE);
    }
    
    private void initializeMapPreview() {
        // Mapbox will be initialized when displaying location
    }
    
    private void updateMapPreview(double lat, double lng) {
        // Show map preview
        cvMapPreview.setVisibility(View.VISIBLE);
        
        // Load Mapbox style and setup map
        mapViewPreview.getMapboxMap().loadStyleUri(com.mapbox.maps.Style.MAPBOX_STREETS, style -> {
            // Move camera to location
            com.mapbox.geojson.Point point = com.mapbox.geojson.Point.fromLngLat(lng, lat);
            com.mapbox.maps.CameraOptions cameraOptions = new com.mapbox.maps.CameraOptions.Builder()
                    .center(point)
                    .zoom(15.0)
                    .build();
            mapViewPreview.getMapboxMap().setCamera(cameraOptions);
            
            // Add marker using helper
            MapboxHelper.addMarker(mapViewPreview, lat, lng);
        });
        
        // Make map preview clickable to edit location
        cvMapPreview.setOnClickListener(v -> openMapActivity());
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == MAP_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            selectedLat = data.getDoubleExtra("latitude", 0);
            selectedLng = data.getDoubleExtra("longitude", 0);
            selectedAddress = data.getStringExtra("address");
            
            if (selectedAddress != null && !selectedAddress.isEmpty()) {
                tvSelectedAddress.setText(selectedAddress);
                tvSelectedAddress.setVisibility(View.VISIBLE);
            }
            
            // Update map preview
            if (selectedLat != 0 && selectedLng != 0) {
                updateMapPreview(selectedLat, selectedLng);
            }
        }
    }
    
    // Mapbox v10 doesn't need onResume/onPause lifecycle methods
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imageClassifier != null) {
            imageClassifier.close();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
