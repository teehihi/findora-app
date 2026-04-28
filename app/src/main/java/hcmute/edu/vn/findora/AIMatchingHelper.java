package hcmute.edu.vn.findora;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hcmute.edu.vn.findora.model.Post;

/**
 * AIMatchingHelper - Lớp trợ giúp tìm kiếm và gợi ý các bài đăng phù hợp.
 * 
 * CHỨC NĂNG CHÍNH:
 * - Tìm các bài đăng "found" phù hợp với bài đăng "lost" và ngược lại
 * - Sử dụng thuật toán AI kết hợp 3 yếu tố theo thứ tự ưu tiên:
 *   1. Image Similarity (Độ tương đồng hình ảnh qua AI) - 50% (Ưu tiên cao nhất)
 *   2. Content Similarity (Độ tương đồng nội dung) - 40%
 *   3. Location Distance (Khoảng cách địa lý) - 10% (Chỉ là yếu tố phụ)
 * 
 * LOGIC MATCHING:
 * - Ưu tiên 1: So sánh ảnh bằng TensorFlow Lite classification
 *   → Nếu cùng label (VD: cả 2 đều là "key") → điểm cao
 * - Ưu tiên 2: So sánh tiêu đề và mô tả
 *   → Tìm từ khóa trùng khớp (mèo, chìa khóa, ví, điện thoại...)
 * - Ưu tiên 3: Khoảng cách địa lý
 *   → Chỉ dùng để xếp hạng khi Image và Content đã match
 *   → Nếu > 500km → loại bỏ hoàn toàn
 * 
 * ĐƯỢC GỌI TỪ:
 * - MainActivity.java: Hiển thị gợi ý AI cho người dùng
 * - PostDetailActivity.java: Hiển thị các bài đăng liên quan
 * 
 * VÍ DỤ SỬ DỤNG:
 * <pre>
 * {@code
 * // Tìm các bài đăng phù hợp với bài đăng hiện tại
 * List<MatchResult> matches = AIMatchingHelper.findMatches(currentPost, allPosts);
 * 
 * // Hiển thị kết quả
 * for (MatchResult match : matches) {
 *     Log.d(TAG, "Match: " + match.post.getTitle() + " - " + match.getScorePercentage() + "%");
 * }
 * }
 * </pre>
 * 
 * THUẬT TOÁN:
 * - Image Classification Matching (TensorFlow Lite)
 * - Jaccard Similarity cho văn bản (tính độ giống nhau giữa 2 tập từ)
 * - Haversine Formula cho khoảng cách địa lý (tính khoảng cách trên mặt cầu)
 * 
 * @author Findora Team
 * @version 2.0
 * @since 2024
 */
public class AIMatchingHelper {
    
    private static final String TAG = "AIMatching";
    
    // ==================== NGƯỠNG CẤU HÌNH ====================
    
    /**
     * Điểm tối thiểu để 2 bài đăng được coi là match (20%).
     * Nếu điểm < 0.2 thì không hiển thị gợi ý.
     */
    private static final double MIN_MATCH_SCORE = 0.2;
    
    /**
     * Khoảng cách tối đa TUYỆT ĐỐI để 2 bài đăng có thể match (500km).
     * Nếu khoảng cách > 500km thì KHÔNG BAO GIỜ match.
     * 
     * VÍ DỤ: TP.HCM ↔ Hà Nội (~1700km) → KHÔNG match
     */
    private static final double ABSOLUTE_MAX_DISTANCE_KM = 500.0;
    
    /**
     * Khoảng cách để tính điểm location (50km).
     * Dùng để tính điểm phụ khi image và text đã match.
     */
    private static final double MAX_DISTANCE_KM = 50.0;
    
    /**
     * Khoảng thời gian tối đa để 2 bài đăng được coi là gần nhau (30 ngày).
     * Nếu thời gian > 30 ngày thì điểm time = 0.
     */
    private static final long MAX_TIME_DIFF_DAYS = 30;
    
