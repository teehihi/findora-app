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

/**
 * Màn hình tạo bài đăng mới - Stitch Design 2024.
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

    // State
    private String selectedType = "lost";
    private Uri selectedImageUri = null;
    private String editPostId = null;
    private String existingImageUrl = null;

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
        
        cvImageUpload.setOnClickListener(v -> pickImage.launch("image/*"));

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
                }
            }
    );

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
                        Toast.makeText(this, "Posted successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Đăng bài");
                    });
        }
    }
}
