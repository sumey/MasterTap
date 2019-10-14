package com.github.jthuraisamy.mastertap.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.github.jthuraisamy.mastertap.MainActivity;
import com.github.jthuraisamy.mastertap.R;

import net.sqlcipher.database.SQLiteException;

public class ChangePasswordDialog extends DialogFragment {
    public static final String TAG = ChangePasswordDialog.class.getSimpleName();

    public ChangePasswordDialog() {}

    public static ChangePasswordDialog create() {
        return new ChangePasswordDialog();
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final MainActivity ctx = (MainActivity) getActivity();
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ctx);

        // Set title bar.
        alertDialog.setTitle(R.string.change_password_title);

        // Set view.
        LayoutInflater inflater = LayoutInflater.from(ctx);
        final View view = inflater.inflate(R.layout.change_password_dialog, null);
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

        final AlertDialog alertDialog = (AlertDialog) getDialog();

        Button saveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View onClick) {
                final MainActivity ctx = (MainActivity) getActivity();
                EditText passwordInput = (EditText) alertDialog.findViewById(R.id.passwordInput);
                EditText newPasswordInput = (EditText) alertDialog.findViewById(R.id.newPasswordInput);
                EditText newPasswordConfirmInput = (EditText) alertDialog.findViewById(R.id.newPasswordConfirmInput);

                String password = passwordInput.getText().toString();
                String newPassword = newPasswordInput.getText().toString();
                String newPasswordConfirm = newPasswordConfirmInput.getText().toString();

                // Validate current password.
                try {
                    MainActivity.cardDao.setKey(password);
                } catch (SQLiteException e) {
                    ctx.vibrator.vibrate(new long[] {0, 125, 125, 125}, -1);
                    ctx.toastMessage(ctx.getString(R.string.invalid_password));
                    passwordInput.setText("");
                    return;
                }

                // Validate new password.
                if (!newPassword.equals(newPasswordConfirm)) {
                    ctx.vibrator.vibrate(new long[] {0, 125, 125, 125}, -1);
                    ctx.toastMessage(ctx.getString(R.string.unmatched_passwords));
                } else if (newPassword.isEmpty()) {
                    ctx.vibrator.vibrate(new long[] {0, 125, 125, 125}, -1);
                    ctx.toastMessage(ctx.getString(R.string.blank_password));
                } else {
                    MainActivity.cardDao.setNewKey(newPassword);
                    ctx.toastMessage(ctx.getString(R.string.password_changed));
                    dismiss();
                }
            }
        });
    }
}
