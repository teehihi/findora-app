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
import androidx.appcompat.app.AppCompatActivity;
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
import hcmute.edu.vn.findora.ResolvePostBottomSheet;
import hcmute.edu.vn.findora.model.Post;

public class MyPostsAdapter extends RecyclerView.Adapter<MyPostsAdapter.ViewHolder> {

    private final Context context;
    private final List<Post> postList;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentTab = "lost"; // Mặc định là tab "Thất lạc"

    public MyPostsAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    public void setCurrentTab(String tab) {
        this.currentTab = tab;
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
        holder.tvMyPostLocation.setText("Đang cập nhật khu vực");

        // Badge (Lost/Found)
        if ("lost".equals(post.getType())) {
            holder.tvMyPostBadge.setText("MẤT");
            holder.tvMyPostBadge.setBackgroundResource(R.drawable.bg_badge_lost);
            holder.tvMyPostBadge.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
        } else {
            holder.tvMyPostBadge.setText("NHẶT ĐƯỢC");
            holder.tvMyPostBadge.setBackgroundResource(R.drawable.bg_badge_found);
            holder.tvMyPostBadge.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
        }

        // Image
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            Glide.with(context)
                 .load(post.getImageUrl())
                 .placeholder(R.drawable.bg_img_placeholder)
                 .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(32)))
                 .into(holder.ivMyPostImage);
        } else {
            Glide.with(context).clear(holder.ivMyPostImage);
            holder.ivMyPostImage.setImageResource(R.drawable.bg_img_placeholder);
        }

        // Time
        if (post.getCreatedAt() != null) {
            Date date = post.getCreatedAt().toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.tvMyPostTime.setText(sdf.format(date));
        } else {
            holder.tvMyPostTime.setText("Gần đây");
        }

        // Card click → PostDetail
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
            if (post.getLat() != null && post.getLng() != null) {
                intent.putExtra("lat", post.getLat());
                intent.putExtra("lng", post.getLng());
                if (post.getAddress() != null) intent.putExtra("address", post.getAddress());
            }
            context.startActivity(intent);
        });

        // Edit Button
        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, CreatePostActivity.class);
            intent.putExtra("editPostId", post.getId());
            context.startActivity(intent);
        });

        // Resolved Button - Check status và hiển thị text phù hợp
        String status = post.getStatus();
        if ("resolved".equals(status) || "closed".equals(status)) {
            // Đã giải quyết → Disable button, đổi text và style
            holder.btnResolved.setText("Đã giải quyết");
            holder.btnResolved.setEnabled(false);
            holder.btnResolved.setBackgroundResource(R.drawable.bg_button_disabled);
            holder.btnResolved.setTextColor(android.graphics.Color.parseColor("#9CA3AF"));
            holder.btnResolved.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle, 0, 0, 0);
        } else {
            // Chưa giải quyết → Enable button, text "Giải quyết"
            holder.btnResolved.setText("Giải quyết");
            holder.btnResolved.setEnabled(true);
            holder.btnResolved.setBackgroundResource(R.drawable.bg_button_primary_ios);
            holder.btnResolved.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
            holder.btnResolved.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            
            holder.btnResolved.setOnClickListener(v -> {
                if ("lost".equals(currentTab)) {
                    // Tab "Thất lạc": Hiển thị flow đầy đủ (3 options)
                    ResolvePostBottomSheet bottomSheet = ResolvePostBottomSheet.newInstance(
                        post.getId(),
                        post.getUserId()
                    );
                    bottomSheet.show(((AppCompatActivity) context).getSupportFragmentManager(), 
                                   "ResolvePostBottomSheet");
                } else {
                    // Tab "Tìm thấy": Đánh dấu đã trả lại cho chủ nhân (đơn giản)
                    resolveFoundPost(post);
                }
            });
        }
    }

    /**
     * Đánh dấu bài "Tìm thấy" là đã trả lại cho chủ nhân
     */
    private void resolveFoundPost(Post post) {
        new androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Xác nhận")
            .setMessage("Bạn đã trả lại đồ vật cho chủ nhân?")
            .setPositiveButton("Đã trả lại", (dialog, which) -> {
                db.collection("posts").document(post.getId())
                    .update("status", "resolved", 
                           "resolvedAt", com.google.firebase.Timestamp.now())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Đã đánh dấu hoàn tất!", Toast.LENGTH_SHORT).show();
                        // Update post object và refresh UI
                        post.setStatus("resolved");
                        notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> 
                        Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            })
            .setNegativeButton("Hủy", null)
            .show();
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
