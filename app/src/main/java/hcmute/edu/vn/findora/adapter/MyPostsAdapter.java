package hcmute.edu.vn.findora.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.findora.CreatePostActivity;
import hcmute.edu.vn.findora.R;
import hcmute.edu.vn.findora.PostDetailActivity;
import hcmute.edu.vn.findora.model.Post;

public class MyPostsAdapter extends RecyclerView.Adapter<MyPostsAdapter.ViewHolder> {

    private final Context context;
    private final List<Post> postList;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public MyPostsAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_my_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.tvMyPostTitle.setText(post.getTitle() != null ? post.getTitle() : "Chưa có tiêu đề");
        
        // Location placeholder - since our Post model might not have explicit location text matching district, we use a default or mapped one
        holder.tvMyPostLocation.setText("Đang cập nhật khu vực");

        // Badge (Lost/Found)
        if ("lost".equals(post.getType())) {
            holder.tvMyPostBadge.setText("MẤT");
            holder.tvMyPostBadge.setBackgroundResource(R.drawable.bg_badge_lost);
            holder.tvMyPostBadge.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
        } else {
            holder.tvMyPostBadge.setText("NHẶT ĐƯỢC");
            // Assuming we reuse the default blue for FOUND
            holder.tvMyPostBadge.setBackgroundResource(R.drawable.bg_badge_found);
            holder.tvMyPostBadge.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
        }

        // Image
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            Glide.with(context)
                 .load(post.getImageUrl())
                 .placeholder(R.drawable.bg_img_placeholder)
                 .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(32))) // Match 16dp radius from XML
                 .into(holder.ivMyPostImage);
        } else {
            Glide.with(context).clear(holder.ivMyPostImage);
            holder.ivMyPostImage.setImageResource(R.drawable.bg_img_placeholder);
        }

        // Time
        if (post.getCreatedAt() != null) {
            Date date = post.getCreatedAt().toDate();
            // Simplified relative time placeholder. In real app, use DateUtils.getRelativeTimeSpanString
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.tvMyPostTime.setText(sdf.format(date));
        } else {
            holder.tvMyPostTime.setText("Gần đây");
        }

        // Handle entire card click to view details
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PostDetailActivity.class);
            intent.putExtra("postId", post.getId());
            intent.putExtra("title", post.getTitle());
            intent.putExtra("description", post.getDescription());
            intent.putExtra("type", post.getType());
            intent.putExtra("userId", post.getUserId());
            intent.putExtra("imageUrl", post.getImageUrl());
            if (post.getCreatedAt() != null) {
                intent.putExtra("timestamp", post.getCreatedAt().getSeconds() * 1000L);
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

        // Edit Button
        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, CreatePostActivity.class);
            intent.putExtra("editPostId", post.getId());
            context.startActivity(intent);
        });

        // Resolved Button
        holder.btnResolved.setOnClickListener(v -> {
            // Logic to mark as resolved / archive.
            db.collection("posts").document(post.getId())
                .update("status", "resolved")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Đã đánh dấu hoàn tất!", Toast.LENGTH_SHORT).show();
                    // Optionally remove from list or dim UI
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMyPostImage;
        TextView tvMyPostBadge, tvMyPostTime, tvMyPostTitle, tvMyPostLocation;
        TextView btnEdit, btnResolved;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMyPostImage = itemView.findViewById(R.id.ivMyPostImage);
            tvMyPostBadge = itemView.findViewById(R.id.tvMyPostBadge);
            tvMyPostTime = itemView.findViewById(R.id.tvMyPostTime);
            tvMyPostTitle = itemView.findViewById(R.id.tvMyPostTitle);
            tvMyPostLocation = itemView.findViewById(R.id.tvMyPostLocation);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnResolved = itemView.findViewById(R.id.btnResolved);
        }
    }
}
