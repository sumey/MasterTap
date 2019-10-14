package com.github.jthuraisamy.mastertap.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.github.jthuraisamy.mastertap.MainActivity;
import com.github.jthuraisamy.mastertap.R;
import com.github.jthuraisamy.mastertap.models.Card;

public class DeleteCardDialog extends DialogFragment {
    public static final String TAG = DeleteCardDialog.class.getSimpleName();
    private static Card card;

    public DeleteCardDialog() {}

    public static DeleteCardDialog create(Card card) {
        DeleteCardDialog.card = card;

        return new DeleteCardDialog();
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final MainActivity ctx = (MainActivity) getActivity();
        AlertDialog.Builder alertDialog = new androidx.appcompat.app.AlertDialog.Builder(ctx);

        alertDialog.setMessage(String.format(getString(R.string.confirm_delete), card.getLabel()));
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.cardDao.deleteCard(card);
                ctx.refreshViewPager();
            }
        });

        return alertDialog.create();
    }
}
