package com.github.jthuraisamy.mastertap.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.github.jthuraisamy.mastertap.MainActivity;
import com.github.jthuraisamy.mastertap.R;

public class DisclaimerDialog extends DialogFragment {
    public static final String TAG = DisclaimerDialog.class.getSimpleName();

    public DisclaimerDialog() {}

    public static DisclaimerDialog create() {
        return new DisclaimerDialog();
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final MainActivity ctx = (MainActivity) getActivity();
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ctx);

        // Set title bar.
        alertDialog.setTitle(R.string.disclaimer_title);

        // Set view.
        LayoutInflater inflater = LayoutInflater.from(ctx);
        final View view = inflater.inflate(R.layout.disclaimer_dialog, null);
        final CheckBox disclaimerCheckBox = (CheckBox) view.findViewById(R.id.disclaimerCheckBox);
        alertDialog.setView(view);

        // Set OnClickListener for negative button.
        alertDialog.setNegativeButton(R.string.decline, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor settingsEditor = settings.edit();
                settingsEditor.putBoolean("isDisclaimerAccepted", false);
                settingsEditor.apply();

                ctx.finish();
            }
        });

        // Set OnClickListener for positive button.
        alertDialog.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor settingsEditor = settings.edit();

                // Commit settings.
                settingsEditor.putBoolean("isDisclaimerAccepted", true);
                if (disclaimerCheckBox.isChecked()) {
                    settingsEditor.putBoolean("showDisclaimer", true);
                } else {
                    settingsEditor.putBoolean("showDisclaimer", false);
                }
                settingsEditor.apply();

                // Continue initialization of MainActivity.
                ctx.initDatabase();
            }
        });

        return alertDialog.create();
    }

    @Override
    public void onResume() {
        super.onResume();

        final androidx.appcompat.app.AlertDialog alertDialog = (androidx.appcompat.app.AlertDialog) getDialog();

        // Set OnCancelListener.
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                final MainActivity ctx = (MainActivity) getActivity();
                ctx.finish();
            }
        });
    }
}
