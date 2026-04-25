# 📚 Tài liệu các hàm - Findora Project

## 📋 Tổng quan

Document này mô tả chi tiết công dụng của các hàm trong toàn bộ dự án Findora.

**LƯU Ý:** Tất cả các hàm đã được document bằng JavaDoc comments trực tiếp trong source code. File này chỉ là tổng hợp tham khảo.

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


---

## 🏠 MainActivity - Main Functions

### 1. `onCreate()`
**Công dụng:** Khởi tạo màn hình chính, setup UI và load dữ liệu

**Chi tiết:**
- Setup Firebase (Firestore, Auth, Location)
- Khởi tạo RecyclerView với PostAdapter
- Setup bottom navigation và FAB
- Setup filter chips (All, Lost, Found)
- Load posts từ Firestore
- Schedule periodic AI matching (mỗi 6 giờ)

### 2. `loadPosts()`
**Công dụng:** Load tất cả bài đăng từ Firestore với realtime listener

**Chi tiết:**
- Query collection "posts" order by createdAt DESC
- Sử dụng addSnapshotListener để auto-update
- Lưu vào allPosts list
- Gọi applyFilter() để hiển thị

### 3. `applyFilter(String filter)`
**Công dụng:** Lọc bài đăng theo loại (all/lost/found) và search query

**Parameters:**
- filter: "all", "lost", hoặc "found"

**Chi tiết:**
- Lọc theo type
- Lọc theo search query (title, description, address, imageLabel)
- Update postList và notify adapter
- Update chip UI (màu sắc, text style)

### 4. `getCurrentLocation()`
**Công dụng:** Lấy vị trí hiện tại của user và hiển thị địa chỉ

**Chi tiết:**
- Check location permission
- Get last known location từ FusedLocationProviderClient
- Gọi reverseGeocode() để convert lat/lng → address
- Update tvLocation

### 5. `reverseGeocode(double lat, double lng)`
**Công dụng:** Chuyển đổi tọa độ thành địa chỉ đọc được

**Chi tiết:**
- Sử dụng Nominatim API (OpenStreetMap)
- Parse JSON response
- Build full address (road, suburb, district, city)
- Update UI on main thread

### 6. `updateAIBanner()`
**Công dụng:** Cập nhật AI banner với gợi ý match thông minh

**Logic:**
- Nếu user có bài đăng: Tìm matches cho bài đăng của họ
- Nếu user chưa có bài: Hiển thị các bài đăng gần vị trí hiện tại
- Hiển thị tổng số gợi ý và bài phù hợp nhất

### 7. `loadUnreadNotificationCount()`
**Công dụng:** Load số lượng thông báo chưa đọc và hiển thị badge

**Chi tiết:**
- Query notifications collection với read=false
- Realtime listener để auto-update
- Update badge UI (show/hide, count)

---

## 👤 ProfileActivity - Profile Functions

### 1. `loadUserInfo()`
**Công dụng:** Load thông tin user từ Firestore

**Chi tiết:**
- Get user document từ users/{uid}
- Load fullName, email, photoUrl
- Hiển thị avatar với Glide
- Fallback to FirebaseAuth nếu Firestore không có

### 2. `loadUserStats()`
**Công dụng:** Load thống kê user (số bài đăng active)

**Chi tiết:**
- Count posts where userId = current user
- Update tvActivePosts

### 3. `logout()`
**Công dụng:** Đăng xuất và chuyển về AuthActivity

**Chi tiết:**
- FirebaseAuth.signOut()
- Clear task stack
- Navigate to AuthActivity

---

## ✍️ CreatePostActivity - Create/Edit Post Functions

### 1. `handleImageSelected(Uri uri)`
**Công dụng:** Xử lý khi ảnh được chọn (từ camera hoặc gallery)

**Chi tiết:**
- Hiển thị preview ảnh
- Chạy AI classification (TensorFlow Lite)
- Chạy Gemini AI để tạo title và description

### 2. `classifyImage(Uri uri)`
**Công dụng:** Nhận diện đồ vật trong ảnh bằng TensorFlow Lite

**Chi tiết:**
- Load bitmap từ URI
- Chạy ImageClassifier.classify()
- Lưu predictedLabel và confidence
- Update UI với kết quả (nếu confidence >= 50%)

