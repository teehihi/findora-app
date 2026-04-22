# Hướng dẫn cài đặt Camera + AI Description

## Tính năng mới
Khi đăng bài, người dùng có thể:
- 📷 Chụp ảnh trực tiếp từ camera
- 🖼️ Chọn ảnh từ thư viện
- 🤖 AI tự động tạo tiêu đề và mô tả dựa trên ảnh

## Cài đặt API Key

### 1. Lấy Gemini API Key (MIỄN PHÍ)
1. Truy cập: https://makersuite.google.com/app/apikey
2. Đăng nhập bằng Google Account
3. Click "Create API Key"
4. Copy API key

### 2. Thêm vào local.properties
Mở file `local.properties` và thêm:
```
GEMINI_API_KEY=your_api_key_here
```

### 3. Sync Gradle
- Click "Sync Now" trong Android Studio
- Hoặc: File > Sync Project with Gradle Files

## Cách sử dụng

### Trong CreatePostActivity:
1. Click vào khung "Tải ảnh lên"
2. Chọn "📷 Chụp ảnh" hoặc "🖼️ Chọn từ thư viện"
3. Sau khi chọn ảnh:
   - AI sẽ tự động phân tích ảnh
   - Tiêu đề và mô tả được tạo tự động
   - Người dùng có thể chỉnh sửa nếu cần

## Permissions cần thiết
App sẽ tự động yêu cầu quyền:
- ✅ CAMERA - Để chụp ảnh
- ✅ INTERNET - Để gọi Gemini API

## Files đã tạo/cập nhật

### Layouts:
- `app/src/main/res/layout/bottom_sheet_image_picker.xml` - Dialog chọn Camera/Gallery
- `app/src/main/res/xml/file_paths.xml` - FileProvider config

### Code:
- `app/src/main/java/hcmute/edu/vn/findora/utils/GeminiHelper.java` - Gemini AI helper
- `app/src/main/java/hcmute/edu/vn/findora/CreatePostActivity.java` - Đã cập nhật với camera + AI

### Config:
- `app/build.gradle.kts` - Thêm Gemini dependency và BuildConfig
- `app/src/main/AndroidManifest.xml` - Thêm CAMERA permission và FileProvider
- `local.properties` - Thêm GEMINI_API_KEY

## Lưu ý
- Gemini API có giới hạn free tier: 60 requests/phút
- Ảnh được resize xuống 1024px để tối ưu
- Nếu không có API key, tính năng AI sẽ bị tắt (người dùng nhập thủ công)
- Offline mode: AI không hoạt động, hiển thị thông báo lỗi

## Test
1. Build và chạy app
2. Vào màn hình tạo bài đăng
3. Click "Tải ảnh lên"
4. Chọn Camera hoặc Gallery
5. Kiểm tra AI có tạo tiêu đề + mô tả không

## Troubleshooting

### Lỗi: "BuildConfig.GEMINI_API_KEY not found"
- Kiểm tra `local.properties` có GEMINI_API_KEY chưa
- Sync Gradle lại
- Clean và Rebuild project

### Lỗi: "Camera permission denied"
- Vào Settings > Apps > Findora > Permissions
- Bật quyền Camera

### AI không hoạt động
- Kiểm tra internet connection
- Kiểm tra API key có đúng không
- Xem Logcat để debug: filter "GeminiHelper"
