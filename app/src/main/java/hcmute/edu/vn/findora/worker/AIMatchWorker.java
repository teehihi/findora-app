package hcmute.edu.vn.findora.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import hcmute.edu.vn.findora.AIMatchingHelper;
import hcmute.edu.vn.findora.model.Post;
import hcmute.edu.vn.findora.utils.NotificationHelper;

/**
 * AIMatchWorker - Worker tự động tìm matches định kỳ
 * 
 * CHỨC NĂNG:
 * - Chạy định kỳ mỗi 6 giờ (có thể cấu hình)
 * - Tìm tất cả bài đăng của user hiện tại
 * - Tìm matches mới cho từng bài đăng
 * - Gửi thông báo nếu tìm thấy matches >= 70%
 * 
 * ĐƯỢC SCHEDULE BỞI:
 * - MainActivity.onCreate() - Khi app khởi động
 * - AuthActivity - Sau khi login thành công
 * 
 * LỢI ÍCH:
 * - User không cần mở app vẫn nhận được thông báo matches mới
 * - Tự động tìm kiếm liên tục
 * - Tiết kiệm battery (WorkManager tối ưu hóa)
 * 
 * VÍ DỤ:
 * User A đăng "Mất mèo" lúc 8h sáng
 * → 2h chiều: User B đăng "Tìm thấy mèo"
 * → 2h chiều: Worker chạy, tìm thấy match
 * → User A nhận notification ngay lập tức
 */
public class AIMatchWorker extends Worker {
    
    private static final String TAG = "AIMatchWorker";
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    
    public AIMatchWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }
    
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "AIMatchWorker started - Finding new matches...");
        
        // Kiểm tra user đã login chưa
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No user logged in, skipping match finding");
            return Result.success();
        }
        
        String userId = currentUser.getUid();
        
        try {
            // Sử dụng CountDownLatch để đợi Firestore query hoàn thành
            CountDownLatch latch = new CountDownLatch(1);
            final Result[] result = {Result.success()};
            
            // Load tất cả bài đăng
            db.collection("posts").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    try {
                        List<Post> allPosts = new ArrayList<>();
                        List<Post> userPosts = new ArrayList<>();
                        
                        // Phân loại bài đăng
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Post post = doc.toObject(Post.class);
                            post.setId(doc.getId());
                            allPosts.add(post);
                            
                            // Lưu bài đăng của user hiện tại
                            if (post.getUserId().equals(userId)) {
                                userPosts.add(post);
                            }
                        }
                        
                        Log.d(TAG, String.format("Loaded %d total posts, %d user posts", 
                            allPosts.size(), userPosts.size()));
                        
                        if (userPosts.isEmpty()) {
                            Log.d(TAG, "User has no posts, skipping");
                            result[0] = Result.success();
                            latch.countDown();
                            return;
                        }
                        
                        // Tìm matches cho từng bài đăng của user
                        int totalMatches = 0;
                        int notificationsSent = 0;
                        
                        for (Post userPost : userPosts) {
                            List<AIMatchingHelper.MatchResult> matches = 
                                AIMatchingHelper.findMatches(userPost, allPosts);
                            
                            totalMatches += matches.size();
                            
                            // Gửi thông báo cho matches >= 70%
                            for (AIMatchingHelper.MatchResult match : matches) {
                                if (match.getScorePercentage() >= 70) {
                                    // Kiểm tra xem đã gửi thông báo cho match này chưa
                                    // (Tránh spam notifications)
                                    if (!hasNotificationBeenSent(userId, match.post.getId())) {
                                        NotificationHelper.sendAIMatchNotification(
                                            userId,
                                            match.post.getId(),
                                            match.post.getTitle(),
                                            match.getScorePercentage()
                                        );
                                        
                                        // Đánh dấu đã gửi
                                        markNotificationAsSent(userId, match.post.getId());
                                        
                                        notificationsSent++;
                                        
                                        Log.d(TAG, String.format(
                                            "Sent notification: %s (%d%%) for user post: %s",
                                            match.post.getTitle(),
                                            match.getScorePercentage(),
                                            userPost.getTitle()
                                        ));
                                    }
                                }
                            }
                        }
                        
                        Log.d(TAG, String.format(
                            "Worker completed: %d total matches, %d notifications sent",
                            totalMatches, notificationsSent
                        ));
                        
                        result[0] = Result.success();
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing matches", e);
                        result[0] = Result.failure();
                    } finally {
                        latch.countDown();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load posts", e);
                    result[0] = Result.failure();
                    latch.countDown();
                });
            
            // Đợi tối đa 30 giây
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            if (!completed) {
                Log.e(TAG, "Worker timeout after 30 seconds");
                return Result.failure();
            }
            
            return result[0];
            
        } catch (Exception e) {
            Log.e(TAG, "Worker failed with exception", e);
            return Result.failure();
        }
    }
    
    /**
     * Kiểm tra xem đã gửi thông báo cho match này chưa
     * (Tránh spam notifications cho cùng 1 match)
     */
    private boolean hasNotificationBeenSent(String userId, String matchPostId) {
        // TODO: Implement check in Firestore
        // Có thể lưu vào collection "sent_notifications" với composite key
        // Format: userId_matchPostId
        
        // Tạm thời return false để gửi notification
        // Sau này có thể cải thiện bằng cách check Firestore
        return false;
    }
    
    /**
     * Đánh dấu đã gửi thông báo cho match này
     */
    private void markNotificationAsSent(String userId, String matchPostId) {
        // TODO: Implement marking in Firestore
        // Lưu vào collection "sent_notifications"
        
        // Tạm thời không làm gì
        // Sau này có thể cải thiện
    }
}
