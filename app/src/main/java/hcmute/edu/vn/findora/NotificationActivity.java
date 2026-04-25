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
        
        // Apply window insets for safe area (status bar)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
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
    
    /**
     * Khởi tạo các view components
     * 
     * CHỨC NĂNG:
     * - Bind tất cả views từ layout
     * - Setup click listeners cho buttons
     * - Setup back button để đóng activity
     * - Setup "Mark all as read" button
     */
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
    
    /**
     * Setup RecyclerView với adapter
     * 
     * CHỨC NĂNG:
     * - Khởi tạo notifications list
     * - Tạo NotificationAdapter
     * - Set LinearLayoutManager
     * - Bind adapter vào RecyclerView
     */
    private void setupRecyclerView() {
        notifications = new ArrayList<>();
        adapter = new NotificationAdapter(this, notifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    
    /**
     * Load tất cả thông báo của user
     * 
     * LƯU Ý:
     * - Cần tạo Firestore index cho query này
     * - Collection: notifications
     * - Fields: userId (Ascending), timestamp (Descending)
     * - Nếu index chưa có, sẽ fallback sang query đơn giản hơn
     */
    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        
        // Try query with orderBy first (requires index)
        db.collection("notifications")
            .whereEqualTo("userId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100) // Giới hạn 100 thông báo gần nhất
            .addSnapshotListener((snapshots, error) -> {
                progressBar.setVisibility(View.GONE);
                
                if (error != null) {
                    android.util.Log.e(TAG, "Query with orderBy failed, trying fallback", error);
                    
                    // Fallback: Query without orderBy (không cần index)
                    loadNotificationsFallback();
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
     * Fallback query khi index chưa có
     * Query đơn giản hơn, không cần index
     */
    private void loadNotificationsFallback() {
        android.util.Log.d(TAG, "Using fallback query (no orderBy)");
        
        db.collection("notifications")
            .whereEqualTo("userId", currentUserId)
            .limit(100)
            .addSnapshotListener((snapshots, error) -> {
                progressBar.setVisibility(View.GONE);
                
                if (error != null) {
                    android.util.Log.e(TAG, "Fallback query also failed", error);
                    showEmpty("Không thể tải thông báo\nVui lòng thử lại sau");
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
                
                // Sort manually by timestamp (descending)
                notifications.sort((n1, n2) -> {
                    if (n1.getTimestamp() == null) return 1;
                    if (n2.getTimestamp() == null) return -1;
                    return n2.getTimestamp().compareTo(n1.getTimestamp());
                });
                
                adapter.notifyDataSetChanged();
                recyclerView.setVisibility(View.VISIBLE);
                layoutEmpty.setVisibility(View.GONE);
                
                // Update "Mark all as read" button visibility
                updateMarkAllReadButton();
                
                // Show toast to inform user
                android.widget.Toast.makeText(this, 
                    "Đang sử dụng chế độ fallback. Hãy tạo Firestore index để tối ưu.", 
                    android.widget.Toast.LENGTH_LONG).show();
            });
    }
    
    /**
     * Hiển thị empty state với message tùy chỉnh
     * 
     * @param message Message hiển thị cho user
     * 
     * ĐƯỢC GỌI KHI:
     * - Query thất bại
     * - Không có thông báo nào
     * - Lỗi kết nối
     */
    private void showEmpty(String message) {
        recyclerView.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        tvEmptyMessage.setText(message);
    }
    
    /**
     * Đánh dấu tất cả thông báo đã đọc
     * 
     * CHỨC NĂNG:
     * - Loop qua tất cả notifications
     * - Gọi NotificationHelper.markAsRead() cho từng notification chưa đọc
     * - Update UI (set read = true)
     * - Notify adapter
     * - Ẩn nút "Mark all as read"
     * - Hiển thị toast confirmation
     * 
     * ĐƯỢC GỌI TỪ:
     * - btnMarkAllRead.setOnClickListener()
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
     * 
     * LOGIC:
     * - Check nếu có notification nào chưa đọc
     * - Nếu có: Hiển thị nút
     * - Nếu không: Ẩn nút
     * 
     * ĐƯỢC GỌI TỪ:
     * - loadNotifications() - Sau khi load xong
     * - loadNotificationsFallback() - Sau khi load xong
     * - markAllAsRead() - Sau khi đánh dấu tất cả
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
