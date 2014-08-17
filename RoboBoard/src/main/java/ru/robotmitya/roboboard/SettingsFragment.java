package ru.robotmitya.roboboard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;

import ru.robotmitya.robocommonlib.AppConst;
import ru.robotmitya.robocommonlib.Log;

/**
 * Created by dmitrydzz on 1/28/14.
 * Application options activity.
 * @author Dmitry Dzakhov.
 *
 */
public final class SettingsFragment extends PreferenceFragment implements OnPreferenceChangeListener {

    private static String mMasterUri;
    private static int mRemoteControlMode;

    public static class REMOTE_CONTROL_MODE {
        public final static int TWO_JOYSTICKS = 0;
        @SuppressWarnings("UnusedDeclaration")
        public final static int ORIENTATION = 1;
    }

    private EditTextPreference mEditTextPreferenceMasterUri;
    private ListPreference mListPreferenceRemoteControlMode;
    private ArrayList<CharSequence> mRemoteControlModeEntries;

    public static String getMasterUri() {
        return mMasterUri;
    }

    public static int getRemoteControlMode() {
        return mRemoteControlMode;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.settings_fragment);

        String key;

        key = getString(R.string.option_master_uri_key);
        mEditTextPreferenceMasterUri = (EditTextPreference) this.findPreference(key);
        onPreferenceChange(mEditTextPreferenceMasterUri, mMasterUri);
        mEditTextPreferenceMasterUri.setOnPreferenceChangeListener(this);


        mRemoteControlModeEntries = new ArrayList<CharSequence>();
        mRemoteControlModeEntries.add(getString(R.string.option_remote_control_mode_entry_two_joysticks));
        mRemoteControlModeEntries.add(getString(R.string.option_remote_control_mode_entry_device_orientation));
        ArrayList<CharSequence> remoteControlModeValues = new ArrayList<CharSequence>();
        remoteControlModeValues.add(getString(R.string.option_remote_control_mode_value_two_joysticks));
        remoteControlModeValues.add(getString(R.string.option_remote_control_mode_value_device_orientation));

        key = getString(R.string.option_remote_control_mode_key);
        mListPreferenceRemoteControlMode = (ListPreference) findPreference(key);
        mListPreferenceRemoteControlMode.setEntries(mRemoteControlModeEntries.toArray(new CharSequence[mRemoteControlModeEntries.size()]));
        mListPreferenceRemoteControlMode.setEntryValues(remoteControlModeValues.toArray(new CharSequence[remoteControlModeValues.size()]));
        mListPreferenceRemoteControlMode.setValue(String.valueOf(mRemoteControlMode));
        onPreferenceChange(mListPreferenceRemoteControlMode, remoteControlModeValues.get(mRemoteControlMode));
        mListPreferenceRemoteControlMode.setOnPreferenceChangeListener(this);
    }

    /**
     * Инициализация некоторых установок.
     * @param context контекст приложения.
     */
    public static void initialize(final Context context) {
        if (context == null) {
            return;
        }

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        String key;
        String defaultValue;

        key = context.getString(R.string.option_master_uri_key);
        defaultValue = context.getString(R.string.option_master_uri_default_value);
        mMasterUri = settings.getString(key, defaultValue);

        key = context.getString(R.string.option_remote_control_mode_key);
        defaultValue = String.valueOf(REMOTE_CONTROL_MODE.TWO_JOYSTICKS);
        mRemoteControlMode = Integer.valueOf(settings.getString(key, defaultValue));
    }

    /**
     * Обработчик листнера изенений настроек.
     * @param preference изменившаяся опция.
     * @param newValue новое значение.
     * @return принять ли изменения.
     */
    public boolean onPreferenceChange(final Preference preference, final Object newValue) {
        if (preference == null) {
            return false;
        }

        String value = (String) newValue;

        if (preference == mEditTextPreferenceMasterUri) {
            mMasterUri = value;
            mEditTextPreferenceMasterUri.setSummary(value);
            return true;
        } else if (preference == mListPreferenceRemoteControlMode) {
            mRemoteControlMode = Integer.valueOf(value);
            mListPreferenceRemoteControlMode.setSummary(mRemoteControlModeEntries.get(mRemoteControlMode));
            sendRemoteControlModeWasChangedBroadcast();
            return true;
        }

        return false;
    }

    private void sendRemoteControlModeWasChangedBroadcast() {
        Intent intent = new Intent(AppConst.RoboBoard.Broadcast.BROADCAST_REMOTE_CONTROL_MODE_SETTINGS_NAME);
        if ((getActivity() != null) && (getActivity().getApplicationContext() != null)) {
            LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).sendBroadcast(intent);
            Log.d(this, AppConst.RoboBoard.Broadcast.BROADCAST_REMOTE_CONTROL_MODE_SETTINGS_NAME + " was sent");
        }
    }
}