package hcmute.edu.vn.findora;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditProfileActivity extends AppCompatActivity {

    private ImageButton btnBack, btnChangeAvatar;
    private ImageView ivEditAvatar;
    private TextInputEditText etFullName, etPhone;
    private MaterialButton btnSaveProfile;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private Uri selectedImageUri = null;
    private String uid;

    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this)
                            .load(selectedImageUri)
                            .transform(new CircleCrop())
                            .into(ivEditAvatar);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        btnBack = findViewById(R.id.btnBack);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        ivEditAvatar = findViewById(R.id.ivEditAvatar);
        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        uid = user.getUid();

        loadCurrentData();

        btnBack.setOnClickListener(v -> finish());
        btnChangeAvatar.setOnClickListener(v -> pickImage.launch("image/*"));
        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void loadCurrentData() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String fullName = doc.getString("fullName");
                        String phone = doc.getString("phone");
                        String photoUrl = doc.getString("photoUrl");

                        if (fullName != null) etFullName.setText(fullName);
                        if (phone != null) etPhone.setText(phone);

                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(photoUrl)
                                    .transform(new CircleCrop())
                                    .placeholder(R.drawable.ic_person)
                                    .into(ivEditAvatar);
                        } else {
                            Glide.with(this)
                                    .load(R.drawable.ic_person)
                                    .transform(new CircleCrop())
                                    .into(ivEditAvatar);
                        }
                    }
                });
    }

    private void saveProfile() {
        String newFullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String newPhone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

        if (newFullName.isEmpty()) {
            etFullName.setError("Vui lòng nhập họ tên");
            return;
        }

        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText("Đang lưu...");

        if (selectedImageUri != null) {
            // Upload ảnh trước
            String filename = "avatars/" + uid + "/" + UUID.randomUUID().toString() + ".jpg";
            StorageReference storageRef = storage.getReference().child(filename);

            storageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        updateFirestore(newFullName, newPhone, uri.toString());
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnSaveProfile.setEnabled(true);
                        btnSaveProfile.setText("Lưu thay đổi");
                    });
        } else {
            // Cập nhật không có ảnh mới
            updateFirestore(newFullName, newPhone, null);
        }
    }

    private void updateFirestore(String newFullName, String newPhone, String newPhotoUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", newFullName);
        updates.put("phone", newPhone);
        if (newPhotoUrl != null) {
            updates.put("photoUrl", newPhotoUrl);
        }

        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSaveProfile.setEnabled(true);
                    btnSaveProfile.setText("Lưu thay đổi");
                });
    }
}
