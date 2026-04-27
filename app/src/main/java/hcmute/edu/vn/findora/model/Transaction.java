package hcmute.edu.vn.findora.model;

import com.google.firebase.Timestamp;

public class Transaction {
    private String id;
    private String userId;
    private String type; // "earn" or "spend"
    private int points;
    private String title;
    private String description;
    private Timestamp timestamp;
    private String relatedPostId;
    private String relatedVoucherId;

    public Transaction() {
        // Required empty constructor for Firestore
    }

    public Transaction(String userId, String type, int points, String title, String description) {
        this.userId = userId;
        this.type = type;
        this.points = points;
        this.title = title;
        this.description = description;
        this.timestamp = Timestamp.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getRelatedPostId() {
        return relatedPostId;
    }

    public void setRelatedPostId(String relatedPostId) {
        this.relatedPostId = relatedPostId;
    }

    public String getRelatedVoucherId() {
        return relatedVoucherId;
    }

    public void setRelatedVoucherId(String relatedVoucherId) {
        this.relatedVoucherId = relatedVoucherId;
    }
}
