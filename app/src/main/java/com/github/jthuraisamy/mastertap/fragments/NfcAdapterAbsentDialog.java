package com.github.jthuraisamy.mastertap.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

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
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ctx);

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
