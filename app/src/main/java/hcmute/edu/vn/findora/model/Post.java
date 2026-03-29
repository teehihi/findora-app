package hcmute.edu.vn.findora.model;

import com.google.firebase.Timestamp;

/**
 * Model đại diện cho một bài đăng (post) trong ứng dụng Findora.
 * Được lưu vào Firestore collection "posts".
 */
public class Post {

    private String id;           // ID document trong Firestore (không lưu vào DB, chỉ dùng ở local)
    private String title;        // Tiêu đề bài đăng
    private String description;  // Mô tả chi tiết
    private String type;         // Loại: "lost" (mất đồ) hoặc "found" (tìm thấy đồ)
    private String userId;       // UID của người đăng
    private Timestamp createdAt; // Thời gian đăng bài

    // Constructor rỗng - bắt buộc phải có để Firestore tự động ánh xạ dữ liệu
    public Post() {}

    // Constructor đầy đủ - dùng khi tạo bài đăng mới
    public Post(String title, String description, String type, String userId, Timestamp createdAt) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    // Getters và Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
