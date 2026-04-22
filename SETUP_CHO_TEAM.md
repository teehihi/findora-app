# Hướng dẫn Setup cho Team

## Clone project từ GitHub

```bash
git clone https://github.com/your-username/findora-app.git
cd findora-app
```

## Setup API Keys

### 1. Tạo file local.properties
```bash
# Copy từ file example
cp local.properties.example local.properties
```

### 2. Lấy Gemini API Key
1. Truy cập: https://makersuite.google.com/app/apikey
2. Đăng nhập Google Account
3. Click "Create API Key"
4. Copy key

### 3. Thêm vào local.properties
Mở file `local.properties` và thay thế:
```properties
GEMINI_API_KEY=paste_your_key_here
```

### 4. Sync Gradle
- Mở project trong Android Studio
- Click "Sync Now"
- Hoặc: File > Sync Project with Gradle Files

## Lưu ý quan trọng

⚠️ **KHÔNG BAO GIỜ commit file `local.properties`**
- File này chứa API keys nhạy cảm
- Đã được thêm vào `.gitignore`
- Mỗi developer có file riêng

⚠️ **KHÔNG BAO GIỜ commit `google-services.json`**
- File này chứa Firebase config
- Cũng đã được ignore

## Files cần ignore (đã config sẵn)
- ✅ `local.properties` - API keys
- ✅ `google-services.json` - Firebase config
- ✅ `build/` - Build artifacts
- ✅ `.gradle/` - Gradle cache

## Kiểm tra trước khi commit

```bash
# Xem files sẽ được commit
git status

# Đảm bảo KHÔNG có:
# - local.properties
# - google-services.json
# - build/
```

## Nếu đã commit nhầm API key

```bash
# Remove from git but keep local file
git rm --cached local.properties

# Commit the removal
git commit -m "Remove local.properties from git"

# Push
git push
```

Sau đó nên đổi API key mới vì key cũ đã bị lộ.

## Team workflow

1. Developer A push code (không có API key)
2. Developer B pull code
3. Developer B tạo `local.properties` riêng
4. Developer B thêm API key của mình
5. Developer B build và run thành công

Mỗi người có API key riêng, không share với nhau.
