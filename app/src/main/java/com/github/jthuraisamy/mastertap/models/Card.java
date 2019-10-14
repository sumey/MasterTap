package com.github.jthuraisamy.mastertap.models;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.jthuraisamy.mastertap.Helper;
import com.github.jthuraisamy.mastertap.R;
import com.github.jthuraisamy.mastertap.TLVParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Card {

    private int id;
    private String label;
    private String pan;
    private String expiryDate;
    private String paymentDirectory;
    private String aidFci;
    private String magStripeData = "";
    private Map<Integer, String> cvc3Map = new HashMap<>();
    private ArrayList<Integer> attemptedUNs = new ArrayList<>();

    public Card() {}

    public Card(Context ctx) {
        label = ctx.getResources().getString(R.string.default_label);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getPaymentDirectory() {
        return paymentDirectory;
    }

    public void setPaymentDirectory(String paymentDirectory) {
        this.paymentDirectory = paymentDirectory;
    }

    public String getAidFci() {
        return aidFci;
    }

    public void setAidFci(String aidFci) {
        this.aidFci = aidFci;
    }

    public String getMagStripeData() {
        return magStripeData;
    }

    public void setMagStripeData(String magStripeData) {
        this.magStripeData = magStripeData;
    }

    public Map<Integer, String> getCvc3Map() {
        return cvc3Map;
    }

    public void setCvc3Map(Map<Integer, String> cvc3Map) {
        this.cvc3Map = cvc3Map;
    }

    /**
     * Return whether this Card has a computed CVC3 value for the given unpredictable number.
     *
     * @param unpredictableNumber int
     * @return boolean
     */
    public boolean hasUN(int unpredictableNumber) {
        return cvc3Map.containsKey(unpredictableNumber);
    }

    public ArrayList<Integer> getAttemptedUNs() {
        return attemptedUNs;
    }

    public void setAttemptedUNs(ArrayList<Integer> attemptedUNs) {
        this.attemptedUNs = attemptedUNs;
    }

    /**
     * Return the total possible number of unpredictable numbers.
     *
     * @return int
     */
    public int getTotalUNs() {
        int unpredictableNumberDigits = getUNDigits();
        return (int) Math.pow(10, unpredictableNumberDigits);
    }

    /**
     * Return the maximum number of digits an unpredictable number can have.
     *
     * @return int
     */
    private int getUNDigits() {
        byte[] magStripeData = Helper.hexToByte(this.magStripeData);

        byte[] pUnAtcTrack1 = TLVParser.readTlv(magStripeData, new byte[]{(byte) 0x9F, 0x63});
        byte[] nAtcTrack1 = TLVParser.readTlv(magStripeData, new byte[]{(byte) 0x9F, 0x64});
        byte[] pUnAtcTrack2 = TLVParser.readTlv(magStripeData, new byte[]{(byte) 0x9F, 0x66});
        byte[] nAtcTrack2 = TLVParser.readTlv(magStripeData, new byte[]{(byte) 0x9F, 0x67});

        int kTrack1 = 0;
        int tTrack1 = nAtcTrack1[0];
        int kTrack2 = 0;
        int tTrack2 = nAtcTrack2[0];

        for (Byte b : pUnAtcTrack1) {
            int i = (int) b;
            if (i < 0) i += 256;

            kTrack1 += Integer.bitCount(i);
        }

        for (Byte b : pUnAtcTrack2) {
            int i = (int) b;
            if (i < 0) i += 256;

            kTrack2 += Integer.bitCount(i);
        }

        return Math.max(kTrack1 - tTrack1, kTrack2 - tTrack2);
    }

    @Override
    @NonNull
    public String toString() {
        return String.format(
            "Card [\n\t" +
                "id = %d\n\t" +
                "label = %s\n\t" +
                "pan = %s\n\t" +
                "expiry_date = %s\n\t" +
                "payment_directory = %s\n\t" +
                "aid_fci = %s\n\t" +
                "magstripe_data = %s\n" +
            "]",
            id, label, pan, expiryDate, paymentDirectory, aidFci, magStripeData
        );
    }
}
