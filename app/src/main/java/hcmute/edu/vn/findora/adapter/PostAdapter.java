package hcmute.edu.vn.findora.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import hcmute.edu.vn.findora.R;
import hcmute.edu.vn.findora.model.Post;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final Context context;
    private final List<Post> postList;

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.tvTitle.setText(post.getTitle());
        holder.tvDescription.setText(post.getDescription());

        // Badge
        if ("lost".equals(post.getType())) {
            holder.tvType.setText("THẤT LẠC");
            holder.tvType.setBackgroundResource(R.drawable.bg_badge_lost);
        } else {
            holder.tvType.setText("TÌM THẤY");
            holder.tvType.setBackgroundResource(R.drawable.bg_badge_found);
        }

        // Image
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            holder.flImageContainer.setVisibility(View.VISIBLE);
            Glide.with(context).load(post.getImageUrl())
                 .placeholder(R.drawable.bg_img_placeholder).centerCrop().into(holder.ivPostImage);
        } else {
            holder.flImageContainer.setVisibility(View.GONE);
            Glide.with(context).clear(holder.ivPostImage);
        }

        // Time
        if (post.getCreatedAt() != null) {
            Date date = post.getCreatedAt().toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm • dd/MM/yyyy", Locale.getDefault());
            holder.tvCreatedAt.setText(sdf.format(date).toUpperCase() + " • RECENT");
        } else {
            holder.tvCreatedAt.setText("RECENT");
        }

        // Avatar
        if (post.getUserId() != null && !post.getUserId().isEmpty() && holder.ivPosterAvatar != null) {
            FirebaseFirestore.getInstance().collection("users").document(post.getUserId()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String photoUrl = doc.getString("photoUrl");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            holder.ivPosterAvatar.setImageTintList(null);
                            Glide.with(context).load(photoUrl).circleCrop()
                                .placeholder(R.drawable.ic_person).error(R.drawable.ic_person)
                                .into(holder.ivPosterAvatar);
                        } else {
                            holder.ivPosterAvatar.setImageResource(R.drawable.ic_person);
                            holder.ivPosterAvatar.setImageTintList(android.content.res.ColorStateList.valueOf(
                                context.getResources().getColor(R.color.outline, null)));
                        }
                    }
                });
        }

        // Like UI - load từ Firestore
        loadLikeState(holder, post);

        // Like button click
        holder.btnLikeAction.setOnClickListener(v -> toggleLike(holder, post));

        // Comment button click → mở PostDetail
        holder.btnCommentAction.setOnClickListener(v -> openDetail(post));

        // Item click
        holder.itemView.setOnClickListener(v -> openDetail(post));
    }

    private void loadLikeState(@NonNull PostViewHolder holder, Post post) {
        if (post.getId() == null || post.getId().isEmpty()) return;

        FirebaseFirestore.getInstance().collection("posts").document(post.getId()).get()
            .addOnSuccessListener(doc -> {
                if (!doc.exists()) return;
                @SuppressWarnings("unchecked")
                List<String> likes = (List<String>) doc.get("likes");
                if (likes == null) likes = new ArrayList<>();

                String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
                boolean isLiked = likes.contains(currentUserId);
                int count = likes.size();

                updateLikeUI(holder, isLiked, count);
            });
    }

    private void toggleLike(@NonNull PostViewHolder holder, Post post) {
        if (post.getId() == null) return;
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUserId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts").document(post.getId()).get()
            .addOnSuccessListener(doc -> {
                if (!doc.exists()) return;
                @SuppressWarnings("unchecked")
                List<String> likes = (List<String>) doc.get("likes");
                if (likes == null) likes = new ArrayList<>();

                boolean isLiked = likes.contains(currentUserId);
                if (isLiked) likes.remove(currentUserId);
                else likes.add(currentUserId);

                final List<String> finalLikes = likes;
                db.collection("posts").document(post.getId())
                    .update("likes", finalLikes)
                    .addOnSuccessListener(v -> updateLikeUI(holder, !isLiked, finalLikes.size()));
            });
    }

    private void updateLikeUI(@NonNull PostViewHolder holder, boolean isLiked, int count) {
        if (isLiked) {
            holder.ivLikeIcon.setImageResource(R.drawable.ic_like_feather_filled);
            holder.tvLikeCount.setTextColor(ContextCompat.getColor(context, R.color.info));
            holder.tvLikeCount.setText(count > 0 ? String.valueOf(count) + " Thích" : "Thích");
        } else {
            holder.ivLikeIcon.setImageResource(R.drawable.ic_like_feather);
            holder.tvLikeCount.setTextColor(ContextCompat.getColor(context, R.color.outline));
            holder.tvLikeCount.setText(count > 0 ? String.valueOf(count) + " Thích" : "Thích");
        }
    }

    private void openDetail(Post post) {
        Intent intent = new Intent(context, hcmute.edu.vn.findora.PostDetailActivity.class);
        intent.putExtra("postId", post.getId());
        intent.putExtra("title", post.getTitle());
        intent.putExtra("description", post.getDescription());
        intent.putExtra("type", post.getType());
        intent.putExtra("userId", post.getUserId());
        if (post.getCreatedAt() != null) intent.putExtra("timestamp", post.getCreatedAt().getSeconds() * 1000L);
        if (post.getImageUrl() != null) intent.putExtra("imageUrl", post.getImageUrl());
        if (post.getLat() != null && post.getLng() != null) {
            intent.putExtra("lat", post.getLat());
            intent.putExtra("lng", post.getLng());
            if (post.getAddress() != null) intent.putExtra("address", post.getAddress());
        }
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() { return postList.size(); }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvTitle, tvDescription, tvCreatedAt, tvLikeCount, tvCommentCount;
        ImageView ivPostImage, ivPosterAvatar, ivLikeIcon;
        android.widget.FrameLayout flImageContainer;
        LinearLayout btnLikeAction, btnCommentAction;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType           = itemView.findViewById(R.id.tvType);
            tvTitle          = itemView.findViewById(R.id.tvTitle);
            tvDescription    = itemView.findViewById(R.id.tvDescription);
            tvCreatedAt      = itemView.findViewById(R.id.tvCreatedAt);
            ivPostImage      = itemView.findViewById(R.id.ivPostImage);
            flImageContainer = itemView.findViewById(R.id.flImageContainer);
            ivPosterAvatar   = itemView.findViewById(R.id.ivPosterAvatar);
            ivLikeIcon       = itemView.findViewById(R.id.ivLikeIcon);
            tvLikeCount      = itemView.findViewById(R.id.tvLikeCount);
            tvCommentCount   = itemView.findViewById(R.id.tvCommentCount);
            btnLikeAction    = itemView.findViewById(R.id.btnLikeAction);
            btnCommentAction = itemView.findViewById(R.id.btnCommentAction);
        }
    }
}
