package com.github.jthuraisamy.mastertap.helpers;

import android.nfc.tech.IsoDep;
import android.util.Log;

import com.github.jthuraisamy.mastertap.Helper;
import com.github.jthuraisamy.mastertap.TLVParser;
import com.github.jthuraisamy.mastertap.models.CardRecord;
import com.google.common.primitives.Bytes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VisaHelper {
    private static final String TAG = "MasterTapLog-" + VisaHelper.class.getSimpleName();

    /**
     * Get PDOL length for the Get Processing Options (GPO) command.
     *
     * @param aidFci byte[]
     * @return int
     */
    private static int getPdolLength(byte[] aidFci) {
        int pdolLength = 0;
        byte[] pdol = TLVParser.readTlv(aidFci, new byte[]{(byte) 0x9F, 0x38});

        for (int i = 0; i < pdol.length; i++) {
            if (i % 3 == 2) {
                pdolLength += pdol[i];
            }
        }

        return pdolLength;
    }

    /**
     * Return the Get Processing Options (GPO) command constructed with the PDOL length derived
     * from the given application selection data elements in byte[] aidFci.
     *
     * @param aidFci byte[]
     * @return byte[]
     */
    public static byte[] getGpoCommand(byte[] aidFci) {
        int pdolLength = VisaHelper.getPdolLength(aidFci);

        try {
            ByteArrayOutputStream gpoCommand = new ByteArrayOutputStream();
            gpoCommand.write(new byte[]{(byte) 0x80, (byte) 0xA8, 0x00, 0x00});
            gpoCommand.write(new byte[]{(byte) (pdolLength + 2), (byte) 0x83, (byte) pdolLength, (byte) 0x80});
            gpoCommand.write(new byte[pdolLength]);

            return gpoCommand.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Retrieve records specified in AFL and remove the contactless indicator from track 2 data.
     *
     * @param tagCommunicator IsoDep
     * @param afl byte[]
     * @return List<CardRecord>
     */
    public static List<CardRecord> getRecordsFromAfl(IsoDep tagCommunicator, byte[] afl) {
        // The AFL must be a multiple of 4 bytes.
        if ((afl.length & 0x03) > 0) return null;

        List<CardRecord> cardRecords = new ArrayList<CardRecord>();

        // Get the number of files.
        int numFiles = afl.length / 4;

        // Read records for each SFI.
        for (int i = 0; i < numFiles; i++) {
            int shortFileIdentifier = afl[4 * i] >> 3;
            int startRecord = afl[4 * i + 1];
            int endRecord = afl[4 * i + 2];

            for (; startRecord <= endRecord; startRecord++) {
                // Initialize a CardRecord.
                CardRecord cardRecord = new CardRecord();
                cardRecord.setShortFileIdentifier(shortFileIdentifier);
                cardRecord.setRecordNumber(startRecord);

                // Generate read record command.
                byte[] readRecordCommand = getReadRecordCommand(shortFileIdentifier, startRecord);

                // Execute read record command and save to CardRecord.
                try {
                    byte[] recordResponse = tagCommunicator.transceive(readRecordCommand);
                    recordResponse = stripContactlessIndicator(recordResponse);
                    cardRecord.setRawResponse(recordResponse);
                    Log.i(TAG, Helper.byteToHex(readRecordCommand) + " = " + Helper.byteToHex(recordResponse));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Append CardRecord to list.
                cardRecords.add(cardRecord);
            }
        }

        return cardRecords;
    }

    /**
     * Construct a read record command with the given SFI and record number.
     *
     * @param sfi int
     * @param recordNumber int
     * @return byte[]
     */
    private static byte[] getReadRecordCommand(int sfi, int recordNumber) {
        byte[] readRecordCommand = new byte[5];

        readRecordCommand[1] = (byte) 0xB2;
        readRecordCommand[2] = (byte) recordNumber;
        readRecordCommand[3] = (byte) ((sfi << 0x03) | 0x04);

        return readRecordCommand;
    }

    /**
     * Remove the contactless indicator digit from the track 2 equivalent data in the given record.
     *
     * @param record byte[]
     * @return byte[]
     */
    private static byte[] stripContactlessIndicator(byte[] record) {
        int track2HeaderIndex = Bytes.indexOf(record, (byte) 0x57);
        if (track2HeaderIndex < 0) return record;

        byte[] track2EquivalentData = TLVParser.readTlv(record, new byte[] {0x57});
        record[track2HeaderIndex + track2EquivalentData.length + 1] = 0x0F;

        return record;
    }
}
