package hcmute.edu.vn.findora;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {

    private boolean isLoginMode = true;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private hcmute.edu.vn.findora.utils.LoadingDialog loadingDialog;

    private ScrollView scrollView;
    private TextView tvTitle, tvSubtitle, tvForgot, tvSwitchPrompt, tvSwitchAction;
    private MaterialButton btnSubmit, btnGoogle, btnFacebook;
    private TextInputEditText etEmail, etPassword, etFullName, etPhone;

    // Register-only views
    private TextView lblFullName, lblPhone;
    private TextInputLayout tilFullName, tilPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        loadingDialog = new hcmute.edu.vn.findora.utils.LoadingDialog(this);

        // Bind views
        scrollView = findViewById(R.id.scrollView);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvForgot = findViewById(R.id.tvForgot);
        tvSwitchPrompt = findViewById(R.id.tvSwitchPrompt);
        tvSwitchAction = findViewById(R.id.tvSwitchAction);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnFacebook = findViewById(R.id.btnFacebook);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        lblFullName = findViewById(R.id.lblFullName);
        lblPhone = findViewById(R.id.lblPhone);
        tilFullName = findViewById(R.id.tilFullName);
        tilPhone = findViewById(R.id.tilPhone);

        // Setup auto-scroll when keyboard appears
        setupAutoScroll();

        tvSwitchAction.setOnClickListener(v -> toggleAuthMode());

        btnSubmit.setOnClickListener(v -> {
            String email = getTextFrom(etEmail);
            String password = getTextFrom(etPassword);

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.msg_fill_all, Toast.LENGTH_SHORT).show();
                return;
            }

            if (isLoginMode) {
                loginUser(email, password);
            } else {
                String fullName = getTextFrom(etFullName);
                String phone = getTextFrom(etPhone);
                if (fullName.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(this, R.string.msg_fill_all, Toast.LENGTH_SHORT).show();
                    return;
                }
                registerUser(email, password, fullName, phone);
            }
        });

        // Forgot Password
        tvForgot.setOnClickListener(v -> forgotPassword());

        // Google & Facebook - thông báo chưa khả dụng
        btnGoogle.setOnClickListener(v ->
            Toast.makeText(this, "Đăng nhập Google sẽ sớm được hỗ trợ", Toast.LENGTH_SHORT).show()
        );
        btnFacebook.setOnClickListener(v ->
            Toast.makeText(this, "Đăng nhập Facebook sẽ sớm được hỗ trợ", Toast.LENGTH_SHORT).show()
        );
    }


    // ========== Auto Scroll Setup ==========

    private void setupAutoScroll() {
        View.OnFocusChangeListener scrollListener = (v, hasFocus) -> {
            if (hasFocus && scrollView != null) {
                scrollView.postDelayed(() -> {
                    scrollView.smoothScrollTo(0, v.getBottom());
                }, 200);
            }
        };

        if (etFullName != null) etFullName.setOnFocusChangeListener(scrollListener);
        if (etPhone != null) etPhone.setOnFocusChangeListener(scrollListener);
        if (etEmail != null) etEmail.setOnFocusChangeListener(scrollListener);
        if (etPassword != null) etPassword.setOnFocusChangeListener(scrollListener);
    }

    // ========== Forgot Password ==========

    private void forgotPassword() {
        String currentEmail = getTextFrom(etEmail);

        // Tạo BottomSheetDialog chuyên nghiệp
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        bottomSheet.setContentView(R.layout.dialog_forgot_password);

        // Bind views trong dialog
        com.google.android.material.textfield.TextInputEditText etResetEmail = bottomSheet.findViewById(R.id.etResetEmail);
        com.google.android.material.textfield.TextInputLayout tilResetEmail = bottomSheet.findViewById(R.id.tilResetEmail);
        com.google.android.material.button.MaterialButton btnSendReset = bottomSheet.findViewById(R.id.btnSendReset);
        TextView tvCancelReset = bottomSheet.findViewById(R.id.tvCancelReset);

        // Điền sẵn email nếu user đã nhập
        if (etResetEmail != null && !currentEmail.isEmpty()) {
            etResetEmail.setText(currentEmail);
        }

        // Nút Hủy
        if (tvCancelReset != null) {
            tvCancelReset.setOnClickListener(v -> bottomSheet.dismiss());
        }

        // Nút Gửi
        if (btnSendReset != null && etResetEmail != null && tilResetEmail != null) {
            btnSendReset.setOnClickListener(v -> {
                String email = etResetEmail.getText() != null ? etResetEmail.getText().toString().trim() : "";

                if (email.isEmpty()) {
                    tilResetEmail.setError("Vui lòng nhập email");
                    etResetEmail.requestFocus();
                    return;
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    tilResetEmail.setError("Email không hợp lệ");
                    etResetEmail.requestFocus();
                    return;
                }

                tilResetEmail.setError(null);

                // Disable và đổi text nút
                btnSendReset.setEnabled(false);
                btnSendReset.setText("Đang gửi...");

                // Kiểm tra email có tồn tại trong Firestore
                db.collection("users")
                        .whereEqualTo("email", email)
                        .limit(1)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (querySnapshot.isEmpty()) {
                                btnSendReset.setEnabled(true);
                                btnSendReset.setText("Gửi link đặt lại");
                                tilResetEmail.setError("Email này chưa đăng ký tài khoản");
                                etResetEmail.requestFocus();
                            } else {
                                sendResetEmail(email, bottomSheet);
                            }
                        })
                        .addOnFailureListener(e -> {
                            sendResetEmail(email, bottomSheet);
                        });
            });
        }

        bottomSheet.show();
    }

    private void sendResetEmail(String email, com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    bottomSheet.dismiss();
                    if (task.isSuccessful()) {
                        showResetResultDialog(true, email, null);
                    } else {
                        String errorMsg = task.getException() != null 
                                ? task.getException().getMessage() 
                                : "Lỗi không xác định";
                        showResetResultDialog(false, email, errorMsg);
                    }
                });
    }

    private void showResetResultDialog(boolean success, String email, String errorMsg) {
        com.google.android.material.bottomsheet.BottomSheetDialog resultSheet = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        resultSheet.setContentView(R.layout.dialog_reset_result);

        android.widget.ImageView ivResultIcon = resultSheet.findViewById(R.id.ivResultIcon);
        TextView tvResultTitle = resultSheet.findViewById(R.id.tvResultTitle);
        TextView tvResultDesc = resultSheet.findViewById(R.id.tvResultDesc);
        TextView tvResultEmail = resultSheet.findViewById(R.id.tvResultEmail);
        com.google.android.material.button.MaterialButton btnDone = resultSheet.findViewById(R.id.btnDone);

        if (success) {
            if (ivResultIcon != null) ivResultIcon.setImageResource(R.drawable.ic_email_sent);
            if (tvResultTitle != null) tvResultTitle.setText("Email đã được gửi!");
            if (tvResultDesc != null) tvResultDesc.setText("Link đặt lại mật khẩu đã được gửi đến.\nVui lòng kiểm tra Hộp thư đến hoặc thư mục Spam.");
            if (tvResultEmail != null) tvResultEmail.setText(email);
        } else {
            if (ivResultIcon != null) ivResultIcon.setImageResource(R.drawable.ic_info);
            if (tvResultTitle != null) tvResultTitle.setText("Gửi thất bại");
            if (tvResultDesc != null) tvResultDesc.setText("Không thể gửi email đặt lại mật khẩu.\n" + (errorMsg != null ? errorMsg : ""));
            if (tvResultEmail != null) tvResultEmail.setVisibility(View.GONE);
            if (btnDone != null) btnDone.setText("Thử lại");
        }

        if (btnDone != null) {
            btnDone.setOnClickListener(v -> {
                resultSheet.dismiss();
                if (!success) {
                    forgotPassword(); // Mở lại dialog quên mật khẩu
                }
            });
        }

        resultSheet.show();
    }

    // ========== Email/Password Auth ==========

    private String getTextFrom(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void toggleAuthMode() {
        isLoginMode = !isLoginMode;
        int registerVisibility = isLoginMode ? View.GONE : View.VISIBLE;

        lblFullName.setVisibility(registerVisibility);
        tilFullName.setVisibility(registerVisibility);
        lblPhone.setVisibility(registerVisibility);
        tilPhone.setVisibility(registerVisibility);

        if (isLoginMode) {
            tvTitle.setText(R.string.login_title);
            tvSubtitle.setText(R.string.login_subtitle);
            tvForgot.setVisibility(View.VISIBLE);
            btnSubmit.setText(R.string.login_btn);
            tvSwitchPrompt.setText(R.string.login_no_account);
            tvSwitchAction.setText(R.string.login_switch_register);
        } else {
            tvTitle.setText(R.string.register_title);
            tvSubtitle.setText(R.string.register_subtitle);
            tvForgot.setVisibility(View.GONE);
            btnSubmit.setText(R.string.register_btn);
            tvSwitchPrompt.setText(R.string.register_have_account);
            tvSwitchAction.setText(R.string.register_switch_login);
        }
    }

    private void loginUser(String email, String password) {
        btnSubmit.setEnabled(false);
        loadingDialog.show("Đang đăng nhập...");
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    loadingDialog.dismiss();
                    btnSubmit.setEnabled(true);
                    if (task.isSuccessful()) {
                        navigateToHome();
                    } else {
                        Toast.makeText(this,
                                getString(R.string.msg_login_failed) + ": " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registerUser(String email, String password, String fullName, String phone) {
        btnSubmit.setEnabled(false);
        loadingDialog.show("Đang tạo tài khoản...");
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        saveUserToFirestore(uid, email, fullName, phone);
                    } else {
                        loadingDialog.dismiss();
                        btnSubmit.setEnabled(true);
                        Toast.makeText(this,
                                getString(R.string.msg_register_failed) + ": " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid, String email, String fullName, String phone) {
        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("fullName", fullName);
        user.put("email", email);
        user.put("phone", phone);
        user.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    loadingDialog.dismiss();
                    Toast.makeText(this, R.string.msg_register_success, Toast.LENGTH_SHORT).show();
                    navigateToHome();
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this,
                            getString(R.string.msg_save_failed) + ": " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToHome() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
