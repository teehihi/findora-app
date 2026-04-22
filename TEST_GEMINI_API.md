# Test Gemini API Key

## Test bằng curl (Windows PowerShell):

```powershell
$apiKey = "AIzaSyBVkEeuLwO3yo4Dg-5uhyhPHcJzDyYFyeY"

# Test 1: List models
curl "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"

# Test 2: Generate content (text only)
$body = @{
    contents = @(
        @{
            parts = @(
                @{
                    text = "Hello, how are you?"
                }
            )
        }
    )
} | ConvertTo-Json -Depth 10

curl -Method POST `
  -Uri "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey" `
  -ContentType "application/json" `
  -Body $body
```

## Test bằng curl (Linux/Mac):

```bash
API_KEY="AIzaSyBVkEeuLwO3yo4Dg-5uhyhPHcJzDyYFyeY"

# Test 1: List models
curl "https://generativelanguage.googleapis.com/v1beta/models?key=$API_KEY"

# Test 2: Generate content
curl -X POST \
  "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "contents": [{
      "parts": [{
        "text": "Hello"
      }]
    }]
  }'
```

## Kết quả mong đợi:

### Nếu API key hợp lệ:
```json
{
  "candidates": [{
    "content": {
      "parts": [{
        "text": "Hello! How can I help you today?"
      }]
    }
  }]
}
```

### Nếu API key không hợp lệ:
```json
{
  "error": {
    "code": 400,
    "message": "API key not valid. Please pass a valid API key.",
    "status": "INVALID_ARGUMENT"
  }
}
```

### Nếu model không tồn tại:
```json
{
  "error": {
    "code": 404,
    "message": "models/xxx is not found",
    "status": "NOT_FOUND"
  }
}
```

## Các models có sẵn:

Chạy lệnh list models để xem:
```bash
curl "https://generativelanguage.googleapis.com/v1beta/models?key=$API_KEY"
```

Các model phổ biến:
- `gemini-1.5-flash` - Nhanh, rẻ
- `gemini-1.5-pro` - Chất lượng cao
- `gemini-pro-vision` - Deprecated (không dùng)

## Nếu test thành công nhưng app vẫn lỗi:

1. Kiểm tra app có quyền INTERNET không
2. Kiểm tra BuildConfig.GEMINI_API_KEY có đúng không
3. Xem Logcat để debug chi tiết
4. Kiểm tra network security config
