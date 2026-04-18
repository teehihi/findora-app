# 🔄 WorkManager - Lập lịch quét tự động

## ✅ Đã implement

### 1. AIMatchWorker
**File**: `worker/AIMatchWorker.java`

**Chức năng**:
- Tự động chạy mỗi 6 giờ
- Tìm tất cả bài đăng của user
- Tìm matches mới (>= 70%)
- Gửi notifications tự động

**Flow**:
```
Worker chạy (mỗi 6h)
    ↓
Load tất cả posts từ Firestore
    ↓
Lọc posts của user hiện tại
    ↓
Với mỗi post của user:
    → Tìm matches bằng AIMatchingHelper
    → Nếu match >= 70%:
        → Gửi notification
        → Log kết quả
    ↓
Worker hoàn thành
```

### 2. WorkManagerHelper
**File**: `utils/WorkManagerHelper.java`

**3 hàm chính**:

```java
// 1. Schedule periodic matching (mỗi 6 giờ)
WorkManagerHelper.schedulePeriodicAIMatching(context);

// 2. Chạy ngay lập tức (one-time)
WorkManagerHelper.runAIMatchingNow(context);

// 3. Cancel scheduled work
WorkManagerHelper.cancelPeriodicAIMatching(context);
```

### 3. Tự động kích hoạt
**File**: `MainActivity.java`

**Khi nào**:
- ✅ Khi app khởi động (onCreate)
- ✅ Khi user login (AuthActivity)
- ✅ Khi user click nút "Tìm ngay" (🔄 icon)

## 📱 Cách hoạt động

### Scenario 1: Tự động tìm matches định kỳ

```
8:00 AM - User A đăng "Mất mèo vàng ở Thủ Đức"
    ↓
2:00 PM - User B đăng "Tìm thấy mèo vàng ở Thủ Đức"
    ↓
2:00 PM - Worker chạy (đúng lịch 6h)
    ↓
2:00 PM - Tìm thấy match 85%
    ↓
2:00 PM - User A nhận notification:
          "🔥 Tìm thấy gợi ý phù hợp 85%: Tìm thấy mèo vàng ở Thủ Đức"
    ↓
User A click notification → Mở bài của User B
```

### Scenario 2: Tìm ngay lập tức

```
User click nút 🔄 "Tìm ngay"
    ↓
WorkManager chạy AIMatchWorker ngay lập tức
    ↓
Tìm matches trong vòng 5-10 giây
    ↓
Gửi notifications nếu có matches
    ↓
Toast: "Đã tìm thấy X gợi ý mới!"
```

## ⚙️ Cấu hình

### Thay đổi tần suất quét

**File**: `WorkManagerHelper.java`

```java
// Hiện tại: Mỗi 6 giờ
private static final long REPEAT_INTERVAL_HOURS = 6;

// Thay đổi thành:
private static final long REPEAT_INTERVAL_HOURS = 3;  // Mỗi 3 giờ
private static final long REPEAT_INTERVAL_HOURS = 12; // Mỗi 12 giờ
private static final long REPEAT_INTERVAL_HOURS = 24; // Mỗi ngày
```

**Lưu ý**: WorkManager yêu cầu tối thiểu 15 phút cho PeriodicWork.

### Thay đổi ngưỡng match

**File**: `AIMatchWorker.java`

```java
// Hiện tại: Gửi notification nếu match >= 70%
if (match.getScorePercentage() >= 70) {
    // Send notification
}

// Thay đổi thành:
if (match.getScorePercentage() >= 60) {  // Dễ hơn
if (match.getScorePercentage() >= 80) {  // Khó hơn
```

### Thêm constraints

**File**: `WorkManagerHelper.java`

```java
Constraints constraints = new Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)  // Có sẵn
    .setRequiresBatteryNotLow(true)                 // Thêm: Pin > 15%
    .setRequiresCharging(true)                      // Thêm: Đang sạc
    .setRequiresDeviceIdle(true)                    // Thêm: Device idle
    .build();
```

## 🧪 Testing

### 1. Test trong Logcat

```bash
# Xem logs của Worker
adb logcat | grep -E "AIMatchWorker|WorkManagerHelper"
```

