package hcmute.edu.vn.findora;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
    private android.widget.ImageView ivAvatar;
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
        ivAvatar = findViewById(R.id.ivAvatar);
        bottomNav = findViewById(R.id.bottomNav);
        fabCreatePost = findViewById(R.id.fabCreatePost);

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
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
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
                        tvUserEmail.setText(email != null ? email : "");

                        if (photoUrl != null && !photoUrl.isEmpty() && ivAvatar != null) {
                            com.bumptech.glide.Glide.with(this)
                                    .load(photoUrl)
                                    .transform(new com.bumptech.glide.load.resource.bitmap.CircleCrop())
                                    .into(ivAvatar);
                        } else if (ivAvatar != null) {
                            // Clear
                            ivAvatar.setImageResource(R.drawable.ic_person);
                        }
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
