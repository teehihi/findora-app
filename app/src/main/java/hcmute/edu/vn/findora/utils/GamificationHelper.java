package hcmute.edu.vn.findora.utils;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

/**
 * GamificationHelper - Xử lý toàn bộ logic điểm thưởng và cấp bậc.
 *
 * ĐIỂM THƯỞNG:
 * - Đăng bài mất đồ:        +5  FP
 * - Đăng bài tìm thấy:      +10 FP
 * - Trả lại thành công:     +50 FP
 * - Tự tìm thấy:            +20 FP
 * - Nhận đánh giá 5 sao:    +5  FP
 * - Báo cáo thành công:     +10 FP
 *
 * CẤP BẬC:
 * - 0   - 99  FP → Người mới
 * - 100 - 499 FP → Người tốt
 * - 500 - 999 FP → Thiên thần
 * - 1000+     FP → Huyền thoại
 */
public class GamificationHelper {

    private static final String TAG = "GamificationHelper";

    // ── Point Constants ───────────────────────────────────────
    public static final int POST_LOST_CREATED      = 5;
    public static final int POST_FOUND_CREATED     = 10;
    public static final int ITEM_RETURNED_SUCCESS  = 50;
    public static final int ITEM_FOUND_MYSELF      = 20;
    public static final int RECEIVE_5_STAR         = 5;
    public static final int REPORT_SUCCESS         = 10;

    // ── Level Thresholds ──────────────────────────────────────
    public static final int LEVEL_GOOD      = 100;
    public static final int LEVEL_ANGEL     = 500;
    public static final int LEVEL_LEGENDARY = 1000;

    private final FirebaseFirestore db;

    public GamificationHelper() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ─────────────────────────────────────────────────────────
    // CORE: Owner confirms item returned → Batch write
    // ─────────────────────────────────────────────────────────

