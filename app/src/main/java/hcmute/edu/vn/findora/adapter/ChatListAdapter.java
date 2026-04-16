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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import hcmute.edu.vn.findora.ChatActivity;
import hcmute.edu.vn.findora.R;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    private final Context context;
    private final List<DocumentSnapshot> chatDocs;
    private final String currentUserId;

    public ChatListAdapter(Context context, List<DocumentSnapshot> chatDocs, String currentUserId) {
        this.context = context;
        this.chatDocs = chatDocs;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot doc = chatDocs.get(position);

        String chatId = doc.getId();
        String postId = doc.getString("postId");
        String postTitle = doc.getString("postTitle");
        String lastMessage = doc.getString("lastMessage");
        com.google.firebase.Timestamp lastTimestamp = doc.getTimestamp("lastTimestamp");

        // Determine other user
        @SuppressWarnings("unchecked")
        List<String> participants = (List<String>) doc.get("participants");
        String otherUserId = "";
        if (participants != null) {
            for (String uid : participants) {
                if (!uid.equals(currentUserId)) {
                    otherUserId = uid;
                    break;
                }
            }
        }

        // Set data
        holder.tvChatListLastMsg.setText(lastMessage != null ? lastMessage : "");
        holder.tvChatListPostTitle.setText(postTitle != null ? "Về: " + postTitle : "");

        if (lastTimestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            holder.tvChatListTime.setText(sdf.format(lastTimestamp.toDate()));
        }

        // Load other user's name
        if (!otherUserId.isEmpty()) {
            String finalOtherUserId = otherUserId;
            FirebaseFirestore.getInstance().collection("users").document(otherUserId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String name = userDoc.getString("fullName");
                            holder.tvChatListName.setText(name != null ? name : "User");

                            String photoUrl = userDoc.getString("photoUrl");
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                Glide.with(context)
                                        .load(photoUrl)
                                        .transform(new CircleCrop())
                                        .placeholder(R.drawable.ic_person)
                                        .error(R.drawable.ic_person)
                                        .into(holder.ivChatListAvatar);
                            } else {
                                holder.ivChatListAvatar.setImageResource(R.drawable.ic_person);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        holder.ivChatListAvatar.setImageResource(R.drawable.ic_person);
                    });

            // Count unread messages
            FirebaseFirestore.getInstance()
                    .collection("chats").document(chatId)
                    .collection("messages")
                    .whereEqualTo("read", false)
                    .whereNotEqualTo("senderId", currentUserId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        int unreadCount = querySnapshot.size();
                        if (unreadCount > 0) {
                            holder.tvUnreadBadge.setVisibility(android.view.View.VISIBLE);
                            holder.tvUnreadBadge.setText(String.valueOf(unreadCount));
                            // Make last message bold if unread
                            holder.tvChatListLastMsg.setTypeface(null, android.graphics.Typeface.BOLD);
                            holder.tvChatListLastMsg.setTextColor(context.getResources().getColor(R.color.on_surface, null));
                        } else {
                            holder.tvUnreadBadge.setVisibility(android.view.View.GONE);
                            holder.tvChatListLastMsg.setTypeface(null, android.graphics.Typeface.NORMAL);
                            holder.tvChatListLastMsg.setTextColor(context.getResources().getColor(R.color.on_surface_variant, null));
                        }
                    })
                    .addOnFailureListener(e -> {
                        holder.tvUnreadBadge.setVisibility(android.view.View.GONE);
                    });

            // Click to open chat
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("chatId", chatId);
                intent.putExtra("otherUserId", finalOtherUserId);
                intent.putExtra("postId", postId != null ? postId : "");
                intent.putExtra("postTitle", postTitle != null ? postTitle : "");
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return chatDocs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivChatListAvatar;
        TextView tvChatListName, tvChatListTime, tvChatListLastMsg, tvChatListPostTitle, tvUnreadBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivChatListAvatar = itemView.findViewById(R.id.ivChatListAvatar);
            tvChatListName = itemView.findViewById(R.id.tvChatListName);
            tvChatListTime = itemView.findViewById(R.id.tvChatListTime);
            tvChatListLastMsg = itemView.findViewById(R.id.tvChatListLastMsg);
            tvChatListPostTitle = itemView.findViewById(R.id.tvChatListPostTitle);
            tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
        }
    }
}
