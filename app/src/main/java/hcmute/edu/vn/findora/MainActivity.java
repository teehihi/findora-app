package hcmute.edu.vn.findora;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.findora.adapter.PostAdapter;
import hcmute.edu.vn.findora.model.Post;

/**
 * Màn hình chính - theo Stitch Design 2024.
 */
public class MainActivity extends AppCompatActivity {

    // UI
    private RecyclerView         recyclerView;
    private ProgressBar          progressBar;
    private LinearLayout         layoutEmpty;
    private BottomNavigationView bottomNav;
    private FloatingActionButton fabCreatePost;
    private TextView             tvHeading;

    // Filter chips
    private TextView chipAll, chipLost, chipFound;
    private String currentFilter = "all";

    // Adapter & data
    private PostAdapter adapter;
    private List<Post>  postList;
    private List<Post>  allPosts;

    // Firebase
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Map padding: Top should apply to root. Bottom should NOT apply to root (so BottomNav stays at bottom).
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);

            // Give the BottomNav internal padding for the gesture bar handle
            bottomNav.setPadding(0, 0, 0, systemBars.bottom);
            // Ensure the main scrollable area doesn't get hidden behind the BottomNav
            findViewById(R.id.recyclerView).setPadding(
                findViewById(R.id.recyclerView).getPaddingLeft(),
                findViewById(R.id.recyclerView).getPaddingTop(),
                findViewById(R.id.recyclerView).getPaddingRight(),
                (int) (90 * getResources().getDisplayMetrics().density) // Height of bottom nav area
            );
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        recyclerView  = findViewById(R.id.recyclerView);
        progressBar   = findViewById(R.id.progressBar);
        layoutEmpty   = findViewById(R.id.layoutEmpty);
        bottomNav     = findViewById(R.id.bottomNav);
        fabCreatePost = findViewById(R.id.fabCreatePost);
        tvHeading     = findViewById(R.id.tvHeading);
        chipAll       = findViewById(R.id.chipAll);
        chipLost      = findViewById(R.id.chipLost);
        chipFound     = findViewById(R.id.chipFound);

        // Heading: "What are we finding today?" -> "finding" is blue italic
        setStylizedHeading();

        allPosts = new ArrayList<>();
        postList = new ArrayList<>();
        adapter  = new PostAdapter(this, postList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
            // Other tabs (Search, Chat) handled here eventually
            return false;
        });

        // Adjust bottom nav icon positions to align with FAB
        bottomNav.post(() -> {
            try {
                // Get the BottomNavigationMenuView
                View menuView = bottomNav.getChildAt(0);
                if (menuView instanceof android.view.ViewGroup) {
                    android.view.ViewGroup menuViewGroup = (android.view.ViewGroup) menuView;
                    // Iterate through each menu item view
                    for (int i = 0; i < menuViewGroup.getChildCount(); i++) {
                        View itemView = menuViewGroup.getChildAt(i);
                        // Add top padding to push icons down
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

        fabCreatePost.setOnClickListener(v -> {
            startActivity(new Intent(this, CreatePostActivity.class));
        });

        fabCreatePost.setOnClickListener(v -> {
            startActivity(new Intent(this, CreatePostActivity.class));
        });

        chipAll.setOnClickListener(v   -> applyFilter("all"));
        chipLost.setOnClickListener(v  -> applyFilter("lost"));
        chipFound.setOnClickListener(v -> applyFilter("found"));

        loadPosts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set bottom nav to home when returning to this activity
        bottomNav.setSelectedItemId(R.id.nav_home);
    }

    private void setStylizedHeading() {
        String fullText = getString(R.string.home_heading);
        SpannableString spannable = new SpannableString(fullText);
        
        String findingWord = getString(R.string.home_heading_finding);
        int start = fullText.indexOf(findingWord);
        int end = start + findingWord.length();
        
        if (start != -1) {
            // Blue color
            spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.primary)), 
                    start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            // Bold Italic
            spannable.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), 
                    start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tvHeading.setText(spannable);
    }

    private void loadPosts() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);

        db.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (error != null || snapshots == null) {
                        showEmptyOrList();
                        return;
                    }

                    allPosts.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Post post = doc.toObject(Post.class);
                        post.setId(doc.getId());
                        allPosts.add(post);
                    }
                    applyFilter(currentFilter);
                });
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        updateChipUI(filter);

        postList.clear();
        for (Post post : allPosts) {
            if ("all".equals(filter) || filter.equals(post.getType())) {
                postList.add(post);
            }
        }
        adapter.notifyDataSetChanged();
        showEmptyOrList();
    }

    private void updateChipUI(String activeFilter) {
        // Reset all to Inactive (Blue Outline)
        resetChip(chipAll, getString(R.string.chip_all));
        resetChip(chipLost, getString(R.string.chip_lost));
        resetChip(chipFound, getString(R.string.chip_found));

        // Set Active (Solid Blue)
        TextView activeChip;
        switch (activeFilter) {
            case "lost":  activeChip = chipLost;  break;
            case "found": activeChip = chipFound; break;
            default:      activeChip = chipAll;   break;
        }
        activeChip.setBackgroundResource(R.drawable.bg_chip_active);
        activeChip.setTextColor(ContextCompat.getColor(this, R.color.white));
    }

    private void resetChip(TextView chip, String text) {
        chip.setBackgroundResource(R.drawable.bg_chip_inactive);
        chip.setTextColor(ContextCompat.getColor(this, R.color.primary));
        chip.setText(text);
    }

    private void showEmptyOrList() {
        if (postList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }
}