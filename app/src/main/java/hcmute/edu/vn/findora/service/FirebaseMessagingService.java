package hcmute.edu.vn.findora.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.RemoteMessage;

import hcmute.edu.vn.findora.ChatActivity;
import hcmute.edu.vn.findora.MainActivity;
import hcmute.edu.vn.findora.PostDetailActivity;
import hcmute.edu.vn.findora.R;

/**
 * Firebase Cloud Messaging Service - Xử lý thông báo push
 *
 * SOUND MAPPING:
 * - new_message  → chat_noti_sound
 * - ai_match, like, comment, post_found, system → sound_noti
 */
public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    private static final String TAG = "FCMService";

    // Notification Channels — mỗi channel có sound riêng
    public static final String CHANNEL_MESSAGES = "messages";
    public static final String CHANNEL_GENERAL  = "general";   // ai_match, like, comment, system

    // Firestore listener cho notification realtime
    private ListenerRegistration notificationListener;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        startNotificationListener();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) notificationListener.remove();
    }

    // ─────────────────────────────────────────────────────────────
    // FCM push (khi app ở background / foreground qua FCM server)
    // ─────────────────────────────────────────────────────────────

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "FCM received from: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            handleDataMessage(remoteMessage.getData());
        }
        if (remoteMessage.getNotification() != null) {
            showNotification(
                remoteMessage.getNotification().getTitle(),
                remoteMessage.getNotification().getBody(),
                CHANNEL_GENERAL, null
            );
        }
    }

    private void handleDataMessage(java.util.Map<String, String> data) {
        String type  = data.get("type");
        String title = data.get("title");
        String body  = data.get("body");
        if (type == null || title == null || body == null) return;

        String channel = "new_message".equals(type) ? CHANNEL_MESSAGES : CHANNEL_GENERAL;
        showNotification(title, body, channel, data);
    }

    // ─────────────────────────────────────────────────────────────
    // Firestore realtime listener — push khi app đang chạy
    // ─────────────────────────────────────────────────────────────

    private void startNotificationListener() {
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        notificationListener = FirebaseFirestore.getInstance()
            .collection("notifications")
            .whereEqualTo("userId", uid)
            .whereEqualTo("read", false)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null || snapshots == null) return;
                for (com.google.firebase.firestore.DocumentChange change : snapshots.getDocumentChanges()) {
                    if (change.getType() == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        java.util.Map<String, Object> data = change.getDocument().getData();
                        String type  = (String) data.get("type");
                        String title = (String) data.get("title");
                        String body  = (String) data.get("body");
                        if (title == null || body == null) continue;

                        // Build extra map để tạo intent
                        java.util.Map<String, String> extras = new java.util.HashMap<>();
                        extras.put("type",       type != null ? type : "system");
                        if (data.get("chatId")    != null) extras.put("chatId",    (String) data.get("chatId"));
                        if (data.get("senderId")  != null) extras.put("senderId",  (String) data.get("senderId"));
                        if (data.get("senderName")!= null) extras.put("senderName",(String) data.get("senderName"));
                        if (data.get("postId")    != null) extras.put("postId",    (String) data.get("postId"));

                        String channel = "new_message".equals(type) ? CHANNEL_MESSAGES : CHANNEL_GENERAL;
                        showNotification(title, body, channel, extras);
                    }
                }
            });
    }

    // ─────────────────────────────────────────────────────────────
    // Hiển thị notification
    // ─────────────────────────────────────────────────────────────

    public void showNotification(String title, String body, String channelId,
                                  java.util.Map<String, String> data) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannels(); // idempotent

        Intent intent = createIntentForNotification(data);
        PendingIntent pi = PendingIntent.getActivity(
            this, (int) System.currentTimeMillis(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
            .setSmallIcon(getIconForChannel(channelId))
            .setContentTitle(title)
            .setContentText(body)
            .setColor(getColorForChannel(channelId))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi);

        nm.notify((int) System.currentTimeMillis(), builder.build());
    }

    // ─────────────────────────────────────────────────────────────
    // Notification Channels với custom sound
    // ─────────────────────────────────────────────────────────────

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Channel tin nhắn → chat_noti_sound
        if (nm.getNotificationChannel(CHANNEL_MESSAGES) == null) {
            Uri chatSound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.chat_noti_sound);
            AudioAttributes chatAttrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
            NotificationChannel msgChannel = new NotificationChannel(
                CHANNEL_MESSAGES, "Tin nhắn", NotificationManager.IMPORTANCE_HIGH);
            msgChannel.setDescription("Thông báo tin nhắn mới");
            msgChannel.setSound(chatSound, chatAttrs);
            msgChannel.enableVibration(true);
            nm.createNotificationChannel(msgChannel);
        }

        // Channel chung (like, AI match, comment...) → sound_noti
        if (nm.getNotificationChannel(CHANNEL_GENERAL) == null) {
            Uri generalSound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.sound_noti);
            AudioAttributes generalAttrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
            NotificationChannel generalChannel = new NotificationChannel(
                CHANNEL_GENERAL, "Thông báo", NotificationManager.IMPORTANCE_DEFAULT);
            generalChannel.setDescription("Like, AI match, bình luận và các thông báo khác");
            generalChannel.setSound(generalSound, generalAttrs);
            nm.createNotificationChannel(generalChannel);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private Intent createIntentForNotification(java.util.Map<String, String> data) {
        if (data == null) return new Intent(this, MainActivity.class);
        String type = data.get("type");
        if ("new_message".equals(type)) {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra("chatId",        data.get("chatId"));
            i.putExtra("otherUserId",   data.get("senderId"));
            i.putExtra("otherUserName", data.get("senderName"));
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return i;
        } else if ("ai_match".equals(type) || "like".equals(type)
                || "comment".equals(type) || "post_found".equals(type)) {
            Intent i = new Intent(this, PostDetailActivity.class);
            i.putExtra("postId", data.get("postId"));
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return i;
        }
        return new Intent(this, MainActivity.class);
    }

    private int getIconForChannel(String channelId) {
        return CHANNEL_MESSAGES.equals(channelId) ? R.drawable.ic_chat : R.drawable.ic_notifications;
    }

    private int getColorForChannel(String channelId) {
        return CHANNEL_MESSAGES.equals(channelId) ? 0xFF34A853 : 0xFF4285F4;
    }

    // ─────────────────────────────────────────────────────────────
    // Token management
    // ─────────────────────────────────────────────────────────────

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .update("fcmToken", token)
                .addOnSuccessListener(v -> Log.d(TAG, "FCM token updated"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update FCM token", e));
        }
    }
}
