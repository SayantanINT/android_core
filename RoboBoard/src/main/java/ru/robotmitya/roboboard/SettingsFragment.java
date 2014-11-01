package ru.robotmitya.roboboard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import android.preference.Preference.OnPreferenceChangeListener;
import android.support.v4.content.LocalBroadcastManager;

import java.net.MalformedURLException;
import java.util.ArrayList;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.jetbrains.annotations.NotNull;
import ru.robotmitya.robocommonlib.AppConst;
import ru.robotmitya.robocommonlib.Log;
import ru.robotmitya.robocommonlib.SensorOrientation;
import ru.robotmitya.robocommonlib.SettingsHelper;

/**
 * Created by dmitrydzz on 1/28/14.
 * Application options activity.
 * @author Dmitry Dzakhov.
 *
 */
public final class SettingsFragment extends PreferenceFragment implements OnPreferenceChangeListener {

    private static boolean mIsPublicMaster;
    private static String mLocalMasterUri;
    private static String mRemoteMasterUriIp;

    private static int mRemoteControlMode;

    private PreferenceCategory mPreferenceCategoryRosCore;
    private CheckBoxPreference mCheckBoxPreferenceIsPublicMaster;
    private EditTextPreference mEditTextPreferenceMasterUri;
    private ListPreference mListPreferenceRemoteControlMode;
    private ArrayList<CharSequence> mRemoteControlModeEntries;
    private CheckBoxPreference mCheckBoxPreferenceLogging;

    public static boolean getIsPublicMaster() {
        return mIsPublicMaster;
    }

    public static String getMasterUri() throws MalformedURLException {
        if (mIsPublicMaster) {
            return mLocalMasterUri;
        }

        return SettingsHelper.getMasterUri(mRemoteMasterUriIp);
    }

    public static int getRemoteControlMode() {
        return mRemoteControlMode;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_fragment);

        String key;

        key = getString(R.string.preference_ros_core_key);
        mPreferenceCategoryRosCore = (PreferenceCategory) this.findPreference(key);
        mPreferenceCategoryRosCore.setLayoutResource(R.layout.preference_category_detailed);

        key = getString(R.string.option_is_public_master_key);
        mCheckBoxPreferenceIsPublicMaster = (CheckBoxPreference) this.findPreference(key);
        onPreferenceChange(mCheckBoxPreferenceIsPublicMaster, mIsPublicMaster);
        mCheckBoxPreferenceIsPublicMaster.setOnPreferenceChangeListener(this);

        key = getString(R.string.option_master_uri_key);
        mEditTextPreferenceMasterUri = (EditTextPreference) this.findPreference(key);
        onPreferenceChange(mEditTextPreferenceMasterUri, mRemoteMasterUriIp);
        mEditTextPreferenceMasterUri.setOnPreferenceChangeListener(this);

        key = getString(R.string.option_logging_key);
        mCheckBoxPreferenceLogging = (CheckBoxPreference) this.findPreference(key);
        onPreferenceChange(mCheckBoxPreferenceLogging, Log.getEnabled());
        mCheckBoxPreferenceLogging.setOnPreferenceChangeListener(this);


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

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Context context = getActivity();
        final boolean hasOrientationSensors =
                SensorOrientation.hasGyroscopeSensor(context) && SensorOrientation.hasGravitySensor(context);
        mListPreferenceRemoteControlMode.setEnabled(hasOrientationSensors);

        return super.onCreateView(inflater, container, savedInstanceState);
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

        key = context.getString(R.string.option_is_public_master_key);
        mIsPublicMaster = settings.getBoolean(key, false);

        try {
            mLocalMasterUri = SettingsHelper.getNewPublicMasterUri();
        } catch (MalformedURLException e) {
            mLocalMasterUri = context.getString(R.string.option_master_uri_default_value);
            try {
                mLocalMasterUri = SettingsHelper.getMasterUri(mLocalMasterUri);
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
                mLocalMasterUri = "";
            }
        }

