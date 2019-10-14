package com.github.jthuraisamy.mastertap;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.cardemulation.CardEmulation;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.preference.PreferenceManager;

import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

import com.github.jthuraisamy.mastertap.adapters.CardFragmentPagerAdapter;
import com.github.jthuraisamy.mastertap.fragments.ChangePasswordDialog;
import com.github.jthuraisamy.mastertap.fragments.DeleteCardDialog;
import com.github.jthuraisamy.mastertap.fragments.DisclaimerDialog;
import com.github.jthuraisamy.mastertap.fragments.EnterPasswordDialog;
import com.github.jthuraisamy.mastertap.fragments.ImportCardsDialog;
import com.github.jthuraisamy.mastertap.fragments.NfcAdapterAbsentDialog;
import com.github.jthuraisamy.mastertap.fragments.NfcAdapterDisabledDialog;
import com.github.jthuraisamy.mastertap.fragments.RegisterPasswordDialog;
import com.github.jthuraisamy.mastertap.fragments.RenameCardDialog;
import com.github.jthuraisamy.mastertap.helpers.PortHelper;
import com.github.jthuraisamy.mastertap.models.Card;
import com.github.jthuraisamy.mastertap.models.SQLiteCardDao;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;


public class MainActivity extends FragmentActivity {
    private static final String TAG = "MasterTapLog-" + MainActivity.class.getSimpleName();
    private static final int SELECT_IMPORT_FILE = 1;

    // isDatabaseReKeyed = true when a password has been registered for the database.
    public static boolean isDatabaseReKeyed = false;
    // isDatabaseAuthenticated = true when the correct password/key has been set for the database.
    public static boolean isDatabaseAuthenticated = false;

    public static SQLiteCardDao cardDao;
    public static List<Card> cards;

    private Menu menu;
    private ViewPager viewPager;
    private int selectedPage = 0;
    private ProgressDialog paymentProgressDialog;
    private final Gson gson = new Gson();
    public Vibrator vibrator;

    private NfcAdapter nfcAdapter;
    private final String[][] nfcTechFilter = new String[][] {new String[] {NfcA.class.getName()}};
    private PendingIntent nfcIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep screen always on to prevent card reading interruptions.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Obtain instance of system vibrator.
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Get NFC adapter and set intent.
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcIntent  = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // Register broadcast receiver for PaymentService.
        LocalBroadcastManager.getInstance(this).registerReceiver(apduReceiver, new IntentFilter("apduProcessing"));

