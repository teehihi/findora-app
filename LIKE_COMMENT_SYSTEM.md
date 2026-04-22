# ❤️💬 Like & Comment System - Findora

## 📋 Tổng quan

Hệ thống Like và Comment đã được implement đầy đủ cho bài đăng với các tính năng:

1. ✅ **Like/Unlike** - Thích/Bỏ thích bài đăng
2. ✅ **Comment** - Bình luận realtime
3. ✅ **Notification** - Thông báo khi có like/comment
4. ✅ **Realtime Updates** - Cập nhật số lượng realtime
5. ✅ **User Info** - Hiển thị avatar và tên người comment

---

## 🎯 Tính năng

### 1. Like System
- **Toggle Like/Unlike**: Click icon trái tim để like/unlike
- **Visual Feedback**: Icon đổi màu đỏ khi đã like
- **Like Count**: Hiển thị số lượng likes
- **Notification**: Gửi thông báo cho chủ bài khi có người like

### 2. Comment System
- **Realtime Comments**: Comments tự động cập nhật
- **User Avatar**: Hiển thị avatar người comment
- **Timestamp**: Hiển thị thời gian relative ("2 giờ trước")
- **Comment Input**: Input field với nút gửi
- **Notification**: Gửi thông báo cho chủ bài khi có comment mới

---

## 📁 Cấu trúc file

### Models
```
app/src/main/java/hcmute/edu/vn/findora/model/
├── Comment.java               # Model comment
└── Post.java                  # Thêm field likes
```

### Adapters
```
app/src/main/java/hcmute/edu/vn/findora/adapter/
└── CommentAdapter.java        # Adapter hiển thị comments
```

### Activities
```
app/src/main/java/hcmute/edu/vn/findora/
└── PostDetailActivity.java    # Thêm like & comment logic
```

### Layouts
```
app/src/main/res/layout/
├── section_like_comment.xml   # Section like & comment
├── item_comment.xml           # Layout comment item
└── activity_post_detail.xml   # Include section
```

### Drawables
```
app/src/main/res/drawable/
├── ic_favorite.xml            # Icon trái tim outline
├── ic_favorite_filled.xml     # Icon trái tim đỏ (liked)
├── ic_send.xml                # Icon gửi comment
└── bg_border_bottom.xml       # Border dưới like bar
```

---

## 🗄️ Firestore Structure

### Collection: `posts`
```javascript
{
  "id": "post123",
  "title": "Mất mèo vàng",
  "description": "...",
  "type": "lost",
  "userId": "user456",
  "likes": ["user789", "user012", "user345"],  // Array of user IDs
  // ... other fields
}
```

### Subcollection: `posts/{postId}/comments`
```javascript
{
  "id": "comment123",
  "userId": "user789",
  "userName": "Nguyễn Văn A",
  "userAvatar": "https://...",
  "text": "Bạn tìm thấy ở đâu?",
  "timestamp": Timestamp
}
```

---

## 🚀 Cách sử dụng

### 1. Like/Unlike
```java
// User click vào icon trái tim
btnLike.setOnClickListener(v -> toggleLike());

// Toggle logic
private void toggleLike() {
    if (isLiked) {
        likesList.remove(currentUserId);
    } else {
        likesList.add(currentUserId);
        // Gửi notification
        sendLikeNotification();
    }
    
    // Update Firestore
    db.collection("posts").document(currentPostId)
        .update("likes", likesList);
}
```

### 2. Comment
```java
// User nhập comment và click gửi
btnSendComment.setOnClickListener(v -> sendComment());

// Send comment logic
private void sendComment() {
    String text = etComment.getText().toString().trim();
    
    Comment comment = new Comment(
        currentUserId,
        userName,
        userAvatar,
        text
    );
    
    // Save to Firestore
    db.collection("posts").document(currentPostId)
        .collection("comments")
        .add(comment);
    
    // Gửi notification
    sendCommentNotification(userName, userAvatar, text);
}
```

### 3. Load Likes & Comments
```java
// Load likes
db.collection("posts").document(postId).get()
    .addOnSuccessListener(doc -> {
        List<String> likes = (List<String>) doc.get("likes");
        updateLikeUI();
    });

// Load comments realtime
db.collection("posts").document(postId)
    .collection("comments")
    .orderBy("timestamp", Query.Direction.ASCENDING)
    .addSnapshotListener((snapshots, error) -> {
        commentsList.clear();
        for (QueryDocumentSnapshot doc : snapshots) {
            Comment comment = doc.toObject(Comment.class);
            commentsList.add(comment);
        }
        commentAdapter.notifyDataSetChanged();
    });
```

