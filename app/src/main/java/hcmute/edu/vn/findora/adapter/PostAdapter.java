package hcmute.edu.vn.findora.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.bumptech.glide.Glide;

import hcmute.edu.vn.findora.R;
import hcmute.edu.vn.findora.model.Post;

/**
 * Adapter hiển thị danh sách bài đăng trong RecyclerView.
 * UI theo Modern Stitch Design (Card với ảnh).
 */
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

        // Badge: LOST (Red) / FOUND (Green)
        if ("lost".equals(post.getType())) {
            holder.tvType.setText("THẤT LẠC");
            holder.tvType.setBackgroundResource(R.drawable.bg_badge_lost);
        } else {
            holder.tvType.setText("TÌM THẤY");
            holder.tvType.setBackgroundResource(R.drawable.bg_badge_found);
        }

        // Load image using Glide
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            holder.flImageContainer.setVisibility(View.VISIBLE);
            Glide.with(context)
                 .load(post.getImageUrl())
                 .placeholder(R.drawable.bg_img_placeholder)
                 .centerCrop()
                 .into(holder.ivPostImage);
        } else {
            holder.flImageContainer.setVisibility(View.GONE);
            // Optionally, clear Glide to prevent recycled views showing wrong info
            Glide.with(context).clear(holder.ivPostImage);
        }

        // Created time: "2 HOURS AGO" style
        if (post.getCreatedAt() != null) {
            Date date = post.getCreatedAt().toDate();
            // Using placeholder logic for relative time for now
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm • dd/MM/yyyy", Locale.getDefault());
            holder.tvCreatedAt.setText(sdf.format(date).toUpperCase() + " • RECENT");
        } else {
            holder.tvCreatedAt.setText("RECENT");
        }
        
        // Load poster avatar
        if (post.getUserId() != null && !post.getUserId().isEmpty() && holder.ivPosterAvatar != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(post.getUserId())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String photoUrl = doc.getString("photoUrl");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            // Clear tint before loading image
                            holder.ivPosterAvatar.setImageTintList(null);
                            Glide.with(context)
                                .load(photoUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .into(holder.ivPosterAvatar);
                        } else {
                            holder.ivPosterAvatar.setImageResource(R.drawable.ic_person);
                            holder.ivPosterAvatar.setImageTintList(android.content.res.ColorStateList.valueOf(
                                context.getResources().getColor(R.color.outline, null)));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    holder.ivPosterAvatar.setImageResource(R.drawable.ic_person);
                    holder.ivPosterAvatar.setImageTintList(android.content.res.ColorStateList.valueOf(
                        context.getResources().getColor(R.color.outline, null)));
                });
        }

        // Handle item click to open detailed view
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, hcmute.edu.vn.findora.PostDetailActivity.class);
            intent.putExtra("postId", post.getId());
            intent.putExtra("title", post.getTitle());
            intent.putExtra("description", post.getDescription());
            intent.putExtra("type", post.getType());
            intent.putExtra("userId", post.getUserId());
            if (post.getCreatedAt() != null) {
                intent.putExtra("timestamp", post.getCreatedAt().getSeconds() * 1000L);
            }
            if (post.getImageUrl() != null) {
                intent.putExtra("imageUrl", post.getImageUrl());
            }
            // Pass location data
            if (post.getLat() != null && post.getLng() != null) {
                intent.putExtra("lat", post.getLat());
                intent.putExtra("lng", post.getLng());
                if (post.getAddress() != null) {
                    intent.putExtra("address", post.getAddress());
                }
            }
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvTitle, tvDescription, tvCreatedAt;
        ImageView ivPostImage, ivPosterAvatar;
        android.widget.FrameLayout flImageContainer;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType        = itemView.findViewById(R.id.tvType);
            tvTitle       = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvCreatedAt   = itemView.findViewById(R.id.tvCreatedAt);
            ivPostImage   = itemView.findViewById(R.id.ivPostImage);
            flImageContainer = itemView.findViewById(R.id.flImageContainer);
            ivPosterAvatar = itemView.findViewById(R.id.ivPosterAvatar);
        }
    }
}
