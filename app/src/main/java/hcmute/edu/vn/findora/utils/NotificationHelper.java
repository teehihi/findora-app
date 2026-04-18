package hcmute.edu.vn.findora.utils;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * NotificationHelper - Gửi thông báo cho users
 * 
 * CHỨC NĂNG:
 * - Gửi thông báo AI match khi tìm thấy bài phù hợp
 * - Gửi thông báo tin nhắn mới
 * - Gửi thông báo hệ thống
 * 
 * SỬ DỤNG:
 * - Được gọi từ CreatePostActivity khi tạo bài mới (tự động tìm matches)
 * - Được gọi từ ChatActivity khi gửi tin nhắn
 */
public class NotificationHelper {
    
    private static final String TAG = "NotificationHelper";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    
    /**
     * Gửi thông báo AI match cho user
     * 
     * @param userId ID của user nhận thông báo
     * @param matchPostId ID của bài đăng match
     * @param matchTitle Tiêu đề bài đăng match
     * @param matchScore Điểm match (0-100)
     */
    public static void sendAIMatchNotification(String userId, String matchPostId, 
                                               String matchTitle, int matchScore) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "ai_match");
        notification.put("userId", userId);
        notification.put("postId", matchPostId);
        notification.put("title", "🔥 Tìm thấy gợi ý phù hợp!");
        notification.put("body", String.format("%s - Độ phù hợp %d%%", matchTitle, matchScore));
        notification.put("timestamp", com.google.firebase.Timestamp.now());
        notification.put("read", false);
        
        // Lưu vào Firestore
        db.collection("notifications")
            .add(notification)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "AI match notification sent: " + documentReference.getId());
                // TODO: Gửi FCM push notification
                sendFCMNotification(userId, notification);
            })
            .addOnFailureListener(e -> Log.e(TAG, "Failed to send AI match notification", e));
    }
    
    /**
     * Gửi thông báo tin nhắn mới
     * 
     * @param userId ID của user nhận thông báo
     * @param senderId ID của người gửi
     * @param senderName Tên người gửi
     * @param message Nội dung tin nhắn
     * @param chatId ID của cuộc trò chuyện
     */
    public static void sendMessageNotification(String userId, String senderId, 
                                               String senderName, String message, String chatId) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "new_message");
        notification.put("userId", userId);
        notification.put("senderId", senderId);
        notification.put("senderName", senderName);
        notification.put("chatId", chatId);
        notification.put("title", senderName);
        notification.put("body", message);
        notification.put("timestamp", com.google.firebase.Timestamp.now());
        notification.put("read", false);
        
        // Lưu vào Firestore
        db.collection("notifications")
            .add(notification)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Message notification sent: " + documentReference.getId());
                sendFCMNotification(userId, notification);
            })
            .addOnFailureListener(e -> Log.e(TAG, "Failed to send message notification", e));
    }
    
    /**
     * Gửi thông báo hệ thống
     * 
     * @param userId ID của user nhận thông báo
     * @param title Tiêu đề thông báo
     * @param body Nội dung thông báo
     */
    public static void sendSystemNotification(String userId, String title, String body) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "system");
        notification.put("userId", userId);
        notification.put("title", title);
        notification.put("body", body);
        notification.put("timestamp", com.google.firebase.Timestamp.now());
        notification.put("read", false);
        
        // Lưu vào Firestore
        db.collection("notifications")
            .add(notification)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "System notification sent: " + documentReference.getId());
                sendFCMNotification(userId, notification);
            })
            .addOnFailureListener(e -> Log.e(TAG, "Failed to send system notification", e));
    }
    
    /**
     * Gửi FCM push notification
     * 
     * LƯU Ý: Cần implement Cloud Function hoặc backend server để gửi FCM
     * Hiện tại chỉ lưu vào Firestore, FCM sẽ được trigger từ Cloud Function
     */
    private static void sendFCMNotification(String userId, Map<String, Object> notification) {
        // Lấy FCM token của user
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String fcmToken = documentSnapshot.getString("fcmToken");
                    if (fcmToken != null) {
                        Log.d(TAG, "FCM token found: " + fcmToken);
                        // TODO: Gọi Cloud Function để gửi FCM
                        // Hoặc gọi backend API để gửi FCM
                    } else {
                        Log.w(TAG, "User has no FCM token");
                    }
                }
            })
            .addOnFailureListener(e -> Log.e(TAG, "Failed to get FCM token", e));
    }
    
    /**
     * Đánh dấu thông báo đã đọc
     */
    public static void markAsRead(String notificationId) {
        db.collection("notifications")
            .document(notificationId)
            .update("read", true)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification marked as read"))
            .addOnFailureListener(e -> Log.e(TAG, "Failed to mark notification as read", e));
    }
    
    /**
     * Lấy số lượng thông báo chưa đọc
     */
    public static void getUnreadCount(String userId, UnreadCountCallback callback) {
        db.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int count = queryDocumentSnapshots.size();
                callback.onCountReceived(count);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to get unread count", e);
                callback.onCountReceived(0);
            });
    }
    
    public interface UnreadCountCallback {
        void onCountReceived(int count);
    }
}
