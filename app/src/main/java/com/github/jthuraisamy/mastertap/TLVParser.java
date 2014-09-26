package com.github.jthuraisamy.mastertap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

public class TLVParser {
    /**
     * Reads TLV values for a given byte array.
     */
    public static byte[] readTlv(byte[] tlv, byte[] tag) {
        if (tlv == null || tlv.length < 1) {
            throw new IllegalArgumentException("Invalid TLV");
        }

        int i = 0;
        int length;
        byte[] rollingTag = new byte[tag.length];
        ByteArrayInputStream inputStream = null;

        try {
            inputStream = new ByteArrayInputStream(tlv);

            while ((inputStream.read()) != -1) {
                i += 1;
                if (i >= tag.length) {
                    rollingTag = Arrays.copyOfRange(tlv, i - tag.length, i);
                }

                if (Arrays.equals(tag, rollingTag)){
                    if ((length = inputStream.read()) != -1){
                        byte[] value = new byte[length];
                        inputStream.read(value, 0, length);
                        return value;
                    }
                }
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

}
