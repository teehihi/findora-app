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

    private ImageView ivDetailImage;
    private TextView tvDetailType, tvDetailTitle, tvDetailTimeHeader, tvDetailDescription, tvDetailLocation, tvUserName, tvUserStatus;
    private TextView tvInfoTime, tvInfoCategory;
    private ImageButton btnBack;
    private com.google.firebase.firestore.FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        // Edge-to-edge support 
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom); // Trả lại top padding để chừa thanh Status bar ra
            return insets;
        });

        // Xoá bỏ khoản đẩy thêm của toolbar vì main đã đẩy rồi
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detailToolbar), (v, insets) -> {
            v.setPadding(v.getPaddingLeft(), dpToPx(16), v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        initViews();
        displayData();

        btnBack.setOnClickListener(v -> finish());
    }

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
            tvDetailType.setBackgroundResource(R.drawable.bg_badge_found); // assuming safe fallback or tint over light blue
            tvDetailType.setTextColor(android.graphics.Color.parseColor("#1976D2"));
        }

        // Format times
        if (timestamp > 0) {
            Date date = new Date(timestamp);
            
            // "Đăng lúc 14:30" style
            SimpleDateFormat relativeSdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            tvDetailTimeHeader.setText("Đăng lúc " + relativeSdf.format(date));
            
            // "14:30, 20/05/2024" style
            SimpleDateFormat infoSdf = new SimpleDateFormat("HH:mm, dd/MM/yyyy", new Locale("vi", "VN"));
            tvInfoTime.setText(infoSdf.format(date));
        } else {
            tvDetailTimeHeader.setText("Vừa đăng");
            tvInfoTime.setText("N/A");
        }

        // Placeholders as requested
        tvDetailLocation.setText("Đang cập nhật (Sắp ra mắt)"); 
        tvInfoCategory.setText("Đồ cá nhân");
        tvUserStatus.setText("Vừa truy cập • Đã xác thực");

        // Load image using Glide if available
        String imageUrl = getIntent().getStringExtra("imageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.bg_img_placeholder) // While loading
                .error(R.drawable.recent_activity_bg)       // If URL is broken
                .centerCrop()
                .into(ivDetailImage);
        } else {
            // Nếu bài đăng không có ảnh, hiển thị 1 ảnh đại diện đẹp hơn thay vì khung xám
            ivDetailImage.setImageResource(R.drawable.recent_activity_bg);
        }
        
        setupOwnerActions(getIntent().getExtras(), title, description, type, imageUrl, userId);
    }

    private void setupOwnerActions(Bundle extras, String title, String description, String type, String imageUrl, String postUserId) {
        TextView btnContact = findViewById(R.id.btnContact);
        TextView btnChat = findViewById(R.id.btnChat);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        
        if (auth.getCurrentUser() != null && auth.getCurrentUser().getUid().equals(postUserId)) {
            // Hiển thị nút sửa
            btnChat.setText("Chỉnh sửa");
            // Set drawable to settings gear icon as edit icon might not exist
            btnChat.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_edit_ios, 0, 0, 0);
            btnChat.setOnClickListener(v -> {
                Intent editIntent = new Intent(this, CreatePostActivity.class);
                editIntent.putExtra("editPostId", extras.getString("postId", ""));
                editIntent.putExtra("title", title);
                editIntent.putExtra("description", description);
                editIntent.putExtra("type", type);
                editIntent.putExtra("imageUrl", imageUrl);
                startActivity(editIntent);
                finish(); // Close detail view
            });

            // Hiển thị nút xóa
            btnContact.setText("Xóa bài");
            // Do NOT overwrite background here to keep the outline style intact
            int iosRed = android.graphics.Color.parseColor("#FF3B30");
            btnContact.setTextColor(iosRed);
            
            android.graphics.drawable.Drawable trashIcon = ContextCompat.getDrawable(this, R.drawable.ic_trash_ios);
            if (trashIcon != null) {
                // Remove xml tint and force the iOS Red color explicitly on the drawable
                androidx.core.graphics.drawable.DrawableCompat.setTint(
                        androidx.core.graphics.drawable.DrawableCompat.wrap(trashIcon).mutate(), iosRed);
                btnContact.setCompoundDrawablesWithIntrinsicBounds(trashIcon, null, null, null);
                // Also remove the built-in app:drawableTint set in xml
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
                        .addOnFailureListener(e -> Toast.makeText(this, "Lỗi dọn dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
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
                } else {
                    tvUserName.setText("User Not Found");
                }
            })
            .addOnFailureListener(e -> {
                tvUserName.setText("Error loading name");
            });
    }
}
