package hcmute.edu.vn.findora.utils;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

import hcmute.edu.vn.findora.worker.AIMatchWorker;

/**
 * WorkManagerHelper - Quản lý scheduled tasks
 * 
 * CHỨC NĂNG:
 * - Schedule periodic AI matching (mỗi 6 giờ)
 * - Cancel scheduled tasks
 * - Check task status
 * 
 * SỬ DỤNG:
 * - Gọi từ MainActivity.onCreate() để schedule
 * - Gọi từ AuthActivity sau khi login
 */
public class WorkManagerHelper {
    
    private static final String TAG = "WorkManagerHelper";
    
    // Work tags
    private static final String AI_MATCH_WORK_TAG = "ai_match_periodic";
    private static final String AI_MATCH_WORK_NAME = "ai_match_worker";
    
    // Intervals
    private static final long REPEAT_INTERVAL_HOURS = 6; // Mỗi 6 giờ
    private static final long FLEX_INTERVAL_HOURS = 1;   // Flex time 1 giờ
    
    /**
     * Schedule periodic AI matching
     * 
     * CHỨC NĂNG:
     * - Tạo PeriodicWorkRequest chạy mỗi 6 giờ
     * - Yêu cầu có network connection
     * - Tự động retry nếu fail
     * 
     * ĐƯỢC GỌI TỪ:
     * - MainActivity.onCreate() - Khi app khởi động
     * - AuthActivity - Sau khi login thành công
     * 
     * @param context Application context
     */
    public static void schedulePeriodicAIMatching(Context context) {
        Log.d(TAG, "Scheduling periodic AI matching...");
        
        // Constraints: Chỉ chạy khi có network
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();
        
        // Tạo PeriodicWorkRequest
        PeriodicWorkRequest matchWorkRequest = new PeriodicWorkRequest.Builder(
            AIMatchWorker.class,
            REPEAT_INTERVAL_HOURS, TimeUnit.HOURS,
            FLEX_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(AI_MATCH_WORK_TAG)
            .build();
        
        // Enqueue work (KEEP existing work nếu đã có)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AI_MATCH_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Giữ work cũ nếu đã có
            matchWorkRequest
        );
        
        Log.d(TAG, String.format(
            "Periodic AI matching scheduled: Every %d hours (flex %d hours)",
            REPEAT_INTERVAL_HOURS, FLEX_INTERVAL_HOURS
        ));
    }
    
    /**
     * Cancel periodic AI matching
     * 
     * CHỨC NĂNG:
     * - Hủy scheduled work
     * 
     * ĐƯỢC GỌI TỪ:
     * - Settings - Khi user tắt tính năng
     * - AuthActivity - Khi user logout
     * 
     * @param context Application context
     */
    public static void cancelPeriodicAIMatching(Context context) {
        Log.d(TAG, "Cancelling periodic AI matching...");
        
        WorkManager.getInstance(context)
            .cancelUniqueWork(AI_MATCH_WORK_NAME);
        
        Log.d(TAG, "Periodic AI matching cancelled");
    }
    
    /**
     * Chạy AI matching ngay lập tức (one-time)
     * 
     * CHỨC NĂNG:
     * - Chạy AIMatchWorker ngay lập tức
     * - Không đợi đến lịch định kỳ
     * 
     * ĐƯỢC GỌI TỪ:
     * - Settings - Khi user click "Tìm ngay"
     * - CreatePostActivity - Sau khi tạo bài mới (optional)
     * 
     * @param context Application context
     */
    public static void runAIMatchingNow(Context context) {
        Log.d(TAG, "Running AI matching immediately...");
        
        // Tạo OneTimeWorkRequest
        androidx.work.OneTimeWorkRequest matchWorkRequest = 
            new androidx.work.OneTimeWorkRequest.Builder(AIMatchWorker.class)
                .addTag(AI_MATCH_WORK_TAG)
                .build();
        
        WorkManager.getInstance(context).enqueue(matchWorkRequest);
        
        Log.d(TAG, "AI matching started immediately");
    }
    
    /**
     * Kiểm tra xem periodic work có đang chạy không
     * 
     * @param context Application context
     * @param callback Callback nhận kết quả
     */
    public static void isPeriodicWorkScheduled(Context context, WorkStatusCallback callback) {
        try {
            List<androidx.work.WorkInfo> workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(AI_MATCH_WORK_NAME)
                .get(); // Blocking call - use with caution
            
            boolean isScheduled = !workInfos.isEmpty() && 
                workInfos.get(0).getState() != androidx.work.WorkInfo.State.CANCELLED;
            callback.onStatusReceived(isScheduled);
        } catch (Exception e) {
            Log.e(TAG, "Failed to check work status", e);
            callback.onStatusReceived(false);
        }
    }
    
    public interface WorkStatusCallback {
        void onStatusReceived(boolean isScheduled);
    }
}