    // ==================== HÀM CHÍNH ====================
    
    /**
     * Tìm các bài đăng phù hợp với bài đăng hiện tại.
     * 
     * CHỨC NĂNG:
     * - Lọc các bài đăng có loại đối lập (lost ↔ found)
     * - Tính điểm match cho từng bài đăng
     * - Lọc các bài đăng có điểm >= MIN_MATCH_SCORE (30%)
     * - Sắp xếp theo điểm số giảm dần
     * 
     * ĐƯỢC GỌI TỪ:
     * - MainActivity.java: Khi load danh sách bài đăng để hiển thị gợi ý AI
     * - PostDetailActivity.java: Khi xem chi tiết bài đăng để hiển thị các bài liên quan
     * 
     * LOGIC:
     * 1. Xác định loại bài đăng cần tìm (lost → tìm found, found → tìm lost)
     * 2. Duyệt qua tất cả bài đăng trong danh sách
     * 3. Bỏ qua bài đăng không phù hợp (cùng loại, cùng user)
     * 4. Tính điểm match bằng calculateMatchScore()
     * 5. Thêm vào danh sách nếu điểm >= 30%
     * 6. Sắp xếp theo điểm giảm dần
     * 
     * @param currentPost Bài đăng hiện tại (lost hoặc found) - bài đăng cần tìm match
     * @param allPosts Danh sách tất cả bài đăng trong hệ thống (từ Firestore)
     * @return Danh sách MatchResult chứa bài đăng và điểm số, sắp xếp theo điểm giảm dần
     * 
     * VÍ DỤ:
     * <pre>
     * {@code
     * // Người dùng mất mèo ở Thủ Đức
     * Post lostCatPost = new Post("Mất mèo", "Mèo vàng, 2 tuổi", "lost", ...);
     * lostCatPost.setLat(10.8505);
     * lostCatPost.setLng(106.7717);
     * 
     * // Tìm các bài đăng "found" phù hợp
     * List<MatchResult> matches = AIMatchingHelper.findMatches(lostCatPost, allPosts);
     * 
     * // Kết quả: Các bài "found mèo" ở Thủ Đức sẽ có điểm cao
     * // VD: "Tìm thấy mèo vàng ở Thủ Đức" → 85%
     * //     "Tìm thấy chó ở Thủ Đức" → 20% (không match vì khác loại vật)
     * //     "Tìm thấy mèo ở Quận 1" → 40% (match nhưng xa)
     * }
     * </pre>
     */
    public static List<MatchResult> findMatches(Post currentPost, List<Post> allPosts) {
        List<MatchResult> matches = new ArrayList<>();
        
        // Chỉ match giữa "lost" và "found"
        String targetType = currentPost.getType().equals("lost") ? "found" : "lost";
        
        for (Post post : allPosts) {
            // Skip nếu không phải loại đối lập
            if (!post.getType().equals(targetType)) continue;
            
            // Skip nếu là bài của chính mình
            if (post.getUserId().equals(currentPost.getUserId())) continue;
            
            // ===== LOẠI BỎ BÀI ĐÃ GIẢI QUYẾT =====
            // Không ghép cặp các bài đã resolved/closed
            String status = post.getStatus();
            if ("resolved".equals(status) || "closed".equals(status)) {
                Log.d(TAG, String.format("Skipped: %s - Already resolved", post.getTitle()));
                continue;
            }
            
            // ===== KIỂM TRA KHOẢNG CÁCH TUYỆT ĐỐI =====
            // Nếu cả 2 bài đều có location, kiểm tra khoảng cách
            if (currentPost.getLat() != null && currentPost.getLng() != null &&
                post.getLat() != null && post.getLng() != null) {
                
                double distance = calculateDistance(
                    currentPost.getLat(), currentPost.getLng(),
                    post.getLat(), post.getLng()
                );
                
                // Nếu quá xa (> 500km) thì KHÔNG match, bỏ qua luôn
                if (distance > ABSOLUTE_MAX_DISTANCE_KM) {
                    Log.d(TAG, String.format("Skipped: %s - Too far (%.1f km > 500km)", 
                        post.getTitle(), distance));
                    continue;
                }
            }
            
            // Tính điểm match
            double score = calculateMatchScore(currentPost, post);
            
            if (score >= MIN_MATCH_SCORE) {
                matches.add(new MatchResult(post, score));
                Log.d(TAG, String.format("Match found: %s (%.2f%%)", 
                    post.getTitle(), score * 100));
            }
        }
        
        // Sắp xếp theo điểm số giảm dần
        Collections.sort(matches, (a, b) -> Double.compare(b.score, a.score));
        
        return matches;
    }
    
    
    // ==================== TÍNH ĐIỂM MATCH ====================
    
