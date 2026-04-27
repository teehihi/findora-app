package hcmute.edu.vn.findora.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

import hcmute.edu.vn.findora.R;
import hcmute.edu.vn.findora.model.FinderUser;

public class FinderSelectionAdapter extends RecyclerView.Adapter<FinderSelectionAdapter.ViewHolder> {

    private List<FinderUser> finders;
    private int selectedPosition = -1;
    private OnFinderSelectedListener listener;

    public interface OnFinderSelectedListener {
        void onFinderSelected(FinderUser finder, int position);
    }

    public FinderSelectionAdapter(List<FinderUser> finders, OnFinderSelectedListener listener) {
        this.finders = finders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_finder_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FinderUser finder = finders.get(position);
        holder.tvName.setText(finder.getName());

        // Load avatar
        if (finder.getPhotoUrl() != null && !finder.getPhotoUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(finder.getPhotoUrl())
                    .circleCrop()
                    .placeholder(R.drawable.bg_avatar_circle)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.bg_avatar_circle);
        }

        // Highlight selected item
        boolean isSelected = position == selectedPosition;
        holder.ivCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.cardFinder.setStrokeColor(isSelected 
                ? holder.itemView.getContext().getColor(R.color.primary)
                : holder.itemView.getContext().getColor(R.color.border));
        holder.cardFinder.setStrokeWidth(isSelected ? 4 : 2);

        holder.cardFinder.setOnClickListener(v -> {
            int oldPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPosition);
            notifyItemChanged(selectedPosition);
            
            if (listener != null) {
                listener.onFinderSelected(finder, selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return finders.size();
    }

    public FinderUser getSelectedFinder() {
        if (selectedPosition >= 0 && selectedPosition < finders.size()) {
            return finders.get(selectedPosition);
        }
        return null;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardFinder;
        ImageView ivAvatar;
        TextView tvName;
        ImageView ivCheck;

        ViewHolder(View itemView) {
            super(itemView);
            cardFinder = itemView.findViewById(R.id.cardFinder);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            ivCheck = itemView.findViewById(R.id.ivCheck);
        }
    }
}
