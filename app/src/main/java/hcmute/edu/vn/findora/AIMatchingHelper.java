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
 * - Sử dụng thuật toán AI kết hợp 3 yếu tố:
 *   1. Text Similarity (Độ tương đồng văn bản) - 40%
 *   2. Location Distance (Khoảng cách địa lý) - 40%
 *   3. Time Proximity (Thời gian gần nhau) - 20%
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
 * - Jaccard Similarity cho văn bản (tính độ giống nhau giữa 2 tập từ)
 * - Haversine Formula cho khoảng cách địa lý (tính khoảng cách trên mặt cầu)
 * - Linear Interpolation cho thời gian (tính điểm dựa trên khoảng thời gian)
 * 
 * @author Findora Team
 * @version 1.0
 * @since 2024
 */
public class AIMatchingHelper {
    
    private static final String TAG = "AIMatching";
    
    // ==================== NGƯỠNG CẤU HÌNH ====================
    
    /**
     * Điểm tối thiểu để 2 bài đăng được coi là match (30%).
     * Nếu điểm < 0.3 thì không hiển thị gợi ý.
     */
    private static final double MIN_MATCH_SCORE = 0.3;
    
    /**
     * Khoảng cách tối đa để 2 bài đăng được coi là gần nhau (50km).
     * Nếu khoảng cách > 50km thì điểm location = 0.
     * 
     * LƯU Ý: Tăng từ 10km lên 50km để phù hợp với khu vực rộng hơn
     * (VD: Thủ Đức ↔ Quận 1 ~10km, TP.HCM ↔ Bình Dương ~30km)
     */
    private static final double MAX_DISTANCE_KM = 50.0;
    
    /**
     * Khoảng cách tối đa TUYỆT ĐỐI để 2 bài đăng có thể match (100km).
     * Nếu khoảng cách > 100km thì KHÔNG BAO GIỜ match, bất kể điểm text cao thế nào.
     * 
     * VÍ DỤ: Thủ Đức (TP.HCM) ↔ Hà Nội (~1700km) → KHÔNG match
     */
    private static final double ABSOLUTE_MAX_DISTANCE_KM = 100.0;
    
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
            
            // ===== KIỂM TRA KHOẢNG CÁCH TUYỆT ĐỐI =====
            // Nếu cả 2 bài đều có location, kiểm tra khoảng cách
            if (currentPost.getLat() != null && currentPost.getLng() != null &&
                post.getLat() != null && post.getLng() != null) {
                
                double distance = calculateDistance(
                    currentPost.getLat(), currentPost.getLng(),
                    post.getLat(), post.getLng()
                );
                
                // Nếu quá xa (> 100km) thì KHÔNG match, bỏ qua luôn
                if (distance > ABSOLUTE_MAX_DISTANCE_KM) {
                    Log.d(TAG, String.format("Skipped: %s - Too far (%.1f km)", 
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
     * CHỨC NĂNG:
     * - Kết hợp 3 yếu tố: Title (50%) + Location (30%) + Time (20%)
     * - Ưu tiên title similarity để match chính xác (mèo với mèo, chìa khóa với chìa khóa)
     * - Trả về điểm từ 0.0 (không match) đến 1.0 (match hoàn hảo)
     * 
     * ĐƯỢC GỌI TỪ:
     * - findMatches(): Tính điểm cho từng cặp bài đăng
     * 
     * CÔNG THỨC:
     * totalScore = (titleScore × 0.5) + (locationScore × 0.3) + (timeScore × 0.2)
     * 
     * @param post1 Bài đăng thứ nhất (thường là bài đăng hiện tại)
     * @param post2 Bài đăng thứ hai (bài đăng cần so sánh)
     * @return Điểm match từ 0.0 đến 1.0
     */
    private static double calculateMatchScore(Post post1, Post post2) {
        double titleScore = calculateTitleSimilarity(post1, post2);
        double locationScore = calculateLocationScore(post1, post2);
        double timeScore = calculateTimeScore(post1, post2);
        
        // Weighted average - ưu tiên title (50%)
        double totalScore = (titleScore * 0.5) + (locationScore * 0.3) + (timeScore * 0.2);
        
        Log.d(TAG, String.format("Scores - Title: %.2f, Location: %.2f, Time: %.2f, Total: %.2f",
            titleScore, locationScore, timeScore, totalScore));
        
        return totalScore;
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
        
        // Bonus LỚN nếu có từ khóa quan trọng giống nhau (ưu tiên matching chính xác)
        double keywordBonus = calculateKeywordBonus(title1, title2) * 2.0; // x2 bonus
        
        double finalScore = Math.min(1.0, jaccard + keywordBonus);
        
        Log.d(TAG, String.format("Title similarity - Jaccard: %.2f, Keyword bonus: %.2f, Final: %.2f",
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
     * 
     * ĐƯỢC GỌI TỪ:
     * - calculateMatchScore(): Tính điểm location (40% tổng điểm)
     * 
     * LOGIC CHUYỂN ĐỔI:
     * - Khoảng cách ≤ 1km → 100% (1.0)
     * - Khoảng cách ≥ 50km → 0% (0.0)
     * - Khoảng cách 1-50km → Linear interpolation
     * - Không có location → 0% (0.0) - BẮT BUỘC phải có location
     * 
     * LƯU Ý: Đã tăng MAX_DISTANCE từ 10km lên 50km để phù hợp với khu vực rộng
     * 
     * @param post1 Bài đăng thứ nhất (có lat, lng)
     * @param post2 Bài đăng thứ hai (có lat, lng)
     * @return Điểm từ 0.0 (xa) đến 1.0 (gần)
     * 
     * VÍ DỤ:
     * <pre>
     * {@code
     * // Post 1: Thủ Đức (10.8505, 106.7717)
     * // Post 2: Thủ Đức (10.8510, 106.7720) → cách ~0.5km
     * double score = calculateLocationScore(post1, post2);
     * // → distance = 0.5km
     * // → score = 1.0 (100% vì < 1km)
     * 
     * // Post 3: Quận 1 (10.7769, 106.7009) → cách ~8km
     * double score2 = calculateLocationScore(post1, post3);
     * // → distance = 8km
     * // → score = 1.0 - (8/50) = 0.84 (84%)
     * 
     * // Post 4: Bình Dương → cách ~30km
     * double score3 = calculateLocationScore(post1, post4);
     * // → distance = 30km
     * // → score = 1.0 - (30/50) = 0.4 (40%)
     * }
     * </pre>
     */
    private static double calculateLocationScore(Post post1, Post post2) {
        // Kiểm tra có location không
        if (post1.getLat() == null || post1.getLng() == null ||
            post2.getLat() == null || post2.getLng() == null) {
            // KHÔNG có location → điểm = 0 (bắt buộc phải có location)
            return 0.0;
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