    /**
     * Tính điểm match tổng hợp giữa 2 bài đăng.
     * 
     * LOGIC MỚI - Ưu tiên theo thứ tự:
     * 1. Hình ảnh (Image AI Classification) - Nếu có
     * 2. Nội dung (Title + Description) - Luôn tính
     * 3. Vị trí (Location) - Yếu tố phụ
     * 
     * ĐƯỢC GỌI TỪ:
     * - findMatches(): Tính điểm cho từng cặp bài đăng
     * 
     * @param post1 Bài đăng thứ nhất (thường là bài đăng hiện tại)
     * @param post2 Bài đăng thứ hai (bài đăng cần so sánh)
     * @return Điểm match từ 0.0 đến 1.0
     */
    private static double calculateMatchScore(Post post1, Post post2) {
        // 1. Image similarity - Ưu tiên cao nhất nếu có
        double imageScore = calculateImageSimilarity(post1, post2);
        boolean hasImageMatch = (post1.getImageLabel() != null && !post1.getImageLabel().isEmpty() &&
                                 post2.getImageLabel() != null && !post2.getImageLabel().isEmpty());
        
        // 2. Content similarity - Title + Description
        double contentScore = calculateContentSimilarity(post1, post2);
        
        // 3. Location score - Yếu tố phụ
        double locationScore = calculateLocationScore(post1, post2);
        
        // Tính điểm tổng - Điều chỉnh trọng số dựa trên việc có image hay không
        double totalScore;
        if (hasImageMatch) {
            // Có image → Ưu tiên Image (50%) + Content (40%) + Location (10%)
            totalScore = (imageScore * 0.5) + (contentScore * 0.4) + (locationScore * 0.1);
            Log.d(TAG, String.format("WITH IMAGE - Image: %.2f, Content: %.2f, Location: %.2f, Total: %.2f",
                imageScore, contentScore, locationScore, totalScore));
        } else {
            // Không có image → Chỉ dựa vào Content (80%) + Location (20%)
            totalScore = (contentScore * 0.8) + (locationScore * 0.2);
            Log.d(TAG, String.format("NO IMAGE - Content: %.2f, Location: %.2f, Total: %.2f",
                contentScore, locationScore, totalScore));
        }
        
        return totalScore;
    }
    
    
    // ==================== IMAGE SIMILARITY ====================
    