**Output mong đợi**:
```
WorkManagerHelper: Scheduling periodic AI matching...
WorkManagerHelper: Periodic AI matching scheduled: Every 6 hours (flex 1 hours)
AIMatchWorker: AIMatchWorker started - Finding new matches...
AIMatchWorker: Loaded 25 total posts, 3 user posts
AIMatchWorker: Sent notification: Mèo vàng (85%) for user post: Mất mèo
AIMatchWorker: Worker completed: 5 total matches, 2 notifications sent
```

### 2. Test chạy ngay lập tức

**Cách 1**: Click nút 🔄 trong app

**Cách 2**: Dùng adb
```bash
# Force run worker
adb shell am broadcast -a androidx.work.diagnostics.REQUEST_DIAGNOSTICS \
    -p hcmute.edu.vn.findora
```

### 3. Kiểm tra work status

**Trong code**:
```java
WorkManagerHelper.isPeriodicWorkScheduled(this, isScheduled -> {
    Log.d(TAG, "Periodic work scheduled: " + isScheduled);
});
```

**Dùng WorkManager Inspector** (Android Studio):
1. View → Tool Windows → App Inspection
2. Chọn tab "WorkManager"
3. Xem danh sách scheduled works

### 4. Test với thời gian ngắn hơn

**Chỉ dùng cho testing** (không deploy production):

```java
// Thay đổi tạm thời thành 15 phút (minimum)
PeriodicWorkRequest matchWorkRequest = new PeriodicWorkRequest.Builder(
    AIMatchWorker.class,
    15, TimeUnit.MINUTES  // Thay vì 6 giờ
)
```

## 📊 Monitoring

### 1. Xem work history

```java
WorkManager.getInstance(context)
    .getWorkInfosByTag(AI_MATCH_WORK_TAG)
    .addOnSuccessListener(workInfos -> {
        for (WorkInfo workInfo : workInfos) {
            Log.d(TAG, "Work state: " + workInfo.getState());
            Log.d(TAG, "Run attempt: " + workInfo.getRunAttemptCount());
        }
    });
```

### 2. Track success rate

**Thêm vào AIMatchWorker**:
```java
// Lưu metrics vào Firestore
Map<String, Object> metrics = new HashMap<>();
metrics.put("timestamp", Timestamp.now());
metrics.put("totalMatches", totalMatches);
metrics.put("notificationsSent", notificationsSent);
metrics.put("executionTime", executionTime);

db.collection("worker_metrics").add(metrics);
```

### 3. Firebase Analytics

```java
// Track worker execution
Bundle bundle = new Bundle();
bundle.putInt("matches_found", totalMatches);
bundle.putInt("notifications_sent", notificationsSent);
FirebaseAnalytics.getInstance(context)
    .logEvent("ai_worker_completed", bundle);
```

## 🚀 Tối ưu hóa

### 1. Tránh spam notifications

**Vấn đề**: Cùng 1 match gửi nhiều lần

**Giải pháp**: Track sent notifications

```java
// Trong AIMatchWorker
private boolean hasNotificationBeenSent(String userId, String matchPostId) {
    // Check trong Firestore collection "sent_notifications"
    String key = userId + "_" + matchPostId;
    
    // Query Firestore
    DocumentSnapshot doc = db.collection("sent_notifications")
        .document(key)
        .get()
        .getResult();
    
    return doc.exists();
}

private void markNotificationAsSent(String userId, String matchPostId) {
    String key = userId + "_" + matchPostId;
    
    Map<String, Object> data = new HashMap<>();
    data.put("userId", userId);
    data.put("matchPostId", matchPostId);
    data.put("sentAt", Timestamp.now());
    
    db.collection("sent_notifications")
        .document(key)
        .set(data);
}
```

### 2. Batch notifications

**Vấn đề**: Nhiều notifications cùng lúc → spam

**Giải pháp**: Nhóm thành 1 notification

```java
// Thay vì gửi từng notification
// Gửi 1 notification tổng hợp:
NotificationHelper.sendSystemNotification(
    userId,
    "🔥 Tìm thấy gợi ý mới!",
    String.format("Có %d bài đăng phù hợp với bạn", totalMatches)
);
```

### 3. Smart scheduling

**Vấn đề**: Chạy vào lúc user ngủ → lãng phí

**Giải pháp**: Chạy vào giờ cao điểm

