package com.github.jthuraisamy.mastertap.helpers;

import com.github.jthuraisamy.mastertap.TLVParser;

public class MastercardHelper {
    /**
     * Return the total possible number of unpredictable numbers.
     *
     * @param magStripeData byte[]
     * @return int
     */
    public static int getTotalUNs(byte[] magStripeData) {
        int unpredictableNumberDigits = getUNDigits(magStripeData);
        return (int) Math.pow(10, unpredictableNumberDigits);
    }

    /**
     * Return the maximum number of digits an unpredictable number can have.
     *
     * @param magStripeData byte[]
     * @return int
     */
    private static int getUNDigits(byte[] magStripeData) {
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
}