        key = context.getString(R.string.option_master_uri_key);
        defaultValue = context.getString(R.string.option_master_uri_default_value);
        mRemoteMasterUriIp = settings.getString(key, defaultValue);
        try {
            mRemoteMasterUriIp = SettingsHelper.fixUrl(mRemoteMasterUriIp);
            if (mRemoteMasterUriIp.equals("")) {
                mRemoteMasterUriIp = context.getString(R.string.option_master_uri_default_value);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        key = context.getString(R.string.option_remote_control_mode_key);
        defaultValue = String.valueOf(AppConst.Common.ControlMode.TWO_JOYSTICKS);
        mRemoteControlMode = Integer.valueOf(settings.getString(key, defaultValue));

        key = context.getString(R.string.option_logging_key);
        Log.setEnabled(settings.getBoolean(key, false));
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

        if (preference == mCheckBoxPreferenceIsPublicMaster) {
            mIsPublicMaster = (Boolean) newValue;
            if (mIsPublicMaster) {
                mCheckBoxPreferenceIsPublicMaster.setSummary(R.string.option_is_public_master_summary);
            } else {
                mCheckBoxPreferenceIsPublicMaster.setSummary(R.string.option_is_not_public_master_summary);
            }
            updateRosCoreGroupTitle();
            return true;
        } else if (preference == mEditTextPreferenceMasterUri) {
            String value = null;
            try {
                value = SettingsHelper.fixUrl((String) newValue);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            if (value == null) {
                return false;
            }
            mRemoteMasterUriIp = value;
            if (mRemoteMasterUriIp.equals("")) {
                mRemoteMasterUriIp = getString(R.string.option_master_uri_default_value);
                mEditTextPreferenceMasterUri.setSummary(getString(R.string.option_master_uri_summary));
            } else {
                mEditTextPreferenceMasterUri.setSummary(mRemoteMasterUriIp);
            }
            mEditTextPreferenceMasterUri.setText(mRemoteMasterUriIp);
            updateRosCoreGroupTitle();
            return true;
        } else if (preference == mListPreferenceRemoteControlMode) {
            String value = (String) newValue;
            mRemoteControlMode = Integer.valueOf(value);
            mListPreferenceRemoteControlMode.setSummary(mRemoteControlModeEntries.get(mRemoteControlMode));
            if (mRemoteControlMode == AppConst.Common.ControlMode.TWO_JOYSTICKS) {
                sendActivateOrientationViewBroadcast(false);
                sendActivateHeadJoystickBroadcast(true);
            } else if (mRemoteControlMode == AppConst.Common.ControlMode.ORIENTATION) {
                sendActivateHeadJoystickBroadcast(false);
                // OrientationView is always disabled in onStart method.
            }
            sendRemoteControlModeWasChangedBroadcast(mRemoteControlMode);
            return true;
        } else if (preference == mCheckBoxPreferenceLogging) {
            Log.setEnabled((Boolean) newValue);
            return true;
        }

        return false;
    }

    private void updateRosCoreGroupTitle() {
        mPreferenceCategoryRosCore.setTitle(getString(R.string.preference_ros_core));

        String masterUriPart;
        try {
            masterUriPart = " " + getMasterUri();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            masterUriPart = "";
        }
        Spannable spannable = new SpannableString(masterUriPart);
        spannable.setSpan(new RelativeSizeSpan(0.7f),
                0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.text_highlight_color)),
                0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mPreferenceCategoryRosCore.setSummary(spannable);
    }

    private void sendActivateOrientationViewBroadcast(final boolean value) {
        Intent intent = new Intent(AppConst.RoboBoard.Broadcast.ORIENTATION_ACTIVATE);
        intent.putExtra(AppConst.RoboBoard.Broadcast.ORIENTATION_ACTIVATE_EXTRA_ENABLED, value);
        if ((getActivity() != null) && (getActivity().getApplicationContext() != null)) {
            LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).sendBroadcast(intent);
            final String valueText = value ? " enable " : " disable ";
            Log.d(this, AppConst.RoboBoard.Broadcast.ORIENTATION_ACTIVATE + " was sent to " + valueText + OrientationView.class.getName());
        }
    }

    private void sendActivateHeadJoystickBroadcast(final boolean value) {
        Intent intent = new Intent(AppConst.RoboBoard.Broadcast.JOYSTICK_ACTIVATE);
        intent.putExtra(AppConst.RoboBoard.Broadcast.JOYSTICK_ACTIVATE_EXTRA_TOPIC, AppConst.RoboHead.HEAD_TOPIC);
        intent.putExtra(AppConst.RoboBoard.Broadcast.JOYSTICK_ACTIVATE_EXTRA_ENABLED, value);
        if ((getActivity() != null) && (getActivity().getApplicationContext() != null)) {
            LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).sendBroadcast(intent);
            final String valueText = value ? " enable " : " disable ";
            Log.d(this, AppConst.RoboBoard.Broadcast.JOYSTICK_ACTIVATE + " was sent to" + valueText + "head joystick");
        }
    }

    private void sendRemoteControlModeWasChangedBroadcast(final int remoteControlMode) {
        Intent intent = new Intent(AppConst.RoboBoard.Broadcast.REMOTE_CONTROL_MODE_SETTINGS_NAME);
        intent.putExtra(AppConst.RoboBoard.Broadcast.REMOTE_CONTROL_MODE_SETTINGS_EXTRA_NAME, remoteControlMode);
        if ((getActivity() != null) && (getActivity().getApplicationContext() != null)) {
            LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).sendBroadcast(intent);
            Log.d(this, AppConst.RoboBoard.Broadcast.REMOTE_CONTROL_MODE_SETTINGS_NAME + " was sent (mode " + remoteControlMode + ")");
        }
    }
}