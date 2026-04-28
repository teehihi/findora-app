package hcmute.edu.vn.findora;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.findora.adapter.LeaderboardAdapter;
import hcmute.edu.vn.findora.model.User;

/**
 * Màn hình Bảng xếp hạng - Hiển thị Top người dùng có nhiều điểm nhất.
 */
public class LeaderboardActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tabWeek, tabMonth, tabAllTime;
    private RecyclerView rvLeaderboard;
    private LeaderboardAdapter adapter;
    private List<User> userList;
    private FirebaseFirestore db;
    private String currentSortType = "points"; // "points", "rating", "totalReturned"

    // Podium views
    private ImageView imgRank1, imgRank2, imgRank3;
    private TextView txtNameRank1, txtNameRank2, txtNameRank3;
    private TextView txtPointsRank1, txtPointsRank2, txtPointsRank3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        db = FirebaseFirestore.getInstance();
        initViews();
        setupRecyclerView();
        loadLeaderboardData("points");

        btnBack.setOnClickListener(v -> finish());
        
        tabWeek.setOnClickListener(v -> switchTab("points"));
        tabMonth.setOnClickListener(v -> switchTab("rating"));
        tabAllTime.setOnClickListener(v -> switchTab("totalReturned"));
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tabWeek = findViewById(R.id.tabWeek);
        tabMonth = findViewById(R.id.tabMonth);
        tabAllTime = findViewById(R.id.tabAllTime);
        rvLeaderboard = findViewById(R.id.rvLeaderboard);

        imgRank1 = findViewById(R.id.imgRank1);
        imgRank2 = findViewById(R.id.imgRank2);
        imgRank3 = findViewById(R.id.imgRank3);

        txtNameRank1 = findViewById(R.id.txtNameRank1);
        txtNameRank2 = findViewById(R.id.txtNameRank2);
        txtNameRank3 = findViewById(R.id.txtNameRank3);

        txtPointsRank1 = findViewById(R.id.txtPointsRank1);
        txtPointsRank2 = findViewById(R.id.txtPointsRank2);
        txtPointsRank3 = findViewById(R.id.txtPointsRank3);
    }

    private void setupRecyclerView() {
        userList = new ArrayList<>();
        adapter = new LeaderboardAdapter(this, userList);
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        rvLeaderboard.setAdapter(adapter);
    }

    private void switchTab(String type) {
        currentSortType = type;
        
        // Update UI Tabs
        tabWeek.setBackground(null);
        tabMonth.setBackground(null);
        tabAllTime.setBackground(null);
        tabWeek.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tabMonth.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tabAllTime.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

        TextView selectedTab = tabWeek;
        if ("rating".equals(type)) selectedTab = tabMonth;
        else if ("totalReturned".equals(type)) selectedTab = tabAllTime;

        selectedTab.setBackgroundResource(R.drawable.bg_tab_selected);
        selectedTab.setTextColor(ContextCompat.getColor(this, R.color.on_primary));

        loadLeaderboardData(type);
    }

    private void loadLeaderboardData(String sortType) {
        // Xác định field để sort
        String sortField = "points";
        if ("rating".equals(sortType)) {
            sortField = "averageRating"; // Field thực tế trong Firestore
        } else if ("totalReturned".equals(sortType)) {
            sortField = "totalReturned";
        }
        
        db.collection("users")
                .orderBy(sortField, Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    List<User> topUsers = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        user.setUid(doc.getId());
                        topUsers.add(user);
                    }
                    updatePodium(topUsers, sortType);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi tải bảng xếp hạng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updatePodium(List<User> topUsers, String sortType) {
        if (topUsers.size() >= 1) {
            User rank1 = topUsers.get(0);
            txtNameRank1.setText(rank1.getFullName());
            txtPointsRank1.setText(getDisplayValue(rank1, sortType));
            loadAvatar(rank1.getPhotoUrl(), imgRank1);
        }
        
        if (topUsers.size() >= 2) {
            User rank2 = topUsers.get(1);
            txtNameRank2.setText(rank2.getFullName());
            txtPointsRank2.setText(getDisplayValue(rank2, sortType));
            loadAvatar(rank2.getPhotoUrl(), imgRank2);
        }

        if (topUsers.size() >= 3) {
            User rank3 = topUsers.get(2);
            txtNameRank3.setText(rank3.getFullName());
            txtPointsRank3.setText(getDisplayValue(rank3, sortType));
            loadAvatar(rank3.getPhotoUrl(), imgRank3);
        }

        // Ranks 4+
        if (topUsers.size() > 3) {
            userList.addAll(topUsers.subList(3, topUsers.size()));
        }
        adapter.setSortType(sortType);
        adapter.notifyDataSetChanged();
    }
    
    private String getDisplayValue(User user, String sortType) {
        if ("points".equals(sortType)) {
            return user.getPoints() + " FP";
        } else if ("rating".equals(sortType)) {
            double rating = user.getRating() != null ? user.getRating() : 0.0;
            return String.format("%.1f ⭐", rating);
        } else if ("totalReturned".equals(sortType)) {
            int totalReturned = user.getTotalReturned() != null ? user.getTotalReturned() : 0;
            return totalReturned + " món";
        }
        return "";
    }

    private void loadAvatar(String url, ImageView imageView) {
        if (url != null && !url.isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(url)
                    .circleCrop()
                    .placeholder(R.drawable.bg_avatar_circle)
                    .into(imageView);
        }
    }
}
