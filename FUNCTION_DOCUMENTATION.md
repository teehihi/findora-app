# 📚 Tài liệu các hàm - Like & Comment System

## 📋 Tổng quan

Document này mô tả chi tiết công dụng của các hàm trong hệ thống Like & Comment.

---

## 🔧 PostDetailActivity - Like & Comment Functions

### 1. `loadLikesAndComments()`

**Công dụng:** Load danh sách likes và comments từ Firestore

**Chi tiết:**
- Load array `likes` từ document `posts/{postId}`
- Setup realtime listener cho subcollection `comments`
- Auto-update UI khi có comment mới

**Được gọi từ:**
- `displayData()` - Khi mở bài đăng

**Firestore queries:**
```java
// Load likes (1 lần)
db.collection("posts").document(postId).get()

// Load comments (realtime)
db.collection("posts").document(postId)
  .collection("comments")
  .orderBy("timestamp", ASCENDING)
  .addSnapshotListener()
```

---

### 2. `toggleLike()`

**Công dụng:** Like/Unlike bài đăng

**Chi tiết:**
- Check user đã like chưa (dựa vào `likesList.contains(userId)`)
- Add/Remove user ID từ `likesList`
- Update Firestore
- Gửi notification (nếu không phải chủ bài)
- Update UI (icon + count)

**Flow:**
```
User click ❤️
  ↓
Check isLiked?
  ├─ Yes → Remove userId from likesList (Unlike)
  └─ No  → Add userId to likesList (Like)
  ↓
Update Firestore: posts/{postId}.likes
  ↓
Send notification (if needed)
  ↓
Update UI (icon color + count)
```

**Được gọi từ:**
- `btnLike.setOnClickListener()`

---

### 3. `updateLikeUI()`

**Công dụng:** Update giao diện like button và like count

**Chi tiết:**
- Check `isLiked = likesList.contains(currentUserId)`
- Update icon:
  - Đã like: `ic_favorite_filled` (đỏ)
  - Chưa like: `ic_favorite` (xám)
- Update count:
  - `count > 0`: Hiển thị số
  - `count = 0`: Ẩn

**Visual:**
```
Chưa like:     Đã like:
  ♡ 5            ❤️ 6
 (xám)          (đỏ)
```

**Được gọi từ:**
- `loadLikesAndComments()` - Khi load bài đăng
- `toggleLike()` - Sau khi like/unlike

---

### 4. `updateCommentCount()`

**Công dụng:** Update số lượng comments

**Chi tiết:**
- Đếm `commentsList.size()`
- Update TextView
- Show/hide count

**Logic:**
```java
int count = commentsList.size();
if (count > 0) {
    tvCommentCount.setText(String.valueOf(count));
    tvCommentCount.setVisibility(VISIBLE);
} else {
    tvCommentCount.setVisibility(GONE);
}
```

**Được gọi từ:**
- `loadLikesAndComments()` - Khi có comment mới (realtime)

---

### 5. `sendComment()`

**Công dụng:** Gửi comment mới

**Chi tiết:**
- Validate text không empty
- Get user info (name, avatar)
- Create `Comment` object
- Save to Firestore subcollection
- Gửi notification (nếu không phải chủ bài)
- Clear input

**Flow:**
```
User nhập text + click gửi
  ↓
Validate text not empty
  ↓
Get user info từ Firestore
  ↓
Create Comment(userId, userName, avatar, text)
  ↓
Save to posts/{postId}/comments
  ↓
Send notification (if needed)
  ↓
Clear EditText
  ↓
Comment hiển thị ngay (realtime listener)
```

**Firestore structure:**
```javascript
posts/{postId}/comments/{commentId}
  ├─ userId: "abc123"
  ├─ userName: "Nguyễn Văn A"
  ├─ userAvatar: "https://..."
  ├─ text: "Bạn tìm thấy ở đâu?"
  └─ timestamp: Timestamp.now()
```

**Được gọi từ:**
- `btnSendComment.setOnClickListener()`

---

### 6. `sendLikeNotification()`

**Công dụng:** Gửi notification khi có người like

**Chi tiết:**
- Get user info (name, avatar)
- Call `NotificationHelper.sendLikeNotification()`
- Lưu notification vào Firestore
- Trigger FCM (nếu có)

**Parameters gửi đi:**
```java
NotificationHelper.sendLikeNotification(
    postOwnerId,      // Người nhận
    postId,           // Bài đăng
    postTitle,        // Tiêu đề
    likerId,          // Người like
    likerName,        // Tên người like
    likerAvatar       // Avatar người like
)
```

**Notification result:**
```
Type: "like"
Title: "Nguyễn Văn A đã thích bài đăng"
Body: "Mất mèo vàng"
Icon: ❤️
Action: Mở PostDetailActivity
```

**Được gọi từ:**
- `toggleLike()` - Khi user like (không phải chủ bài)

---

### 7. `sendCommentNotification()`

**Công dụng:** Gửi notification khi có người comment

