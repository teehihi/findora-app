package hcmute.edu.vn.findora;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextView tvUserName, tvUserEmail, tvActivePosts, tvItemsReturned;
    private BottomNavigationView bottomNav;
    private FloatingActionButton fabCreatePost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvActivePosts = findViewById(R.id.tvActivePosts);
        tvItemsReturned = findViewById(R.id.tvItemsReturned);
        bottomNav = findViewById(R.id.bottomNav);
        fabCreatePost = findViewById(R.id.fabCreatePost);

        loadUserInfo();
        loadUserStats();

        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

        // FAB
        fabCreatePost.setOnClickListener(v -> {
            startActivity(new Intent(this, CreatePostActivity.class));
        });

        // Bottom nav
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
        });
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
                        
                        tvUserName.setText(fullName != null ? fullName : "User");
                        tvUserEmail.setText(email != null ? email : "");
                    } else {
                        // Fallback to Auth if Firestore doc doesn't exist
                        tvUserName.setText(user.getDisplayName() != null ? user.getDisplayName() : "User");
                        tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileActivity", "Error loading user info", e);
                    // Fallback to Auth
                    tvUserName.setText(user.getDisplayName() != null ? user.getDisplayName() : "User");
                    tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "");
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
}
