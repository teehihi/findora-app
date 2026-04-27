package hcmute.edu.vn.findora;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.findora.utils.GamificationHelper;

public class ResolveBottomSheetFragment extends BottomSheetDialogFragment {

    private String postId;
    private String ownerId;
    private Spinner spinnerFinders;
    private RatingBar ratingBar;
    private TextInputEditText etComment;
    private Button btnConfirm;
    private FirebaseFirestore db;
    private List<String> finderIds;
    private List<String> finderNames;

    public static ResolveBottomSheetFragment newInstance(String postId, String ownerId) {
        ResolveBottomSheetFragment fragment = new ResolveBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("postId", postId);
        args.putString("ownerId", ownerId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_resolve, container, false);
        db = FirebaseFirestore.getInstance();
        
        if (getArguments() != null) {
            postId = getArguments().getString("postId");
            ownerId = getArguments().getString("ownerId");
        }

        spinnerFinders = view.findViewById(R.id.spinnerFinders);
        ratingBar = view.findViewById(R.id.ratingBar);
        etComment = view.findViewById(R.id.etComment);
        btnConfirm = view.findViewById(R.id.btnConfirm);

        loadChattedUsers();

        btnConfirm.setOnClickListener(v -> confirmResolution());

        return view;
    }

    private void loadChattedUsers() {
        finderIds = new ArrayList<>();
        finderNames = new ArrayList<>();
        
        // Truy vấn những người đã nhắn tin trong bài đăng này
        db.collection("chats")
                .whereEqualTo("postId", postId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String user1 = doc.getString("user1");
                        String user2 = doc.getString("user2");
                        String finderId = user1.equals(ownerId) ? user2 : user1;
                        
                        if (!finderIds.contains(finderId)) {
                            finderIds.add(finderId);
                            fetchUserName(finderId);
                        }
                    }
                });
    }

    private void fetchUserName(String uid) {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("fullName");
                finderNames.add(name != null ? name : uid);
                
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), 
                        android.R.layout.simple_spinner_item, finderNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerFinders.setAdapter(adapter);
            }
        });
    }

    private void confirmResolution() {
        if (spinnerFinders.getSelectedItem() == null) {
            Toast.makeText(getContext(), "Vui lòng chọn người trả đồ", Toast.LENGTH_SHORT).show();
            return;
        }

        int index = spinnerFinders.getSelectedItemPosition();
        String finderId = finderIds.get(index);
        int rating = (int) ratingBar.getRating();
        String comment = etComment.getText().toString();

        // Lấy thông tin finder để cộng điểm
        db.collection("users").document(finderId).get().addOnSuccessListener(doc -> {
            long points = doc.getLong("points") != null ? doc.getLong("points") : 0;
            int totalRatings = doc.getLong("totalRatings") != null ? doc.getLong("totalRatings").intValue() : 0;
            double avgRating = doc.getDouble("averageRating") != null ? doc.getDouble("averageRating") : 0.0;

            GamificationHelper helper = new GamificationHelper();
            helper.confirmItemReturned(postId, ownerId, finderId, rating, comment, points, totalRatings, avgRating, 
                new GamificationHelper.OnGamificationCallback() {
                    @Override
                    public void onSuccess(String newLevel, long newPoints) {
                        Toast.makeText(getContext(), "Xác nhận thành công! +50 FP cho người tìm thấy.", Toast.LENGTH_LONG).show();
                        dismiss();
                        if (getActivity() != null) getActivity().finish();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(getContext(), "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
        });
    }
}
