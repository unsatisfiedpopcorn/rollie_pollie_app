package ritwik.graphapp;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import static ritwik.graphapp.NfcUtils.NfcConstants.BUNDLE_PATIENT_WEIGHT_KEY;
import static ritwik.graphapp.NfcUtils.NfcConstants.BUNDLE_WHEELCHAIR_WEIGHT_KEY;
import static ritwik.graphapp.NfcUtils.NfcConstants.PATIENT_WEIGHT_PREFIX;
import static ritwik.graphapp.NfcUtils.NfcConstants.WHEELCHAIR_WEIGHT_PREFIX;

public class WeightLog extends AppCompatActivity /*implements OnChartGestureListener, OnChartValueSelectedListener*/ {
    NfcAdapter mNfcAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_log);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        //I added this if statement to keep the selected fragment when rotating the device
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new HomeFragment()).commit();
        }
    }

    // TODO: Create NFC Interface for encapsulation and to ensure activity implements NFC methods
    public NfcAdapter getNfcAdapter() {
        if (mNfcAdapter == null) {
            mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        }
        return mNfcAdapter;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            // Build Fragment
            Fragment homeFragment = new HomeFragment();
            Bundle mBundle = getNfcBundle(intent);
            homeFragment.setArguments(mBundle);

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, homeFragment).commit();
        }
    }

    /**
     * Precondtiion: intent's action must be ACTION_NDEF_DISCOVERED
     *
     * @param intent
     * @return
     */
    @NonNull
    private Bundle getNfcBundle(Intent intent) {
        Bundle nfcBundle = new Bundle();
        ArrayList<String> tokens = new ArrayList<>();
        Parcelable[] rawMessages =
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        // TODO: Validate that message is from a NFC tag that only has text data
        if (rawMessages != null) {
            NdefMessage[] messages = new NdefMessage[rawMessages.length];
            for (int i = 0; i < rawMessages.length; i++) {
                messages[i] = (NdefMessage) rawMessages[i];
            }

            for (NdefMessage message : messages) {
                for (NdefRecord record : message.getRecords()) {
                    byte[] payload = record.getPayload();
                    // Strips first 3 bytes of payload which should be metadata
                    String humanReadablePayload = payload.length < 2 ?
                            "" : new String(payload).substring(3);
                    tokens.add(humanReadablePayload);
                }
            }

        }

        ArrayList<String> patientWeightDateData = filterByPrefix(tokens, PATIENT_WEIGHT_PREFIX);
        ArrayList<String> wheelchairWeightData = filterByPrefix(tokens, WHEELCHAIR_WEIGHT_PREFIX);

        nfcBundle.putStringArrayList(BUNDLE_PATIENT_WEIGHT_KEY, patientWeightDateData);
        nfcBundle.putStringArrayList(BUNDLE_WHEELCHAIR_WEIGHT_KEY, wheelchairWeightData);

        return nfcBundle;
    }

    private ArrayList<String> filterByPrefix(List<String> tokens, char prefix) {
        ArrayList<String> stringList = new ArrayList<>();
        for (String token : tokens) {
            int index = token.indexOf(prefix);
            // Only adds strings with the prefix. Also ignores strings like "   <prefix>"
            if (index != -1 && index + 1 < token.length()) {
                stringList.add(token.substring(index + 1));
            }
        }
        return stringList;
    }


    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;

                    switch (item.getItemId()) {
                        case R.id.navigation_home:
                            selectedFragment = new HomeFragment();
                            break;
                        case R.id.navigation_weightTracker:
                            selectedFragment = new WeightTrackerFragment();
                            break;
                        case R.id.navigation_Visit:
                            selectedFragment = new AppointmentsFragment();
                            break;
                        case R.id.navigation_calendar:
                            selectedFragment = new CalendarFragment();
                            break;
                    }
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                            selectedFragment).commit();

                    return true;
                }
            };
}
