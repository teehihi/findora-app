package hcmute.edu.vn.findora;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

    private TextView tvTitle, tvSubtitle, tvForgot, tvSwitchPrompt, tvSwitchAction;
    private MaterialButton btnSubmit;
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

        // Bind views
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvForgot = findViewById(R.id.tvForgot);
        tvSwitchPrompt = findViewById(R.id.tvSwitchPrompt);
        tvSwitchAction = findViewById(R.id.tvSwitchAction);
        btnSubmit = findViewById(R.id.btnSubmit);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        lblFullName = findViewById(R.id.lblFullName);
        lblPhone = findViewById(R.id.lblPhone);
        tilFullName = findViewById(R.id.tilFullName);
        tilPhone = findViewById(R.id.tilPhone);

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
    }

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
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
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
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        saveUserToFirestore(uid, email, fullName, phone);
                    } else {
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
                    Toast.makeText(this, R.string.msg_register_success, Toast.LENGTH_SHORT).show();
                    navigateToHome();
                })
                .addOnFailureListener(e -> {
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
