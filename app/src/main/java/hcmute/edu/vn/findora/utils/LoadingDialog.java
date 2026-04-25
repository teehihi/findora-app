package hcmute.edu.vn.findora.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import hcmute.edu.vn.findora.R;

/**
 * Reusable loading dialog - dùng chung cho tất cả các tác vụ async.
 */
public class LoadingDialog {

    private Dialog dialog;
    private TextView tvMessage;

    public LoadingDialog(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);
        tvMessage = view.findViewById(R.id.tvLoadingMsg);

        dialog = new Dialog(context);
        dialog.setContentView(view);
        dialog.setCancelable(false);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    public void show(String message) {
        if (tvMessage != null) tvMessage.setText(message);
        if (!dialog.isShowing()) dialog.show();
    }

    public void show() {
        show("Đang xử lý...");
    }

    public void dismiss() {
        if (dialog.isShowing()) dialog.dismiss();
    }

    public boolean isShowing() {
        return dialog.isShowing();
    }
}
