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
        notification.put("title", "Tìm thấy gợi ý phù hợp!");
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
     * DISABLED: Chức năng push notification đã bị tắt
     * Chỉ lưu notification vào Firestore để hiển thị trong app
     */
    private static void sendFCMNotification(String userId, Map<String, Object> notification) {
        // Push notification đã bị tắt
        // Notification chỉ hiển thị trong app (đã lưu vào Firestore ở trên)
        Log.d(TAG, "Push notification disabled. Notification saved to Firestore only.");
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
    
    /**
     * Gửi thông báo comment mới
     * 
     * @param postOwnerId ID của chủ bài đăng (người nhận thông báo)
     * @param postId ID của bài đăng
     * @param postTitle Tiêu đề bài đăng
     * @param commenterId ID người comment
     * @param commenterName Tên người comment
     * @param commenterAvatar Avatar người comment
     * @param commentText Nội dung comment
     */
    public static void sendCommentNotification(String postOwnerId, String postId, String postTitle,
                                               String commenterId, String commenterName, 
                                               String commenterAvatar, String commentText) {
        // Không gửi thông báo cho chính mình
        if (postOwnerId.equals(commenterId)) {
            return;
        }
        
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "comment");
        notification.put("userId", postOwnerId);
        notification.put("postId", postId);
        notification.put("senderId", commenterId);
        notification.put("senderName", commenterName);
        notification.put("senderAvatar", commenterAvatar);
        notification.put("title", commenterName + " đã bình luận");
        notification.put("body", commentText);
        notification.put("timestamp", com.google.firebase.Timestamp.now());
        notification.put("read", false);
        
        // Lưu vào Firestore
        db.collection("notifications")
            .add(notification)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Comment notification sent: " + documentReference.getId());
                sendFCMNotification(postOwnerId, notification);
            })
            .addOnFailureListener(e -> Log.e(TAG, "Failed to send comment notification", e));
    }
    
    /**
     * Gửi thông báo like mới
     * 
     * @param postOwnerId ID của chủ bài đăng (người nhận thông báo)
     * @param postId ID của bài đăng
     * @param postTitle Tiêu đề bài đăng
     * @param likerId ID người like
     * @param likerName Tên người like
     * @param likerAvatar Avatar người like
     */
    public static void sendLikeNotification(String postOwnerId, String postId, String postTitle,
                                           String likerId, String likerName, String likerAvatar) {
        // Không gửi thông báo cho chính mình
        if (postOwnerId.equals(likerId)) {
            return;
        }
        
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "like");
        notification.put("userId", postOwnerId);
        notification.put("postId", postId);
        notification.put("senderId", likerId);
        notification.put("senderName", likerName);
        notification.put("senderAvatar", likerAvatar);
        notification.put("title", likerName + " đã thích bài đăng");
        notification.put("body", postTitle);
        notification.put("timestamp", com.google.firebase.Timestamp.now());
        notification.put("read", false);
        
        // Lưu vào Firestore
        db.collection("notifications")
            .add(notification)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Like notification sent: " + documentReference.getId());
                sendFCMNotification(postOwnerId, notification);
            })
            .addOnFailureListener(e -> Log.e(TAG, "Failed to send like notification", e));
    }
    
    /**
     * Gửi thông báo khi đồ vật được tìm thấy
     * 
     * @param postOwnerId ID của chủ bài đăng (người mất đồ)
     * @param postId ID của bài đăng
     * @param postTitle Tiêu đề bài đăng
     * @param finderId ID người tìm thấy
     * @param finderName Tên người tìm thấy
     */
    public static void sendPostFoundNotification(String postOwnerId, String postId, 
                                                 String postTitle, String finderId, 
                                                 String finderName) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "post_found");
        notification.put("userId", postOwnerId);
        notification.put("postId", postId);
        notification.put("senderId", finderId);
        notification.put("senderName", finderName);
        notification.put("title", "Đồ vật của bạn đã được tìm thấy!");
        notification.put("body", finderName + " có thể đã tìm thấy " + postTitle);
        notification.put("timestamp", com.google.firebase.Timestamp.now());
        notification.put("read", false);
        
        // Lưu vào Firestore
        db.collection("notifications")
            .add(notification)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Post found notification sent: " + documentReference.getId());
                sendFCMNotification(postOwnerId, notification);
            })
            .addOnFailureListener(e -> Log.e(TAG, "Failed to send post found notification", e));
    }
    
    /**
     * Xóa tất cả thông báo của user
     */
    public static void deleteAllNotifications(String userId) {
        db.collection("notifications")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    doc.getReference().delete();
                }
                Log.d(TAG, "All notifications deleted for user: " + userId);
            })
            .addOnFailureListener(e -> Log.e(TAG, "Failed to delete notifications", e));
    }
    
    /**
     * Đánh dấu tất cả thông báo đã đọc
     */
    public static void markAllAsRead(String userId) {
        db.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    doc.getReference().update("read", true);
                }
                Log.d(TAG, "All notifications marked as read for user: " + userId);
            })
            .addOnFailureListener(e -> Log.e(TAG, "Failed to mark all as read", e));
    }
}
