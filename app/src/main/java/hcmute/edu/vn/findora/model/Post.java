package hcmute.edu.vn.findora.model;

import com.google.firebase.Timestamp;

/**
 * Model đại diện cho một bài đăng (post) trong ứng dụng Findora.
 * 
 * CHỨC NĂNG:
 * - Lưu trữ thông tin về đồ vật bị mất hoặc tìm thấy
 * - Được lưu vào Firestore collection "posts"
 * - Hỗ trợ tự động mapping từ Firestore document sang Java object
 * 
 * ĐƯỢC SỬ DỤNG BỞI:
 * - CreatePostActivity.java: Tạo bài đăng mới
 * - MainActivity.java: Hiển thị danh sách bài đăng
 * - PostDetailActivity.java: Hiển thị chi tiết bài đăng
 * - AIMatchingHelper.java: So sánh và tìm bài đăng phù hợp
 * - PostAdapter.java: Hiển thị bài đăng trong RecyclerView
 * 
 * CẤU TRÚC FIRESTORE:
 * <pre>
 * posts (collection)
 *   └─ {postId} (document)
 *       ├─ title: "Mất mèo vàng"
 *       ├─ description: "Mèo vàng, 2 tuổi, mất ở công viên"
 *       ├─ type: "lost" hoặc "found"
 *       ├─ userId: "abc123xyz"
 *       ├─ createdAt: Timestamp
 *       ├─ imageUrl: "https://storage.googleapis.com/..."
 *       ├─ lat: 10.8505
 *       ├─ lng: 106.7717
 *       ├─ address: "56 Hoàng Diệu 2, Phường Linh Chiểu, Thủ Đức, TP.HCM"
 *       ├─ imageLabel: "cat" (từ TensorFlow Lite)
 *       └─ confidence: 0.95
 * </pre>
 * 
 * @author Findora Team
 * @version 1.0
 * @since 2024
 */
public class Post {

    // ==================== THÔNG TIN CƠ BẢN ====================
    
    /**
     * ID document trong Firestore.
     * 
     * LƯU Ý:
     * - Không được lưu vào Firestore (chỉ dùng ở local)
     * - Được set sau khi query từ Firestore: post.setId(doc.getId())
     * 
     * VÍ DỤ: "abc123xyz456"
     */
    private String id;
    
    /**
     * Tiêu đề bài đăng (ngắn gọn, dễ hiểu).
     * 
     * VÍ DỤ:
     * - "Mất mèo vàng"
     * - "Tìm thấy ví da"
     * - "Mất điện thoại iPhone 13"
     */
    private String title;
    
    /**
     * Mô tả chi tiết về đồ vật.
     * 
     * VÍ DỤ:
     * - "Mèo vàng, 2 tuổi, có vòng cổ màu đỏ, mất ở công viên Thủ Đức"
     * - "Ví da màu nâu, bên trong có CMND và thẻ ATM"
     */
    private String description;
    
    /**
     * Loại bài đăng.
     * 
     * GIÁ TRỊ:
     * - "lost": Đồ vật bị mất (người dùng đang tìm)
     * - "found": Đồ vật tìm thấy (người dùng nhặt được)
     * 
     * SỬ DỤNG:
     * - MainActivity: Lọc bài đăng theo loại (chipAll, chipLost, chipFound)
     * - AIMatchingHelper: Match giữa "lost" và "found"
     */
    private String type;
    
    /**
     * UID của người đăng bài (từ Firebase Authentication).
     * 
     * VÍ DỤ: "abc123xyz456"
     * 
     * SỬ DỤNG:
     * - Xác định chủ sở hữu bài đăng
     * - Hiển thị thông tin người đăng (avatar, tên)
     * - Kiểm tra quyền chỉnh sửa/xóa bài đăng
     */
    private String userId;
    
    /**
     * Thời gian đăng bài (Firebase Timestamp).
     * 
     * VÍ DỤ: Timestamp(seconds=1713398400, nanoseconds=0)
     * 
     * SỬ DỤNG:
     * - Sắp xếp bài đăng theo thời gian (mới nhất trước)
     * - Tính điểm time proximity trong AIMatchingHelper
     * - Hiển thị "2 giờ trước", "3 ngày trước"
     */
    private Timestamp createdAt;
    
