package hcmute.edu.vn.findora;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hcmute.edu.vn.findora.adapter.ChatAdapter;
import hcmute.edu.vn.findora.model.ChatMessage;
import hcmute.edu.vn.findora.utils.PresenceManager;

/**
 * Màn hình chat 1-1 giữa hai người dùng về một bài đăng cụ thể.
 * 
 * Firestore structure:
 *   chats/{chatId}
 *     - participants: [uid1, uid2]
 *     - postId: "xxx"
 *     - postTitle: "Ví da màu nâu"
 *     - lastMessage: "Bạn tìm thấy ở đâu?"
 *     - lastTimestamp: Timestamp
 *   chats/{chatId}/messages/{messageId}
 *     - senderId: "uid1"
 *     - text: "Xin chào"
 *     - timestamp: Timestamp
 */
public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private android.widget.ImageButton btnSend, btnBack, btnAttach, btnImage, btnCall, btnMore, btnCancelReply;
    private TextView tvChatUserName, tvOnlineStatus, tvReplyPreviewSender, tvReplyPreviewText;
    private ImageView ivChatAvatar;
    private View viewOnlineIndicator;
    private LinearLayout layoutReplyPreview;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ChatAdapter adapter;
    private List<ChatMessage> messageList;

    private String chatId;
    private String otherUserId;
    private String postId;
    private String postTitle;
    private String currentUserId;
    
    // Reply state
    private ChatMessage replyingTo = null;
    
    // Presence listener
    private com.google.firebase.database.ValueEventListener presenceListener;
    
    // Loading dialog
    private hcmute.edu.vn.findora.utils.LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        loadingDialog = new hcmute.edu.vn.findora.utils.LoadingDialog(this);

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }
        currentUserId = auth.getCurrentUser().getUid();

        // Get extras
        chatId = getIntent().getStringExtra("chatId");
        otherUserId = getIntent().getStringExtra("otherUserId");
        postId = getIntent().getStringExtra("postId");
        postTitle = getIntent().getStringExtra("postTitle");

        // Bind views
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        btnAttach = findViewById(R.id.btnAttach);
        btnImage = findViewById(R.id.btnImage);
        btnCall = findViewById(R.id.btnCall);
        btnMore = findViewById(R.id.btnMore);
        btnCancelReply = findViewById(R.id.btnCancelReply);
        tvChatUserName = findViewById(R.id.tvChatUserName);
        tvOnlineStatus = findViewById(R.id.tvOnlineStatus);
        ivChatAvatar = findViewById(R.id.ivChatAvatar);
        viewOnlineIndicator = findViewById(R.id.viewOnlineIndicator);
        layoutReplyPreview = findViewById(R.id.layoutReplyPreview);
        tvReplyPreviewSender = findViewById(R.id.tvReplyPreviewSender);
        tvReplyPreviewText = findViewById(R.id.tvReplyPreviewText);

        // Apply window insets for safe area
        android.view.View chatHeader = findViewById(R.id.chatHeader);
        if (chatHeader != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(chatHeader, (v, insets) -> {
                androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + dpToPx(12), v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        // Setup
        messageList = new ArrayList<>();
        adapter = new ChatAdapter(this, messageList, currentUserId, otherUserId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
        btnCall.setOnClickListener(v -> makePhoneCall());
        btnImage.setOnClickListener(v -> selectImage());
        btnAttach.setOnClickListener(v -> selectImage());
        btnCancelReply.setOnClickListener(v -> cancelReply());

        // Setup swipe-to-reply
        adapter.setOnReplyListener(message -> {
            replyingTo = message;
            showReplyPreview(message);
        });
        adapter.attachSwipeToReply(rvMessages);

        // Load other user info
        loadOtherUserInfo();
        listenToOtherUserPresence();

        // If no chatId yet, create or find existing chat
        if (chatId == null || chatId.isEmpty()) {
            findOrCreateChat();
        } else {
            listenForMessages();
        }
    }
    
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void listenToOtherUserPresence() {
        if (otherUserId == null || otherUserId.isEmpty()) return;

        presenceListener = PresenceManager.listenToPresence(otherUserId, (isOnline, lastSeenMillis) -> {
            runOnUiThread(() -> {
                if (isOnline) {
                    viewOnlineIndicator.setVisibility(View.VISIBLE);
                    tvOnlineStatus.setText("Online now");
                    tvOnlineStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
                } else {
                    viewOnlineIndicator.setVisibility(View.INVISIBLE);
                    if (lastSeenMillis > 0) {
                        tvOnlineStatus.setText("Hoạt động " + formatLastSeen(lastSeenMillis));
                    } else {
                        tvOnlineStatus.setText("Offline");
                    }
                    tvOnlineStatus.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
                }
            });
        });
    }

    private String formatLastSeen(long millis) {
        long diff = System.currentTimeMillis() - millis;
        long minutes = diff / 60000;
        long hours = minutes / 60;
        long days = hours / 24;

        if (minutes < 1) return "vừa xong";
        if (minutes < 60) return minutes + " phút trước";
        if (hours < 24) return hours + " giờ trước";
        return days + " ngày trước";
    }

    @Override
    protected void onStart() {
        super.onStart();
        PresenceManager.goOnline();
    }

    @Override
    protected void onStop() {
        super.onStop();
        PresenceManager.goOffline();
    }

    private void loadOtherUserInfo() {
        if (otherUserId == null || otherUserId.isEmpty()) return;

        db.collection("users").document(otherUserId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("fullName");
                        tvChatUserName.setText(name != null ? name : "User");

                        String photoUrl = doc.getString("photoUrl");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(photoUrl)
                                    .transform(new CircleCrop())
                                    .placeholder(R.drawable.ic_person)
                                    .error(R.drawable.ic_person)
                                    .into(ivChatAvatar);
                        } else {
                            ivChatAvatar.setImageResource(R.drawable.ic_person);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    ivChatAvatar.setImageResource(R.drawable.ic_person);
                });
    }

    private void findOrCreateChat() {
        // Find existing chat between these two users about this post
        db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean found = false;
                    for (var doc : querySnapshot) {
                        @SuppressWarnings("unchecked")
                        List<String> participants = (List<String>) doc.get("participants");
                        String docPostId = doc.getString("postId");

                        if (participants != null && participants.contains(otherUserId)
                                && postId != null && postId.equals(docPostId)) {
                            // Found existing chat
                            chatId = doc.getId();
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        // Create new chat
                        createNewChat();
                    } else {
                        listenForMessages();
                    }
                })
                .addOnFailureListener(e -> {
                    createNewChat();
                });
    }

    private void createNewChat() {
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("participants", Arrays.asList(currentUserId, otherUserId));
        chatData.put("postId", postId != null ? postId : "");
        chatData.put("postTitle", postTitle != null ? postTitle : "");
        chatData.put("lastMessage", "");
        chatData.put("lastTimestamp", Timestamp.now());

        db.collection("chats").add(chatData)
                .addOnSuccessListener(ref -> {
                    chatId = ref.getId();
                    listenForMessages();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tạo cuộc trò chuyện", Toast.LENGTH_SHORT).show();
                });
    }

    private void listenForMessages() {
        if (chatId == null) return;

        db.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            ChatMessage msg = dc.getDocument().toObject(ChatMessage.class);
                            msg.setId(dc.getDocument().getId());
                            messageList.add(msg);
                            
                            // Mark as read if not sent by current user
                            if (!msg.getSenderId().equals(currentUserId) && !msg.isRead()) {
                                markMessageAsRead(msg.getId());
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) {
                        rvMessages.scrollToPosition(messageList.size() - 1);
                    }
                });
    }
    
    private void markMessageAsRead(String messageId) {
        if (chatId == null || messageId == null) return;
        
        db.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update("read", true)
                .addOnSuccessListener(aVoid -> {
                    // Update unread count on the chat document so ChatListActivity snapshot triggers
                    db.collection("chats").document(chatId)
                            .collection("messages")
                            .whereEqualTo("read", false)
                            .get()
                            .addOnSuccessListener(snap -> {
                                int remaining = 0;
                                for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                                    String sid = doc.getString("senderId");
                                    if (sid != null && !sid.equals(currentUserId)) remaining++;
                                }
                                // Write unreadCount to chat doc to trigger snapshot in ChatListActivity
                                db.collection("chats").document(chatId)
                                        .update("unreadCount_" + currentUserId, remaining);
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ChatActivity", "Failed to mark message as read", e);
                });
    }

    private void sendMessage() {
        String text = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (text.isEmpty() || chatId == null) return;

        etMessage.setText("");
        
        loadingDialog.show("Đang gửi...");

        Timestamp now = Timestamp.now();

        Map<String, Object> msgData = new HashMap<>();
        msgData.put("senderId", currentUserId);
        msgData.put("text", text);
        msgData.put("timestamp", now);
        msgData.put("read", false);

        // Attach reply data if replying
        if (replyingTo != null) {
            msgData.put("replyToId", replyingTo.getId());
            String replyText = replyingTo.getImageUrl() != null && !replyingTo.getImageUrl().isEmpty()
                    ? "[Ảnh]" : replyingTo.getText();
            msgData.put("replyToText", replyText);
            // Sender label: "Bạn" if replying to own message, else other user's name
            boolean replyToSelf = currentUserId.equals(replyingTo.getSenderId());
            msgData.put("replyToSender", replyToSelf ? "Bạn" : tvChatUserName.getText().toString());
            cancelReply();
        }

        db.collection("chats").document(chatId)
                .collection("messages")
                .add(msgData)
                .addOnSuccessListener(documentReference -> {
                    loadingDialog.dismiss();
                    sendMessageNotification(text);
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    Toast.makeText(this, "Lỗi gửi tin nhắn", Toast.LENGTH_SHORT).show();
                });

        Map<String, Object> chatUpdate = new HashMap<>();
        chatUpdate.put("lastMessage", text);
        chatUpdate.put("lastTimestamp", now);
        db.collection("chats").document(chatId).update(chatUpdate);
    }

    private void showReplyPreview(ChatMessage message) {
        layoutReplyPreview.setVisibility(android.view.View.VISIBLE);
        boolean replyToSelf = currentUserId.equals(message.getSenderId());
        String senderName = replyToSelf ? "chính mình" : tvChatUserName.getText().toString();
        tvReplyPreviewSender.setText("Đang trả lời " + senderName);
        String previewText = message.getImageUrl() != null && !message.getImageUrl().isEmpty()
                ? "[Ảnh]" : message.getText();
        tvReplyPreviewText.setText(previewText);
        etMessage.requestFocus();
    }

    private void cancelReply() {
        replyingTo = null;
        layoutReplyPreview.setVisibility(android.view.View.GONE);
    }
    
    /**
     * Gửi notification tin nhắn mới cho người nhận
     */
    private void sendMessageNotification(String messageText) {
        // Lấy thông tin người gửi (current user)
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String senderName = doc.getString("fullName");
                        String senderAvatar = doc.getString("photoUrl");
                        
                        // Gửi notification
                        hcmute.edu.vn.findora.utils.NotificationHelper.sendMessageNotification(
                            otherUserId,
                            currentUserId,
                            senderName != null ? senderName : "Người dùng",
                            messageText,
                            chatId
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ChatActivity", "Failed to get sender info", e);
                });
    }
    
    /**
     * Mở dialer để gọi điện thoại cho user
     */
    private void makePhoneCall() {
        if (otherUserId == null || otherUserId.isEmpty()) {
            android.widget.Toast.makeText(this, "Không thể gọi điện", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        
        loadingDialog.show("Đang lấy số điện thoại...");
        
        // Lấy số điện thoại của user
        db.collection("users").document(otherUserId).get()
                .addOnSuccessListener(doc -> {
                    loadingDialog.dismiss();
                    if (doc.exists()) {
                        String phone = doc.getString("phone");
                        if (phone != null && !phone.isEmpty()) {
                            // Mở dialer với số điện thoại
                            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_DIAL);
                            intent.setData(android.net.Uri.parse("tel:" + phone));
                            startActivity(intent);
                        } else {
                            android.widget.Toast.makeText(this, "Người dùng chưa cập nhật số điện thoại", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    android.util.Log.e("ChatActivity", "Failed to get phone number", e);
                    android.widget.Toast.makeText(this, "Không thể lấy số điện thoại", android.widget.Toast.LENGTH_SHORT).show();
                });
    }
    
    private static final int PICK_IMAGE_REQUEST = 1001;
    
    /**
     * Chọn ảnh từ thư viện
     */
    private void selectImage() {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            android.net.Uri imageUri = data.getData();
            uploadImageAndSendMessage(imageUri);
        }
    }
    
    /**
     * Upload ảnh lên Firebase Storage và gửi tin nhắn với URL ảnh
     */
    private void uploadImageAndSendMessage(android.net.Uri imageUri) {
        if (chatId == null) {
            android.widget.Toast.makeText(this, "Đang khởi tạo chat...", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading dialog
        loadingDialog.show("Đang gửi ảnh...");
        
        // Upload to Firebase Storage
        String fileName = "chat_images/" + chatId + "/" + System.currentTimeMillis() + ".jpg";
        com.google.firebase.storage.StorageReference storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().getReference(fileName);
        
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        
                        // Send message with image URL
                        sendImageMessage(imageUrl);
                        
                        loadingDialog.dismiss();
                        android.widget.Toast.makeText(this, "Đã gửi ảnh", android.widget.Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        loadingDialog.dismiss();
                        android.widget.Toast.makeText(this, "Lỗi lấy URL ảnh", android.widget.Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    android.widget.Toast.makeText(this, "Lỗi upload ảnh", android.widget.Toast.LENGTH_SHORT).show();
                    android.util.Log.e("ChatActivity", "Upload failed", e);
                });
    }
    
    /**
     * Gửi tin nhắn chứa ảnh
     */
    private void sendImageMessage(String imageUrl) {
        if (chatId == null) return;
        
        com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();
        
        // Add message to subcollection
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUserId);
        message.put("text", "[Ảnh]"); // Placeholder text
        message.put("imageUrl", imageUrl);
        message.put("timestamp", now);
        message.put("read", false);
        
        db.collection("chats").document(chatId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(docRef -> {
                    // Send notification
                    sendMessageNotification("[Ảnh]");
                });
        
        // Update chat document with last message
        Map<String, Object> chatUpdate = new HashMap<>();
        chatUpdate.put("lastMessage", "[Ảnh]");
        chatUpdate.put("lastTimestamp", now);
        
        db.collection("chats").document(chatId)
                .update(chatUpdate);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (presenceListener != null && otherUserId != null) {
            PresenceManager.removePresenceListener(otherUserId, presenceListener);
        }
    }
}

