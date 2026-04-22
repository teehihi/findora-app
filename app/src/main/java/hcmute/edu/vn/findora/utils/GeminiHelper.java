package hcmute.edu.vn.findora.utils;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Helper class để sử dụng Google Gemini Vision API
 * 
 * CHỨC NĂNG:
 * - Phân tích ảnh và tạo tiêu đề + mô tả
 * - Sử dụng Gemini Pro Vision model
 * - Async callback để không block UI
 * 
 * API KEY:
 * - Lưu trong local.properties: GEMINI_API_KEY=your_key_here
 * - Get free key tại: https://makersuite.google.com/app/apikey
 */
public class GeminiHelper {
    
    private static final String TAG = "GeminiHelper";
    private static final String MODEL_NAME = "gemini-1.5-pro-latest";
    
    private final GenerativeModelFutures model;
    private final Executor executor;
    
    /**
     * Constructor
     * 
     * @param apiKey Gemini API key
     */
    public GeminiHelper(String apiKey) {
        try {
            GenerativeModel gm = new GenerativeModel(MODEL_NAME, apiKey);
            this.model = GenerativeModelFutures.from(gm);
            this.executor = Executors.newSingleThreadExecutor();
            Log.d(TAG, "GeminiHelper initialized successfully with model: " + MODEL_NAME);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize GeminiHelper", e);
            throw e;
        }
    }
    
    /**
     * Phân tích ảnh và tạo tiêu đề + mô tả
     * 
     * @param bitmap Ảnh cần phân tích
     * @param type Loại bài đăng ("lost" hoặc "found")
     * @param callback Callback trả về kết quả
     */
    public void generateDescription(Bitmap bitmap, String type, DescriptionCallback callback) {
        String prompt = buildPrompt(type);
        
        Log.d(TAG, "Starting image analysis with prompt type: " + type);
        
        try {
            Content content = new Content.Builder()
                .addText(prompt)
                .addImage(bitmap)
                .build();
            
            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
            
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    try {
                        String text = result.getText();
                        Log.d(TAG, "Gemini response received: " + text);
                        
                        // Parse response
                        DescriptionResult parsed = parseResponse(text);
                        callback.onSuccess(parsed);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing Gemini response", e);
                        callback.onError("Lỗi xử lý kết quả: " + e.getMessage());
                    }
                }
                
                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "Gemini API failed", t);
                    String errorMsg = t.getMessage();
                    if (errorMsg != null) {
                        if (errorMsg.contains("API key")) {
                            callback.onError("API key không hợp lệ");
                        } else if (errorMsg.contains("quota")) {
                            callback.onError("Đã hết quota API");
                        } else if (errorMsg.contains("network") || errorMsg.contains("connection")) {
                            callback.onError("Lỗi kết nối mạng");
                        } else {
                            callback.onError("Lỗi API: " + errorMsg);
                        }
                    } else {
                        callback.onError("Không thể kết nối AI");
                    }
                }
            }, executor);
        } catch (Exception e) {
            Log.e(TAG, "Error creating content", e);
            callback.onError("Lỗi xử lý ảnh: " + e.getMessage());
        }
    }
    
    /**
     * Build prompt dựa trên loại bài đăng
     */
    private String buildPrompt(String type) {
        if ("lost".equals(type)) {
            return "Phân tích ảnh này và tạo:\n" +
                   "1. Tiêu đề ngắn gọn (tối đa 50 ký tự) mô tả đồ vật bị mất\n" +
                   "2. Mô tả chi tiết (100-200 ký tự) về đặc điểm, màu sắc, nhãn hiệu\n\n" +
                   "Format trả về:\n" +
                   "TITLE: [tiêu đề]\n" +
                   "DESCRIPTION: [mô tả chi tiết]\n\n" +
                   "Ví dụ:\n" +
                   "TITLE: Mất ví da màu nâu\n" +
                   "DESCRIPTION: Ví da màu nâu, có logo LV, bên trong có CMND và thẻ ATM";
        } else {
            return "Phân tích ảnh này và tạo:\n" +
                   "1. Tiêu đề ngắn gọn (tối đa 50 ký tự) mô tả đồ vật tìm thấy\n" +
                   "2. Mô tả chi tiết (100-200 ký tự) về đặc điểm, màu sắc, nhãn hiệu\n\n" +
                   "Format trả về:\n" +
                   "TITLE: [tiêu đề]\n" +
                   "DESCRIPTION: [mô tả chi tiết]\n\n" +
                   "Ví dụ:\n" +
                   "TITLE: Tìm thấy điện thoại iPhone\n" +
                   "DESCRIPTION: iPhone 13 màu xanh, có ốp lưng trong suốt, màn hình còn nguyên";
        }
    }
    
    /**
     * Parse response từ Gemini
     */
    private DescriptionResult parseResponse(String text) {
        String title = "";
        String description = "";
        
        try {
            // Parse format: TITLE: xxx\nDESCRIPTION: yyy
            String[] lines = text.split("\n");
            for (String line : lines) {
                if (line.startsWith("TITLE:")) {
                    title = line.substring(6).trim();
                } else if (line.startsWith("DESCRIPTION:")) {
                    description = line.substring(12).trim();
                }
            }
            
            // Fallback: nếu không parse được, dùng toàn bộ text
            if (title.isEmpty() && description.isEmpty()) {
                // Split by first sentence
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
    
    /**
     * Callback interface
     */
    public interface DescriptionCallback {
        void onSuccess(DescriptionResult result);
        void onError(String error);
    }
    
    /**
     * Result class
     */
    public static class DescriptionResult {
        public final String title;
        public final String description;
        
        public DescriptionResult(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }
}
