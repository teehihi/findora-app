package hcmute.edu.vn.findora.utils;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Alternative Gemini helper using REST API directly
 * Dùng REST API trực tiếp thay vì SDK để tránh lỗi version
 */
public class GeminiRestHelper {
    
    private static final String TAG = "GeminiRestHelper";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    
    private final String apiKey;
    private final Executor executor;
    
    public GeminiRestHelper(String apiKey) {
        this.apiKey = apiKey;
        this.executor = Executors.newSingleThreadExecutor();
        Log.d(TAG, "GeminiRestHelper initialized");
    }
    
    /**
     * Generate description from image
     */
    public void generateDescription(Bitmap bitmap, String type, DescriptionCallback callback) {
        Log.d(TAG, "generateDescription called with type: " + type);
        
        executor.execute(() -> {
            try {
                String prompt = buildPrompt(type);
                Log.d(TAG, "Prompt: " + prompt);
                
                String base64Image = bitmapToBase64(bitmap);
                Log.d(TAG, "Image converted to base64, length: " + base64Image.length());
                
                // Build JSON request
                JSONObject request = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                
                // Add text part
                JSONObject textPart = new JSONObject();
                textPart.put("text", prompt);
                parts.put(textPart);
                
                // Add image part
                JSONObject imagePart = new JSONObject();
                JSONObject inlineData = new JSONObject();
                inlineData.put("mime_type", "image/jpeg");
                inlineData.put("data", base64Image);
                imagePart.put("inline_data", inlineData);
                parts.put(imagePart);
                
                content.put("parts", parts);
                contents.put(content);
                request.put("contents", contents);
                
                Log.d(TAG, "Request JSON built successfully");
                
                // Make HTTP request
                String urlString = API_URL + "?key=" + apiKey;
                Log.d(TAG, "Calling API: " + API_URL);
                
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000); // 30 seconds
                conn.setReadTimeout(30000);
                
                // Send request
                OutputStream os = conn.getOutputStream();
                os.write(request.toString().getBytes("UTF-8"));
                os.flush();
                os.close();
                
                Log.d(TAG, "Request sent, waiting for response...");
                
                // Read response
                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response code: " + responseCode);
                
                if (responseCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();
                    
                    Log.d(TAG, "Response received: " + response.toString());
                    
                    // Parse response
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String text = jsonResponse
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");
                    
                    Log.d(TAG, "Parsed text: " + text);
                    
                    DescriptionResult result = parseResponse(text);
                    Log.d(TAG, "Final result - Title: " + result.title + ", Description: " + result.description);
                    
                    callback.onSuccess(result);
                } else {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder error = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        error.append(line);
                    }
                    br.close();
                    
                    String errorMsg = error.toString();
                    Log.e(TAG, "API error (" + responseCode + "): " + errorMsg);
                    
                    // Parse error message
                    String userMsg = "Lỗi API: " + responseCode;
                    try {
                        JSONObject errorJson = new JSONObject(errorMsg);
                        if (errorJson.has("error")) {
                            String message = errorJson.getJSONObject("error").getString("message");
                            userMsg = "Lỗi: " + message;
                        }
                    } catch (Exception e) {
                        // Keep default message
                    }
                    
                    callback.onError(userMsg);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Exception in generateDescription", e);
                e.printStackTrace();
                callback.onError("Lỗi: " + e.getMessage());
            }
        });
    }
    
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }
    
    private String buildPrompt(String type) {
        if ("lost".equals(type)) {
            return "Phân tích ảnh này và tạo:\n" +
                   "1. Tiêu đề ngắn gọn (tối đa 50 ký tự) mô tả đồ vật bị mất\n" +
                   "2. Mô tả chi tiết (100-200 ký tự) về đặc điểm, màu sắc, nhãn hiệu\n\n" +
                   "Format trả về:\n" +
                   "TITLE: [tiêu đề]\n" +
                   "DESCRIPTION: [mô tả chi tiết]";
        } else {
            return "Phân tích ảnh này và tạo:\n" +
                   "1. Tiêu đề ngắn gọn (tối đa 50 ký tự) mô tả đồ vật tìm thấy\n" +
                   "2. Mô tả chi tiết (100-200 ký tự) về đặc điểm, màu sắc, nhãn hiệu\n\n" +
                   "Format trả về:\n" +
                   "TITLE: [tiêu đề]\n" +
                   "DESCRIPTION: [mô tả chi tiết]";
        }
    }
    
    private DescriptionResult parseResponse(String text) {
        String title = "";
        String description = "";
        
        try {
            String[] lines = text.split("\n");
            for (String line : lines) {
                if (line.startsWith("TITLE:")) {
                    title = line.substring(6).trim();
                } else if (line.startsWith("DESCRIPTION:")) {
                    description = line.substring(12).trim();
                }
            }
            
            if (title.isEmpty() && description.isEmpty()) {
                int dotIndex = text.indexOf(".");
                if (dotIndex > 0 && dotIndex < 50) {
                    title = text.substring(0, dotIndex).trim();
                    description = text.substring(dotIndex + 1).trim();
                } else {
                    title = text.length() > 50 ? text.substring(0, 50) : text;
                    description = text;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse response", e);
            title = "Đồ vật";
            description = text;
        }
        
        return new DescriptionResult(title, description);
    }
    
    public interface DescriptionCallback {
        void onSuccess(DescriptionResult result);
        void onError(String error);
    }
    
    public static class DescriptionResult {
        public final String title;
        public final String description;
        
        public DescriptionResult(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }
}
