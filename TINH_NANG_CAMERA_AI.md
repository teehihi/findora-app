# Tính năng Camera + AI Description

## Tổng quan
Tính năng cho phép người dùng chụp ảnh trực tiếp hoặc chọn từ thư viện, sau đó AI sẽ tự động tạo tiêu đề và mô tả cho bài đăng.

## Luồng hoạt động

```
Người dùng click "Tải ảnh lên"
    ↓
Bottom Sheet hiện lên với 2 lựa chọn:
    - 📷 Chụp ảnh
    - 🖼️ Chọn từ thư viện
    ↓
Người dùng chọn một trong hai
    ↓
Ảnh được load vào ImageView
    ↓
AI bắt đầu phân tích (2 bước song song):
    1. TensorFlow Lite: Nhận diện đối tượng
    2. Gemini Vision: Tạo tiêu đề + mô tả
    ↓
Kết quả được tự động điền vào form:
    - EditText Title
    - EditText Description
    ↓
Người dùng có thể chỉnh sửa hoặc giữ nguyên
    ↓
Click "Đăng bài" để hoàn tất
```

## Components

### 1. GeminiHelper.java
**Chức năng**: Gọi Google Gemini Vision API để phân tích ảnh

**Methods**:
- `generateDescription(Bitmap, String, DescriptionCallback)`: Phân tích ảnh và tạo mô tả
- `buildPrompt(String)`: Tạo prompt phù hợp với loại bài đăng (lost/found)
- `parseResponse(String)`: Parse kết quả từ API

**Callback Interface**:
```java
interface DescriptionCallback {
    void onSuccess(DescriptionResult result);
    void onError(String error);
}
```

**Result Class**:
```java
class DescriptionResult {
    String title;        // Tiêu đề ngắn gọn
    String description;  // Mô tả chi tiết
}
```

### 2. CreatePostActivity.java

**ActivityResultLaunchers**:
- `pickImage`: Chọn ảnh từ Gallery
- `takePhoto`: Chụp ảnh từ Camera

**Methods mới**:

#### `showImagePickerDialog()`
Hiển thị Bottom Sheet với 3 nút:
- Camera
- Gallery  
- Cancel

#### `openCamera()`
- Tạo file tạm để lưu ảnh
- Sử dụng FileProvider để tạo URI
- Launch camera intent

#### `handleImageSelected(Uri)`
- Hiển thị ảnh trong ImageView
- Gọi TensorFlow Lite classification
- Gọi Gemini AI generation

#### `generateDescriptionWithGemini(Uri)`
- Resize ảnh xuống 1024px
- Gọi GeminiHelper
- Auto-fill title và description
- Hiển thị toast thông báo

#### `resizeBitmap(Bitmap, int)`
- Resize ảnh để tối ưu API call
- Giữ nguyên tỷ lệ aspect ratio

### 3. UI Components

#### bottom_sheet_image_picker.xml
Bottom Sheet dialog với 3 buttons:
- btnCamera: Mở camera
- btnGallery: Mở gallery
- btnCancel: Đóng dialog

#### file_paths.xml
FileProvider configuration cho camera:
```xml
<external-files-path name="images" path="/" />
```

## Permissions

### AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

### Runtime Permission
App tự động yêu cầu quyền CAMERA khi người dùng chọn "Chụp ảnh"

## API Configuration

### build.gradle.kts
```kotlin
buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY") ?: ""}\"")
```

### local.properties
```
GEMINI_API_KEY=your_api_key_here
```

## AI Prompts

### Lost Item Prompt
```
Phân tích ảnh này và tạo:
1. Tiêu đề ngắn gọn (tối đa 50 ký tự) mô tả đồ vật bị mất
2. Mô tả chi tiết (100-200 ký tự) về đặc điểm, màu sắc, nhãn hiệu

Format trả về:
TITLE: [tiêu đề]
DESCRIPTION: [mô tả chi tiết]
```

### Found Item Prompt
```
Phân tích ảnh này và tạo:
1. Tiêu đề ngắn gọn (tối đa 50 ký tự) mô tả đồ vật tìm thấy
2. Mô tả chi tiết (100-200 ký tự) về đặc điểm, màu sắc, nhãn hiệu

Format trả về:
TITLE: [tiêu đề]
DESCRIPTION: [mô tả chi tiết]
```

## Error Handling

### Không có API Key
```java
if (geminiApiKey == null || geminiApiKey.isEmpty()) {
    // geminiHelper = null
    // Tính năng AI bị tắt
}
```

### API Call Failed
```java
@Override
public void onError(String error) {
    tvAiAssisted.setText("⚠️ AI không khả dụng");
    Toast.makeText(this, "Không thể kết nối AI. Vui lòng nhập thủ công.", Toast.LENGTH_SHORT).show();
}
```

### Camera Permission Denied
- App sẽ không crash
- Người dùng vẫn có thể chọn từ Gallery

## Performance Optimization

### Image Resizing
- Resize xuống 1024px trước khi gửi API
- Giảm thời gian upload và xử lý
- Tiết kiệm bandwidth

### Async Processing
- Sử dụng ExecutorService cho background tasks
- UI thread chỉ update UI
- Không block main thread

### API Rate Limiting
- Gemini free tier: 60 requests/phút
- Chỉ gọi API khi có ảnh mới
- Không retry tự động nếu lỗi

## Testing Checklist

- [ ] Camera mở được
- [ ] Gallery mở được
- [ ] Ảnh hiển thị đúng trong ImageView
- [ ] AI tạo tiêu đề và mô tả
- [ ] Có thể chỉnh sửa nội dung AI tạo
- [ ] Đăng bài thành công với ảnh từ camera
- [ ] Đăng bài thành công với ảnh từ gallery
- [ ] Xử lý lỗi khi không có internet
- [ ] Xử lý lỗi khi không có API key
- [ ] Permission camera hoạt động đúng

## Future Improvements

1. **Offline AI**: Sử dụng on-device ML model
2. **Multi-language**: Support English prompts
3. **Image Enhancement**: Auto crop, rotate, enhance
4. **Batch Upload**: Upload nhiều ảnh cùng lúc
5. **AI Confidence**: Hiển thị độ tin cậy của AI
6. **Custom Prompts**: Cho phép user tùy chỉnh prompt
