package hcmute.edu.vn.findora;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hcmute.edu.vn.findora.adapter.SelectableUserAdapter;

/**
 * Dialog để chọn người giúp đỡ từ danh sách những người đã chat về bài viết
 * Sau khi chọn, sẽ cộng điểm cho người được chọn và đánh dấu bài viết là đã giải quyết
 * 
 * DEPRECATED: Chức năng này đã được thay thế bởi ResolvePostBottomSheet
 */
public class SelectHelperDialog extends BottomSheetDialogFragment {

    private static final String ARG_POST_ID = "postId";
    private static final int REWARD_POINTS = 50;

    private RecyclerView rvUsers;
    private MaterialButton btnConfirm;
    private LinearLayout layoutEmpty;

    private SelectableUserAdapter adapter;
    private List<SelectableUserAdapter.UserItem> userList;

    private FirebaseFirestore db;
    private String postId;
    private String ownerId;

    /**
     * Interface callback khi bài viết được giải quyết thành công
     */
    public interface OnResolvedListener {
        void onResolved(String finderName);
    }

    private OnResolvedListener resolvedListener;

    /**
     * Tạo instance mới của SelectHelperDialog
     * 
     * @param postId ID của bài viết cần chọn helper
     * @return Instance của SelectHelperDialog
     */
    public static SelectHelperDialog newInstance(String postId) {
        SelectHelperDialog dialog = new SelectHelperDialog();
        Bundle args = new Bundle();
        args.putString(ARG_POST_ID, postId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnResolvedListener(OnResolvedListener listener) {
        this.resolvedListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        ownerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (getArguments() != null) {
            postId = getArguments().getString(ARG_POST_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_select_helper, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvUsers     = view.findViewById(R.id.rvUsers);
        btnConfirm  = view.findViewById(R.id.btnConfirm);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);

        view.findViewById(R.id.btnClose).setOnClickListener(v -> dismiss());

        // Setup RecyclerView
        userList = new ArrayList<>();
        adapter = new SelectableUserAdapter(requireContext(), userList);
        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvUsers.setAdapter(adapter);

        // Enable confirm button when user selected
        adapter.setOnUserSelectedListener(user -> {
            btnConfirm.setEnabled(true);
            btnConfirm.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#22C55E")));
        });

        btnConfirm.setOnClickListener(v -> confirmAndAwardPoints());

        // Load chatted users for this post
        loadChattedUsers();
    }

    /**
     * Query Firestore: tìm tất cả users đã chat về postId này
     */
    private void loadChattedUsers() {
        db.collection("chats")
                .whereArrayContains("participants", ownerId)
                .whereEqualTo("postId", postId)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    List<String> otherUserIds = new ArrayList<>();

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshots) {
                        @SuppressWarnings("unchecked")
                        List<String> participants = (List<String>) doc.get("participants");
                        if (participants != null) {
                            for (String uid : participants) {
                                if (!uid.equals(ownerId) && !otherUserIds.contains(uid)) {
                                    otherUserIds.add(uid);
                                }
                            }
                        }
                    }

                    if (otherUserIds.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvUsers.setVisibility(View.GONE);
                        return;
                    }

                    // Fetch user info for each ID
                    fetchUserInfos(otherUserIds);
                })
                .addOnFailureListener(e -> {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvUsers.setVisibility(View.GONE);
                });
    }

    /**
     * Lấy thông tin chi tiết của từng user từ Firestore
     * 
     * @param userIds Danh sách user IDs cần lấy thông tin
     */
    private void fetchUserInfos(List<String> userIds) {
        userList.clear();
        final int[] loaded = {0};

        for (String uid : userIds) {
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String name   = doc.getString("fullName");
                            String avatar = doc.getString("photoUrl");
                            userList.add(new SelectableUserAdapter.UserItem(
                                    uid,
                                    name != null ? name : "Người dùng",
                                    avatar
                            ));
                        }
                        loaded[0]++;
                        if (loaded[0] == userIds.size()) {
                            adapter.notifyDataSetChanged();
                            layoutEmpty.setVisibility(userList.isEmpty() ? View.VISIBLE : View.GONE);
                            rvUsers.setVisibility(userList.isEmpty() ? View.GONE : View.VISIBLE);
                        }
                    });
        }
    }

    /**
     * Xác nhận chọn helper và thực hiện WriteBatch:
     * 1. Update post status = "resolved"
     * 2. Cộng điểm cho finder (+50 points, +1 totalReturned)
     * 3. Tạo transaction record
     */
    private void confirmAndAwardPoints() {
        SelectableUserAdapter.UserItem selectedUser = adapter.getSelectedUser();
        if (selectedUser == null) return;

        btnConfirm.setEnabled(false);
        btnConfirm.setText("Đang xử lý...");

        WriteBatch batch = db.batch();

        // 1. Update post status → resolved
        com.google.firebase.firestore.DocumentReference postRef =
                db.collection("posts").document(postId);
        batch.update(postRef, "status", "resolved");
        batch.update(postRef, "resolvedBy", selectedUser.userId);

        // 2. Increment finder's points +50
        com.google.firebase.firestore.DocumentReference finderRef =
                db.collection("users").document(selectedUser.userId);
        batch.update(finderRef, "points", FieldValue.increment(REWARD_POINTS));
        batch.update(finderRef, "totalReturned", FieldValue.increment(1));

        // 3. Create transaction record
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("type", "return_success");
        transaction.put("userId", selectedUser.userId);
        transaction.put("from", ownerId);
        transaction.put("to", selectedUser.userId);
        transaction.put("points", REWARD_POINTS);
        transaction.put("postId", postId);
        transaction.put("title", "Trả lại đồ vật thành công");
        transaction.put("timestamp", FieldValue.serverTimestamp());

        com.google.firebase.firestore.DocumentReference txRef =
                db.collection("transactions").document();
        batch.set(txRef, transaction);

        // Commit batch
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(),
                            "Đã xác nhận và tặng " + REWARD_POINTS + " điểm cho " + selectedUser.name + "!",
                            Toast.LENGTH_LONG).show();
                    if (resolvedListener != null) resolvedListener.onResolved(selectedUser.name);
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText("Xác nhận & Tặng điểm");
                });
    }
}
