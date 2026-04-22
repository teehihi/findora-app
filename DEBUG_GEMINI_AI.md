# Debug Gemini AI Issue - ĐÃ KHẮC PHỤC

## Lỗi gốc: "models/gemini-1.5-flash is not found for API version v1beta"

### Nguyên nhân:
SDK của Google Generative AI có vấn đề với model name và API version.

### Giải pháp:
✅ Đã chuyển sang dùng REST API trực tiếp thay vì SDK

### Files đã tạo:
- `GeminiRestHelper.java` - Gọi Gemini API bằng HTTP request trực tiếp
- Không cần dependency phức tạp, chỉ cần HttpURLConnection

## Cách hoạt động mới:

1. Convert ảnh sang Base64
2. Tạo JSON request theo format của Gemini API
3. POST request đến: `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent`
4. Parse JSON response

## Test lại:

1. Clean và Rebuild project
2. Chạy app
3. Tải ảnh lên
4. Kiểm tra Logcat:
   - `GeminiRestHelper: GeminiRestHelper initialized`
   - `GeminiRestHelper: Response code: 200`
   - `GeminiRestHelper: Gemini response: ...`

## Nếu vẫn lỗi:

### Lỗi 400 - Bad Request
→ Format JSON sai (đã fix)

### Lỗi 403 - Forbidden
→ API key không hợp lệ
→ Kiểm tra lại API key

### Lỗi 429 - Too Many Requests
→ Đã hết quota (60 requests/phút)
→ Đợi 1 phút

### Lỗi 404 - Not Found
→ URL sai (đã fix)

## Ưu điểm của REST API:

✅ Không phụ thuộc SDK version
✅ Dễ debug (xem raw request/response)
✅ Ít dependency hơn
✅ Kiểm soát tốt hơn

## Dependencies cần thiết:

Chỉ cần Android standard libraries:
- HttpURLConnection
- JSONObject
- Base64

Không cần:
- ❌ com.google.ai.client.generativeai
- ❌ kotlinx-coroutines
- ❌ guava (có thể giữ nếu dùng cho mục đích khác)

## 1. Sync Gradle
**QUAN TRỌNG**: Sau khi thêm API key vào `local.properties`, bạn PHẢI sync Gradle!

```
File > Sync Project with Gradle Files
```

Hoặc click nút "Sync Now" ở góc trên bên phải.

## 2. Clean và Rebuild
```
Build > Clean Project
Build > Rebuild Project
```

Điều này sẽ regenerate file `BuildConfig.java` với API key mới.

## 3. Kiểm tra Logcat
Khi chạy app, mở Logcat và filter:
- Tag: `CreatePost`
- Tag: `GeminiHelper`

Tìm các log:
```
D/CreatePost: Gemini API Key length: XX
D/CreatePost: GeminiHelper initialized successfully
D/GeminiHelper: GeminiHelper initialized successfully with model: gemini-1.5-flash
```

Nếu thấy:
```
W/CreatePost: Gemini API key not configured
```
→ BuildConfig chưa được update, cần sync Gradle lại.

## 4. Kiểm tra API Key hợp lệ

### Test API key bằng curl:
```bash
curl "https://generativelanguage.googleapis.com/v1/models?key=YOUR_API_KEY"
```

Nếu API key đúng, sẽ trả về danh sách models.

Nếu sai, sẽ báo lỗi:
```json
{
  "error": {
    "code": 400,
    "message": "API key not valid"
  }
}
```

## 5. Kiểm tra Internet Permission
Đảm bảo app có quyền Internet (đã có trong AndroidManifest.xml):
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## 6. Kiểm tra Model Name
Code đã update sang model mới: `gemini-1.5-flash`

Model cũ `gemini-pro-vision` đã deprecated.

## 7. Kiểm tra Dependencies
Đảm bảo có đủ dependencies trong `build.gradle.kts`:
```kotlin
implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
implementation("com.google.guava:guava:31.1-android")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
```

## 8. Test với ảnh đơn giản
Thử với ảnh nhỏ, đơn giản trước (< 1MB).

## Các lỗi thường gặp:

### Lỗi: "API key not valid"
→ API key sai hoặc chưa được enable
→ Kiểm tra lại tại: https://makersuite.google.com/app/apikey

### Lỗi: "quota exceeded"
→ Đã hết quota miễn phí (60 requests/phút)
→ Đợi 1 phút rồi thử lại

### Lỗi: "network error"
→ Không có internet
→ Kiểm tra kết nối mạng

### Lỗi: "model not found"
→ Model name sai
→ Đã fix: dùng `gemini-1.5-flash`

## Checklist đầy đủ:

- [ ] API key đã thêm vào `local.properties`
- [ ] Đã Sync Gradle
- [ ] Đã Clean và Rebuild project
- [ ] Kiểm tra Logcat thấy "GeminiHelper initialized successfully"
- [ ] Test API key bằng curl (hoặc Postman)
- [ ] Có internet connection
- [ ] Dependencies đã update
- [ ] Chạy lại app

## Nếu vẫn lỗi:

1. Xóa app khỏi thiết bị
2. Clean project
3. Rebuild project
4. Install lại app
5. Kiểm tra Logcat kỹ để xem lỗi cụ thể

## Contact Support:
Nếu vẫn không được, gửi Logcat output để debug chi tiết hơn.
