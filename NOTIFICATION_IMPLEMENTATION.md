# 🔔 Hệ thống Thông báo Findora

## ✅ Đã implement

### 1. Firebase Cloud Messaging Service
**File**: `FirebaseMessagingService.java`

**Chức năng**:
- Nhận thông báo từ FCM
- Hiển thị notification với icon và màu sắc phù hợp
- Xử lý click vào notification (mở đúng màn hình)
- Tự động lưu FCM token vào Firestore

**3 loại notification channels**:
1. **AI Match** (Ưu tiên cao) - Icon: ⚡ Blue
2. **Messages** (Ưu tiên cao) - Icon: 💬 Green  
3. **System** (Ưu tiên trung bình) - Icon: 🔔 Gray

### 2. Notification Helper
**File**: `NotificationHelper.java`

**3 hàm chính**:

```java
// 1. Gửi thông báo AI match
NotificationHelper.sendAIMatchNotification(
    userId,           // User nhận thông báo
    matchPostId,      // ID bài đăng match
    matchTitle,       // Tiêu đề bài match
    matchScore        // Điểm % (70-100)
);

// 2. Gửi thông báo tin nhắn
NotificationHelper.sendMessageNotification(
    userId,           // User nhận
    senderId,         // Người gửi
    senderName,       // Tên người gửi
    message,          // Nội dung
    chatId            // ID cuộc trò chuyện
);

// 3. Gửi thông báo hệ thống
NotificationHelper.sendSystemNotification(
    userId,           // User nhận
    title,            // Tiêu đề
    body              // Nội dung
);
```

### 3. Tự động gửi AI Match Notification
**File**: `CreatePostActivity.java` → `findMatchesForNewPost()`

**Flow**:
1. User tạo bài đăng mới
2. Bài đăng được lưu vào Firestore
3. Tự động load tất cả bài đăng
4. Sử dụng `AIMatchingHelper.findMatches()` để tìm matches
5. Với mỗi match >= 70%:
   - Gửi thông báo cho user có bài match
   - "🔥 Tìm thấy gợi ý phù hợp 85%: Mèo vàng ở Thủ Đức"

### 4. Hiển thị số thông báo chưa đọc
**File**: `MainActivity.java` → `loadUnreadNotificationCount()`

**Chức năng**:
- Load số lượng notifications chưa đọc
- Hiển thị badge trên notification icon
- Tự động refresh khi vào màn hình

### 5. Permissions & Manifest
**File**: `AndroidManifest.xml`

**Đã thêm**:
- `POST_NOTIFICATIONS` permission (Android 13+)
- Firebase Messaging Service registration
- Intent filters cho FCM

## 📱 Cách hoạt động

### Khi có bài đăng mới match:

```
User A đăng: "Mất mèo vàng ở Thủ Đức"
    ↓
AI tìm thấy match với bài của User B: "Tìm thấy mèo vàng ở Thủ Đức" (85%)
    ↓
Gửi notification cho User B:
    Title: "🔥 Tìm thấy gợi ý phù hợp!"
    Body: "Mất mèo vàng ở Thủ Đức - Độ phù hợp 85%"
    ↓
User B click vào notification
    ↓
Mở PostDetailActivity với bài đăng của User A
```

### Khi có tin nhắn mới:

```
User A gửi tin nhắn: "Bạn còn giữ đồ không?"
    ↓
Gửi notification cho User B:
    Title: "Nguyễn Văn A"
    Body: "Bạn còn giữ đồ không?"
    ↓
User B click vào notification
    ↓
Mở ChatActivity với User A
```

## 🚧 Cần làm thêm

### 1. Setup FCM Cloud Function (Quan trọng!)

Hiện tại notifications chỉ được lưu vào Firestore, chưa gửi push notification thực sự.

**Cần tạo Cloud Function**:

```javascript
// functions/index.js
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.sendNotification = functions.firestore
    .document('notifications/{notificationId}')
    .onCreate(async (snap, context) => {
        const notification = snap.data();
        const userId = notification.userId;
        
        // Lấy FCM token của user
        const userDoc = await admin.firestore()
            .collection('users')
            .doc(userId)
            .get();
        
        if (!userDoc.exists) return;
        
        const fcmToken = userDoc.data().fcmToken;
        if (!fcmToken) return;
        
        // Gửi notification
        const message = {
            token: fcmToken,
            notification: {
                title: notification.title,
                body: notification.body
            },
            data: {
                type: notification.type,
                postId: notification.postId || '',
                chatId: notification.chatId || '',
                senderId: notification.senderId || ''
            },
            android: {
                priority: 'high',
                notification: {
                    channelId: getChannelId(notification.type),
                    sound: 'default'
                }
            }
        };
        
        try {
            await admin.messaging().send(message);
            console.log('Notification sent successfully');
        } catch (error) {
            console.error('Error sending notification:', error);
        }
    });

function getChannelId(type) {
    switch(type) {
        case 'ai_match': return 'ai_match';
        case 'new_message': return 'messages';
        default: return 'system';
    }
}
```

