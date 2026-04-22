# 🔔 Hệ thống Notification nâng cao - Findora

## 📋 Tổng quan

Hệ thống notification đã được nâng cấp với các tính năng:

1. ✅ **In-app Notification Center** - Trung tâm thông báo trong ứng dụng
2. ✅ **Realtime Badge** - Hiển thị số thông báo chưa đọc realtime
3. ✅ **Multiple Notification Types** - Nhiều loại thông báo
4. ✅ **Push Notifications** - Thông báo đẩy (FCM)
5. ✅ **Auto-send on events** - Tự động gửi khi có sự kiện

---

## 🎯 Các loại thông báo

### 1. AI Match Notification (ai_match)
- **Khi nào**: AI tìm thấy bài đăng phù hợp (>= 70%)
- **Gửi từ**: `AIMatchWorker.java` (chạy định kỳ mỗi 6 giờ)
- **Icon**: ⚡ AI Sparkle
- **Màu**: Blue (#0057BD)
- **Action**: Mở `PostDetailActivity` với bài đăng match

### 2. New Message Notification (new_message)
- **Khi nào**: Có tin nhắn mới trong chat
- **Gửi từ**: `ChatActivity.java` (khi gửi tin nhắn)
- **Icon**: 💬 Chat
- **Màu**: Green (#22C55E)
- **Action**: Mở `ChatActivity` với cuộc trò chuyện

### 3. Comment Notification (comment)
- **Khi nào**: Có người comment bài đăng
- **Gửi từ**: `PostDetailActivity.java` (khi thêm comment)
- **Icon**: 💭 Comment
- **Màu**: Gray
- **Action**: Mở `PostDetailActivity` với bài đăng
- **Note**: Chưa implement (cần thêm tính năng comment)

### 4. Like Notification (like)
- **Khi nào**: Có người like bài đăng
- **Gửi từ**: `PostDetailActivity.java` (khi like)
- **Icon**: ❤️ Heart
- **Màu**: Red (#EF4444)
- **Action**: Mở `PostDetailActivity` với bài đăng
- **Note**: Chưa implement (cần thêm tính năng like)

### 5. Post Found Notification (post_found)
- **Khi nào**: Đồ vật của bạn được tìm thấy
- **Gửi từ**: Manual trigger hoặc AI matching
- **Icon**: ✅ Check Circle
- **Màu**: Green
- **Action**: Mở `PostDetailActivity` với bài đăng

### 6. System Notification (system)
- **Khi nào**: Thông báo hệ thống (cập nhật, bảo trì, v.v.)
- **Gửi từ**: Admin hoặc Cloud Functions
- **Icon**: 🔔 Bell
- **Màu**: Gray
- **Action**: Mở `MainActivity`

---

## 📁 Cấu trúc file

### Models
```
app/src/main/java/hcmute/edu/vn/findora/model/
├── Notification.java          # Model thông báo
└── ChatMessage.java           # Model tin nhắn (đã có)
```

### Activities
```
app/src/main/java/hcmute/edu/vn/findora/
├── NotificationActivity.java  # Màn hình notification center
├── MainActivity.java          # Thêm badge và click handler
└── ChatActivity.java          # Gửi notification khi chat
```

### Adapters
```
app/src/main/java/hcmute/edu/vn/findora/adapter/
└── NotificationAdapter.java   # Adapter hiển thị notifications
```

### Utils
```
app/src/main/java/hcmute/edu/vn/findora/utils/
└── NotificationHelper.java    # Helper gửi notifications (đã cập nhật)
```

### Layouts
```
app/src/main/res/layout/
├── activity_notification.xml  # Layout notification center
└── item_notification.xml      # Layout item notification
```

### Drawables
```
app/src/main/res/drawable/
├── bg_notification_badge.xml  # Badge đỏ cho số thông báo
├── bg_unread_dot.xml          # Chấm xanh cho unread
├── ic_comment.xml             # Icon comment
└── ic_check_circle.xml        # Icon check
```

---

## 🔥 Cách sử dụng

### 1. Gửi AI Match Notification
```java
// Tự động gửi từ AIMatchWorker
NotificationHelper.sendAIMatchNotification(
    userId,           // ID người nhận
    matchPostId,      // ID bài đăng match
    matchTitle,       // Tiêu đề bài đăng
    matchScore        // Điểm match (0-100)
);
```

### 2. Gửi Message Notification
```java
// Tự động gửi từ ChatActivity khi gửi tin nhắn
NotificationHelper.sendMessageNotification(
    otherUserId,      // ID người nhận
    currentUserId,    // ID người gửi
    senderName,       // Tên người gửi
    messageText,      // Nội dung tin nhắn
    chatId            // ID cuộc trò chuyện
);
```

### 3. Gửi Comment Notification (TODO)
```java
// Gọi khi user comment bài đăng
NotificationHelper.sendCommentNotification(
    postOwnerId,      // ID chủ bài đăng
    postId,           // ID bài đăng
    postTitle,        // Tiêu đề bài đăng
    commenterId,      // ID người comment
    commenterName,    // Tên người comment
    commenterAvatar,  // Avatar người comment
    commentText       // Nội dung comment
);
```

### 4. Gửi Like Notification (TODO)
```java
// Gọi khi user like bài đăng
NotificationHelper.sendLikeNotification(
    postOwnerId,      // ID chủ bài đăng
    postId,           // ID bài đăng
    postTitle,        // Tiêu đề bài đăng
    likerId,          // ID người like
    likerName,        // Tên người like
    likerAvatar       // Avatar người like
);
```

### 5. Gửi System Notification
```java
// Gọi từ admin hoặc Cloud Functions
NotificationHelper.sendSystemNotification(
    userId,           // ID người nhận
    title,            // Tiêu đề
    body              // Nội dung
);
```

### 6. Đánh dấu đã đọc
```java
// Tự động khi click vào notification
NotificationHelper.markAsRead(notificationId);

// Hoặc đánh dấu tất cả
NotificationHelper.markAllAsRead(userId);
```

### 7. Lấy số thông báo chưa đọc
```java
// Realtime listener (đã implement trong MainActivity)
db.collection("notifications")
    .whereEqualTo("userId", currentUserId)
    .whereEqualTo("read", false)
    .addSnapshotListener((snapshots, error) -> {
        int count = snapshots.size();
        updateBadge(count);
    });
```

---

## 🗄️ Firestore Structure

### Collection: `notifications`
```javascript
{
  "id": "notif123",
  "type": "ai_match",           // Loại thông báo
  "userId": "user456",          // Người nhận
  "title": "🔥 Tìm thấy gợi ý phù hợp!",
  "body": "Mèo vàng - Độ phù hợp 95%",
  "timestamp": Timestamp,
  "read": false,
  
  // Optional fields
  "postId": "post789",          // Cho ai_match, comment, like
  "chatId": "chat012",          // Cho new_message
  "senderId": "user345",        // Cho new_message, comment, like
  "senderName": "Nguyễn Văn A",
  "senderAvatar": "https://...",
  "matchScore": 95              // Cho ai_match
}
```

### Indexes cần tạo
Vào Firebase Console > Firestore > Indexes và tạo:

1. **Index cho load notifications**
   - Collection: `notifications`
   - Fields: `userId` (Ascending), `timestamp` (Descending)

2. **Index cho unread count**
   - Collection: `notifications`
   - Fields: `userId` (Ascending), `read` (Ascending)

---

## 🎨 UI Components

### 1. Notification Badge (MainActivity)
- Hiển thị số thông báo chưa đọc
- Realtime update
- Màu đỏ (#EF4444)
- Tối đa hiển thị "99+"

### 2. Notification Center (NotificationActivity)
- Danh sách tất cả thông báo
- Sắp xếp theo thời gian (mới nhất trước)
- Hiển thị unread indicator (chấm xanh)
- Background khác cho unread (#F0F7FF)
- Nút "Đánh dấu tất cả đã đọc"

### 3. Notification Item
- Icon tương ứng loại thông báo
- Avatar người gửi (cho message, comment, like)
- Title và body
- Thời gian relative ("2 giờ trước")
- Unread indicator

---

## 🚀 Tính năng cần thêm (TODO)

### 1. Comment System
- [ ] Thêm collection `comments` trong Firestore
- [ ] UI comment trong `PostDetailActivity`
- [ ] Gửi notification khi comment
- [ ] Hiển thị số comment trong post card

### 2. Like System
- [ ] Thêm field `likes` (array) trong Post model
- [ ] UI like button trong `PostDetailActivity`
- [ ] Gửi notification khi like
- [ ] Hiển thị số like trong post card

### 3. Push Notifications (FCM)
- [ ] Setup Cloud Functions để gửi FCM
- [ ] Lưu FCM token vào Firestore khi login
- [ ] Trigger FCM khi có notification mới
- [ ] Handle notification click từ system tray

### 4. Notification Settings
- [ ] Màn hình settings cho notifications
- [ ] Bật/tắt từng loại notification
- [ ] Bật/tắt push notifications
- [ ] Bật/tắt sound/vibration

### 5. Notification Grouping
- [ ] Group notifications theo loại
- [ ] "Bạn có 5 tin nhắn mới"
- [ ] "Bạn có 3 gợi ý AI mới"

### 6. Mark as Read Optimization
- [ ] Batch update để tránh spam Firestore
- [ ] Debounce mark as read
- [ ] Offline support

---

## 🔧 Testing

### Test AI Match Notification
1. Đăng bài "Mất mèo vàng" (user A)
2. Đăng bài "Tìm thấy mèo vàng" (user B)
3. Click "Refresh Matches" trong MainActivity
4. User A sẽ nhận notification

### Test Message Notification
1. User A mở chat với User B
2. User A gửi tin nhắn
3. User B sẽ nhận notification (nếu không đang mở chat)

### Test Badge Update
1. Gửi notification cho user
2. Badge sẽ tự động update (realtime)
3. Click vào notification
4. Badge giảm đi 1

---

## 📊 Performance Considerations

### 1. Firestore Reads
- Sử dụng `limit(100)` để giới hạn số notifications load
- Implement pagination nếu cần
- Cache notifications đã load

### 2. Realtime Listeners
- Chỉ listen khi app ở foreground
- Detach listener khi activity destroy
- Sử dụng `whereEqualTo("read", false)` để giảm data transfer

### 3. Notification Spam Prevention
- Không gửi notification cho chính mình
- Check duplicate trước khi gửi (trong AIMatchWorker)
- Debounce notification sending

---

## 🐛 Known Issues

1. **FCM not implemented**: Push notifications chưa hoạt động (cần Cloud Functions)
2. **Comment/Like not implemented**: Cần thêm tính năng trước khi test notifications
3. **Notification sound**: Chưa có custom sound
4. **Notification grouping**: Chưa group notifications

---

## 📝 Notes

- Tất cả notifications được lưu vào Firestore collection `notifications`
- Realtime listener đảm bảo badge update ngay lập tức
- Notification icon và màu sắc theo Material Design 3
- Hỗ trợ dark mode (cần thêm theme)

---

## 🎉 Kết luận

Hệ thống notification đã được implement với đầy đủ tính năng cơ bản:
- ✅ In-app notification center
- ✅ Realtime badge
- ✅ Multiple notification types
- ✅ Auto-send on chat
- ✅ Auto-send on AI match

Các tính năng nâng cao (comment, like, FCM) có thể thêm sau khi có yêu cầu cụ thể.