        // Show disclaimer.
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDisclaimerAccepted = settings.getBoolean("isDisclaimerAccepted", false);
        boolean showDisclaimer = settings.getBoolean("showDisclaimer", true);
        if (!isDisclaimerAccepted || showDisclaimer) {
            if (!isFragmentVisible(DisclaimerDialog.TAG)) {
                DisclaimerDialog disclaimerDialog = DisclaimerDialog.create();
                disclaimerDialog.show(getSupportFragmentManager(), DisclaimerDialog.TAG);
            }
        } else {
            // Initialize database if disclaimer was already accepted.
            initDatabase();
        }
    }

    /**
     * Check if PaymentService is the default NFC payment app, and if not request user to set it.
     */
    private void checkDefaultPaymentApp() {
        CardEmulation cardEmulationManager = CardEmulation.getInstance(NfcAdapter.getDefaultAdapter(this));
        ComponentName paymentServiceComponent = new ComponentName(getApplicationContext(), PaymentService.class.getCanonicalName());
        boolean isPaymentServiceDefault = cardEmulationManager.isDefaultServiceForCategory(paymentServiceComponent, CardEmulation.CATEGORY_PAYMENT);
        if (!isPaymentServiceDefault) {
            Intent intent = new Intent(CardEmulation.ACTION_CHANGE_DEFAULT);
            intent.putExtra(CardEmulation.EXTRA_CATEGORY, CardEmulation.CATEGORY_PAYMENT);
            intent.putExtra(CardEmulation.EXTRA_SERVICE_COMPONENT, paymentServiceComponent);
            startActivityForResult(intent, 0);
        }
    }

    /**
     * Initialize the database and authenticate the user.
     */
    public void initDatabase() {
        // Load SQLCipher libraries and Card DAO.
        SQLiteDatabase.loadLibs(this);
        cardDao = new SQLiteCardDao(this);

        // Return if database is already authenticated.
        if (!isDatabaseAuthenticated) {
            // Check if database has been re-keyed.
            try {
                cardDao.setKey("defaultKey");
                cardDao.open();

                // Register a password if the default key worked.
                if (!isFragmentVisible(RegisterPasswordDialog.TAG)) {
                    RegisterPasswordDialog registerPasswordDialog = RegisterPasswordDialog.create();
                    registerPasswordDialog.show(getSupportFragmentManager(), RegisterPasswordDialog.TAG);
                }
            } catch (SQLiteException e) {
                // An exception was raised so the database has indeed been re-keyed.
                isDatabaseReKeyed = true;

                // Prompt user to enter her password.
                if (!isFragmentVisible(EnterPasswordDialog.TAG)) {
                    EnterPasswordDialog enterPasswordDialog = EnterPasswordDialog.create();
                    enterPasswordDialog.show(getSupportFragmentManager(), EnterPasswordDialog.TAG);
                }
            }

            // Retrieve stored cards.
            cards = cardDao.getCards();
        }

        // Setup ViewPager.
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(new CardFragmentPagerAdapter(this));
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                selectedPage = position;

                // Ensure that the appropriate card is selected for PaymentService.
                if (position == 0) deselectPaymentCard();

                setVisibleOptionsMenuItems();
            }

            @Override
            public void onPageSelected(int position) {
                // Ensure that the appropriate card is selected for PaymentService.
                if (position > 0) selectPaymentCard(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    private final BroadcastReceiver apduReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

            int paymentCardIndex = settings.getInt("paymentCardIndex", 0);
            String inboundApduDescription = intent.getStringExtra("inboundApduDescription");
            boolean transactionInProgress = intent.getBooleanExtra("transactionInProgress", false);

            if (paymentProgressDialog == null) {
                paymentProgressDialog = new ProgressDialog(MainActivity.this);
                paymentProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                paymentProgressDialog.setTitle(R.string.payment_progress_title);
            }

            if (transactionInProgress) {
                paymentProgressDialog.setMessage(inboundApduDescription);
                paymentProgressDialog.show();
            } else {
                paymentProgressDialog.dismiss();
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        // Set visible menu items.
        setVisibleOptionsMenuItems();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        Card card = cards.get(selectedPage);

        switch (id) {
            case R.id.action_lock:
                // Lock database.
                cardDao.close();
                isDatabaseAuthenticated = false;

                // Deselect card for PaymentService.
                deselectPaymentCard();

                // Show enter password dialog.
                EnterPasswordDialog enterPasswordDialog = EnterPasswordDialog.create();
                enterPasswordDialog.show(getSupportFragmentManager(), EnterPasswordDialog.TAG);
                break;
            case R.id.action_change_password:
                // Show change password dialog.
                ChangePasswordDialog changePasswordDialog = ChangePasswordDialog.create();
                changePasswordDialog.show(getSupportFragmentManager(), ChangePasswordDialog.TAG);
                break;
            case R.id.action_rename:
                // Show rename card dialog.
                RenameCardDialog renameCardDialog = RenameCardDialog.create(card);
                renameCardDialog.show(getSupportFragmentManager(), RenameCardDialog.TAG);
                break;
            case R.id.action_delete:
                // Show delete card dialog.
                DeleteCardDialog deleteCardDialog = DeleteCardDialog.create(card);
                deleteCardDialog.show(getSupportFragmentManager(), DeleteCardDialog.TAG);
                break;
            case R.id.action_import:
                importCardsFromFile();
                break;
            case R.id.action_export:
                exportCardsToJsonFile();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (nfcAdapter == null) {
            NfcAdapterAbsentDialog nfcAdapterAbsentDialog = NfcAdapterAbsentDialog.create();
            nfcAdapterAbsentDialog.show(getSupportFragmentManager(), NfcAdapterAbsentDialog.TAG);
        } else {
            if (!nfcAdapter.isEnabled()) {
                NfcAdapterDisabledDialog nfcAdapterDisabledDialog = NfcAdapterDisabledDialog.create();
                nfcAdapterDisabledDialog.show(getSupportFragmentManager(), NfcAdapterDisabledDialog.TAG);
            }

            nfcAdapter.enableForegroundDispatch(this, nfcIntent, null, nfcTechFilter);
        }

        // Ask for password if database is not authenticated.
        if (isDatabaseReKeyed && !isDatabaseAuthenticated) {
            // Prompt user to enter her password.
            if (!isFragmentVisible(EnterPasswordDialog.TAG)) {
                EnterPasswordDialog enterPasswordDialog = EnterPasswordDialog.create();
                enterPasswordDialog.show(getSupportFragmentManager(), EnterPasswordDialog.TAG);
            }
        }

        // Check if PaymentService is the default payment app.
        checkDefaultPaymentApp();

        // Ensure that the appropriate card is selected for PaymentService.
        selectPaymentCard(selectedPage);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Ensure that no cards are selected for the PaymentService.
        deselectPaymentCard();

        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Intent is only enabled if the user is authenticated.
        if (isDatabaseAuthenticated) {
            // Scroll to "Add Card" page.
            viewPager.setCurrentItem(0, true);

            // Toast message.
            toastMessage(getString(R.string.contact_established));

            // Read the card.
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            new CardReaderTask(this).execute(tag);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_IMPORT_FILE) {
                Uri selectedFileUri = data.getData();
                String selectedFilePath = selectedFileUri.getPath();
                File selectedFile = new File(selectedFilePath);
                importCardsFromFile(selectedFile);
            }
        }
    }

    /**
     * Select the currently showing card for the PaymentService.
     */
    public void selectShownCardForPayment() {
        selectPaymentCard(selectedPage);
    }

    /**
     * Set the PaymentService card to null.
     */
    private void deselectPaymentCard() {
        selectPaymentCard(0);
    }

    /**
     * Select the card for the PaymentService.
     *
     * @param cardIndex The index of the card in static member List<Card> cards.
     *                  If the value is zero (0), no card is selected for payment.
     */
    private void selectPaymentCard(int cardIndex) {
        // Ensure that the appropriate card is selected for PaymentService.
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor settingsEditor = settings.edit();
        settingsEditor.putInt("paymentCardIndex", cardIndex);
        settingsEditor.commit();
    }

    /**
     * Return whether there are any visible fragments with the given tag.
     *
     * @param tag String
     * @return boolean
     */
    public boolean isFragmentVisible(String tag) {
        return (getSupportFragmentManager().findFragmentByTag(tag) != null);
    }

    public void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Set options menu items depending on the selected page.
     */
    private void setVisibleOptionsMenuItems() {
        if (menu == null) return;

        switch (selectedPage) {
            // Hide rename and delete card buttons when AddCardFragment is selected.
            case 0:
                menu.findItem(R.id.action_rename).setVisible(false);
                menu.findItem(R.id.action_delete).setVisible(false);
                break;
            // Show rename and delete card buttons when CardFragment is selected.
            default:
                menu.findItem(R.id.action_rename).setVisible(true);
                menu.findItem(R.id.action_delete).setVisible(true);
        }
    }

    /**
     * Refresh the view pager, then scroll to the card with the given PAN.
     *
     * @param pan String
     */
    public void refreshViewPager(String pan) {
        refreshViewPager();
        scrollToCard(pan);
    }

    /**
     * Refresh the view pager.
     */
    public void refreshViewPager() {
        cards = cardDao.getCards();
        viewPager.setAdapter(new CardFragmentPagerAdapter(this));
        viewPager.getAdapter().notifyDataSetChanged();
    }

    /**
     * Scroll to the card with the given PAN.
     *
     * @param pan String
     */
    private void scrollToCard(String pan) {
        for (int i = 1; i < cards.size(); i++) {
            if (cards.get(i).getPan().equals(pan)) {
                viewPager.setCurrentItem(i, true);
                return;
            }
        }
    }

    /**
     * Import cards from the default JSON file path.
     */
    private void importCardsFromFile() {
        // Path to exported JSON file.
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File file = new File(path, getPackageName() + ".json");

        importCardsFromFile(file);
    }

    /**
     * Import cards from the given File.
     *
     * @param file File
     */
    private void importCardsFromFile(File file) {
        // Check if external storage is readable, and return if not.
        if (!PortHelper.isExternalStorageReadable()) {
            toastMessage(getString(R.string.import_io_error));
            return;
        }

        // Try reading the given file. However, if it is not found or if the file contains no
        // valid cards, allow the user to select a file.
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            Card[] cards = gson.fromJson(bufferedReader, Card[].class);
            List<Card> validCards = PortHelper.validateCards(cards);
            bufferedReader.close();
            fileReader.close();

            if (validCards.size() > 0) {
                // Prompt the user to select which cards they would like to import.
                if (!isFragmentVisible(ImportCardsDialog.TAG)) {
                    ImportCardsDialog importCardsDialog = ImportCardsDialog.create(validCards);
                    importCardsDialog.show(getSupportFragmentManager(), ImportCardsDialog.TAG);
                }
            } else {
                toastMessage(getString(R.string.no_valid_cards_found));
                selectImportFile();
            }
        } catch (FileNotFoundException e) {
            selectImportFile();
        } catch (JsonSyntaxException e) {
            toastMessage(getString(R.string.no_valid_cards_found));
            selectImportFile();
        } catch (IOException e) {
            toastMessage(e.getMessage());
        }
    }

    /**
     * Chose a JSON file for import.
     */
    private void selectImportFile() {
        toastMessage(getString(R.string.select_cards_file));
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("application/json");
        startActivityForResult(intent, SELECT_IMPORT_FILE);
    }

    /**
     * Export all cards to the default JSON file path.
     */
    private void exportCardsToJsonFile() {
        String jsonCards = gson.toJson(cards.subList(1, cards.size()));

        if (PortHelper.isExternalStorageWritable()) {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            File file = new File(path, getPackageName() + ".json");

            try {
                path.mkdirs();
                file.createNewFile();
                OutputStream outputStream = new FileOutputStream(file);
                outputStream.write(jsonCards.getBytes());
                outputStream.close();
                toastMessage(getString(R.string.export_success));
            } catch (IOException e) {
                e.printStackTrace();
                toastMessage(getString(R.string.export_io_error));
            }
        }
    }
}