    /**
     * URL ảnh tải lên Firebase Storage.
     * 
     * VÍ DỤ: "https://firebasestorage.googleapis.com/v0/b/findora.appspot.com/o/posts%2Fabc123.jpg?alt=media&token=xyz"
     * 
     * SỬ DỤNG:
     * - Hiển thị ảnh trong RecyclerView (PostAdapter)
     * - Hiển thị ảnh chi tiết (PostDetailActivity)
     * - Nhận diện đồ vật bằng TensorFlow Lite (ImageClassifier)
     */
    private String imageUrl;
    
    // ==================== THÔNG TIN VỊ TRÍ ====================
    
    /**
     * Vĩ độ (Latitude) của vị trí đồ vật.
     * 
     * PHẠM VI: -90 đến +90
     * VÍ DỤ: 10.8505 (Thủ Đức, TP.HCM)
     * 
     * SỬ DỤNG:
     * - Hiển thị marker trên bản đồ (MapActivity)
     * - Tính khoảng cách trong AIMatchingHelper (Haversine formula)
     */
    private Double lat;
    
    /**
     * Kinh độ (Longitude) của vị trí đồ vật.
     * 
     * PHẠM VI: -180 đến +180
     * VÍ DỤ: 106.7717 (Thủ Đức, TP.HCM)
     * 
     * LƯU Ý: Mapbox sử dụng thứ tự (lng, lat) - khác với Google Maps (lat, lng)
     * 
     * SỬ DỤNG:
     * - Hiển thị marker trên bản đồ (MapActivity)
     * - Tính khoảng cách trong AIMatchingHelper
     */
    private Double lng;
    
    /**
     * Địa chỉ đọc được (từ Reverse Geocoding).
     * 
     * VÍ DỤ: "56 Hoàng Diệu 2, Phường Linh Chiểu, Thủ Đức, TP.HCM"
     * 
     * NGUỒN:
     * - Mapbox Geocoding API (primary)
     * - Nominatim API (fallback)
     * - Vietnam Provinces API (last resort)
     * 
     * SỬ DỤNG:
     * - Hiển thị địa chỉ trong PostAdapter
     * - Hiển thị địa chỉ trong PostDetailActivity
     */
    private String address;
    
    // ==================== THÔNG TIN AI ====================
    
    /**
     * Nhãn nhận diện từ hình ảnh (TensorFlow Lite MobileNet).
     * 
     * VÍ DỤ:
     * - "cat" (mèo)
     * - "dog" (chó)
     * - "wallet" (ví)
     * - "mobile phone" (điện thoại)
     * 
     * NGUỒN: ImageClassifier.java (TensorFlow Lite)
     * 
     * SỬ DỤNG:
     * - Tự động gợi ý tiêu đề khi tạo bài đăng
     * - Tăng độ chính xác matching trong AIMatchingHelper
     */
    private String imageLabel;
    
    /**
     * Độ tin cậy của mô hình AI (0.0 - 1.0).
     * 
     * VÍ DỤ:
     * - 0.95 = 95% chắc chắn là "cat"
     * - 0.60 = 60% chắc chắn là "wallet"
     * 
     * NGUỒN: ImageClassifier.java (TensorFlow Lite)
     * 
     * SỬ DỤNG:
     * - Quyết định có hiển thị gợi ý AI hay không (threshold: 0.5)
     * - Hiển thị độ tin cậy cho người dùng
     */
    private Double confidence;

    // ==================== CONSTRUCTORS ====================
    
    /**
     * Constructor rỗng - BẮT BUỘC cho Firestore.
     * 
     * CHỨC NĂNG:
     * - Firestore sử dụng constructor này để tự động mapping document → object
     * - Firestore gọi các setter để điền dữ liệu
     * 
     * ĐƯỢC GỌI TỪ:
     * - Firestore: Khi query document từ collection "posts"
     * 
     * VÍ DỤ:
     * <pre>
     * {@code
     * // Firestore tự động tạo object
     * db.collection("posts").get().addOnSuccessListener(snapshots -> {
     *     for (QueryDocumentSnapshot doc : snapshots) {
     *         Post post = doc.toObject(Post.class);  // Gọi constructor rỗng
     *         // Firestore tự động gọi setTitle(), setDescription(), ...
     *     }
     * });
     * }
     * </pre>
     */
    public Post() {}

