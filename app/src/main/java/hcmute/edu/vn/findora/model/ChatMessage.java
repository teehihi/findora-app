package hcmute.edu.vn.findora.model;

import com.google.firebase.Timestamp;

/**
 * Model đại diện cho một tin nhắn trong cuộc trò chuyện.
 * Lưu trong Firestore: chats/{chatId}/messages/{messageId}
 */
public class ChatMessage {
    private String id;
    private String senderId;
    private String text;
    private Timestamp timestamp;
    private boolean read;

    public ChatMessage() {}

    public ChatMessage(String senderId, String text, Timestamp timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
        this.read = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