```java
// Chỉ chạy từ 8h sáng đến 10h tối
Calendar calendar = Calendar.getInstance();
int hour = calendar.get(Calendar.HOUR_OF_DAY);

if (hour < 8 || hour > 22) {
    Log.d(TAG, "Outside active hours, skipping");
    return Result.retry(); // Retry sau
}
```

## 🔧 Troubleshooting

### Worker không chạy?

**1. Kiểm tra constraints**:
```java
// Có network không?
ConnectivityManager cm = (ConnectivityManager) 
    context.getSystemService(Context.CONNECTIVITY_SERVICE);
NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
boolean isConnected = activeNetwork != null && 
    activeNetwork.isConnectedOrConnecting();
```

**2. Kiểm tra battery optimization**:
- Settings → Apps → Findora → Battery → Unrestricted

**3. Kiểm tra work state**:
```java
WorkManager.getInstance(context)
    .getWorkInfosForUniqueWork(AI_MATCH_WORK_NAME)
    .addOnSuccessListener(workInfos -> {
        if (!workInfos.isEmpty()) {
            WorkInfo workInfo = workInfos.get(0);
            Log.d(TAG, "State: " + workInfo.getState());
            Log.d(TAG, "Next run: " + workInfo.getNextScheduleTimeMillis());
        }
    });
```

### Worker chạy nhưng không gửi notification?

**1. Kiểm tra user login**:
```java
FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
if (user == null) {
    Log.e(TAG, "User not logged in!");
}
```

**2. Kiểm tra có posts không**:
```java
if (userPosts.isEmpty()) {
    Log.w(TAG, "User has no posts");
}
```

**3. Kiểm tra match score**:
```java
Log.d(TAG, "Match score: " + match.getScorePercentage() + "%");
// Phải >= 70% mới gửi notification
```

## 📈 Metrics quan trọng

### 1. Worker execution rate
- Số lần worker chạy thành công / tổng số lần schedule
- Target: > 95%

### 2. Match discovery rate
- Số matches tìm được / số lần worker chạy
- Target: > 20%

### 3. Notification engagement
- Số notifications được click / tổng số gửi
- Target: > 30%

### 4. Battery impact
- Battery usage của WorkManager
- Target: < 2% per day

## 🎯 Best Practices

### 1. ✅ DO
- Schedule work khi app khởi động
- Cancel work khi user logout
- Log execution results
- Handle failures gracefully
- Use constraints để tiết kiệm battery

### 2. ❌ DON'T
- Chạy quá thường xuyên (< 6 giờ)
- Gửi quá nhiều notifications
- Block main thread
- Ignore battery optimization
- Hardcode values (dùng constants)

## 🔮 Future Enhancements

### 1. Smart frequency
- Tăng tần suất nếu user active
- Giảm tần suất nếu user inactive

### 2. ML-based scheduling
- Học thói quen user
- Chạy vào lúc user thường mở app

### 3. Priority matching
- Ưu tiên matches gần user
- Ưu tiên matches mới

### 4. Batch processing
- Xử lý nhiều users cùng lúc
- Tối ưu Firestore queries

## 📝 Checklist

### Setup (✅ Done)
- [x] Add WorkManager dependency
- [x] Create AIMatchWorker
- [x] Create WorkManagerHelper
- [x] Schedule in MainActivity
- [x] Add refresh button

### Testing (🚧 Next)
- [ ] Test periodic execution
- [ ] Test immediate execution
- [ ] Test with multiple users
- [ ] Test notification delivery
- [ ] Test battery impact

### Optimization (🔮 Future)
- [ ] Implement notification deduplication
- [ ] Add batch notifications
- [ ] Add smart scheduling
- [ ] Add metrics tracking
- [ ] Add error reporting

## 🎉 Kết luận

**Đã có**:
- ✅ Tự động quét mỗi 6 giờ
- ✅ Nút "Tìm ngay" để test
- ✅ Gửi notifications tự động
- ✅ Tiết kiệm battery (WorkManager tối ưu)

**Lợi ích**:
- User không cần mở app vẫn nhận notifications
- Tăng engagement và retention
- Tăng tỷ lệ tìm lại đồ thành công

**Next steps**:
1. Test với real users
2. Monitor metrics
3. Optimize dựa trên feedback
4. Add advanced features
