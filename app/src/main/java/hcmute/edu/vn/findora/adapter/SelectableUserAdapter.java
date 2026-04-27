package hcmute.edu.vn.findora.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import hcmute.edu.vn.findora.R;

public class SelectableUserAdapter extends RecyclerView.Adapter<SelectableUserAdapter.ViewHolder> {

    public static class UserItem {
        public String userId;
        public String name;
        public String avatarUrl;

        public UserItem(String userId, String name, String avatarUrl) {
            this.userId = userId;
            this.name = name;
            this.avatarUrl = avatarUrl;
        }
    }

    public interface OnUserSelectedListener {
        void onUserSelected(UserItem user);
    }

    private final Context context;
    private final List<UserItem> users;
    private int selectedPosition = -1;
    private OnUserSelectedListener listener;

    public SelectableUserAdapter(Context context, List<UserItem> users) {
        this.context = context;
        this.users = users;
    }

    public void setOnUserSelectedListener(OnUserSelectedListener listener) {
        this.listener = listener;
    }

    public UserItem getSelectedUser() {
        if (selectedPosition >= 0 && selectedPosition < users.size()) {
            return users.get(selectedPosition);
        }
        return null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_selectable, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserItem user = users.get(position);
        boolean isSelected = (position == selectedPosition);

        holder.tvName.setText(user.name);
        holder.radioSelect.setChecked(isSelected);

        // Background: selected vs default
        if (isSelected) {
            holder.rootLayout.setBackgroundResource(R.drawable.bg_user_selectable_selected);
        } else {
            holder.rootLayout.setBackgroundColor(android.graphics.Color.WHITE);
        }

        // Load avatar
        if (user.avatarUrl != null && !user.avatarUrl.isEmpty()) {
            Glide.with(context)
                    .load(user.avatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.avatar_placeholder)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.avatar_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
            if (listener != null) listener.onUserSelected(user);
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout rootLayout;
        ImageView ivAvatar;
        TextView tvName, tvSubtitle;
        RadioButton radioSelect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            rootLayout  = itemView.findViewById(R.id.rootLayout);
            ivAvatar    = itemView.findViewById(R.id.ivAvatar);
            tvName      = itemView.findViewById(R.id.tvName);
            tvSubtitle  = itemView.findViewById(R.id.tvSubtitle);
            radioSelect = itemView.findViewById(R.id.radioSelect);
        }
    }
}
