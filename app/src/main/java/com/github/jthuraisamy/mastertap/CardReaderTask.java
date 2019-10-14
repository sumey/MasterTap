package com.github.jthuraisamy.mastertap;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.util.Log;

import com.github.jthuraisamy.mastertap.fragments.RenameCardDialog;
import com.github.jthuraisamy.mastertap.helpers.MastercardHelper;
import com.github.jthuraisamy.mastertap.helpers.VisaHelper;
import com.github.jthuraisamy.mastertap.models.Card;
import com.github.jthuraisamy.mastertap.models.CardRecord;
import com.google.common.primitives.Bytes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardReaderTask extends AsyncTask<Tag, Integer, String> {
    private static final String TAG = "MasterTapLog-" + CardReaderTask.class.getSimpleName();

    private final Context ctx;
    private final MainActivity mainActivity;
    private ProgressDialog progressDialog;
    private IsoDep tagCommunicator;

    private Card card;
    Map<Integer, String> cvc3Map = new HashMap<>();

    private int taskStatus = 0;
    private final int STATUS_INVALID_AID = 1;
    private final int STATUS_TAG_LOST = 2;

    public CardReaderTask(Context ctx) {
        this.ctx = ctx;
        this.mainActivity = (MainActivity) ctx;
        MainActivity.cardDao.open();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        // Show progress dialog.
        progressDialog = new ProgressDialog(ctx);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMessage(ctx.getText(R.string.card_reading_progress_message));
        progressDialog.setProgressNumberFormat(ctx.getText(R.string.card_read_progress_format).toString());
        progressDialog.show();
    }

    @Override
    protected String doInBackground(Tag... params) {
        Tag tag = params[0];

        tagCommunicator = IsoDep.get(tag);

        try {
            tagCommunicator.connect();
            tagCommunicator.setTimeout(5000);

            if (tagCommunicator.isConnected()) {
                readCard();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void readCard() {
        try {
            // SELECT PPSE "2PAY.SYS.DDF01".
            byte[] paymentDirectory = tagCommunicator.transceive(ApduConstants.SELECT_PPSE);

            // Get Application Identifier (AID) from response.
            byte[] aid = TLVParser.readTlv(paymentDirectory, new byte[]{0x4F});

            // Select AID.
            ByteArrayOutputStream selectAidCommand = new ByteArrayOutputStream();
            selectAidCommand.write(new byte[] {0x00, (byte) 0xA4, 0x04, 0x00, 0x07});
            selectAidCommand.write(aid);
            selectAidCommand.write(new byte[] {0x00});
            byte[] aidFci = tagCommunicator.transceive(selectAidCommand.toByteArray());
            Log.i(TAG, "AID = " + Helper.byteToHex(aid));
            Log.i(TAG, "PPSE = " + Helper.byteToHex(paymentDirectory));
            Log.i(TAG, "AIDFCI = " + Helper.byteToHex(aidFci));

            // Reject non-MasterCards.
            if (Arrays.equals(aid, ApduConstants.SUPPORTED_AID)) {
                readMasterCard(paymentDirectory, aidFci);
            } else {
                taskStatus = STATUS_INVALID_AID;
            }
        } catch (IOException e) {
            taskStatus = STATUS_TAG_LOST;
        }
    }

    private void readVisa(byte[] paymentDirectory, byte[] aidFci) {
        try {
            // Construct Get Processing Options (GPO) command.
            byte[] gpoCommand = VisaHelper.getGpoCommand(aidFci);
            Log.i(TAG, "GPO Command = " + Helper.byteToHex(gpoCommand));

            // Execute GPO command.
            byte[] gpoResponse = tagCommunicator.transceive(gpoCommand);
            Log.i(TAG, "GPO Response = " + Helper.byteToHex(gpoResponse));

            // Modify Application Interchange Profile (AIP) in GPO response to support MSD.
            if (gpoResponse[0] == (byte) 0x77) {
                int aipHeaderIndex = Bytes.indexOf(gpoResponse, new byte[]{(byte) 0x82, 0x02});
                gpoResponse[aipHeaderIndex + 3] |= 0x80;
            } else if (gpoResponse[0] == (byte) 0x80) {
                gpoResponse[3] |= 0x80;
            }
            Log.i(TAG, "Modified GPO Response = " + Helper.byteToHex(gpoResponse));

            // Retrieve records from Application File Locator (AFL) in GPO response.
            byte[] afl = new byte[] {};
            if (gpoResponse[0] == (byte) 0x77) {
                afl = TLVParser.readTlv(gpoResponse, new byte[]{(byte) 0x94});
            } else if (gpoResponse[0] == (byte) 0x80) {
                afl = Arrays.copyOfRange(gpoResponse, 4, gpoResponse.length - 2);
            }
            Log.i(TAG, "AFL = " + Helper.byteToHex(afl));
            List<CardRecord> records = VisaHelper.getRecordsFromAfl(tagCommunicator, afl);

        } catch (IOException e) {
            taskStatus = STATUS_TAG_LOST;
        }
    }

    private void readMasterCard(byte[] paymentDirectory, byte[] aidFci) {
        try {
            // Read Mag Stripe Application Data.
            byte[] magStripeData = tagCommunicator.transceive(ApduConstants.READ_MAGSTRIPE_RECORDS);
            byte[] track1Data = TLVParser.readTlv(magStripeData, new byte[]{0x56});
            String pan = new String(Arrays.copyOfRange(track1Data, 1, 17));
            String expiryDate = new String(Arrays.copyOfRange(track1Data, 45, 49));
            Log.i(TAG, Helper.byteToHex(magStripeData));
            Log.i(TAG, pan);

            // Check if card exists in the database.
            card = MainActivity.cardDao.getCard(pan);

            // If card does not exist, create it, then retrieve it so it has the correct ID.
            if (card == null) {
                card = new Card(ctx);
                card.setPan(pan);
                card.setExpiryDate(expiryDate);
                card.setPaymentDirectory(Helper.byteToHex(paymentDirectory));
                card.setAidFci(Helper.byteToHex(aidFci));
                card.setMagStripeData(Helper.byteToHex(magStripeData));

                MainActivity.cardDao.addCard(card);
                card = MainActivity.cardDao.getCard(pan);
            }
            Log.i(TAG, MainActivity.cardDao.getCard(pan).toString());

            // Determine the maximum digits for unpredictable numbers (n_un).
            cvc3Map = card.getCvc3Map();
            int totalUnpredictableNumbers = MastercardHelper.getTotalUNs(magStripeData);

            // Compute Cryptographic Checksums (ccc) into a map of new CVC3 values.
            int unpredictableNumber = 0;
            while (unpredictableNumber < totalUnpredictableNumbers) {
                if (cvc3Map.containsKey(unpredictableNumber)) {
                    // Update progress.
                    unpredictableNumber++;
                    publishProgress(unpredictableNumber, totalUnpredictableNumbers);
                    continue;
                }

                // Calculate BCD (binary coded decimal) 4-byte array for unpredictableNumber.
                byte[] bcdBytes = Helper.intToBcdArray(unpredictableNumber);

                // Get Processing Options, return AIP/AFL.
                byte[] processingOptions = tagCommunicator.transceive(ApduConstants.GET_PROCESSING_OPTIONS);
                Log.i(TAG, Helper.byteToHex(processingOptions));

                // Compute Cryptographic Checksum for unpredictableNumber, returning CVC3/ATC.
                ByteArrayOutputStream cccCommand = new ByteArrayOutputStream();
                cccCommand.write(new byte[]{(byte) 0x80, 0x2A, (byte) 0x8E, (byte) 0x80, 0x04});
                cccCommand.write(bcdBytes);
                cccCommand.write(new byte[]{0x00});
                byte[] cvc3Response = tagCommunicator.transceive(cccCommand.toByteArray());

                // Add CVC3 response to cvc3Map.
                cvc3Map.put(unpredictableNumber, Helper.byteToHex(cvc3Response));

                // Update progress.
                unpredictableNumber++;
                publishProgress(unpredictableNumber, totalUnpredictableNumbers);
            }
        } catch (IOException e) {
            taskStatus = STATUS_TAG_LOST;
        }
    }

    protected void onProgressUpdate(Integer... progress) {
        progressDialog.setMax(progress[1]);
        progressDialog.setProgress(progress[0]);
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        // Write CVC3 responses to database.
        MainActivity.cardDao.addCvc3Map(card, cvc3Map);

        // Dismiss progress dialog.
        if (progressDialog.isShowing()) progressDialog.dismiss();

        switch (taskStatus) {
            // If the device has lost contact with the card:
            case STATUS_TAG_LOST:
                mainActivity.toastMessage(ctx.getString(R.string.contact_lost));
                break;
            // If the card is not a valid MasterCard credit card.
            case STATUS_INVALID_AID:
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(ctx);
                alertDialog.setMessage(ctx.getText(R.string.only_mastercard));
                alertDialog.setNeutralButton(R.string.ok, null);
                alertDialog.create();
                alertDialog.show();
                return;
            default:
                break;
        }

        // Provided that the card is a valid MasterCard credit card,
        if (card != null) {
            // Prompt rename card dialog if the card still retains the default label.
            if (card.getLabel().equals(ctx.getString(R.string.default_label))) {
                if (!mainActivity.isFragmentVisible(RenameCardDialog.TAG)) {
                    RenameCardDialog renameCardDialog = RenameCardDialog.create(card);
                    renameCardDialog.show(mainActivity.getSupportFragmentManager(), "RenameCardDialog");
                }
            // Otherwise, refresh cards and scroll to this card.
            } else {
                mainActivity.refreshViewPager(card.getPan());
            }
        }

        // Close database handles.
        MainActivity.cardDao.close();
    }
}
