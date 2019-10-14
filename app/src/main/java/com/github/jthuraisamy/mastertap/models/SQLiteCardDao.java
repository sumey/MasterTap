package com.github.jthuraisamy.mastertap.models;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.github.jthuraisamy.mastertap.interfaces.CardDao;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQLiteCardDao extends SQLiteOpenHelper implements CardDao {
    private static final String TAG = "MasterTapLog-" + SQLiteCardDao.class.getSimpleName();

    private final Context ctx;
    private static String key = "defaultKey";

    // Database handles:
    private SQLiteDatabase dbReadable;
    private SQLiteDatabase dbWritable;

    // Define database name and version.
    private static final String DATABASE_NAME = "MasterTap";
    private static final int DATABASE_VERSION = 1;

    // Define table of cards.
    private static final String TABLE_CARDS                 = "cards";
    private static final String KEY_CARDS_ID                = "_id";
    private static final String KEY_CARDS_LABEL             = "label";
    private static final String KEY_CARDS_PAN               = "pan";
    private static final String KEY_CARDS_EXPIRY_DATE       = "expiry_date";
    private static final String KEY_CARDS_PAYMENT_DIRECTORY = "payment_directory";
    private static final String KEY_CARDS_AID_FCI           = "aid_fci";
    private static final String KEY_CARDS_MAGSTRIPE_DATA    = "magstripe_data";

    // Define table of CVC3s.
    private static final String TABLE_CVC3         = "card_cvc3s";
    private static final String KEY_CVC3_ID        = "_id";
    private static final String KEY_CVC3_CARD_ID   = "card_id";
    private static final String KEY_CVC3_UN        = "unpredictable_number";
    private static final String KEY_CVC3_RESPONSE  = "response";
    private static final String KEY_CVC3_ATTEMPTED = "is_attempted";

    // Define table creation statements.
    private static final String CREATE_CARDS_TABLE = String.format(
            "CREATE TABLE %s (" +
                    "%s INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "%s TEXT," +
                    "%s TEXT NOT NULL UNIQUE," +
                    "%s TEXT," +
                    "%s TEXT," +
                    "%s TEXT," +
                    "%s TEXT" +
                    ")",
            TABLE_CARDS,
            KEY_CARDS_ID,
            KEY_CARDS_LABEL,
            KEY_CARDS_PAN,
            KEY_CARDS_EXPIRY_DATE,
            KEY_CARDS_PAYMENT_DIRECTORY,
            KEY_CARDS_AID_FCI,
            KEY_CARDS_MAGSTRIPE_DATA
    );

    private static final String CREATE_CVC3_TABLE = String.format(
            "CREATE TABLE %s (" +
                    "%s INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "%s INTEGER," +
                    "%s INTEGER," +
                    "%s TEXT," +
                    "%s INTEGER," +
                    "UNIQUE(%s, %s)," +
                    "FOREIGN KEY(%s) REFERENCES %s(%s) ON DELETE CASCADE" +
                    ")",
            TABLE_CVC3,
            KEY_CVC3_ID,
            KEY_CVC3_CARD_ID,
            KEY_CVC3_UN,
            KEY_CVC3_RESPONSE,
            KEY_CVC3_ATTEMPTED,
            KEY_CVC3_CARD_ID, KEY_CVC3_UN,
            KEY_CVC3_CARD_ID, TABLE_CARDS, KEY_CARDS_ID
    );

    public SQLiteCardDao(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);

        this.ctx = ctx;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_CARDS_TABLE);
        db.execSQL(CREATE_CVC3_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CARDS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CVC3);

        onCreate(db);
    }

    public boolean isOpen() {
        if ((dbReadable != null) && (dbWritable != null)) {
            return dbReadable.isOpen() && dbWritable.isOpen();
        }

        return false;
    }

    public void open() {
        close();

        Log.i(TAG, "Opening database with key = " + key);
        dbReadable = this.getReadableDatabase(key);
        dbWritable = this.getWritableDatabase(key);
    }

    public void close() {
        if ((dbReadable != null) && (dbWritable != null)) {
            if (dbReadable.isOpen() && dbWritable.isOpen()) {
                dbReadable.close();
                dbWritable.close();
            }
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        SQLiteCardDao.key = key;

        open();
    }

    public void setNewKey(String newKey) {
        dbWritable.execSQL("PRAGMA key = '" + key + "'");
        dbWritable.execSQL("PRAGMA rekey = '" + newKey + "'");

        key = newKey;

        open();
    }

    public void importCards(List<Card> cards) {
        for (Card card : cards) {
            if (getCard(card.getPan()) == null) {
                // If a card with this PAN does not exist, add it.
                addCard(card);
            } else {
                // If a card with this PAN already exists, replace it rather than updating it.
                // Updating it could lead to CVC3 values that are not in sequential order.
                deleteCard(card);
                addCard(card);
            }
        }
    }

    public void addCard(Card card) {
        if (!isOpen()) open();

        ContentValues values = new ContentValues();
        values.put(KEY_CARDS_LABEL, card.getLabel());
        values.put(KEY_CARDS_PAN, card.getPan());
        values.put(KEY_CARDS_EXPIRY_DATE, card.getExpiryDate());
        values.put(KEY_CARDS_PAYMENT_DIRECTORY, card.getPaymentDirectory());
        values.put(KEY_CARDS_AID_FCI, card.getAidFci());
        values.put(KEY_CARDS_MAGSTRIPE_DATA, card.getMagStripeData());

        dbWritable.insertWithOnConflict(TABLE_CARDS, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        // Add CVC3 values from the card.
        addCvc3Map(getCard(card.getPan()), card.getCvc3Map());

        // Add attempted UNs.
        for (int attemptedUN : card.getAttemptedUNs()) {
            attemptUN(getCard(card.getPan()), attemptedUN);
        }
    }

    public void addCvc3Map(Card card, Map<Integer, String> cvc3Map) {
        if (!isOpen()) open();

        dbWritable.beginTransaction();

        for (Map.Entry<Integer, String> entry : cvc3Map.entrySet()) {
            Integer unpredictableNumber = entry.getKey();
            String response = entry.getValue();
            addCvc3(card, unpredictableNumber, response);
        }

        dbWritable.setTransactionSuccessful();
        dbWritable.endTransaction();
    }

    public void addCvc3(Card card, int unpredictableNumber, String response) {
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_CVC3_CARD_ID, card.getId());
            values.put(KEY_CVC3_UN, unpredictableNumber);
            values.put(KEY_CVC3_RESPONSE, response);

            dbWritable.insertWithOnConflict(TABLE_CVC3, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Card> getCards() {
        List<Card> cards = new ArrayList<>();

        // Put an empty card at index 0 for the AddCardFragment.
        cards.add(0, new Card(ctx));

        try {
            if (!isOpen()) open();
        } catch (Exception e) {
            return cards;
        }

        String selectQuery = String.format(
                "SELECT %s FROM %s",
                KEY_CARDS_PAN, TABLE_CARDS
        );

        Cursor cursor = dbReadable.rawQuery(selectQuery, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                cards.add(this.getCard(cursor.getString(0)));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return cards;
    }

    public Card getCard(String pan) {
        if (!isOpen()) open();

        Card card;

        String[] columns = {
                KEY_CARDS_ID,
                KEY_CARDS_LABEL,
                KEY_CARDS_PAN,
                KEY_CARDS_EXPIRY_DATE,
                KEY_CARDS_PAYMENT_DIRECTORY,
                KEY_CARDS_AID_FCI,
                KEY_CARDS_MAGSTRIPE_DATA
        };
        String selection = KEY_CARDS_PAN + "=?";
        String[] selectionArgs = {pan};

        Cursor cursor = dbReadable.query(TABLE_CARDS, columns, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            card = new Card(ctx);
            card.setId(Integer.parseInt(cursor.getString(0)));
            card.setLabel(cursor.getString(1));
            card.setPan(cursor.getString(2));
            card.setExpiryDate(cursor.getString(3));
            card.setPaymentDirectory(cursor.getString(4));
            card.setAidFci(cursor.getString(5));
            card.setMagStripeData(cursor.getString(6));
            card.setCvc3Map(getCvc3MapByCardId(card.getId()));
            card.setAttemptedUNs(getAttemptedUNsByCardId(card.getId()));
            Log.i(TAG, "CVC3 Map Size: " + card.getCvc3Map().size() + " for id = " + card.getId());
        } else {
            card = null;
        }

        cursor.close();
        return card;
    }

    public Map<Integer, String> getCvc3MapByCardId(int id) {
        if (!isOpen()) open();

        Map<Integer, String> cvc3Map = new HashMap<>();

        String selectQuery = String.format(
                "SELECT %s, %s FROM %s WHERE %s = %d",
                KEY_CVC3_UN, KEY_CVC3_RESPONSE, TABLE_CVC3, KEY_CVC3_CARD_ID, id
        );

        Cursor cursor = dbReadable.rawQuery(selectQuery, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                cvc3Map.put(cursor.getInt(0), cursor.getString(1));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return cvc3Map;
    }

    public ArrayList<Integer> getAttemptedUNsByCardId(int id) {
        if (!isOpen()) open();

        ArrayList<Integer> attemptedUNs = new ArrayList<>();

        String selectQuery = String.format(
                "SELECT %s FROM %s WHERE %s = %d AND %s = 1",
                KEY_CVC3_UN, TABLE_CVC3, KEY_CVC3_CARD_ID, id, KEY_CVC3_ATTEMPTED
        );

        Cursor cursor = dbReadable.rawQuery(selectQuery, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                attemptedUNs.add(cursor.getInt(0));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return attemptedUNs;
    }

    public void attemptUN(Card card, int unpredictableNumber) {
        if (!isOpen()) open();

        Map<Integer, String> cvc3Map = card.getCvc3Map();

        // Cannot attempt using a UN response if the UN doesn't exist.
        if (!cvc3Map.containsKey(unpredictableNumber))
            return;

        ContentValues values = new ContentValues();
        values.put(KEY_CVC3_ATTEMPTED, 1);

        String selection = String.format("%s=? AND %s=?", KEY_CVC3_CARD_ID, KEY_CVC3_UN);
        String[] selectionArgs = {Integer.toString(card.getId()), Integer.toString(unpredictableNumber)};

        dbWritable.update(TABLE_CVC3, values, selection, selectionArgs);
    }

    public void updateCard(Card card) {
        if (!isOpen()) open();

        ContentValues values = new ContentValues();
        values.put(KEY_CARDS_LABEL, card.getLabel());
        values.put(KEY_CARDS_PAN, card.getPan());
        values.put(KEY_CARDS_EXPIRY_DATE, card.getExpiryDate());
        values.put(KEY_CARDS_PAYMENT_DIRECTORY, card.getPaymentDirectory());
        values.put(KEY_CARDS_AID_FCI, card.getAidFci());
        values.put(KEY_CARDS_MAGSTRIPE_DATA, card.getMagStripeData());

        String selection = KEY_CARDS_PAN + "=?";
        String[] selectionArgs = {card.getPan()};

        dbWritable.update(TABLE_CARDS, values, selection, selectionArgs);

        // Add CVC3 values from the card.
        addCvc3Map(getCard(card.getPan()), card.getCvc3Map());

        // Add attempted UNs.
        for (int attemptedUN : card.getAttemptedUNs()) {
            attemptUN(getCard(card.getPan()), attemptedUN);
        }
    }

    public void deleteCard(Card card) {
        if (!isOpen()) open();

        String selection = KEY_CARDS_PAN + "=?";
        String[] selectionArgs = {card.getPan()};

        dbWritable.delete(TABLE_CARDS, selection, selectionArgs);
    }
}
