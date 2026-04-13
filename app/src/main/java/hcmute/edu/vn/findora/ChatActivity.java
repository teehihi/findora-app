package hcmute.edu.vn.findora;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
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
    private TextInputEditText etMessage;
    private com.google.android.material.floatingactionbutton.FloatingActionButton btnSend;
    private android.widget.ImageButton btnBack;
    private TextView tvChatUserName, tvChatPostTitle;
    private ImageView ivChatAvatar;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ChatAdapter adapter;
    private List<ChatMessage> messageList;

    private String chatId;
    private String otherUserId;
    private String postId;
    private String postTitle;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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
        tvChatUserName = findViewById(R.id.tvChatUserName);
        tvChatPostTitle = findViewById(R.id.tvChatPostTitle);
        ivChatAvatar = findViewById(R.id.ivChatAvatar);

        // Setup
        messageList = new ArrayList<>();
        adapter = new ChatAdapter(this, messageList, currentUserId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);

        tvChatPostTitle.setText(postTitle != null && !postTitle.isEmpty() ? "Về: " + postTitle : "");

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());

        // Load other user info
        loadOtherUserInfo();

        // If no chatId yet, create or find existing chat
        if (chatId == null || chatId.isEmpty()) {
            findOrCreateChat();
        } else {
            listenForMessages();
        }
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
                                    .into(ivChatAvatar);
                        }
                    }
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
                        }
                    }
                    adapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) {
                        rvMessages.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        String text = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (text.isEmpty() || chatId == null) return;

        etMessage.setText("");

        Timestamp now = Timestamp.now();

        // Add message to subcollection
        Map<String, Object> msgData = new HashMap<>();
        msgData.put("senderId", currentUserId);
        msgData.put("text", text);
        msgData.put("timestamp", now);

        db.collection("chats").document(chatId)
                .collection("messages")
                .add(msgData);

        // Update chat document with last message
        Map<String, Object> chatUpdate = new HashMap<>();
        chatUpdate.put("lastMessage", text);
        chatUpdate.put("lastTimestamp", now);

        db.collection("chats").document(chatId)
                .update(chatUpdate);
    }
}
