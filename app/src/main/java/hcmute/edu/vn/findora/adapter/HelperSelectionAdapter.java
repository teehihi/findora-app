package hcmute.edu.vn.findora.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

import hcmute.edu.vn.findora.R;
import hcmute.edu.vn.findora.model.User;

public class HelperSelectionAdapter extends RecyclerView.Adapter<HelperSelectionAdapter.ViewHolder> {

    public interface OnHelperSelectedListener {
        void onHelperSelected(User user);
    }

    private final List<User> helpers;
    private int selectedPosition = -1;
    private OnHelperSelectedListener listener;

    public HelperSelectionAdapter(List<User> helpers, OnHelperSelectedListener listener) {
        this.helpers = helpers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_helper_select, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = helpers.get(position);
        boolean isSelected = (position == selectedPosition);

        holder.tvName.setText(user.getFullName());
        holder.ivCheckmark.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        // Update card styling
        if (isSelected) {
            holder.cardHelper.setStrokeColor(holder.itemView.getContext().getResources().getColor(R.color.primary_green));
            holder.cardHelper.setCardBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.green_50));
        } else {
            holder.cardHelper.setStrokeColor(holder.itemView.getContext().getResources().getColor(R.color.gray_200));
            holder.cardHelper.setCardBackgroundColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
        }

        // Load avatar
        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_profile_placeholder);
        }

        holder.cardHelper.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            
            if (previousPosition != -1) {
                notifyItemChanged(previousPosition);
            }
            notifyItemChanged(selectedPosition);
            
            if (listener != null) {
                listener.onHelperSelected(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return helpers.size();
    }

    public User getSelectedHelper() {
        if (selectedPosition >= 0 && selectedPosition < helpers.size()) {
            return helpers.get(selectedPosition);
        }
        return null;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardHelper;
        ImageView ivAvatar;
        TextView tvName;
        ImageView ivCheckmark;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardHelper = itemView.findViewById(R.id.cardHelper);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            ivCheckmark = itemView.findViewById(R.id.ivCheckmark);
        }
    }
}
