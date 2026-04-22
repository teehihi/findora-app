package hcmute.edu.vn.findora.model;

import com.google.firebase.Timestamp;

/**
 * Model đại diện cho một comment trong bài đăng
 * 
 * CẤU TRÚC FIRESTORE:
 * posts/{postId}/comments/{commentId}
 *   ├─ userId: "abc123"
 *   ├─ userName: "Nguyễn Văn A"
 *   ├─ userAvatar: "https://..."
 *   ├─ text: "Bạn tìm thấy ở đâu?"
 *   └─ timestamp: Timestamp
 */
public class Comment {
    
    private String id;
    private String userId;
    private String userName;
    private String userAvatar;
    private String text;
    private Timestamp timestamp;
    
    public Comment() {}
    
    public Comment(String userId, String userName, String userAvatar, String text) {
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.text = text;
        this.timestamp = Timestamp.now();
    }
    
    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
