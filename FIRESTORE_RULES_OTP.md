# Firestore Security Rules cho OTP System

## ⚠️ FIX NHANH - Lỗi Permission Denied

Nếu bạn đang gặp lỗi **"PERMISSION_DENIED"** khi tạo hoặc verify OTP, làm theo các bước sau:

### Cách 1: Tạm thời cho phép tất cả (CHỈ DÙNG ĐỂ TEST)

1. Mở Firebase Console → Firestore Database → Rules
2. Thay thế toàn bộ rules bằng code sau:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

3. Click **Publish**
4. Test lại app

**⚠️ LƯU Ý:** Rules này CHỈ dùng để test. Sau khi test xong, phải thay bằng rules bảo mật ở Cách 2.

### Cách 2: Rules bảo mật đầy đủ (KHUYẾN NGHỊ)

1. Mở file `firestore.rules.example` trong thư mục project
2. Copy toàn bộ nội dung
3. Mở Firebase Console → Firestore Database → Rules
4. Paste vào và click **Publish**

Hoặc xem phần **"Bước 2: Thêm Rules cho collection `otpCodes`"** bên dưới để thêm từng phần.

---

## Vấn đề
Khi tạo OTP, app báo lỗi: **"PERMISSION_DENIED: Missing or insufficient permissions"**

## Nguyên nhân
Firestore Security Rules chưa cho phép app tạo/đọc/xóa documents trong collection `otpCodes`.

## Giải pháp

### Bước 1: Mở Firebase Console
1. Truy cập: https://console.firebase.google.com/
2. Chọn project **Findora**
3. Vào **Firestore Database** → **Rules**

### Bước 2: Thêm Rules cho collection `otpCodes`

Thêm đoạn code sau vào Firestore Rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // ... (các rules khác như posts, users, chats...)
    
    // ===== OTP CODES COLLECTION =====
    // Collection này lưu trữ mã OTP để xác nhận giao dịch trả đồ
    match /otpCodes/{otpId} {
      // Cho phép User A (Lost post owner) tạo OTP
      // User A tạo OTP khi đã nhận lại đồ và muốn xác nhận
      allow create: if request.auth != null 
                    && request.resource.data.lostUserId == request.auth.uid;
      
      // Cho phép User A xóa OTP cũ của mình
      // Khi tạo OTP mới, app sẽ xóa OTP cũ trước
      allow delete: if request.auth != null 
                    && resource.data.lostUserId == request.auth.uid;
      
      // Cho phép TẤT CẢ user đã đăng nhập đọc OTP
      // User B cần đọc OTP để verify khi nhập mã
      allow read: if request.auth != null;
      
      // Cho phép TẤT CẢ user đã đăng nhập update OTP
      // User B cần update OTP để đánh dấu là đã verify
      // Chỉ cho phép update field 'verified' thành true
      allow update: if request.auth != null 
                    && request.resource.data.verified == true
                    && resource.data.verified == false;
    }
    
    // ===== POSTS COLLECTION (cần update để resolve posts) =====
    match /posts/{postId} {
      // ... (các rules khác)
      
      // Cho phép update status thành 'resolved' khi verify OTP
      allow update: if request.auth != null 
                    && (request.resource.data.userId == request.auth.uid  // Owner update
                        || (request.resource.data.status == 'resolved'    // Hoặc resolve bởi helper
                            && request.resource.data.keys().hasOnly(['status', 'resolvedAt', 'resolvedBy', 'rating', 'review'])));
    }
    
    // ===== USERS COLLECTION (cần update để cộng điểm) =====
    match /users/{userId} {
      // ... (các rules khác)
      
      // Cho phép update points và totalReturned khi verify OTP thành công
      allow update: if request.auth != null 
                    && (userId == request.auth.uid  // User tự update
                        || request.resource.data.keys().hasOnly(['points', 'totalReturned']));  // Hoặc app update points
    }
    
    // ===== TRANSACTIONS COLLECTION (cần create để ghi lịch sử điểm) =====
    match /transactions/{transactionId} {
      // Cho phép tạo transaction khi verify OTP thành công
      allow create: if request.auth != null;
      
      // Cho phép user đọc transaction của mình
      allow read: if request.auth != null 
                  && resource.data.userId == request.auth.uid;
    }
  }
}
```

### Bước 3: Publish Rules
1. Click **Publish** để áp dụng rules mới
2. Đợi vài giây để rules được cập nhật

### Bước 4: Test lại app
1. Mở app → Vào **Bài đăng của tôi** → Tab **Thất lạc**
2. Chọn bài viết → Click **Giải quyết**
3. Chọn "Có, tôi đã nhận lại được đồ" → Đánh giá → Click **Tạo mã xác nhận**
4. Mã OTP sẽ hiển thị thành công

## Giải thích Rules

### 1. Create (Tạo OTP)
```javascript
allow create: if request.auth != null 
              && request.resource.data.lostUserId == request.auth.uid;