    /**
     * Tính độ tương đồng hình ảnh giữa 2 bài đăng (AI Classification).
     * 
     * CHỨC NĂNG:
     * - So sánh imageLabel từ TensorFlow Lite classification
     * - Nếu cùng label (VD: cả 2 đều là "key") → điểm cao
     * - Điểm được nhân với confidence để đảm bảo độ chính xác
     * 
     * ĐƯỢC GỌI TỪ:
     * - calculateMatchScore(): Tính điểm image similarity
     * 
     * @param post1 Bài đăng thứ nhất
     * @param post2 Bài đăng thứ hai
     * @return Điểm từ 0.0 đến 1.0 (hoặc -1 nếu không có image)
     */
    private static double calculateImageSimilarity(Post post1, Post post2) {
        String label1 = post1.getImageLabel();
        String label2 = post2.getImageLabel();
        
        // Nếu không có label → trả về 0 (sẽ không dùng image score)
        if (label1 == null || label1.isEmpty() || label2 == null || label2.isEmpty()) {
            return 0.0;
        }
        
        // So sánh label (case-insensitive)
        if (label1.equalsIgnoreCase(label2)) {
            // Cùng label → lấy confidence trung bình
            double conf1 = post1.getConfidence() != null ? post1.getConfidence() : 0.5;
            double conf2 = post2.getConfidence() != null ? post2.getConfidence() : 0.5;
            
            // Điểm = trung bình confidence của 2 ảnh
            double score = (conf1 + conf2) / 2.0;
            
            Log.d(TAG, String.format("✓ Image MATCH! '%s' vs '%s' (Conf: %.0f%% & %.0f%%) → Score: %.2f",
                label1, label2, conf1*100, conf2*100, score));
            
            return score;
        }
        
        Log.d(TAG, String.format("✗ Image mismatch: '%s' vs '%s'", label1, label2));
        return 0.0;
    }
    
    
    // ==================== CONTENT SIMILARITY ====================
    
    /**
     * Tính độ tương đồng nội dung giữa 2 bài đăng (Title + Description).
     * 
     * CHỨC NĂNG:
     * - Kết hợp Title similarity (70%) và Description similarity (30%)
     * - Ưu tiên title vì title thường chứa từ khóa chính
     * 
     * ĐƯỢC GỌI TỪ:
     * - calculateMatchScore(): Tính điểm content similarity (40% tổng điểm)
     * 
     * @param post1 Bài đăng thứ nhất
     * @param post2 Bài đăng thứ hai
     * @return Điểm từ 0.0 đến 1.0
     */
    private static double calculateContentSimilarity(Post post1, Post post2) {
        double titleScore = calculateTitleSimilarity(post1, post2);
        double descScore = calculateTextSimilarity(post1, post2);
        
        // Title quan trọng hơn description
        return (titleScore * 0.7) + (descScore * 0.3);
    }
    
    
    // ==================== TEXT SIMILARITY ====================
    
    /**
     * Tính độ tương đồng TIÊU ĐỀ giữa 2 bài đăng (chỉ so sánh title).
     * 
     * CHỨC NĂNG:
     * - Ưu tiên so sánh tiêu đề để match chính xác (mèo với mèo, chìa khóa với chìa khóa)
     * - Sử dụng Jaccard Similarity + Keyword Matching
     * - Thêm bonus lớn nếu có từ khóa quan trọng giống nhau
     * 
     * ĐƯỢC GỌI TỪ:
     * - calculateMatchScore(): Tính điểm title similarity (50% tổng điểm)
     * 
     * @param post1 Bài đăng thứ nhất
     * @param post2 Bài đăng thứ hai
     * @return Điểm từ 0.0 đến 1.0+
     */
    private static double calculateTitleSimilarity(Post post1, Post post2) {
        String title1 = post1.getTitle().toLowerCase();
        String title2 = post2.getTitle().toLowerCase();
        
        Log.d(TAG, String.format("Comparing titles: '%s' vs '%s'", title1, title2));
        
        // Tokenize thành words
        String[] words1 = title1.split("\\s+");
        String[] words2 = title2.split("\\s+");
        
        // Tạo set để tính Jaccard
        java.util.Set<String> set1 = new java.util.HashSet<>();
        java.util.Set<String> set2 = new java.util.HashSet<>();
        
        for (String word : words1) {
            if (word.length() > 1) set1.add(word); // Bỏ qua từ quá ngắn
        }
        for (String word : words2) {
            if (word.length() > 1) set2.add(word);
        }
        
        // Tính Jaccard similarity
        java.util.Set<String> intersection = new java.util.HashSet<>(set1);
        intersection.retainAll(set2);
        
        java.util.Set<String> union = new java.util.HashSet<>(set1);
        union.addAll(set2);
        
        if (union.isEmpty()) return 0.0;
        
        double jaccard = (double) intersection.size() / union.size();
        
        // Bonus LỚN nếu có từ khóa quan trọng giống nhau
        double keywordBonus = calculateKeywordBonus(title1, title2) * 3.0; // x3 bonus để dễ match hơn
        
        double finalScore = Math.min(1.0, jaccard + keywordBonus);
        
        Log.d(TAG, String.format("  → Jaccard: %.2f, Keyword bonus: %.2f, Final: %.2f",
            jaccard, keywordBonus, finalScore));
        
        return finalScore;
    }
    
