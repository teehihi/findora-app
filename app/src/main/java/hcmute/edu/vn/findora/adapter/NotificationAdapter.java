package hcmute.edu.vn.findora.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import hcmute.edu.vn.findora.ChatActivity;
import hcmute.edu.vn.findora.PostDetailActivity;
import hcmute.edu.vn.findora.R;
import hcmute.edu.vn.findora.model.Notification;
import hcmute.edu.vn.findora.utils.NotificationHelper;

/**
 * Adapter hiển thị danh sách thông báo trong RecyclerView
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    
    private final Context context;
    private final List<Notification> notifications;
    
    public NotificationAdapter(Context context, List<Notification> notifications) {
        this.context = context;
        this.notifications = notifications;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
            .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        
        // Set icon
        holder.ivIcon.setImageResource(notification.getIconResource());
        holder.ivIcon.setColorFilter(
            ContextCompat.getColor(context, notification.getColorResource())
        );
        
        // Set avatar nếu có
        if (notification.getSenderAvatar() != null && !notification.getSenderAvatar().isEmpty()) {
            holder.ivAvatar.setVisibility(View.VISIBLE);
            Glide.with(context)
                .load(notification.getSenderAvatar())
                .placeholder(R.drawable.avatar_placeholder)
                .circleCrop()
                .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setVisibility(View.GONE);
        }
        
        // Set title và body
        holder.tvTitle.setText(notification.getTitle());
        holder.tvBody.setText(notification.getBody());
        
        // Set time
        if (notification.getTimestamp() != null) {
            long timeMs = notification.getTimestamp().toDate().getTime();
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                timeMs,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            );
            holder.tvTime.setText(timeAgo);
        }
        
        // Hiển thị unread indicator
        holder.viewUnread.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
        
        // Set background cho unread
        if (!notification.isRead()) {
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(context, R.color.notification_unread_bg)
            );
        } else {
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(context, android.R.color.transparent)
            );
        }
        
        // Click listener
        holder.itemView.setOnClickListener(v -> {
            // Đánh dấu đã đọc
            if (!notification.isRead()) {
                NotificationHelper.markAsRead(notification.getId());
                notification.setRead(true);
                notifyItemChanged(position);
            }
            
            // Navigate dựa trên loại thông báo
            handleNotificationClick(notification);
        });
    }
    
    @Override
    public int getItemCount() {
        return notifications.size();
    }
    
    /**
     * Xử lý click vào notification
     */
    private void handleNotificationClick(Notification notification) {
        Intent intent;
        
        switch (notification.getType()) {
            case Notification.TYPE_NEW_MESSAGE:
                // Mở ChatActivity
                if (notification.getChatId() != null) {
                    intent = new Intent(context, ChatActivity.class);
                    intent.putExtra("chatId", notification.getChatId());
                    intent.putExtra("otherUserId", notification.getSenderId());
                    intent.putExtra("otherUserName", notification.getSenderName());
                    context.startActivity(intent);
                }
                break;
                
            case Notification.TYPE_AI_MATCH:
            case Notification.TYPE_COMMENT:
            case Notification.TYPE_LIKE:
            case Notification.TYPE_POST_FOUND:
                // Mở PostDetailActivity
                if (notification.getPostId() != null) {
                    intent = new Intent(context, PostDetailActivity.class);
                    intent.putExtra("postId", notification.getPostId());
                    context.startActivity(intent);
                }
                break;
                
            case Notification.TYPE_SYSTEM:
                // Không làm gì hoặc mở MainActivity
                break;
        }
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        ImageView ivAvatar;
        TextView tvTitle;
        TextView tvBody;
        TextView tvTime;
        View viewUnread;
        
        ViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
            ivAvatar = itemView.findViewById(R.id.ivNotificationAvatar);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvBody = itemView.findViewById(R.id.tvNotificationBody);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            viewUnread = itemView.findViewById(R.id.viewUnreadIndicator);
        }
    }
}