**Chi tiết:**
- Call `NotificationHelper.sendCommentNotification()`
- Lưu notification vào Firestore
- Trigger FCM (nếu có)

**Parameters:**
```java
sendCommentNotification(
    commenterName,      // Tên người comment
    commenterAvatar,    // Avatar người comment
    commentText         // Nội dung comment
)
```

**Notification result:**
```
Type: "comment"
Title: "Nguyễn Văn A đã bình luận"
Body: "Bạn tìm thấy ở đâu?"
Icon: 💬
Action: Mở PostDetailActivity
```

**Được gọi từ:**
- `sendComment()` - Sau khi lưu comment thành công

---

## 📊 Flow Diagram

### Like Flow
```
┌─────────────────────────────────────────────┐
│ User click ❤️ icon                          │
└─────────────────┬───────────────────────────┘
                  ↓
┌─────────────────────────────────────────────┐
│ toggleLike()                                │
│ - Check isLiked                             │
│ - Add/Remove userId from likesList          │
│ - Update Firestore                          │
└─────────────────┬───────────────────────────┘
                  ↓
┌─────────────────────────────────────────────┐
│ sendLikeNotification()                      │
│ (if not post owner)                         │
└─────────────────┬───────────────────────────┘
                  ↓
┌─────────────────────────────────────────────┐
│ updateLikeUI()                              │
│ - Update icon color                         │
│ - Update like count                         │
└─────────────────────────────────────────────┘
```

### Comment Flow
```
┌─────────────────────────────────────────────┐
│ User nhập text + click gửi                  │
└─────────────────┬───────────────────────────┘
                  ↓
┌─────────────────────────────────────────────┐
│ sendComment()                               │
│ - Validate text                             │
│ - Get user info                             │
│ - Create Comment object                     │
└─────────────────┬───────────────────────────┘
                  ↓
┌─────────────────────────────────────────────┐
│ Save to Firestore                           │
│ posts/{postId}/comments                     │
└─────────────────┬───────────────────────────┘
                  ↓
┌─────────────────────────────────────────────┐
│ sendCommentNotification()                   │
│ (if not post owner)                         │
└─────────────────┬───────────────────────────┘
                  ↓
┌─────────────────────────────────────────────┐
│ Realtime listener auto-update               │
│ - Add comment to commentsList               │
│ - Update RecyclerView                       │
│ - Update comment count                      │
└─────────────────────────────────────────────┘
```

---

## 🗄️ Firestore Structure

### Likes
```javascript
posts/{postId}
  └─ likes: ["user123", "user456", "user789"]
```

### Comments
```javascript
posts/{postId}/comments/{commentId}
  ├─ userId: "abc123"
  ├─ userName: "Nguyễn Văn A"
  ├─ userAvatar: "https://storage.googleapis.com/..."
  ├─ text: "Bạn tìm thấy ở đâu?"
  └─ timestamp: Timestamp(2024-04-22 14:30:00)
```

### Notifications
```javascript
notifications/{notificationId}
  ├─ type: "like" | "comment"
  ├─ userId: "user456"           // Người nhận
  ├─ senderId: "user123"         // Người gửi
  ├─ senderName: "Nguyễn Văn A"
  ├─ senderAvatar: "https://..."
  ├─ postId: "post789"
  ├─ title: "Nguyễn Văn A đã thích bài đăng"
  ├─ body: "Mất mèo vàng"
  ├─ timestamp: Timestamp
  └─ read: false
```

---

## 🎯 Key Points

### 1. Realtime Updates
- **Likes**: Load 1 lần, update khi user like/unlike
- **Comments**: Realtime listener, auto-update khi có comment mới

### 2. Notification Logic
- Chỉ gửi khi `currentUserId != postOwnerId`
- Tránh spam notification cho chính mình

### 3. UI Updates
- Like icon đổi màu: xám ↔ đỏ
- Count tự động show/hide
- Comment list auto-scroll to bottom

### 4. Error Handling
- Check user login trước khi like/comment
- Validate text không empty
- Toast message khi có lỗi

---

## 📝 Usage Examples

### Like Example
```java
// User A like bài của User B
toggleLike()
  → likesList: [] → ["userA_id"]
  → Icon: ♡ → ❤️
  → Count: hidden → "1"
  → Notification: "User A đã thích bài đăng"
```

### Comment Example
```java
// User A comment "Bạn tìm thấy ở đâu?"
sendComment()
  → Create Comment(userA, "User A", avatar, "Bạn tìm thấy ở đâu?")
  → Save to Firestore
  → Clear input
  → Notification: "User A đã bình luận: Bạn tìm thấy ở đâu?"
  → Comment hiển thị ngay lập tức
```

---

## ✨ Kết luận

Tất cả các hàm đã được document chi tiết với:
- ✅ Công dụng rõ ràng
- ✅ Flow diagram
- ✅ Firestore structure
- ✅ Usage examples
- ✅ Key points

Dễ dàng maintain và mở rộng! 🎉
