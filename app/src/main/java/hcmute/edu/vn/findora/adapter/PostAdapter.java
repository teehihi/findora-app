package hcmute.edu.vn.findora.adapter;

import android.content.Context;
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

        // Badge: LOST (Red) / FOUND (Blue)
        if ("lost".equals(post.getType())) {
            holder.tvType.setText("LOST");
            holder.tvType.setBackgroundResource(R.drawable.bg_chip_active);
            holder.tvType.getBackground().setTint(context.getResources().getColor(R.color.badge_lost_bg));
        } else {
            holder.tvType.setText("FOUND");
            holder.tvType.setBackgroundResource(R.drawable.bg_chip_active);
            holder.tvType.getBackground().setTint(context.getResources().getColor(R.color.badge_found_bg));
        }

        // Placeholder for image (until Firebase Storage is implemented)
        holder.ivPostImage.setImageResource(R.drawable.bg_img_placeholder);

        // Created time: "2 HOURS AGO" style
        if (post.getCreatedAt() != null) {
            Date date = post.getCreatedAt().toDate();
            // Using placeholder logic for relative time for now
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.tvCreatedAt.setText(sdf.format(date).toUpperCase() + " • RECENT");
        } else {
            holder.tvCreatedAt.setText("RECENT");
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvTitle, tvDescription, tvCreatedAt;
        ImageView ivPostImage;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType        = itemView.findViewById(R.id.tvType);
            tvTitle       = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvCreatedAt   = itemView.findViewById(R.id.tvCreatedAt);
            ivPostImage   = itemView.findViewById(R.id.ivPostImage);
        }
    }
}
