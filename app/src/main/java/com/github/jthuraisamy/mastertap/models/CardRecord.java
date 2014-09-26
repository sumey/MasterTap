package com.github.jthuraisamy.mastertap.models;

public class CardRecord {
    private int shortFileIdentifier;
    private int recordNumber;
    private byte[] rawResponse;

    public CardRecord() {}

    public CardRecord(int shortFileIdentifier, int recordNumber, byte[] rawResponse) {
        this.shortFileIdentifier = shortFileIdentifier;
        this.recordNumber = recordNumber;
        this.rawResponse = rawResponse;
    }

    public int getShortFileIdentifier() {
        return shortFileIdentifier;
    }

    public void setShortFileIdentifier(int shortFileIdentifier) {
        this.shortFileIdentifier = shortFileIdentifier;
    }

    public int getRecordNumber() {
        return recordNumber;
    }

    public void setRecordNumber(int recordNumber) {
        this.recordNumber = recordNumber;
    }

    public byte[] getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(byte[] rawResponse) {
        this.rawResponse = rawResponse;
    }
}