    /**
     * Tính độ tương đồng văn bản giữa 2 bài đăng (title + description).
     * 
     * CHỨC NĂNG:
     * - Sử dụng Jaccard Similarity để so sánh 2 tập từ
     * - Thêm bonus nếu có từ khóa quan trọng giống nhau (mèo, chó, ví, điện thoại...)
     * 
     * ĐƯỢC GỌI TỪ:
     * - calculateMatchScore(): Tính điểm text similarity (40% tổng điểm)
     * 
     * THUẬT TOÁN JACCARD:
     * Jaccard = |A ∩ B| / |A ∪ B|
     * - A: Tập từ của bài đăng 1
     * - B: Tập từ của bài đăng 2
     * - ∩: Giao (từ xuất hiện ở cả 2 bài)
     * - ∪: Hợp (tất cả từ của 2 bài)
     * 
     * @param post1 Bài đăng thứ nhất
     * @param post2 Bài đăng thứ hai
     * @return Điểm từ 0.0 (hoàn toàn khác) đến 1.0+ (giống nhau + có keyword bonus)
     * 
     * VÍ DỤ:
     * <pre>
     * {@code
     * // Post 1: "Mất mèo vàng ở công viên"
     * // Post 2: "Tìm thấy mèo vàng"
     * // → words1 = {mất, mèo, vàng, công, viên}
     * // → words2 = {tìm, thấy, mèo, vàng}
     * // → intersection = {mèo, vàng} → size = 2
     * // → union = {mất, mèo, vàng, công, viên, tìm, thấy} → size = 7
     * // → jaccard = 2/7 = 0.286
     * // → keywordBonus = 0.1 (có từ "mèo")
     * // → totalScore = 0.286 + 0.1 = 0.386 (38.6%)
     * }
     * </pre>
     */
    private static double calculateTextSimilarity(Post post1, Post post2) {
        String text1 = (post1.getTitle() + " " + post1.getDescription()).toLowerCase();
        String text2 = (post2.getTitle() + " " + post2.getDescription()).toLowerCase();
        
        // Tokenize thành words
        String[] words1 = text1.split("\\s+");
        String[] words2 = text2.split("\\s+");
        
        // Tạo set để tính Jaccard
        java.util.Set<String> set1 = new java.util.HashSet<>();
        java.util.Set<String> set2 = new java.util.HashSet<>();
        
        for (String word : words1) {
            if (word.length() > 2) set1.add(word); // Bỏ qua từ quá ngắn
        }
        for (String word : words2) {
            if (word.length() > 2) set2.add(word);
        }
        
        // Tính Jaccard similarity
        java.util.Set<String> intersection = new java.util.HashSet<>(set1);
        intersection.retainAll(set2);
        
        java.util.Set<String> union = new java.util.HashSet<>(set1);
        union.addAll(set2);
        
        if (union.isEmpty()) return 0.0;
        
        double jaccard = (double) intersection.size() / union.size();
        
        // Bonus nếu có từ khóa quan trọng giống nhau
        double keywordBonus = calculateKeywordBonus(text1, text2);
        
        return Math.min(1.0, jaccard + keywordBonus);
    }
    
    
    /**
     * Tính bonus điểm cho các từ khóa quan trọng.
     * 
     * CHỨC NĂNG:
     * - Kiểm tra xem 2 bài đăng có chứa cùng từ khóa quan trọng không
     * - Mỗi từ khóa match → +10% điểm
     * 
     * ĐƯỢC GỌI TỪ:
     * - calculateTextSimilarity(): Thêm bonus vào điểm Jaccard
     * 
     * DANH SÁCH TỪ KHÓA:
     * - Động vật: mèo, chó
     * - Đồ vật: ví, điện thoại, iphone, samsung, chìa khóa, xe, túi xách, laptop, máy tính
     * - Trang sức: đồng hồ, nhẫn, vòng, dây chuyền
     * 
     * @param text1 Văn bản của bài đăng 1 (đã lowercase)
     * @param text2 Văn bản của bài đăng 2 (đã lowercase)
     * @return Điểm bonus (0.0 nếu không có keyword match, 0.1 cho mỗi keyword match)
     * 
     * VÍ DỤ:
     * <pre>
     * {@code
     * // text1 = "mất mèo và chìa khóa xe"
     * // text2 = "tìm thấy mèo và chìa khóa"
     * // → Match: "mèo" (1), "chìa khóa" (1) → 2 keywords
     * // → bonus = 2 × 0.1 = 0.2 (20%)
     * }
     * </pre>
     */
    private static double calculateKeywordBonus(String text1, String text2) {
        String[] keywords = {
            // Động vật
            "mèo", "chó", "thú cưng", "pet", "con mèo", "con chó",
            // Đồ điện tử
            "ví", "điện thoại", "iphone", "samsung", "oppo", "xiaomi", "vivo",
            "laptop", "máy tính", "macbook", "ipad", "tablet", "tai nghe", "airpod",
            // Đồ dùng cá nhân
            "chìa khóa", "chìa khoá", "xe", "xe máy", "ô tô", "túi xách", "ba lô", "cặp",
            "ví tiền", "thẻ", "cmnd", "cccd", "bằng lái", "giấy tờ",
            // Trang sức
            "đồng hồ", "nhẫn", "vòng", "dây chuyền", "bông tai", "lắc tay",
            // Quần áo
            "áo", "quần", "giày", "dép", "mũ", "kính", "kính mắt",
            // Khác
            "sách", "vở", "bút", "cặp sách", "balo"
        };
        
        int matchCount = 0;
        for (String keyword : keywords) {
            if (text1.contains(keyword) && text2.contains(keyword)) {
                matchCount++;
            }
        }
        
        return matchCount * 0.1; // Mỗi keyword match +10%
    }
    
    
    // ==================== LOCATION SCORE ====================
    
