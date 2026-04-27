package hcmute.edu.vn.findora;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.findora.adapter.MyPostsAdapter;
import hcmute.edu.vn.findora.model.Post;

public class MyPostsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MyPostsAdapter myPostsAdapter;
    private List<Post> allPosts;       // Chứa toàn bộ bài đăng kéo về
    private List<Post> displayedPosts; // Danh sách bài đăng hiện đang được filter (Theo tab)
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TextView tvEmptyState;
    private ImageButton btnBack;

    // Tabs
    private TextView btnTabLost, btnTabFound;
    private String currentFilter = "lost"; // Mặc định mở tab Lost

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_posts);

        // Edge-to-edge support cho Toolbar top padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.myPostsToolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top + dpToPx(16), v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.recyclerView);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnBack = findViewById(R.id.btnBack);
        btnTabLost = findViewById(R.id.btnTabLost);
        btnTabFound = findViewById(R.id.btnTabFound);

        btnBack.setOnClickListener(v -> finish());

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        allPosts = new ArrayList<>();
        displayedPosts = new ArrayList<>();
        myPostsAdapter = new MyPostsAdapter(this, displayedPosts);
        recyclerView.setAdapter(myPostsAdapter);

        // Setup Tabs
        setupTabClickListeners();
    }

    private void setupTabClickListeners() {
        btnTabLost.setOnClickListener(v -> {
            if (!currentFilter.equals("lost")) {
                currentFilter = "lost";
                updateTabStyles();
                filterPosts();
            }
        });

        btnTabFound.setOnClickListener(v -> {
            if (!currentFilter.equals("found")) {
                currentFilter = "found";
                updateTabStyles();
                filterPosts();
            }
        });
    }

    private void updateTabStyles() {
        if ("lost".equals(currentFilter)) {
            // Lost is Active
            btnTabLost.setBackgroundResource(R.drawable.bg_tab_selected);
            btnTabLost.setTextColor(getResources().getColor(R.color.white));
            // Found is Inactive
            btnTabFound.setBackgroundResource(R.drawable.bg_tab_unselected);
            btnTabFound.setTextColor(android.graphics.Color.parseColor("#5F6368"));
        } else {
            // Found is Active
            btnTabFound.setBackgroundResource(R.drawable.bg_tab_selected);
            btnTabFound.setTextColor(getResources().getColor(R.color.white));
            // Lost is Inactive
            btnTabLost.setBackgroundResource(R.drawable.bg_tab_unselected);
            btnTabLost.setTextColor(android.graphics.Color.parseColor("#5F6368"));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyPosts(); // Refresh whenever screen becomes active (e.g., return from edit)
    }

    private void loadMyPosts() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Bạn cần đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("posts")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allPosts.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Post post = doc.toObject(Post.class);
                        if (post != null) {
                            post.setId(doc.getId());
                            allPosts.add(post);
                        }
                    }
                    
                    // Priority sort: latest first
                    allPosts.sort((p1, p2) -> {
                        if (p1.getCreatedAt() == null) return 1;
                        if (p2.getCreatedAt() == null) return -1;
                        return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                    });
                    
                    filterPosts(); // Fill displayedPosts based on current tab
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("MyPostsActivity", "Error loading posts", e);
                });
    }

    private void filterPosts() {
        displayedPosts.clear();
        for (Post post : allPosts) {
            if (currentFilter.equals(post.getType())) {
                displayedPosts.add(post);
            }
        }

        myPostsAdapter.notifyDataSetChanged();

        if (displayedPosts.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}