    /**
     * Xử lý khi Owner xác nhận đã nhận lại đồ vật từ Finder.
     *
     * Thực hiện ATOMIC (Batch):
     * 1. Cập nhật Post: status="resolved", resolvedBy, rating, ratingComment
     * 2. Cộng +50 FP cho Finder, tăng totalReturned
     * 3. Cập nhật averageRating của Finder
     * 4. Kiểm tra và cập nhật level của Finder
     * 5. Tạo Transaction record
     *
     * @param postId        ID bài đăng
     * @param ownerId       UID chủ bài đăng
     * @param finderId      UID người trả lại
     * @param rating        Đánh giá 1-5 sao
     * @param ratingComment Nhận xét (có thể null)
     * @param finderCurrentPoints Điểm hiện tại của finder (để tính level)
     * @param finderTotalRatings  Tổng số lần được đánh giá hiện tại
     * @param finderAverageRating Điểm trung bình hiện tại
     * @param callback      Callback kết quả
     */
    public void confirmItemReturned(
            String postId,
            String ownerId,
            String finderId,
            int rating,
            String ratingComment,
            long finderCurrentPoints,
            int finderTotalRatings,
            double finderAverageRating,
            OnGamificationCallback callback) {

        WriteBatch batch = db.batch();

        // ── 1. Update Post ────────────────────────────────────
        DocumentReference postRef = db.collection("posts").document(postId);
        Map<String, Object> postUpdate = new HashMap<>();
        postUpdate.put("status", "resolved");
        postUpdate.put("resolvedBy", finderId);
        postUpdate.put("rating", rating);
        postUpdate.put("ratingComment", ratingComment != null ? ratingComment : "");
        postUpdate.put("resolvedAt", com.google.firebase.Timestamp.now());
        batch.update(postRef, postUpdate);

        // ── 2 & 3 & 4. Update Finder ──────────────────────────
        DocumentReference finderRef = db.collection("users").document(finderId);

        long newPoints = finderCurrentPoints + ITEM_RETURNED_SUCCESS;
        int newTotalRatings = finderTotalRatings + 1;
        double newAvgRating = ((finderAverageRating * finderTotalRatings) + rating) / newTotalRatings;
        String newLevel = calculateLevel(newPoints);

        Map<String, Object> finderUpdate = new HashMap<>();
        finderUpdate.put("points", newPoints);
        finderUpdate.put("totalReturned", FieldValue.increment(1));
        finderUpdate.put("totalRatings", newTotalRatings);
        finderUpdate.put("averageRating", newAvgRating);
        finderUpdate.put("level", newLevel);

        // Bonus +5 FP nếu nhận 5 sao
        if (rating == 5) {
            finderUpdate.put("points", newPoints + RECEIVE_5_STAR);
        }

        batch.update(finderRef, finderUpdate);

        // ── 5. Create Transaction ─────────────────────────────
        DocumentReference txRef = db.collection("transactions").document();
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("transactionId", txRef.getId());
        transaction.put("type", "return_success");
        transaction.put("fromUser", ownerId);
        transaction.put("toUser", finderId);
        transaction.put("points", rating == 5 ? ITEM_RETURNED_SUCCESS + RECEIVE_5_STAR : ITEM_RETURNED_SUCCESS);
        transaction.put("postId", postId);
        transaction.put("timestamp", com.google.firebase.Timestamp.now());
        transaction.put("description", "Trả lại đồ vật thành công" + (rating == 5 ? " + 5 sao" : ""));
        batch.set(txRef, transaction);

        // ── Commit Batch ──────────────────────────────────────
        batch.commit()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Batch commit success: post resolved, finder +FP");
                if (callback != null) callback.onSuccess(newLevel, newPoints);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Batch commit failed", e);
                if (callback != null) callback.onFailure(e.getMessage());
            });
    }

    // ─────────────────────────────────────────────────────────
    // Award points for post creation
    // ─────────────────────────────────────────────────────────

    /**
     * Cộng điểm khi tạo bài đăng mới.
     * @param userId UID người đăng
     * @param postType "lost" hoặc "found"
     * @param postId ID bài đăng
     */
    public void awardPostCreationPoints(String userId, String postType, String postId) {
        int pts = "found".equals(postType) ? POST_FOUND_CREATED : POST_LOST_CREATED;
        String txType = "found".equals(postType) ? "post_found" : "post_lost";
        String desc = "found".equals(postType) ? "Đăng bài tìm thấy đồ vật" : "Đăng bài mất đồ vật";

        WriteBatch batch = db.batch();

        // Update user points
        DocumentReference userRef = db.collection("users").document(userId);
        batch.update(userRef, "points", FieldValue.increment(pts));

        // Create transaction
        DocumentReference txRef = db.collection("transactions").document();
        Map<String, Object> tx = new HashMap<>();
        tx.put("transactionId", txRef.getId());
        tx.put("type", txType);
        tx.put("fromUser", userId);
        tx.put("toUser", userId);
        tx.put("points", pts);
        tx.put("postId", postId);
        tx.put("timestamp", com.google.firebase.Timestamp.now());
        tx.put("description", desc);
        batch.set(txRef, tx);

        batch.commit()
            .addOnSuccessListener(v -> Log.d(TAG, "Post creation points awarded: +" + pts))
            .addOnFailureListener(e -> Log.e(TAG, "Failed to award post creation points", e));
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    /**
     * Tính cấp bậc dựa trên điểm.
     */
    public static String calculateLevel(long points) {
        if (points >= LEVEL_LEGENDARY) return "Huyền thoại";
        if (points >= LEVEL_ANGEL)     return "Thiên thần";
        if (points >= LEVEL_GOOD)      return "Người tốt";
        return "Người mới";
    }

    /**
     * Tính % tiến độ đến cấp tiếp theo (0-100).
     */
    public static int calculateProgressPercent(long points) {
        if (points >= LEVEL_LEGENDARY) return 100;
        if (points >= LEVEL_ANGEL) {
            return (int) (((points - LEVEL_ANGEL) * 100) / (LEVEL_LEGENDARY - LEVEL_ANGEL));
        }
        if (points >= LEVEL_GOOD) {
            return (int) (((points - LEVEL_GOOD) * 100) / (LEVEL_ANGEL - LEVEL_GOOD));
        }
        return (int) ((points * 100) / LEVEL_GOOD);
    }

    /**
     * Lấy ngưỡng điểm của cấp tiếp theo.
     */
    public static long getNextLevelThreshold(long points) {
        if (points >= LEVEL_LEGENDARY) return LEVEL_LEGENDARY;
        if (points >= LEVEL_ANGEL)     return LEVEL_LEGENDARY;
        if (points >= LEVEL_GOOD)      return LEVEL_ANGEL;
        return LEVEL_GOOD;
    }

    // ─────────────────────────────────────────────────────────
    // Callback Interface
    // ─────────────────────────────────────────────────────────

    public interface OnGamificationCallback {
        void onSuccess(String newLevel, long newPoints);
        void onFailure(String errorMessage);
    }
}