    /**
     * Tính điểm dựa trên khoảng cách địa lý giữa 2 bài đăng.
     * 
     * CHỨC NĂNG:
     * - Tính khoảng cách thực tế bằng Haversine Formula
     * - Chuyển đổi khoảng cách thành điểm (càng gần = điểm càng cao)
     * - Đây là yếu tố PHỤ, chỉ dùng để xếp hạng khi Image và Content đã match
     * 
     * ĐƯỢC GỌI TỪ:
     * - calculateMatchScore(): Tính điểm location (10% tổng điểm - yếu tố phụ)
     * 
     * LOGIC CHUYỂN ĐỔI:
     * - Khoảng cách ≤ 1km → 100% (1.0)
     * - Khoảng cách ≥ 50km → 0% (0.0)
     * - Khoảng cách 1-50km → Linear interpolation
     * - Không có location → 50% (0.5) - điểm trung bình
     * 
     * @param post1 Bài đăng thứ nhất (có lat, lng)
     * @param post2 Bài đăng thứ hai (có lat, lng)
     * @return Điểm từ 0.0 (xa) đến 1.0 (gần)
     */
    private static double calculateLocationScore(Post post1, Post post2) {
        // Kiểm tra có location không
        if (post1.getLat() == null || post1.getLng() == null ||
            post2.getLat() == null || post2.getLng() == null) {
            // KHÔNG có location → điểm trung bình (vì location chỉ là yếu tố phụ)
            return 0.5;
        }
        
        double distance = calculateDistance(
            post1.getLat(), post1.getLng(),
            post2.getLat(), post2.getLng()
        );
        
        // Chuyển đổi khoảng cách thành điểm (0-1)
        // Càng gần = điểm càng cao
        if (distance <= 1.0) return 1.0;  // < 1km = 100%
        if (distance >= MAX_DISTANCE_KM) return 0.0; // > 50km = 0%
        
        // Linear interpolation
        return 1.0 - (distance / MAX_DISTANCE_KM);
    }
    
    
    /**
     * Tính khoảng cách giữa 2 điểm trên Trái Đất bằng Haversine Formula.
     * 
     * CHỨC NĂNG:
     * - Tính khoảng cách đường chim bay giữa 2 tọa độ (lat, lng)
     * - Sử dụng công thức Haversine để tính trên mặt cầu
     * 
     * ĐƯỢC GỌI TỪ:
     * - calculateLocationScore(): Tính khoảng cách thực tế giữa 2 bài đăng
     * 
     * CÔNG THỨC HAVERSINE:
     * a = sin²(Δlat/2) + cos(lat1) × cos(lat2) × sin²(Δlng/2)
     * c = 2 × atan2(√a, √(1-a))
     * distance = R × c
     * - R = 6371 km (bán kính Trái Đất)
     * 
     * @param lat1 Vĩ độ điểm 1 (latitude, đơn vị: độ)
     * @param lon1 Kinh độ điểm 1 (longitude, đơn vị: độ)
     * @param lat2 Vĩ độ điểm 2 (latitude, đơn vị: độ)
     * @param lon2 Kinh độ điểm 2 (longitude, đơn vị: độ)
     * @return Khoảng cách tính bằng km (kilometer)
     * 
     * VÍ DỤ:
     * <pre>
     * {@code
     * // Thủ Đức → Quận 1
     * double distance = calculateDistance(10.8505, 106.7717, 10.7769, 106.7009);
     * // → distance ≈ 8.2 km
     * 
     * // Thủ Đức → Bình Thạnh
     * double distance2 = calculateDistance(10.8505, 106.7717, 10.8142, 106.7054);
     * // → distance2 ≈ 6.5 km
     * }
     * </pre>
     */
    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Bán kính Trái Đất (km)
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    
    // ==================== TIME SCORE ====================
    