---

## 🎨 UI Components

### Like Bar
```
┌─────────────────────────────┐
│ ❤️ 12        💬 5           │
└─────────────────────────────┘
```

### Comment Item
```
┌─────────────────────────────┐
│ 👤 Nguyễn Văn A    2 giờ trước│
│    Bạn tìm thấy ở đâu?       │
└─────────────────────────────┘
```

### Comment Input
```
┌─────────────────────────────┐
│ [Viết bình luận...]      ➤  │
└─────────────────────────────┘
```

---

## 🔔 Notifications

### Like Notification
```java
NotificationHelper.sendLikeNotification(
    postOwnerId,      // Chủ bài đăng
    postId,           // ID bài đăng
    postTitle,        // Tiêu đề bài đăng
    likerId,          // Người like
    likerName,        // Tên người like
    likerAvatar       // Avatar người like
);
```

### Comment Notification
```java
NotificationHelper.sendCommentNotification(
    postOwnerId,      // Chủ bài đăng
    postId,           // ID bài đăng
    postTitle,        // Tiêu đề bài đăng
    commenterId,      // Người comment
    commenterName,    // Tên người comment
    commenterAvatar,  // Avatar người comment
    commentText       // Nội dung comment
);
```

---

## 📊 Features

### Like System
- ✅ Toggle like/unlike
- ✅ Visual feedback (icon đổi màu)
- ✅ Like count realtime
- ✅ Notification cho chủ bài
- ✅ Không gửi notification cho chính mình

### Comment System
- ✅ Realtime comments
- ✅ User avatar và tên
- ✅ Timestamp relative
- ✅ Comment input với validation
- ✅ Notification cho chủ bài
- ✅ Không gửi notification cho chính mình
- ✅ Auto scroll to bottom khi có comment mới

---

## 🎯 Testing

### Test Like
1. User A mở bài đăng của User B
2. User A click icon trái tim
3. Icon đổi màu đỏ, số likes tăng lên
4. User B nhận notification "User A đã thích bài đăng"
5. User A click lại → Unlike, số likes giảm

### Test Comment
1. User A mở bài đăng của User B
2. User A nhập comment "Bạn tìm thấy ở đâu?"
3. User A click gửi
4. Comment hiển thị ngay lập tức
5. User B nhận notification "User A đã bình luận"

### Test Realtime
1. User A và User B cùng mở bài đăng
2. User A comment
3. User B thấy comment mới ngay lập tức (không cần refresh)

---

## 🐛 Known Issues

1. **No pagination**: Comments không có pagination (load tất cả)
2. **No edit/delete**: Chưa có chức năng sửa/xóa comment
3. **No reply**: Chưa có chức năng reply comment
4. **No emoji**: Chưa có emoji picker

---

## 🔮 Future Enhancements

### 1. Comment Features
- [ ] Edit comment
- [ ] Delete comment
- [ ] Reply to comment (nested comments)
- [ ] Emoji picker
- [ ] Mention users (@username)
- [ ] Comment pagination

### 2. Like Features
- [ ] Show list of users who liked
- [ ] Different reactions (love, haha, wow, sad, angry)
- [ ] Like animation

### 3. UI/UX
- [ ] Skeleton loading
- [ ] Pull to refresh
- [ ] Swipe to delete comment
- [ ] Image in comments
- [ ] Link preview

---

## 📝 Notes

- Likes được lưu dưới dạng array trong document `posts`
- Comments được lưu trong subcollection `posts/{postId}/comments`
- Realtime listener đảm bảo comments update ngay lập tức
- Notification chỉ gửi khi không phải chủ bài (tránh spam)

---

## ✨ Kết luận

Hệ thống Like & Comment đã hoàn chỉnh với đầy đủ tính năng cơ bản:
- ❤️ Like/Unlike với visual feedback
- 💬 Comment realtime với user info
- 🔔 Notification tự động
- 📊 Count realtime

Sẵn sàng để test và sử dụng! 🎉
