package hcmute.edu.vn.findora.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.findora.R;
import hcmute.edu.vn.findora.model.Transaction;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private Context context;
    private List<Transaction> transactions;
    private SimpleDateFormat dateFormat;

    public TransactionAdapter(Context context, List<Transaction> transactions) {
        this.context = context;
        this.transactions = transactions;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);

        // Set title
        holder.tvTitle.setText(transaction.getTitle());

        // Set date time
        if (transaction.getTimestamp() != null) {
            Date date = transaction.getTimestamp().toDate();
            holder.tvDateTime.setText(dateFormat.format(date));
        }

        // Set points and styling based on type
        int points = transaction.getPoints();
        String type = transaction.getType();

        if ("earn".equals(type)) {
            // Earning points - Green (#22C55E)
            holder.tvPoints.setText("+" + points);
            holder.tvPoints.setTextColor(android.graphics.Color.parseColor("#22C55E"));
            holder.ivIcon.setImageResource(R.drawable.ic_check_circle);
            holder.ivIcon.setColorFilter(android.graphics.Color.parseColor("#22C55E"));
            holder.iconContainer.setBackgroundResource(R.drawable.bg_circle_green_light);
        } else {
            // Spending points - Grey (#71717A) - Use bookmark icon for voucher redemption
            holder.tvPoints.setText("-" + points);
            holder.tvPoints.setTextColor(android.graphics.Color.parseColor("#71717A"));
            holder.ivIcon.setImageResource(R.drawable.ic_bookmark_feather);
            holder.ivIcon.setColorFilter(android.graphics.Color.parseColor("#71717A"));
            holder.iconContainer.setBackgroundResource(R.drawable.bg_circle_grey_light);
        }
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        FrameLayout iconContainer;
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvDateTime;
        TextView tvPoints;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconContainer = itemView.findViewById(R.id.iconContainer);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvPoints = itemView.findViewById(R.id.tvPoints);
        }
    }
}
