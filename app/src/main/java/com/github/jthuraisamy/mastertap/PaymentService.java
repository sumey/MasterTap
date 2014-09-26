package com.github.jthuraisamy.mastertap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.github.jthuraisamy.mastertap.models.Card;

import java.util.Arrays;

public class PaymentService extends HostApduService implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String TAG = "MasterTapLog-" + PaymentService.class.getSimpleName();

    private Card card;
    private String inboundApduDescription;
    private boolean transactionInProgress;

    @Override
    public void onCreate() {
        super.onCreate();

        // Register the OnSharedPreferenceChangeListener.
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        settings.registerOnSharedPreferenceChangeListener(this);

        Log.i(TAG, "PaymentService initialized.");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences settings, String key) {
        if (key.equals("paymentCardIndex")) {
            int paymentCardIndex = settings.getInt(key, 0);
            card = MainActivity.cards.get(paymentCardIndex);
            Log.i(TAG, "Payment card changed to: " + card.getPan());
        }
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        // Return error response if no card is selected.
        if (card.getPan() == null) return ApduConstants.UNKNOWN_ERROR_RESPONSE;

        byte[] responseApdu;

        if (Arrays.equals(commandApdu, ApduConstants.SELECT_PPSE)) {
            transactionInProgress = true;
            inboundApduDescription = "Selecting PPSE…";
            responseApdu = Helper.hexToByte(card.getPaymentDirectory());
        } else if (Arrays.equals(commandApdu, ApduConstants.SELECT_AID)) {
            inboundApduDescription = "Selecting AID…";
            responseApdu = Helper.hexToByte(card.getAidFci());
        } else if (Arrays.equals(commandApdu, ApduConstants.READ_MAGSTRIPE_RECORDS)) {
            inboundApduDescription = "Reading MagStripe records…";
            responseApdu = Helper.hexToByte(card.getMagStripeData());
        } else if (isGpoCommand(commandApdu)) {
            inboundApduDescription = "Getting processing options…";
            responseApdu = ApduConstants.GET_PROCESSING_OPTIONS_RESPONSE;
        } else if (isCccCommand(commandApdu)) {
            inboundApduDescription = "Computing cryptographic checksum…";
            int unpredictableNumber = getUN(commandApdu);
            boolean hasUN = card.hasUN(unpredictableNumber);
            boolean isAttemptedUN = card.getAttemptedUNs().contains(unpredictableNumber);
            Log.i(TAG, "UN: " + String.valueOf(unpredictableNumber));

            // Return CVC3 values if there is an unattempted response for the given UN.
            if (hasUN && !isAttemptedUN) {
                responseApdu = Helper.hexToByte(card.getCvc3Map().get(unpredictableNumber));
                MainActivity.cardDao.attemptUN(card, unpredictableNumber);
            } else {
                responseApdu = ApduConstants.UNKNOWN_ERROR_RESPONSE;
            }
        } else {
            transactionInProgress = false;
            inboundApduDescription = "Received Unknown APDU";
            responseApdu = ApduConstants.UNKNOWN_ERROR_RESPONSE;
        }

        Log.i(TAG, "ID: " + inboundApduDescription);
        Log.i(TAG, "Rx: " + Helper.byteToHex(commandApdu));
        Log.i(TAG, "Tx: " + Helper.byteToHex(responseApdu));
        sendApduBroadcast();
        return responseApdu;
    }

    /**
     * Return true if the given command APDU is a "Get Processing Options" command.
     *
     * @param commandApdu byte[]
     * @return boolean
     */
    private boolean isGpoCommand(byte[] commandApdu) {
        return (
            commandApdu.length > 4                                      &&
            commandApdu[0] == ApduConstants.GET_PROCESSING_OPTIONS[0]   &&
            commandApdu[1] == ApduConstants.GET_PROCESSING_OPTIONS[1]   &&
            commandApdu[2] == ApduConstants.GET_PROCESSING_OPTIONS[2]   &&
            commandApdu[3] == ApduConstants.GET_PROCESSING_OPTIONS[3]
        );
    }

    /**
     * Return true if the given command APDU is a "Compute Cryptographic Checksum" command.
     *
     * @param commandApdu byte[]
     * @return boolean
     */
    private boolean isCccCommand(byte[] commandApdu) {
        return (
            commandApdu.length > 4                                              &&
            commandApdu[0] == ApduConstants.COMPUTE_CRYPTOGRAPHIC_CHECKSUM[0]   &&
            commandApdu[1] == ApduConstants.COMPUTE_CRYPTOGRAPHIC_CHECKSUM[1]   &&
            commandApdu[2] == ApduConstants.COMPUTE_CRYPTOGRAPHIC_CHECKSUM[2]   &&
            commandApdu[3] == ApduConstants.COMPUTE_CRYPTOGRAPHIC_CHECKSUM[3]
        );
    }

    /**
     * Send an Intent to the MainActivity with transaction details.
     */
    private void sendApduBroadcast() {
        Intent intent = new Intent("apduProcessing");
        intent.putExtra("transactionInProgress", transactionInProgress);
        intent.putExtra("inboundApduDescription", inboundApduDescription);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Return the unpredictable number from the given "Compute Cryptographic Checksum" command.
     *
     * @param commandApdu byte[]
     * @return int
     */
    private int getUN(byte[] commandApdu) {
        byte[] commandTag = new byte[] {(byte) 0x80, 0x2A, (byte) 0x8E, (byte) 0x80};
        byte[] bcdArray = TLVParser.readTlv(commandApdu, commandTag);
        return Helper.bcdArrayToInt(bcdArray);
    }

    @Override
    public void onDeactivated(int reason) {}
}
