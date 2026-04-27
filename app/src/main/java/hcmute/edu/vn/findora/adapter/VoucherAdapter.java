package hcmute.edu.vn.findora.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

import hcmute.edu.vn.findora.R;

/**
 * Adapter cho danh sách Voucher trong Chợ Voucher.
 * Xử lý logic đổi màu nút bấm dựa trên số điểm người dùng đang có.
 */
public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder> {

    // Giả sử có một Model Voucher đơn giản
    public static class Voucher {
        public String id;
        public String name;
        public int pointsRequired;
        public String brandLogo;
        public String brandName;
        public String voucherCode;
        public int imageResId;

        public Voucher(String id, String name, int pointsRequired, String brandLogo, String brandName, String voucherCode, int imageResId) {
            this.id = id;
            this.name = name;
            this.pointsRequired = pointsRequired;
            this.brandLogo = brandLogo;
            this.brandName = brandName;
            this.voucherCode = voucherCode;
            this.imageResId = imageResId;
        }
    }

    private Context context;
    private List<Voucher> voucherList;
    private long userCurrentPoints;

    public VoucherAdapter(Context context, List<Voucher> voucherList, long userCurrentPoints) {
        this.context = context;
        this.voucherList = voucherList;
        this.userCurrentPoints = userCurrentPoints;
    }

    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_voucher, parent, false);
        return new VoucherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        Voucher voucher = voucherList.get(position);

        holder.txtVoucherName.setText(voucher.name);
        holder.txtVoucherPoints.setText(voucher.pointsRequired + " FP"); 
        holder.txtBrandName.setText(voucher.brandName);

        // Set ảnh voucher
        if (voucher.imageResId != 0) {
            holder.imgVoucher.setImageResource(voucher.imageResId);
        }

        // Metadata với ngày hết hạn 21/5
        holder.txtMetadata.setText("Còn " + (10 + position * 5) + " • Hết hạn 21/5");

        // --- Logic: Thay đổi màu nút bấm dựa trên số điểm ---
        if (userCurrentPoints >= voucher.pointsRequired) {
            // Đủ điểm: Nút màu xanh (Primary Green)
            holder.btnRedeem.setEnabled(true);
            holder.btnRedeem.setText("Thu thập");
            holder.btnRedeem.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.primary)));
            holder.btnRedeem.setTextColor(ContextCompat.getColor(context, R.color.white));
            
            // Xử lý sự kiện click để hiển thị mã voucher
            holder.btnRedeem.setOnClickListener(v -> {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
                builder.setTitle("Đổi voucher thành công!");
                builder.setMessage("Mã voucher của bạn:\n\n" + voucher.voucherCode + "\n\nVui lòng sử dụng mã này khi thanh toán.");
                builder.setPositiveButton("OK", null);
                builder.show();
            });
        } else {
            // Không đủ điểm: Nút màu xám nhạt (Locked)
            holder.btnRedeem.setEnabled(false);
            holder.btnRedeem.setText("Chưa đủ");
            holder.btnRedeem.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.surface_container_high))); 
            holder.btnRedeem.setTextColor(ContextCompat.getColor(context, R.color.outline)); 
            holder.btnRedeem.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return voucherList.size();
    }

    public static class VoucherViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imgVoucher;
        TextView txtVoucherName, txtVoucherPoints, txtBrandName, txtMetadata;
        MaterialButton btnRedeem;

        public VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            imgVoucher = itemView.findViewById(R.id.imgVoucher);
            txtVoucherName = itemView.findViewById(R.id.txtVoucherName);
            txtVoucherPoints = itemView.findViewById(R.id.txtVoucherPoints);
            txtBrandName = itemView.findViewById(R.id.txtBrandName);
            txtMetadata = itemView.findViewById(R.id.txtMetadata);
            btnRedeem = itemView.findViewById(R.id.btnRedeem);
        }
    }
}
