package hcmute.edu.vn.findora.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.RemoteMessage;

import hcmute.edu.vn.findora.ChatActivity;
import hcmute.edu.vn.findora.MainActivity;
import hcmute.edu.vn.findora.PostDetailActivity;
import hcmute.edu.vn.findora.R;

/**
 * Firebase Cloud Messaging Service - Xử lý thông báo push
 * 
 * CHỨC NĂNG:
 * - Nhận thông báo từ Firebase Cloud Messaging
 * - Hiển thị notification cho user
 * - Xử lý click vào notification
 * 
 * LOẠI THÔNG BÁO:
 * 1. AI_MATCH: Tìm thấy bài đăng phù hợp (AI matching)
 * 2. NEW_MESSAGE: Tin nhắn mới
 * 3. POST_VIEW: Có người xem bài đăng của bạn
 * 4. SYSTEM: Thông báo hệ thống
 */
public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {
    
    private static final String TAG = "FCMService";
    
    // Notification Channels
    private static final String CHANNEL_AI_MATCH = "ai_match";
    private static final String CHANNEL_MESSAGES = "messages";
    private static final String CHANNEL_SYSTEM = "system";
    
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        
        // Kiểm tra có data payload không
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }
        
        // Kiểm tra có notification payload không
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            showNotification(
                remoteMessage.getNotification().getTitle(),
                remoteMessage.getNotification().getBody(),
                "system",
                null
            );
        }
    }
    
    /**
     * Xử lý data message từ FCM
     */
    private void handleDataMessage(java.util.Map<String, String> data) {
        String type = data.get("type");
        String title = data.get("title");
        String body = data.get("body");
        
        if (type == null || title == null || body == null) {
            Log.w(TAG, "Invalid notification data");
            return;
        }
        
        switch (type) {
            case "ai_match":
                // Thông báo AI tìm thấy match
                showNotification(title, body, CHANNEL_AI_MATCH, data);
                break;
                
            case "new_message":
                // Thông báo tin nhắn mới
                showNotification(title, body, CHANNEL_MESSAGES, data);
                break;
                
            case "post_view":
            case "system":
                // Thông báo hệ thống
                showNotification(title, body, CHANNEL_SYSTEM, data);
                break;
                
            default:
                Log.w(TAG, "Unknown notification type: " + type);
        }
    }
    
    /**
     * Hiển thị notification
     */
    private void showNotification(String title, String body, String channelId, java.util.Map<String, String> data) {
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Tạo notification channel (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels(notificationManager);
        }
        
        // Tạo intent khi click vào notification
        Intent intent = createIntentForNotification(data);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Chọn icon và màu dựa trên loại thông báo
        int icon = getIconForChannel(channelId);
        int color = getColorForChannel(channelId);
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(body)
            .setColor(color)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);
        
        // Hiển thị notification
        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }
    
    /**
     * Tạo notification channels (Android 8.0+)
     */
    private void createNotificationChannels(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel 1: AI Match (Ưu tiên cao)
            NotificationChannel aiChannel = new NotificationChannel(
                CHANNEL_AI_MATCH,
                "Gợi ý AI",
                NotificationManager.IMPORTANCE_HIGH
            );
            aiChannel.setDescription("Thông báo khi AI tìm thấy bài đăng phù hợp");
            aiChannel.enableVibration(true);
            notificationManager.createNotificationChannel(aiChannel);
            
            // Channel 2: Messages (Ưu tiên cao)
            NotificationChannel messageChannel = new NotificationChannel(
                CHANNEL_MESSAGES,
                "Tin nhắn",
                NotificationManager.IMPORTANCE_HIGH
            );
            messageChannel.setDescription("Thông báo tin nhắn mới");
            messageChannel.enableVibration(true);
            notificationManager.createNotificationChannel(messageChannel);
            
            // Channel 3: System (Ưu tiên trung bình)
            NotificationChannel systemChannel = new NotificationChannel(
                CHANNEL_SYSTEM,
                "Hệ thống",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            systemChannel.setDescription("Thông báo hệ thống");
            notificationManager.createNotificationChannel(systemChannel);
        }
    }
    
    /**
     * Tạo intent dựa trên loại thông báo
     */
    private Intent createIntentForNotification(java.util.Map<String, String> data) {
        if (data == null) {
            return new Intent(this, MainActivity.class);
        }
        
        String type = data.get("type");
        
        if ("new_message".equals(type)) {
            // Mở ChatActivity
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("chatId", data.get("chatId"));
            intent.putExtra("otherUserId", data.get("senderId"));
            intent.putExtra("otherUserName", data.get("senderName"));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return intent;
            
        } else if ("ai_match".equals(type)) {
            // Mở PostDetailActivity
            Intent intent = new Intent(this, PostDetailActivity.class);
            intent.putExtra("postId", data.get("postId"));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return intent;
            
        } else {
            // Mở MainActivity
            return new Intent(this, MainActivity.class);
        }
    }
    
    /**
     * Lấy icon cho notification
     */
    private int getIconForChannel(String channelId) {
        switch (channelId) {
            case CHANNEL_AI_MATCH:
                return R.drawable.ic_ai_sparkle;
            case CHANNEL_MESSAGES:
                return R.drawable.ic_chat;
            default:
                return R.drawable.ic_notifications;
        }
    }
    
    /**
     * Lấy màu cho notification
     */
    private int getColorForChannel(String channelId) {
        switch (channelId) {
            case CHANNEL_AI_MATCH:
                return 0xFF4285F4; // Blue
            case CHANNEL_MESSAGES:
                return 0xFF34A853; // Green
            default:
                return 0xFF5F6368; // Gray
        }
    }
    
    /**
     * Được gọi khi FCM token mới được tạo
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        
        // Gửi token lên server hoặc lưu vào Firestore
        sendTokenToServer(token);
    }
    
    /**
     * Gửi FCM token lên Firestore
     */
    private void sendTokenToServer(String token) {
        // TODO: Lưu token vào Firestore user document
        com.google.firebase.auth.FirebaseUser user = 
            com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        
        if (user != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token updated"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update FCM token", e));
        }
    }
}
