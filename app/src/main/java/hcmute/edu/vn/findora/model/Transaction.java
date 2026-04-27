package hcmute.edu.vn.findora.model;

import com.google.firebase.Timestamp;

/**
 * Model lịch sử điểm thưởng.
 * Firestore collection: "transactions"
 */
public class Transaction {

    /** ID document (auto-generated) */
    private String transactionId;

    /**
     * Loại giao dịch:
     * - "return_success"  : Trả lại đồ vật thành công (+50 FP)
     * - "found_myself"    : Tự tìm thấy (+20 FP)
     * - "post_lost"       : Đăng bài mất đồ (+5 FP)
     * - "post_found"      : Đăng bài tìm thấy (+10 FP)
     * - "receive_5_star"  : Nhận đánh giá 5 sao (+5 FP)
     * - "report_success"  : Báo cáo thành công (+10 FP)
     */
    private String type;

    /** UID người mất đồ (owner) */
    private String fromUser;

    /** UID người nhận điểm (finder) */
    private String toUser;

    /** Số điểm được cộng */
    private int points;

    /** ID bài đăng liên quan */
    private String postId;

    /** Thời gian giao dịch */
    private Timestamp timestamp;

    /** Mô tả ngắn gọn */
    private String description;

    // ── Constructors ──────────────────────────────────────────
    public Transaction() {}

    public Transaction(String type, String fromUser, String toUser,
                       int points, String postId, String description) {
        this.type = type;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.points = points;
        this.postId = postId;
        this.description = description;
        this.timestamp = Timestamp.now();
    }

    // ── Getters & Setters ─────────────────────────────────────
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFromUser() { return fromUser; }
    public void setFromUser(String fromUser) { this.fromUser = fromUser; }

    public String getToUser() { return toUser; }
    public void setToUser(String toUser) { this.toUser = toUser; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
