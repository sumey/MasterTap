package com.github.jthuraisamy.mastertap.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.github.jthuraisamy.mastertap.MainActivity;
import com.github.jthuraisamy.mastertap.R;

public class NfcAdapterDisabledDialog extends DialogFragment {
    public static final String TAG = NfcAdapterDisabledDialog.class.getSimpleName();

    public NfcAdapterDisabledDialog() {}

    public static NfcAdapterDisabledDialog create() {
        return new NfcAdapterDisabledDialog();
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final MainActivity ctx = (MainActivity) getActivity();
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ctx);

        alertDialog.setMessage(getText(R.string.nfc_disabled));

        // Set OnCancelListener.
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                ctx.finish();
            }
        });

        // Set OnClickListener for negative button.
        alertDialog.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ctx.finish();
            }
        });

        // Set OnClickListener for positive button.
        alertDialog.setPositiveButton(R.string.enable, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            }
        });

        return alertDialog.create();
    }
}