    /**
     * Tính điểm dựa trên khoảng thời gian giữa 2 bài đăng.
     * 
     * CHỨC NĂNG:
     * - Tính khoảng cách thời gian giữa 2 bài đăng (đơn vị: ngày)
     * - Chuyển đổi thành điểm (càng gần = điểm càng cao)
     * 
     * ĐƯỢC GỌI TỪ:
     * - calculateMatchScore(): Tính điểm time proximity (20% tổng điểm)
     * 
     * LOGIC CHUYỂN ĐỔI:
     * - Cùng ngày (≤ 1 ngày) → 100% (1.0)
     * - Cách nhau ≥ 30 ngày → 0% (0.0)
     * - Cách nhau 1-30 ngày → Linear interpolation
     * - Không có timestamp → 50% (0.5) - điểm trung bình
     * 
     * @param post1 Bài đăng thứ nhất (có createdAt)
     * @param post2 Bài đăng thứ hai (có createdAt)
     * @return Điểm từ 0.0 (xa về thời gian) đến 1.0 (gần về thời gian)
     * 
     * VÍ DỤ:
     * <pre>
     * {@code
     * // Post 1: Đăng hôm nay (18/04/2026)
     * // Post 2: Đăng hôm qua (17/04/2026) → cách 1 ngày
     * double score = calculateTimeScore(post1, post2);
     * // → diffDays = 1
     * // → score = 1.0 (100% vì ≤ 1 ngày)
     * 
     * // Post 3: Đăng 15 ngày trước (03/04/2026)
     * double score2 = calculateTimeScore(post1, post3);
     * // → diffDays = 15
     * // → score = 1.0 - (15/30) = 0.5 (50%)
     * 
     * // Post 4: Đăng 40 ngày trước (09/03/2026)
     * double score3 = calculateTimeScore(post1, post4);
     * // → diffDays = 40
     * // → score = 0.0 (0% vì > 30 ngày)
     * }
     * </pre>
     */
    private static double calculateTimeScore(Post post1, Post post2) {
        if (post1.getCreatedAt() == null || post2.getCreatedAt() == null) {
            return 0.5; // Điểm trung bình nếu không có timestamp
        }
        
        long time1 = post1.getCreatedAt().toDate().getTime();
        long time2 = post2.getCreatedAt().toDate().getTime();
        
        long diffMillis = Math.abs(time1 - time2);
        long diffDays = diffMillis / (1000 * 60 * 60 * 24);
        
        // Chuyển đổi thời gian thành điểm
        if (diffDays <= 1) return 1.0;  // Cùng ngày = 100%
        if (diffDays >= MAX_TIME_DIFF_DAYS) return 0.0; // > 30 ngày = 0%
        
        // Linear interpolation
        return 1.0 - ((double) diffDays / MAX_TIME_DIFF_DAYS);
    }
    
    
    // ==================== MATCH RESULT CLASS ====================
    
