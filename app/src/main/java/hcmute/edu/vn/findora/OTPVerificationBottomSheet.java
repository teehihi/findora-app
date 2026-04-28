package hcmute.edu.vn.findora;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class OTPVerificationBottomSheet extends BottomSheetDialogFragment {

    private String postId;
    private String postOwnerId;
    private String helperUserId;

    private EditText etOtp1, etOtp2, etOtp3, etOtp4;
    private TextView tvError;
    private MaterialButton btnVerify;
    private ImageButton btnClose;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public static OTPVerificationBottomSheet newInstance(String postId, String postOwnerId, String helperUserId) {
        OTPVerificationBottomSheet fragment = new OTPVerificationBottomSheet();
        Bundle args = new Bundle();
        args.putString("postId", postId);
        args.putString("postOwnerId", postOwnerId);
        args.putString("helperUserId", helperUserId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            postId = getArguments().getString("postId");
            postOwnerId = getArguments().getString("postOwnerId");
            helperUserId = getArguments().getString("helperUserId");
        }
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_otp_verification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etOtp1 = view.findViewById(R.id.etOtp1);
        etOtp2 = view.findViewById(R.id.etOtp2);
        etOtp3 = view.findViewById(R.id.etOtp3);
        etOtp4 = view.findViewById(R.id.etOtp4);
        tvError = view.findViewById(R.id.tvError);
        btnVerify = view.findViewById(R.id.btnVerify);
        btnClose = view.findViewById(R.id.btnClose);

        btnClose.setOnClickListener(v -> dismiss());

        setupOtpInputs();

        btnVerify.setOnClickListener(v -> verifyOtp());
        
        // Ensure keyboard doesn't cover input
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
        }
    }

    private void setupOtpInputs() {
        EditText[] otpFields = {etOtp1, etOtp2, etOtp3, etOtp4};

        for (int i = 0; i < otpFields.length; i++) {
            final int index = i;
            otpFields[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < otpFields.length - 1) {
                        otpFields[index + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            // Handle backspace
            otpFields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL && otpFields[index].getText().length() == 0 && index > 0) {
                    otpFields[index - 1].requestFocus();
                    return true;
                }
                return false;
            });
        }

        // Auto-focus first field
        etOtp1.requestFocus();
    }

    private void verifyOtp() {
        String enteredOtp = etOtp1.getText().toString() +
                etOtp2.getText().toString() +
                etOtp3.getText().toString() +
                etOtp4.getText().toString();

        if (enteredOtp.length() != 4) {
            showError("Vui lòng nhập đầy đủ 4 số");
            return;
        }

        // Fetch OTP from Firestore
        db.collection("posts").document(postId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        showError("Không tìm thấy bài viết");
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> pendingOtp = (Map<String, Object>) doc.get("pendingOtp");

                    if (pendingOtp == null) {
                        showError("Chưa có mã xác nhận. Yêu cầu người nhận tạo mã.");
                        return;
                    }

                    String correctOtp = (String) pendingOtp.get("otp");
                    Timestamp expiresAt = (Timestamp) pendingOtp.get("expiresAt");
                    Boolean verified = (Boolean) pendingOtp.get("verified");

                    // Check if already verified
                    if (verified != null && verified) {
                        showError("Mã này đã được sử dụng");
                        return;
                    }

                    // Check expiration
                    if (expiresAt != null && expiresAt.toDate().getTime() < System.currentTimeMillis()) {
                        showError("Mã đã hết hạn. Yêu cầu người nhận tạo mã mới.");
                        return;
                    }

                    // Verify OTP
                    if (!enteredOtp.equals(correctOtp)) {
                        showError("Mã không đúng. Vui lòng thử lại.");
                        return;
                    }

                    // OTP is correct, resolve the post
                    resolvePost(pendingOtp);
                })
                .addOnFailureListener(e -> {
                    showError("Lỗi: " + e.getMessage());
                });
    }

    private void resolvePost(Map<String, Object> otpData) {
        WriteBatch batch = db.batch();

        // Get rating and review from OTP data
        Long ratingLong = (Long) otpData.get("rating");
        int rating = ratingLong != null ? ratingLong.intValue() : 0;
        String review = (String) otpData.get("review");

        // Update Post
        Map<String, Object> postUpdates = new HashMap<>();
        postUpdates.put("status", "resolved");
        postUpdates.put("resolvedAt", Timestamp.now());
        postUpdates.put("resolvedBy", helperUserId);
        postUpdates.put("rating", rating);
        if (review != null && !review.isEmpty()) {
            postUpdates.put("review", review);
        }
        postUpdates.put("pendingOtp.verified", true);

        batch.update(db.collection("posts").document(postId), postUpdates);

        // Update helper's points and stats
        db.collection("users").document(helperUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        long currentPoints = doc.getLong("points") != null ? doc.getLong("points") : 0;
                        long totalReturned = doc.getLong("totalReturned") != null ? doc.getLong("totalReturned") : 0;

                        Map<String, Object> userUpdates = new HashMap<>();
                        userUpdates.put("points", currentPoints + 50);
                        userUpdates.put("totalReturned", totalReturned + 1);

                        batch.update(db.collection("users").document(helperUserId), userUpdates);

                        // Create Transaction
                        Map<String, Object> transactionData = new HashMap<>();
                        transactionData.put("userId", helperUserId);
                        transactionData.put("type", "earn");
                        transactionData.put("points", 50);
                        transactionData.put("title", "Trả đồ thành công");
                        transactionData.put("description", "Trả lại đồ thất lạc cho chủ nhân");
                        transactionData.put("timestamp", Timestamp.now());
                        transactionData.put("relatedPostId", postId);

                        batch.set(db.collection("transactions").document(), transactionData);

                        commitBatch(batch);
                    }
                });
    }

    private void commitBatch(WriteBatch batch) {
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Xác nhận thành công! Bạn đã nhận 50 điểm.", Toast.LENGTH_LONG).show();
                    dismiss();
                    // Refresh activity
                    if (getActivity() != null) {
                        getActivity().recreate();
                    }
                })
                .addOnFailureListener(e -> {
                    showError("Lỗi: " + e.getMessage());
                });
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
}
