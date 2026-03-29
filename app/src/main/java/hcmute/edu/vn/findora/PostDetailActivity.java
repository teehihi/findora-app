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
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
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
            tvDetailType.setText("LOST");
            tvDetailType.getBackground().setTint(ContextCompat.getColor(this, R.color.badge_lost_bg));
        } else {
            tvDetailType.setText("FOUND");
            tvDetailType.getBackground().setTint(ContextCompat.getColor(this, R.color.badge_found_bg));
        }

        // Format times
        if (timestamp > 0) {
            Date date = new Date(timestamp);
            
            // "Posted 30 minutes ago" style (using simple format for now)
            SimpleDateFormat relativeSdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            tvDetailTimeHeader.setText("Posted at " + relativeSdf.format(date));
            
            // "Today, 4:00 PM" style
            SimpleDateFormat infoSdf = new SimpleDateFormat("E, h:mm a", Locale.getDefault());
            tvInfoTime.setText(infoSdf.format(date));
        } else {
            tvDetailTimeHeader.setText("Posted recently");
            tvInfoTime.setText("N/A");
        }

        // Placeholders as requested
        tvDetailLocation.setText("Ben Thanh Market Area"); 
        tvInfoCategory.setText("Personal Item");
        tvUserStatus.setText("Active recently • Verified User");

        // Placeholder image
        ivDetailImage.setImageResource(R.drawable.bg_img_placeholder);
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
