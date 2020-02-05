package com.github.jthuraisamy.mastertap.helpers;

import android.os.Environment;

import com.github.jthuraisamy.mastertap.Helper;
import com.github.jthuraisamy.mastertap.models.Card;

import java.util.ArrayList;
import java.util.List;

public class PortHelper {
    /**
     * Checks if external storage is available for read and write.
     *
     * @return boolean
     */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /**
     * Checks if external storage is available to at least read.
     *
     * @return boolean
     */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    /**
     * Given a Card[], return an ArrayList of the valid Cards.
     *
     * @param cards Card[]
     * @return validCards
     */
    public static List<Card> validateCards(Card[] cards) {
        List<Card> validCards = new ArrayList<>();

        for (Card card : cards) {
            if (validateCard(card))
                validCards.add(card);
        }

        return validCards;
    }

    /**
     * Perform tests on the given card then return whether it is valid or not.
     *
     * @param card Card
     * @return boolean
     */
    public static boolean validateCard(Card card) {
        try {
            Helper.prettyPan(card.getPan());
            Helper.prettyExpiryDate(card.getExpiryDate());
            Helper.hexToByte(card.getPaymentDirectory());
            Helper.hexToByte(card.getAidFci());
            Helper.hexToByte(card.getMagStripeData());
            for (String cvc3Response : card.getCvc3Map().values())
                Helper.hexToByte(cvc3Response);
        } catch (Exception e) {
            return false;
        }

        return true;
    }
}
