package com.github.jthuraisamy.mastertap.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.github.jthuraisamy.mastertap.Helper;
import com.github.jthuraisamy.mastertap.MainActivity;
import com.github.jthuraisamy.mastertap.R;
import com.github.jthuraisamy.mastertap.models.Card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImportCardsDialog extends DialogFragment {
    public static final String TAG = ImportCardsDialog.class.getSimpleName();
    private static List<Card> cards;
    private List<Card> selectedCards;

    public ImportCardsDialog() {}

    public static ImportCardsDialog create(List<Card> cards) {
        ImportCardsDialog.cards = cards;

        return new ImportCardsDialog();
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final MainActivity ctx = (MainActivity) getActivity();
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ctx);

        // Populate selection items.
        CharSequence[] cardChoiceItems = new CharSequence[cards.size()];
        for (int i = 0; i < cards.size(); i++) {
            cardChoiceItems[i] = Helper.prettyPan(cards.get(i).getPan());
        }

        // Select all cards by default.
        boolean[] checkedItems = new boolean[cards.size()];
        Arrays.fill(checkedItems, true);
        selectedCards = new ArrayList<>();
        selectedCards.addAll(cards);

        // Set title bar.
        alertDialog.setTitle(R.string.import_cards_title);

        // Set multi-choice items.
        alertDialog.setMultiChoiceItems(cardChoiceItems, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i, boolean isSelected) {
                if (isSelected) {
                    selectedCards.add(cards.get(i));
                } else {
                    selectedCards.remove(cards.get(i));
                }

                for (Card card : selectedCards) {
                    Log.i(TAG, "Selected: " + Helper.prettyPan(card.getPan()));
                }
            }
        });

        alertDialog.setNegativeButton(R.string.cancel, null);

        alertDialog.setPositiveButton(R.string.import_cards, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.cardDao.importCards(selectedCards);
                ctx.refreshViewPager();
                ctx.toastMessage(getString(R.string.import_success));
            }
        });

        return alertDialog.create();
    }
}
