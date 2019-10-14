package com.github.jthuraisamy.mastertap.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.github.jthuraisamy.mastertap.MainActivity;
import com.github.jthuraisamy.mastertap.R;

import net.sqlcipher.database.SQLiteException;

public class EnterPasswordDialog extends DialogFragment {
    public static final String TAG = EnterPasswordDialog.class.getSimpleName();

    public EnterPasswordDialog() {}

    public static EnterPasswordDialog create() {
        return new EnterPasswordDialog();
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final MainActivity ctx = (MainActivity) getActivity();
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ctx);

        // Set title bar.
        alertDialog.setTitle(R.string.enter_password_title);

        // Set view.
        LayoutInflater inflater = LayoutInflater.from(ctx);
        final View view = inflater.inflate(R.layout.enter_password_dialog, null);
        alertDialog.setView(view);

        // Set OnClickListener for negative button.
        alertDialog.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ctx.finish();
            }
        });

        // Set OnClickListener for positive button as null, then define it in onResume.
        alertDialog.setPositiveButton(R.string.authenticate, null);

        return alertDialog.create();
    }

    @Override
    public void onResume() {
        super.onResume();

        final AlertDialog alertDialog = (AlertDialog) getDialog();

        // Set OnCancelListener.
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                final MainActivity ctx = (MainActivity) getActivity();
                ctx.finish();
            }
        });

        // Set OnEditorActionListener for EditText.
        EditText passwordInput = (EditText) alertDialog.findViewById(R.id.passwordInput);
        passwordInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submitPassword(alertDialog);
                    return true;
                } else {
                    return false;
                }
            }
        });

        // Set OnClickListener for save button.
        Button saveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View onClick) {
                submitPassword(alertDialog);
            }
        });
    }

    private void submitPassword(AlertDialog alertDialog) {
        final MainActivity ctx = (MainActivity) getActivity();
        EditText passwordInput = (EditText) alertDialog.findViewById(R.id.passwordInput);
        String password = passwordInput.getText().toString();

        try {
            MainActivity.cardDao.setKey(password);

            // An SQLiteException wasn't raised at this point, so the db is authenticated.
            MainActivity.isDatabaseAuthenticated = true;

            // Refresh view pager with cards.
            ctx.refreshViewPager();

            // Select shown payment card.
            ctx.selectShownCardForPayment();

            // Dismiss dialog.
            ctx.toastMessage(ctx.getString(R.string.authenticated));
            dismiss();
        } catch (SQLiteException e) {
            ctx.vibrator.vibrate(new long[] {0, 125, 125, 125}, -1);
            ctx.toastMessage(ctx.getString(R.string.invalid_password));
            passwordInput.setText("");
        }

    }
}
