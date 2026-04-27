package hcmute.edu.vn.findora;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ReportUserDialog extends BottomSheetDialogFragment {

    private static final String ARG_USER_ID = "userId";
    private static final String ARG_USER_NAME = "userName";
    private static final String ARG_USER_AVATAR = "userAvatar";
    private static final int MAX_CHARS = 500;

    private ImageButton btnClose;
    private MaterialButton btnSubmit, btnCancel;
    private EditText etReason;
    private TextView tvCharCount, tvUserName;
    private ImageView ivUserAvatar;
    private LinearLayout layoutUserInfo;

    private String reportedUserId;
    private String reportedUserName;
    private String reportedUserAvatar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public static ReportUserDialog newInstance(String userId, String userName, String userAvatar) {
        ReportUserDialog dialog = new ReportUserDialog();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_USER_NAME, userName);
        args.putString(ARG_USER_AVATAR, userAvatar);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            reportedUserId = getArguments().getString(ARG_USER_ID);
            reportedUserName = getArguments().getString(ARG_USER_NAME);
            reportedUserAvatar = getArguments().getString(ARG_USER_AVATAR);
        }
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_report_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set adjustResize for keyboard
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        initViews(view);
        setupListeners();
        displayUserInfo();
    }

    private void initViews(View view) {
        btnClose = view.findViewById(R.id.btnClose);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        btnCancel = view.findViewById(R.id.btnCancel);
        etReason = view.findViewById(R.id.etReason);
        tvCharCount = view.findViewById(R.id.tvCharCount);
        tvUserName = view.findViewById(R.id.tvUserName);
        ivUserAvatar = view.findViewById(R.id.ivUserAvatar);
        layoutUserInfo = view.findViewById(R.id.layoutUserInfo);
    }

    private void setupListeners() {
        btnClose.setOnClickListener(v -> dismiss());
        btnCancel.setOnClickListener(v -> dismiss());
        btnSubmit.setOnClickListener(v -> submitReport());

        // Character counter
        etReason.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                tvCharCount.setText(length + "/" + MAX_CHARS);
                
                if (length > MAX_CHARS) {
                    etReason.setError("Vượt quá " + MAX_CHARS + " ký tự");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void displayUserInfo() {
        if (reportedUserName != null) {
            tvUserName.setText(reportedUserName);
        }

        if (reportedUserAvatar != null && !reportedUserAvatar.isEmpty()) {
            Glide.with(this)
                    .load(reportedUserAvatar)
                    .placeholder(R.drawable.avatar_placeholder)
                    .into(ivUserAvatar);
        }
    }

    private void submitReport() {
        String reason = etReason.getText().toString().trim();

        if (reason.isEmpty()) {
            etReason.setError("Vui lòng nhập lý do báo cáo");
            etReason.requestFocus();
            return;
        }

        if (reason.length() < 20) {
            etReason.setError("Lý do phải có ít nhất 20 ký tự");
            etReason.requestFocus();
            return;
        }

        if (reason.length() > MAX_CHARS) {
            etReason.setError("Vượt quá " + MAX_CHARS + " ký tự");
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang gửi...");

        String reporterId = mAuth.getCurrentUser().getUid();

        // Create report document
        Map<String, Object> report = new HashMap<>();
        report.put("reporterId", reporterId);
        report.put("reportedUserId", reportedUserId);
        report.put("reportedUserName", reportedUserName);
        report.put("reason", reason);
        report.put("status", "pending");
        report.put("timestamp", FieldValue.serverTimestamp());

        db.collection("reports")
                .add(report)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Đã gửi báo cáo thành công", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Gửi báo cáo");
                });
    }
}