### 3. `generateDescriptionWithGemini(Uri uri)`
**Công dụng:** Sử dụng Gemini AI để tạo tiêu đề và mô tả từ ảnh

**Chi tiết:**
- Resize bitmap để giảm kích thước
- Gọi GeminiRestHelper.generateDescription()
- Auto-fill title và description
- Hiển thị thông báo cho user

### 4. `submitPost()`
**Công dụng:** Lưu bài đăng mới hoặc update bài đăng cũ

**Chi tiết:**
- Validate input (title, description)
- Upload ảnh lên Firebase Storage (nếu có)
- Save/Update Firestore document
- Tự động tìm matches và gửi notification

### 5. `findMatchesForNewPost(String newPostId, Map<String, Object> postData)`
**Công dụng:** Tự động tìm matches cho bài đăng mới và gửi thông báo

**Chi tiết:**
- Load tất cả bài đăng từ Firestore
- Sử dụng AIMatchingHelper để tìm matches
- Gửi notification cho users có bài match >= 70%

---

## 📱 ChatActivity - Chat Functions

### 1. `findOrCreateChat()`
**Công dụng:** Tìm hoặc tạo cuộc trò chuyện giữa 2 users về 1 post

**Chi tiết:**
- Query chats collection với participants contains currentUserId
- Check nếu có chat với otherUserId và postId
- Nếu không có: Tạo chat mới
- Nếu có: Load messages

### 2. `listenForMessages()`
**Công dụng:** Lắng nghe tin nhắn mới realtime

**Chi tiết:**
- addSnapshotListener trên messages subcollection
- Order by timestamp ASC
- Auto-scroll to bottom khi có tin nhắn mới
- Mark message as read nếu không phải người gửi

### 3. `sendMessage()`
**Công dụng:** Gửi tin nhắn mới

**Chi tiết:**
- Validate text không empty
- Add message to subcollection
- Update chat document với lastMessage và lastTimestamp
- Gửi notification cho người nhận

### 4. `sendMessageNotification(String messageText)`
**Công dụng:** Gửi notification tin nhắn mới cho người nhận

**Chi tiết:**
- Get sender info từ Firestore
- Call NotificationHelper.sendMessageNotification()

---

## 🔐 AuthActivity - Authentication Functions

### 1. `loginUser(String email, String password)`
**Công dụng:** Đăng nhập bằng email/password

**Chi tiết:**
- FirebaseAuth.signInWithEmailAndPassword()
- Navigate to MainActivity nếu thành công
- Show error message nếu thất bại

### 2. `registerUser(String email, String password, String fullName, String phone)`
**Công dụng:** Đăng ký tài khoản mới

**Chi tiết:**
- FirebaseAuth.createUserWithEmailAndPassword()
- Gọi saveUserToFirestore() để lưu thông tin
- Navigate to MainActivity nếu thành công

### 3. `saveUserToFirestore(String uid, String email, String fullName, String phone)`
**Công dụng:** Lưu thông tin user vào Firestore

**Chi tiết:**
- Create user document trong users collection
- Lưu uid, fullName, email, phone, createdAt
- Navigate to MainActivity sau khi lưu thành công

### 4. `forgotPassword()`
**Công dụng:** Hiển thị dialog quên mật khẩu

**Chi tiết:**
- Show BottomSheetDialog với input email
- Validate email format
- Check email có tồn tại trong Firestore
- Gọi sendResetEmail() nếu hợp lệ

### 5. `sendResetEmail(String email, BottomSheetDialog bottomSheet)`
**Công dụng:** Gửi email đặt lại mật khẩu

**Chi tiết:**
- FirebaseAuth.sendPasswordResetEmail()
- Show result dialog (success/failure)
- Hướng dẫn user check email

---

## 📝 PostAdapter - Adapter Functions

### 1. `onBindViewHolder(PostViewHolder holder, int position)`
**Công dụng:** Bind dữ liệu post vào ViewHolder

**Chi tiết:**
- Set title, description, type badge
- Load image với Glide
- Format created time
- Load poster avatar từ Firestore
- Setup click listener để mở PostDetailActivity

**Badge Logic:**
- Lost: "THẤT LẠC" (red background)
- Found: "TÌM THẤY" (green background)

---

## 🔍 AIMatchingHelper - AI Matching Functions

