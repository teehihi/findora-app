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

        // Description: 2 lines collapsed, click to expand
        holder.tvDescription.setMaxLines(2);
        holder.tvDescription.setEllipsize(android.text.TextUtils.TruncateAt.END);
        holder.tvDescription.setTag(false); // false = collapsed
        holder.tvDescription.setOnClickListener(v -> {
            boolean expanded = (boolean) holder.tvDescription.getTag();
            if (expanded) {
                holder.tvDescription.setMaxLines(2);
                holder.tvDescription.setEllipsize(android.text.TextUtils.TruncateAt.END);
            } else {
                holder.tvDescription.setMaxLines(Integer.MAX_VALUE);
                holder.tvDescription.setEllipsize(null);
            }
            holder.tvDescription.setTag(!expanded);
        });
        holder.tvDescription.setText(post.getDescription());

        // Badge
        if ("lost".equals(post.getType())) {
            holder.tvType.setText("THẤT LẠC");
            holder.tvType.setBackgroundResource(R.drawable.bg_badge_lost);
        } else {
            holder.tvType.setText("TÌM THẤY");
            holder.tvType.setBackgroundResource(R.drawable.bg_badge_found);
        }

        // Image - click to open fullscreen
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            holder.flImageContainer.setVisibility(View.VISIBLE);
            Glide.with(context).load(post.getImageUrl())
                 .placeholder(R.drawable.bg_img_placeholder).centerCrop().into(holder.ivPostImage);
            holder.flImageContainer.setOnClickListener(v -> openFullImage(post.getImageUrl()));
        } else {
            holder.flImageContainer.setVisibility(View.GONE);
            Glide.with(context).clear(holder.ivPostImage);
        }

        // Relative time
        if (post.getCreatedAt() != null) {
            holder.tvCreatedAt.setText(getRelativeTime(post.getCreatedAt().toDate()));
        } else {
            holder.tvCreatedAt.setText("Vừa xong");
        }

        // Location
        if (holder.tvLocation != null) {
            String address = post.getAddress();
            holder.tvLocation.setText(address != null && !address.isEmpty() ? address : "Đang cập nhật khu vực");
        }

        // Load poster name + avatar
        if (post.getUserId() != null && !post.getUserId().isEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(post.getUserId()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        if (holder.tvPosterName != null) {
                            String name = doc.getString("fullName");
                            holder.tvPosterName.setText(name != null ? name : "Người dùng");
                        }
                        String photoUrl = doc.getString("photoUrl");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            holder.ivPosterAvatar.setImageTintList(null);
                            Glide.with(context).load(photoUrl).circleCrop()
                                .placeholder(R.drawable.ic_person).error(R.drawable.ic_person)
                                .into(holder.ivPosterAvatar);
                        } else {
                            holder.ivPosterAvatar.setImageResource(R.drawable.ic_person);
                        }
                    }
                });
        }

        // Like state
        loadLikeState(holder, post);

        // Comment count
        loadCommentCount(holder, post);

        // Header click → open detail
        if (holder.layoutHeader != null) {
            holder.layoutHeader.setOnClickListener(v -> openDetail(post));
        }

        // Title/desc area click → open detail (handled separately from description expand)
        if (holder.layoutTitleDesc != null) {
            holder.layoutTitleDesc.setOnClickListener(v -> openDetail(post));
            // But description itself handles expand, so stop propagation
            holder.tvDescription.setOnClickListener(v -> {
                boolean expanded = (boolean) holder.tvDescription.getTag();
                if (expanded) {
                    holder.tvDescription.setMaxLines(2);
                    holder.tvDescription.setEllipsize(android.text.TextUtils.TruncateAt.END);
                } else {
                    holder.tvDescription.setMaxLines(Integer.MAX_VALUE);
                    holder.tvDescription.setEllipsize(null);
                }
                holder.tvDescription.setTag(!expanded);
            });
        }

        holder.btnLikeAction.setOnClickListener(v -> toggleLike(holder, post));
        holder.btnCommentAction.setOnClickListener(v -> openDetail(post));
    }

    private void openFullImage(String imageUrl) {
        android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_full_image);
        ImageView ivFull = dialog.findViewById(R.id.ivFullImage);
        View btnClose = dialog.findViewById(R.id.btnCloseFullImage);
        Glide.with(context).load(imageUrl).into(ivFull);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        ivFull.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private String getRelativeTime(Date date) {
        long now = System.currentTimeMillis();
        long diff = now - date.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        java.util.Calendar postCal = java.util.Calendar.getInstance();
        postCal.setTime(date);
        java.util.Calendar nowCal = java.util.Calendar.getInstance();

        int postYear = postCal.get(java.util.Calendar.YEAR);
        int nowYear = nowCal.get(java.util.Calendar.YEAR);
        int postMonth = postCal.get(java.util.Calendar.MONTH);
        int nowMonth = nowCal.get(java.util.Calendar.MONTH);
        int postDay = postCal.get(java.util.Calendar.DAY_OF_YEAR);
        int nowDay = nowCal.get(java.util.Calendar.DAY_OF_YEAR);

        if (seconds < 60) return "Vừa xong";
        if (minutes < 60) return minutes + " phút trước";
        if (hours < 24 && postDay == nowDay) return hours + " giờ trước";

        // Hôm qua
        if (postYear == nowYear && nowDay - postDay == 1) {
            return "Hôm qua lúc " + new SimpleDateFormat("HH:mm", new Locale("vi")).format(date);
        }

        // Cùng tháng, cùng năm → ngày tháng + giờ
        if (postYear == nowYear && postMonth == nowMonth) {
            return new SimpleDateFormat("d 'tháng' M 'lúc' HH:mm", new Locale("vi")).format(date);
        }

        // Cùng năm, khác tháng → chỉ ngày tháng
        if (postYear == nowYear) {
            return new SimpleDateFormat("d 'tháng' M", new Locale("vi")).format(date);
        }

        // Khác năm → ngày tháng năm + giờ
        return new SimpleDateFormat("d 'tháng' M, yyyy 'lúc' HH:mm", new Locale("vi")).format(date);
    }

    private void loadCommentCount(@NonNull PostViewHolder holder, Post post) {
        if (post.getId() == null || post.getId().isEmpty()) return;
        FirebaseFirestore.getInstance().collection("posts").document(post.getId())
            .collection("comments").get()
            .addOnSuccessListener(snap -> {
                int count = snap.size();
                if (holder.tvCommentCount != null) {
                    if (count > 0) {
                        holder.tvCommentCount.setText(String.valueOf(count));
                        holder.tvCommentCount.setVisibility(View.VISIBLE);
                    } else {
                        holder.tvCommentCount.setVisibility(View.GONE);
                    }
                }
            });
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
            if (holder.tvLikeCount != null) holder.tvLikeCount.setTextColor(ContextCompat.getColor(context, R.color.info));
        } else {
            holder.ivLikeIcon.setImageResource(R.drawable.ic_like_feather);
            if (holder.tvLikeCount != null) holder.tvLikeCount.setTextColor(ContextCompat.getColor(context, R.color.outline));
        }
        if (holder.tvLikeCount != null) {
            if (count > 0) {
                holder.tvLikeCount.setText(String.valueOf(count));
                holder.tvLikeCount.setVisibility(View.VISIBLE);
            } else {
                holder.tvLikeCount.setVisibility(View.GONE);
            }
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
        TextView tvPosterName, tvLocation;
        ImageView ivPostImage, ivPosterAvatar, ivLikeIcon;
        android.widget.FrameLayout flImageContainer;
        LinearLayout btnLikeAction, btnCommentAction, layoutHeader, layoutTitleDesc;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType           = itemView.findViewById(R.id.tvType);
            tvTitle          = itemView.findViewById(R.id.tvTitle);
            tvDescription    = itemView.findViewById(R.id.tvDescription);
            tvCreatedAt      = itemView.findViewById(R.id.tvCreatedAt);
            tvPosterName     = itemView.findViewById(R.id.tvPosterName);
            tvLocation       = itemView.findViewById(R.id.tvLocation);
            ivPostImage      = itemView.findViewById(R.id.ivPostImage);
            flImageContainer = itemView.findViewById(R.id.flImageContainer);
            ivPosterAvatar   = itemView.findViewById(R.id.ivPosterAvatar);
            ivLikeIcon       = itemView.findViewById(R.id.ivLikeIcon);
            tvLikeCount      = itemView.findViewById(R.id.tvLikeCount);
            tvCommentCount   = itemView.findViewById(R.id.tvCommentCount);
            btnLikeAction    = itemView.findViewById(R.id.btnLikeAction);
            btnCommentAction = itemView.findViewById(R.id.btnCommentAction);
            layoutHeader     = itemView.findViewById(R.id.layoutHeader);
            layoutTitleDesc  = itemView.findViewById(R.id.layoutTitleDesc);
        }
    }
}