**Deploy Cloud Function**:
```bash
cd functions
npm install firebase-functions firebase-admin
firebase deploy --only functions
```

### 2. Lưu FCM Token khi login

**File**: `AuthActivity.java`

Thêm sau khi login thành công:

```java
// Sau khi login thành công
com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
    .addOnCompleteListener(task -> {
        if (task.isSuccessful()) {
            String token = task.getResult();
            
            // Lưu vào Firestore
            db.collection("users")
                .document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> 
                    Log.d(TAG, "FCM token saved"))
                .addOnFailureListener(e -> 
                    Log.e(TAG, "Failed to save FCM token", e));
        }
    });
```

### 3. Request Notification Permission (Android 13+)

**File**: `MainActivity.java`

Thêm vào `onCreate()`:

```java
// Request notification permission for Android 13+
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (ContextCompat.checkSelfPermission(this, 
            Manifest.permission.POST_NOTIFICATIONS) 
            != PackageManager.PERMISSION_GRANTED) {
        
        ActivityCompat.requestPermissions(this,
            new String[]{Manifest.permission.POST_NOTIFICATIONS},
            NOTIFICATION_PERMISSION_REQUEST_CODE);
    }
}
```

### 4. Gửi thông báo tin nhắn

**File**: `ChatActivity.java`

Thêm vào hàm `sendMessage()`:

```java
// Sau khi gửi tin nhắn thành công
NotificationHelper.sendMessageNotification(
    otherUserId,      // User nhận
    currentUserId,    // Người gửi
    currentUserName,  // Tên người gửi
    messageText,      // Nội dung
    chatId            // ID chat
);
```

### 5. Tạo màn hình Notifications

**File**: `NotificationsActivity.java` (Mới)

**Chức năng**:
- Hiển thị danh sách notifications
- Phân loại theo type (AI Match, Messages, System)
- Đánh dấu đã đọc khi click
- Pull to refresh

**Layout**:
```
┌─────────────────────────┐
│  Notifications          │
├─────────────────────────┤
│ 🔥 AI Match             │
│ Tìm thấy gợi ý phù hợp  │
│ Mèo vàng - 85%          │
│ 2 giờ trước             │
├─────────────────────────┤
│ 💬 Nguyễn Văn A         │
│ Bạn còn giữ đồ không?   │
│ 5 giờ trước             │
├─────────────────────────┤
│ 🔔 Hệ thống             │
│ Có người xem bài của bạn│
│ 1 ngày trước            │
└─────────────────────────┘
```

## 📊 Testing

### Test AI Match Notification:

1. User A login và đăng bài "Mất mèo vàng ở Thủ Đức"
2. User B login và đăng bài "Tìm thấy mèo vàng ở Thủ Đức"
3. User A sẽ nhận notification: "🔥 Tìm thấy gợi ý phù hợp 85%"
4. Click vào notification → Mở bài đăng của User B

### Test Message Notification:

1. User A gửi tin nhắn cho User B
2. User B nhận notification: "Nguyễn Văn A: Bạn còn giữ đồ không?"
3. Click vào notification → Mở ChatActivity với User A

### Test trong Logcat:

```bash
adb logcat | grep -E "FCMService|NotificationHelper|CreatePost"
```

Sẽ thấy:
```
CreatePost: Sent AI match notification to user xyz: Mèo vàng (85%)
NotificationHelper: AI match notification sent: abc123
FCMService: Message received from Firebase
```

## 🎯 Kết luận

**Đã hoàn thành**:
- ✅ Firebase Messaging Service
- ✅ Notification Helper với 3 loại thông báo
- ✅ Tự động gửi AI match notification khi tạo bài mới
- ✅ Hiển thị số notifications chưa đọc
- ✅ Permissions và Manifest setup

**Cần làm tiếp**:
- 🚧 Setup FCM Cloud Function (quan trọng nhất!)
- 🚧 Lưu FCM token khi login
- 🚧 Request notification permission Android 13+
- 🚧 Gửi notification khi có tin nhắn mới
- 🚧 Tạo màn hình Notifications

**Ưu tiên**:
1. Setup Cloud Function (để push notification hoạt động)
2. Lưu FCM token (để có thể gửi notification)
3. Các tính năng khác

**Thời gian ước tính**:
- Cloud Function setup: 30 phút
- FCM token handling: 15 phút
- Notification screen: 1-2 giờ
- Testing: 30 phút

**Tổng**: ~3 giờ để hoàn thiện hệ thống notification