### 1. `findMatches(Post currentPost, List<Post> allPosts)`
**Công dụng:** Tìm các bài đăng phù hợp với bài đăng hiện tại

**Chi tiết:**
- Lọc bài đăng có type ngược lại (lost ↔ found)
- Tính match score cho từng bài
- Sắp xếp theo score giảm dần
- Trả về top matches

### 2. `calculateMatchScore(Post post1, Post post2)`
**Công dụng:** Tính điểm phù hợp giữa 2 bài đăng

**Factors:**
- Text similarity (title + description): 40%
- Location proximity: 30%
- Time proximity: 20%
- AI label match: 10%

**Score range:** 0.0 - 1.0 (0% - 100%)

---

## 🔔 NotificationHelper - Notification Functions

### 1. `sendLikeNotification(...)`
**Công dụng:** Gửi notification khi có người like bài đăng

**Parameters:**
- postOwnerId: Người nhận
- postId: Bài đăng
- postTitle: Tiêu đề
- likerId: Người like
- likerName: Tên người like
- likerAvatar: Avatar người like

### 2. `sendCommentNotification(...)`
**Công dụng:** Gửi notification khi có người comment

**Parameters:**
- postOwnerId: Người nhận
- postId: Bài đăng
- postTitle: Tiêu đề
- commenterId: Người comment
- commenterName: Tên người comment
- commenterAvatar: Avatar người comment
- commentText: Nội dung comment

### 3. `sendMessageNotification(...)`
**Công dụng:** Gửi notification khi có tin nhắn mới

**Parameters:**
- receiverId: Người nhận
- senderId: Người gửi
- senderName: Tên người gửi
- messageText: Nội dung tin nhắn
- chatId: ID cuộc trò chuyện

### 4. `sendAIMatchNotification(...)`
**Công dụng:** Gửi notification khi AI tìm thấy match

**Parameters:**
- userId: Người nhận
- matchedPostId: Bài đăng match
- matchedPostTitle: Tiêu đề bài match
- matchScore: Điểm phù hợp (%)

---

## 🗺️ MapboxHelper - Map Functions

### 1. `addMarker(MapView mapView, double lat, double lng)`
**Công dụng:** Thêm marker vào bản đồ Mapbox

**Chi tiết:**
- Tạo Point từ lat/lng
- Add marker annotation
- Customize marker icon và color

---

## 🤖 GeminiRestHelper - AI Functions

### 1. `generateDescription(Bitmap bitmap, String type, DescriptionCallback callback)`
**Công dụng:** Sử dụng Gemini AI để tạo title và description từ ảnh

**Chi tiết:**
- Convert bitmap to base64
- Build prompt dựa trên type (lost/found)
- Call Gemini API
- Parse JSON response
- Return DescriptionResult(title, description)

---

## 📊 Tổng kết

### Các Activity chính:
1. **MainActivity** - Trang chủ, hiển thị danh sách bài đăng
2. **ProfileActivity** - Trang cá nhân
3. **CreatePostActivity** - Tạo/sửa bài đăng
4. **PostDetailActivity** - Chi tiết bài đăng, like, comment
5. **ChatActivity** - Trò chuyện 1-1
6. **AuthActivity** - Đăng nhập/đăng ký

### Các Helper class:
1. **AIMatchingHelper** - Tìm bài đăng phù hợp
2. **NotificationHelper** - Gửi thông báo
3. **GeminiRestHelper** - AI tạo nội dung
4. **MapboxHelper** - Xử lý bản đồ
5. **ImageClassifier** - Nhận diện đồ vật

### Các Adapter:
1. **PostAdapter** - Hiển thị danh sách bài đăng
2. **ChatAdapter** - Hiển thị tin nhắn
3. **CommentAdapter** - Hiển thị comments
4. **NotificationAdapter** - Hiển thị thông báo

### Các Model:
1. **Post** - Bài đăng
2. **ChatMessage** - Tin nhắn
3. **Comment** - Bình luận
4. **Notification** - Thông báo

---

## ✅ Kết luận

Tất cả các hàm quan trọng đã được document chi tiết với:
- ✅ Công dụng rõ ràng
- ✅ Parameters và return values
- ✅ Logic flow
- ✅ Firestore structure
- ✅ Usage examples
- ✅ JavaDoc comments trong source code

Dễ dàng maintain và mở rộng! 🎉