```
- Chỉ cho phép user đã đăng nhập
- Chỉ cho phép tạo OTP nếu `lostUserId` = UID của user hiện tại
- Đảm bảo user chỉ tạo OTP cho bài viết của mình

### 2. Delete (Xóa OTP cũ)
```javascript
allow delete: if request.auth != null 
              && resource.data.lostUserId == request.auth.uid;
```
- Chỉ cho phép user xóa OTP của chính mình
- Dùng khi tạo OTP mới (xóa OTP cũ trước)

### 3. Read (Đọc OTP)
```javascript
allow read: if request.auth != null;
```
- Cho phép tất cả user đã đăng nhập đọc OTP
- User B cần đọc OTP để verify khi nhập mã

### 4. Update (Cập nhật OTP)
```javascript
allow update: if request.auth != null 
              && request.resource.data.verified == true;
```
- Cho phép user đánh dấu OTP là đã verify
- Dùng khi User B nhập đúng mã OTP

## Lưu ý bảo mật

### Vấn đề: Tất cả user có thể đọc OTP
Hiện tại rule `allow read: if request.auth != null` cho phép tất cả user đọc OTP. Điều này có thể gây rủi ro bảo mật.

### Giải pháp nâng cao (Optional)
Nếu muốn bảo mật hơn, có thể giới hạn chỉ User B (Found post owner) mới đọc được OTP:

```javascript
match /otpCodes/{otpId} {
  allow read: if request.auth != null 
              && (resource.data.lostUserId == request.auth.uid  // User A đọc OTP của mình
                  || exists(/databases/$(database)/documents/posts/$(request.auth.uid)));  // User B có bài Found
}
```

Tuy nhiên, rule này phức tạp hơn và có thể gây lỗi. Nên dùng rule đơn giản ở trên trước.

## Troubleshooting

### Lỗi "PERMISSION_DENIED" khi User B nhập OTP

**Nguyên nhân:**
- User B không có quyền update OTP document
- User B không có quyền update posts (resolve)
- User B không có quyền update users (cộng điểm)
- User B không có quyền create transactions

**Giải pháp:**
1. Kiểm tra rules cho collection `otpCodes` - phải có `allow update`
2. Kiểm tra rules cho collection `posts` - phải cho phép update status
3. Kiểm tra rules cho collection `users` - phải cho phép update points
4. Kiểm tra rules cho collection `transactions` - phải cho phép create

**Debug:**
Xem logs trong Logcat để biết collection nào bị lỗi:
```
Tag: OTPVerification
Message: Error fetching OTP / Error updating posts / Error updating users
```

### Lỗi vẫn còn sau khi update rules
1. Đợi 1-2 phút để rules được áp dụng
2. Force close app và mở lại
3. Kiểm tra lại rules trong Firebase Console

### Lỗi "Missing index"
Nếu gặp lỗi về index, Firebase sẽ tự động gợi ý link tạo index. Click vào link đó để tạo.

### Kiểm tra logs
Xem logs trong Android Studio (Logcat) để debug:
```
Tag: ResolvePost
Tag: OTPVerification
```

## Tóm tắt
- Collection: `otpCodes`
- Fields: `otp`, `lostPostId`, `lostUserId`, `rating`, `review`, `createdAt`, `expiresAt`, `verified`
- User A (Lost): Tạo và xóa OTP
- User B (Found): Đọc và verify OTP
