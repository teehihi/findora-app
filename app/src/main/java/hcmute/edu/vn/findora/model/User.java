package hcmute.edu.vn.findora.model;

import com.google.firebase.Timestamp;

/**
 * Model đại diện cho người dùng trong Findora.
 * Firestore collection: "users"
 */
public class User {

    private String uid;
    private String fullName;
    private String email;
    private String phone;
    private String photoUrl;
    private String authProvider;
    private Timestamp createdAt;

    // ── Gamification ──────────────────────────────────────────
    /** Tổng điểm FindoPoint */
    private long points;

    /** Cấp bậc: "Người mới" | "Người tốt" | "Thiên thần" | "Huyền thoại" */
    private String level = "Người mới";

    /** Số lần đã trả lại đồ vật thành công */
    private Integer totalReturned;

    /** Số lần đã nhận lại đồ vật */
    private int totalReceived;

    /** Điểm đánh giá trung bình (1.0 - 5.0) */
    private double averageRating;

    /** Tổng số lần được đánh giá */
    private int totalRatings;

    // ── Constructors ──────────────────────────────────────────
    public User() {}

    public User(String uid, String fullName, String email, String photoUrl) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.photoUrl = photoUrl;
        this.points = 0;
        this.level = "Người mới";
        this.totalReturned = 0;
        this.totalReceived = 0;
        this.averageRating = 0.0;
        this.totalRatings = 0;
    }

    // ── Getters & Setters ─────────────────────────────────────
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public long getPoints() { return points; }
    public void setPoints(long points) { this.points = points; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public Integer getTotalReturned() { return totalReturned; }
    public void setTotalReturned(Integer totalReturned) { this.totalReturned = totalReturned; }

    public int getTotalReceived() { return totalReceived; }
    public void setTotalReceived(int totalReceived) { this.totalReceived = totalReceived; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    
    // Alias for leaderboard
    public Double getRating() { return averageRating; }

    public int getTotalRatings() { return totalRatings; }
    public void setTotalRatings(int totalRatings) { this.totalRatings = totalRatings; }
}
