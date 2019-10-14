package com.github.jthuraisamy.mastertap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Helper {

    /**
     * Return a BCD representation of an integer as a 4-byte array.
     * @param i int
     * @return  byte[4]
     */
    public static byte[] intToBcdArray(int i) {
        if (i < 0)
            throw new IllegalArgumentException("Argument cannot be a negative integer.");

        StringBuilder binaryString = new StringBuilder();

        while (true) {
            int quotient = i / 10;
            int remainder = i % 10;
            String nibble = String.format("%4s", Integer.toBinaryString(remainder)).replace(' ', '0');
            binaryString.insert(0, nibble);

            if (quotient == 0) {
                break;
            } else {
                i = quotient;
            }
        }

        return ByteBuffer.allocate(4).putInt(Integer.parseInt(binaryString.toString(), 2)).array();
    }

    /**
     * Return an integer representation of a BCD array.
     */
    public static int bcdArrayToInt(byte[] bcdArray) {
        StringBuilder decimalString = new StringBuilder();

        for (Byte b : bcdArray) {
            decimalString.append(String.format("%02X", b));
        }

        return Integer.parseInt(decimalString.toString());
    }

    /**
     * Return a hexadecimal string representation of a byte array.
     *
     * @param bytes byte[]
     * @return      String
     */
    public static String byteToHex(byte[] bytes) {
        StringBuilder bcdString = new StringBuilder();

        for (Byte b : bytes) {
            bcdString.append(String.format("%02X ", b));
        }

        return bcdString.toString().trim();
    }


    /**
     * Return a byte array of a hexadecimal string representation.
     *
     * @param hexString String
     * @return          byte[]
     */
    public static byte[] hexToByte(String hexString) {
        String[] hexBytes = hexString.split(" ");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (String hexByte : hexBytes) {
            try {
                baos.write(new byte[] {Integer.valueOf(hexByte, 16).byteValue()});
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return baos.toByteArray();
    }

    /**
     * Return Primary Account Number with a space between each digit group.
     *
     * @param pan String
     * @return    String
     */
    public static String prettyPan(String pan) {
        return pan.replaceAll("(.{4}(?!$))", "$1 ").trim();
    }

    /**
     * Insert slash between month and year.
     *
     * @param expiryDate String
     * @return           String
     */
    public static String prettyExpiryDate(String expiryDate) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyMM");
        SimpleDateFormat outputFormat = new SimpleDateFormat("MM/yy");

        try {
            Date parsedDate = inputFormat.parse(expiryDate);
            return outputFormat.format(parsedDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }
}
