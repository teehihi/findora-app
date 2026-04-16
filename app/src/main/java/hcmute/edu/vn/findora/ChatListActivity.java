package hcmute.edu.vn.findora;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.findora.adapter.ChatListAdapter;

/**
 * Màn hình danh sách các cuộc trò chuyện.
 * Hiển thị tất cả chat mà user hiện tại tham gia.
 */
public class ChatListActivity extends AppCompatActivity {

    private RecyclerView rvChatList;
    private LinearLayout layoutEmpty;
    private BottomNavigationView bottomNav;
    private FloatingActionButton fabCreatePost;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ChatListAdapter adapter;
    private List<DocumentSnapshot> chatDocs;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }
        currentUserId = auth.getCurrentUser().getUid();

        rvChatList = findViewById(R.id.rvChatList);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        bottomNav = findViewById(R.id.bottomNav);
        fabCreatePost = findViewById(R.id.fabCreatePost);

        chatDocs = new ArrayList<>();
        adapter = new ChatListAdapter(this, chatDocs, currentUserId);
        rvChatList.setLayoutManager(new LinearLayoutManager(this));
        rvChatList.setAdapter(adapter);

        fabCreatePost.setOnClickListener(v -> {
            startActivity(new Intent(this, CreatePostActivity.class));
        });

        // Bottom Navigation
        bottomNav.setSelectedItemId(R.id.nav_chat);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            } else if (id == R.id.nav_chat) {
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNav.setSelectedItemId(R.id.nav_chat);
        loadChats();
    }

    private void loadChats() {
        // Try without orderBy first to avoid index requirement
        db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        android.util.Log.e("ChatListActivity", "Error loading chats: " + error.getMessage(), error);
                        // Show error toast for debugging
                        android.widget.Toast.makeText(this, "Lỗi tải tin nhắn: " + error.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                        showEmpty();
                        return;
                    }
                    
                    if (snapshots == null) {
                        android.util.Log.d("ChatListActivity", "Snapshots is null");
                        showEmpty();
                        return;
                    }

                    android.util.Log.d("ChatListActivity", "Loaded " + snapshots.size() + " chats");
                    
                    chatDocs.clear();
                    
                    // Add all documents to list
                    List<DocumentSnapshot> tempList = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        tempList.add(doc);
                    }
                    
                    // Sort by lastTimestamp manually
                    tempList.sort((a, b) -> {
                        com.google.firebase.Timestamp tsA = a.getTimestamp("lastTimestamp");
                        com.google.firebase.Timestamp tsB = b.getTimestamp("lastTimestamp");
                        if (tsA == null && tsB == null) return 0;
                        if (tsA == null) return 1;
                        if (tsB == null) return -1;
                        return tsB.compareTo(tsA); // Descending order
                    });
                    
                    chatDocs.addAll(tempList);
                    adapter.notifyDataSetChanged();

                    if (chatDocs.isEmpty()) {
                        showEmpty();
                    } else {
                        rvChatList.setVisibility(View.VISIBLE);
                        layoutEmpty.setVisibility(View.GONE);
                    }
                });
    }

    private void showEmpty() {
        rvChatList.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }
}