    /**
     * Constructor đầy đủ - Dùng khi tạo bài đăng mới.
     * 
     * ĐƯỢC GỌI TỪ:
     * - CreatePostActivity.java: Khi người dùng tạo bài đăng mới
     * 
     * @param title Tiêu đề bài đăng
     * @param description Mô tả chi tiết
     * @param type Loại bài đăng ("lost" hoặc "found")
     * @param userId UID người đăng
     * @param createdAt Thời gian đăng bài
     * @param imageUrl URL ảnh trên Firebase Storage
     * 
     * VÍ DỤ:
     * <pre>
     * {@code
     * Post newPost = new Post(
     *     "Mất mèo vàng",
     *     "Mèo vàng, 2 tuổi, có vòng cổ đỏ",
     *     "lost",
     *     FirebaseAuth.getInstance().getCurrentUser().getUid(),
     *     Timestamp.now(),
     *     "https://storage.googleapis.com/..."
     * );
     * 
     * // Thêm thông tin vị trí
     * newPost.setLat(10.8505);
     * newPost.setLng(106.7717);
     * newPost.setAddress("Thủ Đức, TP.HCM");
     * 
     * // Lưu vào Firestore
     * db.collection("posts").add(newPost);
     * }
     * </pre>
     */
    public Post(String title, String description, String type, String userId, Timestamp createdAt, String imageUrl) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.userId = userId;
        this.createdAt = createdAt;
        this.imageUrl = imageUrl;
    }

    // ==================== GETTERS & SETTERS ====================
    
    // --- ID ---
    /** Lấy ID document. @return ID trong Firestore */
    public String getId() { return id; }
    /** Set ID document. @param id ID từ Firestore */
    public void setId(String id) { this.id = id; }

    // --- Title ---
    /** Lấy tiêu đề. @return Tiêu đề bài đăng */
    public String getTitle() { return title; }
    /** Set tiêu đề. @param title Tiêu đề mới */
    public void setTitle(String title) { this.title = title; }

    // --- Description ---
    /** Lấy mô tả. @return Mô tả chi tiết */
    public String getDescription() { return description; }
    /** Set mô tả. @param description Mô tả mới */
    public void setDescription(String description) { this.description = description; }

    // --- Type ---
    /** Lấy loại bài đăng. @return "lost" hoặc "found" */
    public String getType() { return type; }
    /** Set loại bài đăng. @param type "lost" hoặc "found" */
    public void setType(String type) { this.type = type; }

    // --- User ID ---
    /** Lấy UID người đăng. @return UID từ Firebase Auth */
    public String getUserId() { return userId; }
    /** Set UID người đăng. @param userId UID từ Firebase Auth */
    public void setUserId(String userId) { this.userId = userId; }

    // --- Created At ---
    /** Lấy thời gian đăng. @return Firebase Timestamp */
    public Timestamp getCreatedAt() { return createdAt; }
    /** Set thời gian đăng. @param createdAt Firebase Timestamp */
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    // --- Image URL ---
    /** Lấy URL ảnh. @return URL trên Firebase Storage */
    public String getImageUrl() { return imageUrl; }
    /** Set URL ảnh. @param imageUrl URL trên Firebase Storage */
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    // --- Location: Latitude ---
    /** Lấy vĩ độ. @return Latitude (VD: 10.8505) */
    public Double getLat() { return lat; }
    /** Set vĩ độ. @param lat Latitude (-90 đến +90) */
    public void setLat(Double lat) { this.lat = lat; }
    
    // --- Location: Longitude ---
    /** Lấy kinh độ. @return Longitude (VD: 106.7717) */
    public Double getLng() { return lng; }
    /** Set kinh độ. @param lng Longitude (-180 đến +180) */
    public void setLng(Double lng) { this.lng = lng; }
    
    // --- Location: Address ---
    /** Lấy địa chỉ. @return Địa chỉ đầy đủ */
    public String getAddress() { return address; }
    /** Set địa chỉ. @param address Địa chỉ từ geocoding */
    public void setAddress(String address) { this.address = address; }
    
    // --- AI: Image Label ---
    /** Lấy nhãn AI. @return Nhãn từ TensorFlow Lite */
    public String getImageLabel() { return imageLabel; }
    /** Set nhãn AI. @param imageLabel Nhãn từ ImageClassifier */
    public void setImageLabel(String imageLabel) { this.imageLabel = imageLabel; }
    
    // --- AI: Confidence ---
    /** Lấy độ tin cậy AI. @return Confidence (0.0 - 1.0) */
    public Double getConfidence() { return confidence; }
    /** Set độ tin cậy AI. @param confidence Độ tin cậy (0.0 - 1.0) */
    public void setConfidence(Double confidence) { this.confidence = confidence; }
}
