package com.github.jthuraisamy.mastertap.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.github.jthuraisamy.mastertap.MainActivity;
import com.github.jthuraisamy.mastertap.R;

public class NfcAdapterAbsentDialog extends DialogFragment {
    public static final String TAG = NfcAdapterAbsentDialog.class.getSimpleName();

    public NfcAdapterAbsentDialog() {}

    public static NfcAdapterAbsentDialog create() {
        return new NfcAdapterAbsentDialog();
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final MainActivity ctx = (MainActivity) getActivity();
        androidx.appcompat.app.AlertDialog.Builder alertDialog = new AlertDialog.Builder(ctx);

        alertDialog.setMessage(getText(R.string.no_nfc));

        // Set OnCancelListener.
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                ctx.finish();
            }
        });

        // Set OnClickListener for neutral button.
        alertDialog.setNeutralButton(R.string.exit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ctx.finish();
            }
        });

        return alertDialog.create();
    }
}
