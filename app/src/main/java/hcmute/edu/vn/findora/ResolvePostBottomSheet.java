package hcmute.edu.vn.findora;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map;

import hcmute.edu.vn.findora.adapter.HelperSelectionAdapter;
import hcmute.edu.vn.findora.model.User;
import hcmute.edu.vn.findora.model.Transaction;

public class ResolvePostBottomSheet extends BottomSheetDialogFragment {

    private static final int STEP_1 = 1;
    private static final int STEP_2 = 2;
    private static final int STEP_3 = 3;

    private static final int OPTION_RECEIVED = 1;
    private static final int OPTION_FOUND_SELF = 2;
    private static final int OPTION_NOT_FOUND = 3;

    private int currentStep = STEP_1;
    private int selectedOption = 0;
    private String selectedHelperId = null;
    private int selectedRating = 0;
    private String generatedOtp = "";

    private String postId;
    private String postOwnerId;

    // Views
    private LinearLayout layoutStep1, layoutStep2, layoutStep3;
    private MaterialCardView cardOption1, cardOption2, cardOption3, cardOtpDisplay;
    private ImageView radioOption1, radioOption2, radioOption3;
    private MaterialButton btnStep1Continue, btnStep2Back, btnStep2Continue, btnStep3Back, btnStep3Complete, btnGenerateOtp;
    private ImageView star1, star2, star3, star4, star5;
    private EditText etFeedback;
    private ImageButton btnClose;
    private TextView tvOtpCode;
    private android.widget.ProgressBar progressOtp;
    private LinearLayout layoutOtpInfo;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public static ResolvePostBottomSheet newInstance(String postId, String postOwnerId) {
        ResolvePostBottomSheet fragment = new ResolvePostBottomSheet();
        Bundle args = new Bundle();
        args.putString("postId", postId);
        args.putString("postOwnerId", postOwnerId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            postId = getArguments().getString("postId");
            postOwnerId = getArguments().getString("postOwnerId");
        }
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_resolve_flow, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupStep1();
        setupStep2();
        setupStep3();
        
        // Ensure keyboard doesn't cover input
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
        }
    }

    private void initViews(View view) {
        layoutStep1 = view.findViewById(R.id.layoutStep1);
        layoutStep2 = view.findViewById(R.id.layoutStep2);
        layoutStep3 = view.findViewById(R.id.layoutStep3);

        cardOption1 = view.findViewById(R.id.cardOption1);
        cardOption2 = view.findViewById(R.id.cardOption2);
        cardOption3 = view.findViewById(R.id.cardOption3);
        cardOtpDisplay = view.findViewById(R.id.cardOtpDisplay);

        radioOption1 = view.findViewById(R.id.radioOption1);
        radioOption2 = view.findViewById(R.id.radioOption2);
        radioOption3 = view.findViewById(R.id.radioOption3);

        btnStep1Continue = view.findViewById(R.id.btnStep1Continue);
        btnStep2Back = view.findViewById(R.id.btnStep2Back);
        btnStep2Continue = view.findViewById(R.id.btnStep2Continue);
        btnStep3Back = view.findViewById(R.id.btnStep3Back);
        btnStep3Complete = view.findViewById(R.id.btnStep3Complete);
        btnGenerateOtp = view.findViewById(R.id.btnGenerateOtp);
        
        star1 = view.findViewById(R.id.star1);
        star2 = view.findViewById(R.id.star2);
        star3 = view.findViewById(R.id.star3);
        star4 = view.findViewById(R.id.star4);
        star5 = view.findViewById(R.id.star5);
        
        etFeedback = view.findViewById(R.id.etFeedback);
        tvOtpCode = view.findViewById(R.id.tvOtpCode);
        progressOtp = view.findViewById(R.id.progressOtp);
        layoutOtpInfo = view.findViewById(R.id.layoutOtpInfo);
        btnClose = view.findViewById(R.id.btnClose);

        btnClose.setOnClickListener(v -> dismiss());
    }

    private void setupStep1() {
        cardOption1.setOnClickListener(v -> selectOption(OPTION_RECEIVED));
        cardOption2.setOnClickListener(v -> selectOption(OPTION_FOUND_SELF));
        cardOption3.setOnClickListener(v -> selectOption(OPTION_NOT_FOUND));

        btnStep1Continue.setOnClickListener(v -> {
            if (selectedOption == OPTION_RECEIVED) {
                navigateToStep(STEP_2);
            } else {
                // Skip to final resolution
                resolvePost(null, 0, "");
            }
        });
    }

    private void selectOption(int option) {
        selectedOption = option;
        
        // Reset all cards
        resetCard(cardOption1, radioOption1);
        resetCard(cardOption2, radioOption2);
        resetCard(cardOption3, radioOption3);

        // Highlight selected card
        if (option == OPTION_RECEIVED) {
            highlightCard(cardOption1, radioOption1);
        } else if (option == OPTION_FOUND_SELF) {
            highlightCard(cardOption2, radioOption2);
        } else if (option == OPTION_NOT_FOUND) {
            highlightCard(cardOption3, radioOption3);
        }

        btnStep1Continue.setEnabled(true);
    }

    private void resetCard(MaterialCardView card, ImageView radio) {
        card.setCardBackgroundColor(getResources().getColor(android.R.color.white));
        card.setStrokeColor(getResources().getColor(R.color.gray_200));
        radio.setImageResource(R.drawable.ic_radio_unchecked);
    }

    private void highlightCard(MaterialCardView card, ImageView radio) {
        card.setCardBackgroundColor(getResources().getColor(R.color.green_50));
        card.setStrokeColor(getResources().getColor(R.color.primary_green));
        radio.setImageResource(R.drawable.ic_radio_checked);
    }

    private void setupStep2() {
        ImageView[] stars = {star1, star2, star3, star4, star5};
        
        for (int i = 0; i < stars.length; i++) {
            final int rating = i + 1;
            stars[i].setOnClickListener(v -> setRating(rating));
        }

        btnStep2Back.setOnClickListener(v -> navigateToStep(STEP_1));
        btnStep2Continue.setOnClickListener(v -> navigateToStep(STEP_3));
    }

    private void setupStep3() {
        btnGenerateOtp.setOnClickListener(v -> generateOtpWithLoading());
        btnStep3Back.setOnClickListener(v -> navigateToStep(STEP_2));
        btnStep3Complete.setOnClickListener(v -> {
            if (generatedOtp.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng tạo mã xác nhận trước", Toast.LENGTH_SHORT).show();
                return;
            }
            dismiss();
        });
    }

    private void setRating(int rating) {
        selectedRating = rating;
        ImageView[] stars = {star1, star2, star3, star4, star5};
        
        for (int i = 0; i < stars.length; i++) {
            if (i < rating) {
                stars[i].setImageResource(R.drawable.ic_star_filled);
                stars[i].setColorFilter(getResources().getColor(R.color.primary_green));
            } else {
                stars[i].setImageResource(R.drawable.ic_star_outline);
                stars[i].setColorFilter(getResources().getColor(R.color.gray_300));
            }
        }
    }

    private void navigateToStep(int step) {
        currentStep = step;
        
        layoutStep1.setVisibility(step == STEP_1 ? View.VISIBLE : View.GONE);
        layoutStep2.setVisibility(step == STEP_2 ? View.VISIBLE : View.GONE);
        layoutStep3.setVisibility(step == STEP_3 ? View.VISIBLE : View.GONE);
        
        // Reset OTP display when entering step 3
        if (step == STEP_3) {
            resetOtpDisplay();
        }
    }
    
    /**
     * Reset OTP display to initial state
     */
    private void resetOtpDisplay() {
        btnGenerateOtp.setVisibility(View.VISIBLE);
        cardOtpDisplay.setVisibility(View.GONE);
        layoutOtpInfo.setVisibility(View.GONE);
        progressOtp.setVisibility(View.GONE);
        generatedOtp = "";
    }
    
    /**
     * Generate OTP with loading animation
     */
    private void generateOtpWithLoading() {
        // Show loading
        btnGenerateOtp.setVisibility(View.GONE);
        progressOtp.setVisibility(View.VISIBLE);
        
        // Simulate network delay (500ms)
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            generateOtp();
            saveOtpToFirestore();
            
            // Hide loading, show OTP
            progressOtp.setVisibility(View.GONE);
            cardOtpDisplay.setVisibility(View.VISIBLE);
            layoutOtpInfo.setVisibility(View.VISIBLE);
        }, 500);
    }
    
    /**
     * Generate random 4-digit OTP
     */
    private void generateOtp() {
        java.util.Random random = new java.util.Random();
        int otp = 1000 + random.nextInt(9000); // 1000-9999
        generatedOtp = String.valueOf(otp);
        tvOtpCode.setText(generatedOtp);
    }
    
    /**
     * Save OTP to Firestore with expiration time (1 hour)
     * OTP được lưu vào collection riêng để dễ truy cập từ 2 phía
     * 
     * LOGIC:
     * 1. Xóa tất cả OTP cũ của bài viết này (nếu có)
     * 2. Tạo OTP mới
     */
    private void saveOtpToFirestore() {
        String feedback = etFeedback.getText().toString().trim();
        
        // Bước 1: Xóa OTP cũ (nếu có) trước khi tạo OTP mới
        // Sử dụng lostPostId + lostUserId để tìm OTP cũ
        db.collection("otpCodes")
                .whereEqualTo("lostPostId", postId)
                .whereEqualTo("lostUserId", postOwnerId)
                .whereEqualTo("verified", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Có OTP cũ → Xóa
                        WriteBatch batch = db.batch();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            batch.delete(doc.getReference());
                            android.util.Log.d("ResolvePost", "Deleting old OTP: " + doc.getId());
                        }
                        
                        // Commit batch delete
                        batch.commit()
                                .addOnSuccessListener(aVoid -> {
                                    android.util.Log.d("ResolvePost", "Old OTPs deleted successfully");
                                    // Tạo OTP mới sau khi xóa thành công
                                    createNewOtp(feedback);
                                })
                                .addOnFailureListener(e -> {
                                    android.util.Log.e("ResolvePost", "Error deleting old OTPs: " + e.getMessage(), e);
                                    // Vẫn tạo OTP mới dù xóa cũ thất bại
                                    createNewOtp(feedback);
                                });
                    } else {
                        // Không có OTP cũ → Tạo mới luôn
                        android.util.Log.d("ResolvePost", "No old OTP found, creating new one");
                        createNewOtp(feedback);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ResolvePost", "Error querying old OTPs: " + e.getMessage(), e);
                    // Vẫn tạo OTP mới dù query thất bại
                    createNewOtp(feedback);
                });
    }
    
    /**
     * Tạo OTP mới và lưu vào Firestore
     */
    private void createNewOtp(String feedback) {
        Map<String, Object> otpData = new HashMap<>();
        otpData.put("otp", generatedOtp);
        otpData.put("lostPostId", postId); // Bài viết Lost (người mất đồ)
        otpData.put("lostUserId", postOwnerId); // User mất đồ
        otpData.put("rating", selectedRating);
        otpData.put("review", feedback);
        otpData.put("createdAt", Timestamp.now());
        otpData.put("expiresAt", new Timestamp(System.currentTimeMillis() / 1000 + 3600, 0)); // 1 hour = 3600 seconds
        otpData.put("verified", false);
        
        // Lưu vào collection otpCodes (auto-generate ID để tránh conflict)
        db.collection("otpCodes")
                .add(otpData)
                .addOnSuccessListener(docRef -> {
                    android.util.Log.d("ResolvePost", "OTP created successfully: " + generatedOtp + " (ID: " + docRef.getId() + ")");
                    Toast.makeText(getContext(), "Mã đã được tạo thành công. Đưa mã này cho người trả đồ.", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ResolvePost", "Error creating OTP: " + e.getMessage(), e);
                    
                    // Hiển thị lỗi chi tiết
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && errorMsg.contains("PERMISSION_DENIED")) {
                        Toast.makeText(getContext(), 
                            "Lỗi quyền truy cập Firestore. Vui lòng kiểm tra Security Rules cho collection 'otpCodes'.", 
                            Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Lỗi: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                    
                    // KHÔNG reset OTP display để user vẫn thấy mã
                    // User có thể chụp màn hình hoặc ghi nhớ mã này
                });
    }

    private void resolvePost(String helperId, int rating, String review) {
        WriteBatch batch = db.batch();

        // Update Post
        Map<String, Object> postUpdates = new HashMap<>();
        postUpdates.put("status", "resolved");
        postUpdates.put("resolvedAt", Timestamp.now());
        
        if (helperId != null) {
            postUpdates.put("resolvedBy", helperId);
            postUpdates.put("rating", rating);
            postUpdates.put("review", review);
        }
        
        batch.update(db.collection("posts").document(postId), postUpdates);

        // If helper was selected, update their points and stats
        if (helperId != null && selectedOption == OPTION_RECEIVED) {
            db.collection("users").document(helperId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            long currentPoints = doc.getLong("points") != null ? doc.getLong("points") : 0;
                            long totalReturned = doc.getLong("totalReturned") != null ? doc.getLong("totalReturned") : 0;

                            Map<String, Object> userUpdates = new HashMap<>();
                            userUpdates.put("points", currentPoints + 50);
                            userUpdates.put("totalReturned", totalReturned + 1);
                            
                            batch.update(db.collection("users").document(helperId), userUpdates);

                            // Create Transaction
                            Map<String, Object> transactionData = new HashMap<>();
                            transactionData.put("userId", helperId);
                            transactionData.put("type", "earn");
                            transactionData.put("points", 50);
                            transactionData.put("title", "Giúp tìm đồ");
                            transactionData.put("description", "Giúp tìm lại đồ thất lạc");
                            transactionData.put("timestamp", Timestamp.now());
                            transactionData.put("relatedPostId", postId);

                            batch.set(db.collection("transactions").document(), transactionData);

                            commitBatch(batch);
                        }
                    });
        } else {
            commitBatch(batch);
        }
    }

    private void commitBatch(WriteBatch batch) {
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Đã đánh dấu bài viết là đã giải quyết", Toast.LENGTH_SHORT).show();
                    dismiss();
                    // Refresh activity để cập nhật UI
                    if (getActivity() != null) {
                        getActivity().recreate();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
