package hcmute.edu.vn.findora.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

import hcmute.edu.vn.findora.R;
import hcmute.edu.vn.findora.model.User;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private Context context;
    private List<User> userList;
    private String sortType = "points"; // "points", "rating", "totalReturned"

    public LeaderboardAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
    }
    
    public void setSortType(String sortType) {
        this.sortType = sortType;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);
        holder.txtRank.setText(String.valueOf(position + 4)); // Starts from Rank 4
        holder.txtName.setText(user.getFullName());
        
        // Debug log
        android.util.Log.d("LeaderboardAdapter", 
            "User: " + user.getFullName() + 
            " | sortType: " + sortType +
            " | points: " + user.getPoints() +
            " | rating: " + user.getRating() +
            " | totalReturned: " + user.getTotalReturned());
        
        // Display value based on sort type
        if ("points".equals(sortType)) {
            holder.txtPoints.setText(user.getPoints() + " FP");
        } else if ("rating".equals(sortType)) {
            double rating = user.getRating() != null ? user.getRating() : 0.0;
            holder.txtPoints.setText(String.format("%.1f ⭐", rating));
        } else if ("totalReturned".equals(sortType)) {
            Integer totalReturned = user.getTotalReturned();
            int value = (totalReturned != null) ? totalReturned : 0;
            holder.txtPoints.setText(value + " món");
        }

        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            com.bumptech.glide.Glide.with(context)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .placeholder(R.drawable.bg_avatar_circle)
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.bg_avatar_circle);
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtRank, txtName, txtPoints;
        ShapeableImageView imgAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtRank = itemView.findViewById(R.id.txtRank);
            txtName = itemView.findViewById(R.id.txtName);
            txtPoints = itemView.findViewById(R.id.txtPoints);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
        }
    }
}
