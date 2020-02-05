package com.github.jthuraisamy.mastertap.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.github.jthuraisamy.mastertap.MainActivity;
import com.github.jthuraisamy.mastertap.R;

public class RegisterPasswordDialog extends DialogFragment {
    public static final String TAG = RegisterPasswordDialog.class.getSimpleName();

    public RegisterPasswordDialog() {}

    public static RegisterPasswordDialog create() {
        return new RegisterPasswordDialog();
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final MainActivity ctx = (MainActivity) getActivity();
        AlertDialog.Builder alertDialog = new androidx.appcompat.app.AlertDialog.Builder(ctx);

        // Set title bar.
        alertDialog.setTitle(R.string.register_password_title);

        // Set view.
        LayoutInflater inflater = LayoutInflater.from(ctx);
        final View view = inflater.inflate(R.layout.register_password_dialog, null);
        alertDialog.setView(view);

        // Set OnClickListener for negative button.
        alertDialog.setNegativeButton(R.string.cancel, null);

        // Set OnClickListener for positive button as null, then define it in onResume.
        alertDialog.setPositiveButton(R.string.save, null);

        return alertDialog.create();
    }

    @Override
    public void onResume() {
        super.onResume();

        final androidx.appcompat.app.AlertDialog alertDialog = (androidx.appcompat.app.AlertDialog) getDialog();
        Button saveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View onClick) {
                final MainActivity ctx = (MainActivity) getActivity();
                EditText newPasswordInput = (EditText) alertDialog.findViewById(R.id.newPasswordInput);
                EditText newPasswordConfirmInput = (EditText) alertDialog.findViewById(R.id.newPasswordConfirmInput);

                String newPassword = newPasswordInput.getText().toString();
                String newPasswordConfirm = newPasswordConfirmInput.getText().toString();

                if (!newPassword.equals(newPasswordConfirm)) {
                    ctx.vibrator.vibrate(new long[] {0, 125, 125, 125}, -1);
                    ctx.toastMessage(ctx.getString(R.string.unmatched_passwords));
                } else if (newPassword.isEmpty()) {
                    ctx.vibrator.vibrate(new long[] {0, 125, 125, 125}, -1);
                    ctx.toastMessage(ctx.getString(R.string.blank_password));
                } else {
                    MainActivity.cardDao.setNewKey(newPassword);
                    MainActivity.isDatabaseAuthenticated = true;
                    ctx.toastMessage(ctx.getString(R.string.authenticated));
                    dismiss();
                }
            }
        });
    }
}
