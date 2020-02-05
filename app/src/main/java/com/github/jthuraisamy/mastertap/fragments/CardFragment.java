package com.github.jthuraisamy.mastertap.fragments;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.github.jthuraisamy.mastertap.Helper;
import com.github.jthuraisamy.mastertap.MainActivity;
import com.github.jthuraisamy.mastertap.R;
import com.github.jthuraisamy.mastertap.models.Card;

import java.util.ArrayList;

public class CardFragment extends Fragment {
    private String label;
    private String pan;
    private String expiryDate;
    private int totalUNs;
    private int calculatedUNs;
    private ArrayList<Integer> attemptedUNs;

    public static CardFragment create(int page) {
        Card card = MainActivity.cards.get(page);

        Bundle args = new Bundle();
        args.putString("label", card.getLabel());
        args.putString("pan", Helper.prettyPan(card.getPan()));
        args.putString("expiryDate", Helper.prettyExpiryDate(card.getExpiryDate()));
        args.putInt("totalUNs", card.getTotalUNs());
        args.putInt("calculatedUNs", card.getCvc3Map().size());
        args.putIntegerArrayList("attemptedUNs", card.getAttemptedUNs());

        CardFragment fragment = new CardFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve bundle contents.
        label = getArguments().getString("label");
        pan = getArguments().getString("pan");
        expiryDate = getArguments().getString("expiryDate");
        calculatedUNs = getArguments().getInt("calculatedUNs");
        attemptedUNs = getArguments().getIntegerArrayList("attemptedUNs");
        totalUNs = getArguments().getInt("totalUNs");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.card_fragment, container, false);

        TextView tvLabel = (TextView) view.findViewById(R.id.cardLabel);
        TextView tvPan = (TextView) view.findViewById(R.id.cardPan);
        TextView tvExpiryDate = (TextView) view.findViewById(R.id.cardExpiryDate);
        TextView tvTransactionsRemaining = (TextView) view.findViewById(R.id.transactionsRemaining);
        TextView tvCloningProgress = (TextView) view.findViewById(R.id.cloningProgressLabel);
        ProgressBar pbCloningProgress = (ProgressBar) view.findViewById(R.id.cloningProgressBar);

        // Render label, PAN, and expiry date on card image.
        tvLabel.setText(label + "\u0020");
        tvPan.setText(pan);
        tvExpiryDate.setText(expiryDate);

        // Set remaining transaction attempts.
        int remainingAttempts = calculatedUNs - attemptedUNs.size();
        tvTransactionsRemaining.setText(String.format(getString(R.string.attempts_remaining), remainingAttempts));

        // Set cloning progress.
        float cloningProgressPct = (calculatedUNs * 100.0f) / totalUNs;
        tvCloningProgress.setText(String.format(getString(R.string.cloning_progress), cloningProgressPct));
        pbCloningProgress.setProgress((int) cloningProgressPct);

        // Deterministically colour card image based on PAN.
//        ImageView cardImage = (ImageView) view.findViewById(R.id.card);
//        Drawable cardBackground = getResources().getDrawable(R.drawable.card_border);
//        long colorFilter = Long.parseLong("FF00" + pan.replace(" ", "").substring(12), 16) + 0x00005030;
//        cardBackground.setColorFilter((int) colorFilter, PorterDuff.Mode.LIGHTEN);
//        cardImage.setBackground(cardBackground);

        return view;
    }
}