    /**
     * Class chứa kết quả match giữa 2 bài đăng.
     * 
     * CHỨC NĂNG:
     * - Lưu trữ bài đăng được match và điểm số tương ứng
     * - Cung cấp phương thức chuyển đổi điểm sang phần trăm
     * 
     * ĐƯỢC SỬ DỤNG BỞI:
     * - findMatches(): Trả về danh sách MatchResult
     * - MainActivity.java: Hiển thị danh sách gợi ý với điểm %
     * - PostDetailActivity.java: Hiển thị các bài liên quan với điểm %
     * 
     * VÍ DỤ:
     * <pre>
     * {@code
     * // Tạo MatchResult
     * MatchResult result = new MatchResult(foundPost, 0.85);
     * 
     * // Lấy thông tin
     * Post post = result.post;  // Bài đăng được match
     * double score = result.score;  // 0.85
     * int percentage = result.getScorePercentage();  // 85
     * 
     * // Hiển thị
     * Log.d(TAG, post.getTitle() + " - Match: " + percentage + "%");
     * // → "Tìm thấy mèo vàng - Match: 85%"
     * }
     * </pre>
     */
    public static class MatchResult {
        /**
         * Bài đăng được match (Post object từ Firestore).
         */
        public Post post;
        
        /**
         * Điểm match từ 0.0 đến 1.0.
         * - 0.0 = không match
         * - 0.3 = match tối thiểu (30%)
         * - 1.0 = match hoàn hảo (100%)
         */
        public double score;
        
        /**
         * Constructor tạo MatchResult.
         * 
         * @param post Bài đăng được match
         * @param score Điểm match (0.0 - 1.0)
         */
        public MatchResult(Post post, double score) {
            this.post = post;
            this.score = score;
        }
        
        /**
         * Chuyển đổi điểm match sang phần trăm.
         * 
         * ĐƯỢC GỌI TỪ:
         * - MainActivity.java: Hiển thị "Match: 85%" trên UI
         * - PostDetailActivity.java: Hiển thị điểm % cho các bài liên quan
         * 
         * @return Điểm phần trăm (0-100)
         * 
         * VÍ DỤ:
         * <pre>
         * {@code
         * MatchResult result = new MatchResult(post, 0.856);
         * int percentage = result.getScorePercentage();
         * // → percentage = 85 (làm tròn xuống)
         * }
         * </pre>
         */
        public int getScorePercentage() {
            return (int) (score * 100);
        }
    }
}
