package hcmute.edu.vn.findora;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

    private String postId;
    private String postOwnerId;

    // Views
    private LinearLayout layoutStep1, layoutStep2, layoutStep3;
    private MaterialCardView cardOption1, cardOption2, cardOption3;
    private ImageView radioOption1, radioOption2, radioOption3;
    private MaterialButton btnStep1Continue, btnStep2Back, btnStep2Continue, btnStep3Back, btnStep3Confirm;
    private RecyclerView recyclerHelpers;
    private ImageView star1, star2, star3, star4, star5;
    private EditText etFeedback;
    private ImageButton btnClose;

    private HelperSelectionAdapter helperAdapter;
    private List<User> helpersList = new ArrayList<>();

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
        loadHelpers();
    }

    private void initViews(View view) {
        layoutStep1 = view.findViewById(R.id.layoutStep1);
        layoutStep2 = view.findViewById(R.id.layoutStep2);
        layoutStep3 = view.findViewById(R.id.layoutStep3);

        cardOption1 = view.findViewById(R.id.cardOption1);
        cardOption2 = view.findViewById(R.id.cardOption2);
        cardOption3 = view.findViewById(R.id.cardOption3);

        radioOption1 = view.findViewById(R.id.radioOption1);
        radioOption2 = view.findViewById(R.id.radioOption2);
        radioOption3 = view.findViewById(R.id.radioOption3);

        btnStep1Continue = view.findViewById(R.id.btnStep1Continue);
        btnStep2Back = view.findViewById(R.id.btnStep2Back);
        btnStep2Continue = view.findViewById(R.id.btnStep2Continue);
        btnStep3Back = view.findViewById(R.id.btnStep3Back);
        btnStep3Confirm = view.findViewById(R.id.btnStep3Confirm);

        recyclerHelpers = view.findViewById(R.id.recyclerHelpers);
        
        star1 = view.findViewById(R.id.star1);
        star2 = view.findViewById(R.id.star2);
        star3 = view.findViewById(R.id.star3);
        star4 = view.findViewById(R.id.star4);
        star5 = view.findViewById(R.id.star5);
        
        etFeedback = view.findViewById(R.id.etFeedback);
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
        recyclerHelpers.setLayoutManager(new LinearLayoutManager(getContext()));
        helperAdapter = new HelperSelectionAdapter(helpersList, user -> {
            selectedHelperId = user.getUid();
            btnStep2Continue.setEnabled(true);
        });
        recyclerHelpers.setAdapter(helperAdapter);

        btnStep2Back.setOnClickListener(v -> navigateToStep(STEP_1));
        btnStep2Continue.setOnClickListener(v -> navigateToStep(STEP_3));
    }

    private void setupStep3() {
        ImageView[] stars = {star1, star2, star3, star4, star5};
        
        for (int i = 0; i < stars.length; i++) {
            final int rating = i + 1;
            stars[i].setOnClickListener(v -> setRating(rating));
        }

        btnStep3Back.setOnClickListener(v -> navigateToStep(STEP_2));
        btnStep3Confirm.setOnClickListener(v -> {
            String feedback = etFeedback.getText().toString().trim();
            resolvePost(selectedHelperId, selectedRating, feedback);
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
    }

    private void loadHelpers() {
        db.collection("posts").document(postId).collection("interested")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    helpersList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String userId = doc.getId();
                        loadUserDetails(userId);
                    }
                });
    }

    private void loadUserDetails(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            helpersList.add(user);
                            helperAdapter.notifyDataSetChanged();
                        }
                    }
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
