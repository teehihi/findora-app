package hcmute.edu.vn.findora;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class ConfirmReceivedDialog extends BottomSheetDialogFragment {

    public interface OnConfirmListener {
        void onSomeoneReturned();
        void onFoundMyself();
        void onClosePost();
    }

    private OnConfirmListener listener;

    public static ConfirmReceivedDialog newInstance() {
        return new ConfirmReceivedDialog();
    }

    public void setOnConfirmListener(OnConfirmListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_confirm_received, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton btnSomeoneReturned = view.findViewById(R.id.btnSomeoneReturned);
        MaterialButton btnFoundMyself     = view.findViewById(R.id.btnFoundMyself);
        MaterialButton btnClosePost       = view.findViewById(R.id.btnClosePost);
        MaterialButton btnCancel          = view.findViewById(R.id.btnCancel);

        btnSomeoneReturned.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onSomeoneReturned();
        });

        btnFoundMyself.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onFoundMyself();
        });

        btnClosePost.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onClosePost();
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }
}
