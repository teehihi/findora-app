# 🔔 Notification System - Quick Start

## ✅ Đã implement

### 1. In-app Notification Center
- Màn hình `NotificationActivity` hiển thị tất cả thông báo
- Click vào notification icon (chuông) ở MainActivity để mở
- Badge đỏ hiển thị số thông báo chưa đọc (realtime)

### 2. Các loại thông báo
- **AI Match** (⚡): Tìm thấy bài đăng phù hợp
- **New Message** (💬): Tin nhắn mới
- **Comment** (💭): Có người comment (chưa có UI comment)
- **Like** (❤️): Có người like (chưa có UI like)
- **Post Found** (✅): Đồ vật được tìm thấy
- **System** (🔔): Thông báo hệ thống

### 3. Tự động gửi notification
- ✅ Khi có tin nhắn mới (ChatActivity)
- ✅ Khi AI tìm thấy match (AIMatchWorker - chạy mỗi 6 giờ)

## 🚀 Cách test

### Test 1: Message Notification
1. User A login
2. User A chat với User B
3. User A gửi tin nhắn
4. User B sẽ thấy badge tăng lên
5. User B mở notification center → thấy thông báo tin nhắn

### Test 2: AI Match Notification
1. User A đăng bài "Mất mèo vàng"
2. User B đăng bài "Tìm thấy mèo vàng"
3. Click nút "Refresh" (⟳) ở MainActivity
4. User A sẽ nhận notification AI match

### Test 3: Badge Realtime
1. Gửi notification cho user
2. Badge tự động update (không cần refresh)
3. Click vào notification
4. Badge giảm đi

## 📋 TODO - Tính năng cần thêm

### 1. Comment System (để test comment notification)
```java
// Trong PostDetailActivity, thêm:
NotificationHelper.sendCommentNotification(
    post.getUserId(),
    postId,
    post.getTitle(),
    currentUserId,
    currentUserName,
    currentUserAvatar,
    commentText
);
```

### 2. Like System (để test like notification)
```java
// Trong PostDetailActivity, thêm:
NotificationHelper.sendLikeNotification(
    post.getUserId(),
    postId,
    post.getTitle(),
    currentUserId,
    currentUserName,
    currentUserAvatar
);
```

### 3. Push Notifications (FCM)
- Cần setup Cloud Functions
- Gửi FCM khi có notification mới
- Handle click từ system tray

## 🗄️ Firestore Setup

### Tạo Indexes (bắt buộc)
Vào Firebase Console > Firestore > Indexes:

1. **notifications** collection
   - `userId` (Ascending) + `timestamp` (Descending)
   - `userId` (Ascending) + `read` (Ascending)

## 📱 UI Preview

```
┌─────────────────────────────┐
│  ← Thông báo    Đánh dấu đã đọc │
├─────────────────────────────┤
│ ⚡ 🔥 Tìm thấy gợi ý phù hợp!  │ ●
│    Mèo vàng - Độ phù hợp 95%  │
│    2 giờ trước                │
├─────────────────────────────┤
│ 💬 Nguyễn Văn A               │ ●
│    Bạn tìm thấy ở đâu?        │
│    5 phút trước               │
├─────────────────────────────┤
│ ✅ Đồ vật của bạn đã được...  │
│    Có người tìm thấy ví da    │
│    1 ngày trước               │
└─────────────────────────────┘
```

## 🎯 Files đã tạo/sửa

### Tạo mới
- `NotificationActivity.java`
- `Notification.java` (model)
- `NotificationAdapter.java`
- `activity_notification.xml`
- `item_notification.xml`
- `bg_notification_badge.xml`
- `bg_unread_dot.xml`
- `ic_comment.xml`
- `ic_check_circle.xml`

### Cập nhật
- `MainActivity.java` - Thêm badge và click handler
- `ChatActivity.java` - Gửi notification khi chat
- `NotificationHelper.java` - Thêm các method mới
- `activity_main.xml` - Thêm badge view
- `colors.xml` - Thêm màu mới
- `strings.xml` - Thêm strings mới
- `AndroidManifest.xml` - Thêm NotificationActivity

## ✨ Hoàn thành!

Hệ thống notification đã sẵn sàng sử dụng. Build và test ngay!
