package ru.robotmitya.roboboard;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by dmitrydzz on 4/27/14.
 *
 */
public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
