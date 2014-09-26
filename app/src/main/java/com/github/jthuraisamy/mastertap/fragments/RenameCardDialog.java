package com.github.jthuraisamy.mastertap.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.github.jthuraisamy.mastertap.MainActivity;
import com.github.jthuraisamy.mastertap.R;
import com.github.jthuraisamy.mastertap.models.Card;

public class RenameCardDialog extends DialogFragment {
    public static final String TAG = RenameCardDialog.class.getSimpleName();
    private static Card card;

    public RenameCardDialog() {}

    public static RenameCardDialog create(Card card) {
        RenameCardDialog.card = card;

        return new RenameCardDialog();
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final MainActivity ctx = (MainActivity) getActivity();
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ctx);

        // Set title bar.
        alertDialog.setIcon(R.drawable.ic_action_new_label);
        alertDialog.setTitle(R.string.prompt_label);

        // Set view.
        LayoutInflater inflater = LayoutInflater.from(ctx);
        final View view = inflater.inflate(R.layout.rename_card_dialog, null);
        final EditText labelInput = (EditText) view.findViewById(R.id.cardLabelInput);
        labelInput.setHint(card.getLabel());
        labelInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        alertDialog.setView(view);

        // Set OnCancelListener.
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                ctx.refreshViewPager(card.getPan());
            }
        });

        // Set OnClickListener for negative button.
        alertDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ctx.refreshViewPager(card.getPan());
            }
        });

        // Set OnClickListener for positive button.
        alertDialog.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String newLabel = labelInput.getText().toString();

                // Get default label if input is blank.
                if (newLabel.isEmpty())
                    newLabel = getString(R.string.default_label);

                // Set new label and commit to DB.
                card.setLabel(newLabel);
                MainActivity.cardDao.updateCard(card);

                // Update viewPager and scroll to the renamed card.
                ctx.refreshViewPager(card.getPan());
            }
        });

        return alertDialog.create();
    }
}
