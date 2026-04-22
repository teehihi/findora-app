# Fix Camera Crash

## Vấn đề:
App bị crash khi chọn "Chụp ảnh" từ bottom sheet.

## Nguyên nhân:
Thiếu runtime permission cho CAMERA (Android 6.0+).

## Giải pháp đã áp dụng:

### 1. Thêm Permission Launcher
```java
private final ActivityResultLauncher<String> requestCameraPermission = registerForActivityResult(
    new ActivityResultContracts.RequestPermission(),
    isGranted -> {
        if (isGranted) {
            openCamera();
        } else {
            Toast.makeText(this, "Cần quyền Camera để chụp ảnh", Toast.LENGTH_SHORT).show();
        }
    }
);
```

### 2. Kiểm tra Permission trước khi mở Camera
```java
private void checkCameraPermissionAndOpen() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            == PackageManager.PERMISSION_GRANTED) {
        openCamera();
    } else {
        requestCameraPermission.launch(Manifest.permission.CAMERA);
    }
}
```

### 3. Thêm try-catch trong openCamera()
Để tránh crash nếu có lỗi khác (FileProvider, storage, etc.)

## Luồng hoạt động mới:

```
User click "📷 Chụp ảnh"
    ↓
checkCameraPermissionAndOpen()
    ↓
Kiểm tra permission
    ↓
    ├─ Đã có quyền → openCamera()
    │                    ↓
    │                 Tạo file tạm
    │                    ↓
    │                 Launch camera
    │                    ↓
    │                 User chụp ảnh
    │                    ↓
    │                 handleImageSelected()
    │
    └─ Chưa có quyền → Hiện dialog xin quyền
                           ↓
                       User cho phép → openCamera()
                       User từ chối → Toast thông báo
```

## Test:

1. Clean và Rebuild project
2. Uninstall app cũ (để reset permissions)
3. Install app mới
4. Vào màn hình tạo bài đăng
5. Click "Tải ảnh lên"
6. Click "📷 Chụp ảnh"
7. Lần đầu: Dialog xin quyền Camera sẽ hiện
8. Click "Allow"
9. Camera mở
10. Chụp ảnh
11. Ảnh được load và AI phân tích

## Nếu vẫn crash:

Kiểm tra Logcat để xem lỗi cụ thể:
- FileNotFoundException → Vấn đề FileProvider
- SecurityException → Vấn đề permission
- IllegalArgumentException → URI không hợp lệ

## Files đã cập nhật:

- `CreatePostActivity.java`:
  - Thêm import Manifest, PackageManager, ActivityCompat
  - Thêm requestCameraPermission launcher
  - Thêm checkCameraPermissionAndOpen() method
  - Thêm try-catch trong openCamera()
  - Update button click listener

## Permissions trong AndroidManifest.xml:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

## FileProvider trong AndroidManifest.xml:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

## file_paths.xml:

```xml
<paths>
    <external-files-path name="images" path="/" />
</paths>
```

Tất cả đã được config đúng!
