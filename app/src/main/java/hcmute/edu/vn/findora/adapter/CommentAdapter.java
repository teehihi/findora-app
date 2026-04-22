package hcmute.edu.vn.findora.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import hcmute.edu.vn.findora.R;
import hcmute.edu.vn.findora.model.Comment;

/**
 * Adapter hiển thị danh sách comments
 */
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {
    
    private final Context context;
    private final List<Comment> comments;
    
    public CommentAdapter(Context context, List<Comment> comments) {
        this.context = context;
        this.comments = comments;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
            .inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment comment = comments.get(position);
        
        // Set avatar
        if (comment.getUserAvatar() != null && !comment.getUserAvatar().isEmpty()) {
            Glide.with(context)
                .load(comment.getUserAvatar())
                .placeholder(R.drawable.avatar_placeholder)
                .circleCrop()
                .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.avatar_placeholder);
        }
        
        // Set name and text
        holder.tvUserName.setText(comment.getUserName());
        holder.tvCommentText.setText(comment.getText());
        
        // Set time
        if (comment.getTimestamp() != null) {
            long timeMs = comment.getTimestamp().toDate().getTime();
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                timeMs,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            );
            holder.tvTime.setText(timeAgo);
        }
    }
    
    @Override
    public int getItemCount() {
        return comments.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvUserName;
        TextView tvCommentText;
        TextView tvTime;
        
        ViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivCommentAvatar);
            tvUserName = itemView.findViewById(R.id.tvCommentUserName);
            tvCommentText = itemView.findViewById(R.id.tvCommentText);
            tvTime = itemView.findViewById(R.id.tvCommentTime);
        }
    }
}
