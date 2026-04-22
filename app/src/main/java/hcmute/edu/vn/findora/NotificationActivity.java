package hcmute.edu.vn.findora;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.findora.adapter.NotificationAdapter;
import hcmute.edu.vn.findora.model.Notification;
import hcmute.edu.vn.findora.utils.NotificationHelper;

/**
 * NotificationActivity - In-app Notification Center
 * 
 * CHỨC NĂNG:
 * - Hiển thị tất cả thông báo của user
 * - Phân loại theo loại (AI match, tin nhắn, comment, like)
 * - Đánh dấu đã đọc khi click
 * - Navigate đến màn hình tương ứng
 * 
 * LAYOUT:
 * - Toolbar với nút back và "Đánh dấu tất cả đã đọc"
 * - RecyclerView hiển thị danh sách thông báo
 * - Empty state khi không có thông báo
 * 
 * ĐƯỢC MỞ TỪ:
 * - MainActivity: Click vào notification icon
 * - Push notification: Click vào notification
 */
public class NotificationActivity extends AppCompatActivity {
    
    private static final String TAG = "NotificationActivity";
    
    // UI Components
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyMessage;
    private ImageButton btnBack;
    private TextView btnMarkAllRead;
    
    // Data
    private NotificationAdapter adapter;
    private List<Notification> notifications;
    
    // Firebase
    private FirebaseFirestore db;
    private String currentUserId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
            ? FirebaseAuth.getInstance().getCurrentUser().getUid()
            : null;
        
        if (currentUserId == null) {
            finish();
            return;
        }
        
        // Initialize UI
        initViews();
        setupRecyclerView();
        loadNotifications();
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewNotifications);
        progressBar = findViewById(R.id.progressBar);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        btnBack = findViewById(R.id.btnBack);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        
        // Back button
        btnBack.setOnClickListener(v -> finish());
        
        // Mark all as read
        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
    }
    
    private void setupRecyclerView() {
        notifications = new ArrayList<>();
        adapter = new NotificationAdapter(this, notifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    
    /**
     * Load tất cả thông báo của user
     */
    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        
        db.collection("notifications")
            .whereEqualTo("userId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100) // Giới hạn 100 thông báo gần nhất
            .addSnapshotListener((snapshots, error) -> {
                progressBar.setVisibility(View.GONE);
                
                if (error != null) {
                    showEmpty("Không thể tải thông báo");
                    return;
                }
                
                if (snapshots == null || snapshots.isEmpty()) {
                    showEmpty("Bạn chưa có thông báo nào");
                    return;
                }
                
                notifications.clear();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Notification notification = doc.toObject(Notification.class);
                    notification.setId(doc.getId());
                    notifications.add(notification);
                }
                
                adapter.notifyDataSetChanged();
                recyclerView.setVisibility(View.VISIBLE);
                layoutEmpty.setVisibility(View.GONE);
                
                // Update "Mark all as read" button visibility
                updateMarkAllReadButton();
            });
    }
    
    /**
     * Hiển thị empty state
     */
    private void showEmpty(String message) {
        recyclerView.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        tvEmptyMessage.setText(message);
    }
    
    /**
     * Đánh dấu tất cả thông báo đã đọc
     */
    private void markAllAsRead() {
        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                NotificationHelper.markAsRead(notification.getId());
            }
        }
        
        // Update UI
        for (Notification notification : notifications) {
            notification.setRead(true);
        }
        adapter.notifyDataSetChanged();
        updateMarkAllReadButton();
        
        android.widget.Toast.makeText(this, 
            "Đã đánh dấu tất cả đã đọc", 
            android.widget.Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Cập nhật hiển thị nút "Mark all as read"
     */
    private void updateMarkAllReadButton() {
        boolean hasUnread = false;
        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                hasUnread = true;
                break;
            }
        }
        
        btnMarkAllRead.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
    }
}
