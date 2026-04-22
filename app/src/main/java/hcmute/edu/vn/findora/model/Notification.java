package hcmute.edu.vn.findora.model;

import com.google.firebase.Timestamp;

/**
 * Model đại diện cho một thông báo trong ứng dụng Findora.
 * 
 * CHỨC NĂNG:
 * - Lưu trữ thông tin về các thông báo (AI match, tin nhắn, comment, like)
 * - Được lưu vào Firestore collection "notifications"
 * - Hỗ trợ tự động mapping từ Firestore document sang Java object
 * 
 * LOẠI THÔNG BÁO:
 * - ai_match: AI tìm thấy bài đăng phù hợp
 * - new_message: Tin nhắn mới
 * - comment: Có người comment bài đăng
 * - like: Có người like bài đăng
 * - post_found: Đồ vật của bạn đã được tìm thấy
 * - system: Thông báo hệ thống
 * 
 * CẤU TRÚC FIRESTORE:
 * <pre>
 * notifications (collection)
 *   └─ {notificationId} (document)
 *       ├─ type: "ai_match" | "new_message" | "comment" | "like" | "system"
 *       ├─ userId: "abc123xyz" (người nhận)
 *       ├─ title: "🔥 Tìm thấy gợi ý phù hợp!"
 *       ├─ body: "Mèo vàng - Độ phù hợp 95%"
 *       ├─ timestamp: Timestamp
 *       ├─ read: false
 *       ├─ postId: "post123" (optional)
 *       ├─ chatId: "chat456" (optional)
 *       ├─ senderId: "user789" (optional)
 *       └─ senderName: "Nguyễn Văn A" (optional)
 * </pre>
 * 
 * @author Findora Team
 * @version 1.0
 * @since 2024
 */
public class Notification {
    
    // ==================== NOTIFICATION TYPES ====================
    public static final String TYPE_AI_MATCH = "ai_match";
    public static final String TYPE_NEW_MESSAGE = "new_message";
    public static final String TYPE_COMMENT = "comment";
    public static final String TYPE_LIKE = "like";
    public static final String TYPE_POST_FOUND = "post_found";
    public static final String TYPE_SYSTEM = "system";
    
    // ==================== FIELDS ====================
    
    private String id;              // Document ID trong Firestore
    private String type;            // Loại thông báo
    private String userId;          // ID người nhận
    private String title;           // Tiêu đề thông báo
    private String body;            // Nội dung thông báo
    private Timestamp timestamp;    // Thời gian tạo
    private boolean read;           // Đã đọc chưa
    
    // Optional fields (tùy loại thông báo)
    private String postId;          // ID bài đăng liên quan
    private String chatId;          // ID cuộc trò chuyện
    private String senderId;        // ID người gửi (cho message, comment, like)
    private String senderName;      // Tên người gửi
    private String senderAvatar;    // Avatar người gửi
    private Integer matchScore;     // Điểm match (cho AI match)
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Constructor rỗng - BẮT BUỘC cho Firestore
     */
    public Notification() {}
    
    /**
     * Constructor đầy đủ cho thông báo cơ bản
     */
    public Notification(String type, String userId, String title, String body) {
        this.type = type;
        this.userId = userId;
        this.title = title;
        this.body = body;
        this.timestamp = Timestamp.now();
        this.read = false;
    }
    
    // ==================== GETTERS & SETTERS ====================
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    
    public String getSenderAvatar() { return senderAvatar; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }
    
    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Lấy icon tương ứng với loại thông báo
     */
    public int getIconResource() {
        switch (type) {
            case TYPE_AI_MATCH:
                return hcmute.edu.vn.findora.R.drawable.ic_ai_sparkle;
            case TYPE_NEW_MESSAGE:
                return hcmute.edu.vn.findora.R.drawable.ic_chat;
            case TYPE_COMMENT:
                return hcmute.edu.vn.findora.R.drawable.ic_comment;
            case TYPE_LIKE:
                return hcmute.edu.vn.findora.R.drawable.ic_favorite;
            case TYPE_POST_FOUND:
                return hcmute.edu.vn.findora.R.drawable.ic_check_circle;
            default:
                return hcmute.edu.vn.findora.R.drawable.ic_notifications;
        }
    }
    
    /**
     * Lấy màu tương ứng với loại thông báo
     */
    public int getColorResource() {
        switch (type) {
            case TYPE_AI_MATCH:
                return hcmute.edu.vn.findora.R.color.primary;
            case TYPE_NEW_MESSAGE:
                return hcmute.edu.vn.findora.R.color.success;
            case TYPE_LIKE:
                return hcmute.edu.vn.findora.R.color.error;
            default:
                return hcmute.edu.vn.findora.R.color.text_secondary;
        }
    }
}
