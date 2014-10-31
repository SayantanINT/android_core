package ru.robotmitya.robohead;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Camera;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import org.json.JSONException;

import java.net.MalformedURLException;
import java.util.ArrayList;

import ru.robotmitya.robocommonlib.AppConst;
import ru.robotmitya.robocommonlib.Log;
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

    private static CameraSizesSet mCameraSizesSet;

    private static int mCameraIndex;
    private static String mFrontCameraMode;
    private static String mBackCameraMode;
    private static boolean mDriveReverse;

    /**
     * Robot's Bluetooth adapter MAC-address.
     */
    private static String mRoboBodyMac; // "00:12:03:31:01:22"

    private PreferenceCategory mPreferenceCategoryRosCore;
    private CheckBoxPreference mCheckBoxPreferenceIsPublicMaster;
    private EditTextPreference mEditTextPreferenceMasterUri;
    private ListPreference mListPreferenceCamera;
    private ListPreference mListPreferenceFrontCameraMode;
    private ListPreference mListPreferenceBackCameraMode;
    private CheckBoxPreference mCheckBoxPreferenceDriveReverse;
    private CheckBoxPreference mCheckBoxPreferenceLogging;

    /**
     * EditText for mRoboBodyMac option.
     */
    private EditTextPreference mEditTextPreferenceRoboBodyMac;

    public static boolean getIsPublicMaster() {
        return mIsPublicMaster;
    }

    public static String getMasterUri() throws MalformedURLException {
        if (mIsPublicMaster) {
            return mLocalMasterUri;
        }

        return SettingsHelper.getMasterUri(mRemoteMasterUriIp);
    }

    public static int getCameraIndex() {
        return mCameraIndex;
    }

    public static String getFrontCameraMode() {
        return mFrontCameraMode;
    }

    public static String getBackCameraMode() {
        return mBackCameraMode;
    }

    public static boolean getDriveReverse() {
        return mDriveReverse;
    }

    @SuppressLint("CommitPrefEdits")
    public static void setCameraIndex(final Context context, final int cameraIndex) {
        mCameraIndex = cameraIndex;

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        settings.edit().putString(context.getString(R.string.option_camera_index_key), String.valueOf(cameraIndex)).commit();
    }

    /**
     * Аксессор поля mRoboBodyMac.
     * @return MAC-адрес Bluetooth-адаптера контроллера робота.
     */
    public static String getRoboBodyMac() {
        return mRoboBodyMac;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getActivity();

        addPreferencesFromResource(R.xml.settings_fragment);

        String key;
        String title;

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


        ArrayList<CharSequence> cameraEntries = getCameraEntries(context);
        ArrayList<CharSequence> cameraValues = getCameraValues();

        key = getString(R.string.option_camera_index_key);
        mListPreferenceCamera = (ListPreference) this.findPreference(key);
        mListPreferenceCamera.setEntries(cameraEntries.toArray(new CharSequence[cameraEntries.size()]));
        mListPreferenceCamera.setEntryValues(cameraValues.toArray(new CharSequence[cameraValues.size()]));
        mListPreferenceCamera.setValue(String.valueOf(mCameraIndex));
        title = getString(R.string.option_camera_index_title) + ": " + getCameraValueDescription(mCameraIndex, context);
        mListPreferenceCamera.setTitle(title);
        mListPreferenceCamera.setDialogTitle(R.string.option_camera_index_dialog_title);
        mListPreferenceCamera.setOnPreferenceChangeListener(this);

        ArrayList<CharSequence> cameraModeEntries = getCameraModeEntries(mCameraSizesSet, context);
        ArrayList<CharSequence> cameraModeValues = getCameraModeValues(mCameraSizesSet);

        key = getString(R.string.option_front_camera_mode_key);
        mListPreferenceFrontCameraMode = (ListPreference) this.findPreference(key);
        mListPreferenceFrontCameraMode.setEntries(cameraModeEntries.toArray(new CharSequence[cameraModeEntries.size()]));
        mListPreferenceFrontCameraMode.setEntryValues(cameraModeValues.toArray(new CharSequence[cameraModeValues.size()]));
        mListPreferenceFrontCameraMode.setValue(mFrontCameraMode);
        title = getString(R.string.option_front_camera_mode_title) + ": " + getCameraModeValueDescription(mFrontCameraMode, mCameraSizesSet, context);
        mListPreferenceFrontCameraMode.setTitle(title);
        mListPreferenceFrontCameraMode.setDialogTitle(R.string.option_front_camera_mode_dialog_title);
        mListPreferenceFrontCameraMode.setOnPreferenceChangeListener(this);

        key = getString(R.string.option_back_camera_mode_key);
        mListPreferenceBackCameraMode = (ListPreference) this.findPreference(key);
        mListPreferenceBackCameraMode.setEntries(cameraModeEntries.toArray(new CharSequence[cameraModeEntries.size()]));
        mListPreferenceBackCameraMode.setEntryValues(cameraModeValues.toArray(new CharSequence[cameraModeValues.size()]));
        mListPreferenceBackCameraMode.setValue(String.valueOf(mBackCameraMode));
        title = getString(R.string.option_back_camera_mode_title) + ": " + getCameraModeValueDescription(mBackCameraMode, mCameraSizesSet, context);
        mListPreferenceBackCameraMode.setTitle(title);
        mListPreferenceBackCameraMode.setDialogTitle(R.string.option_back_camera_mode_dialog_title);
        mListPreferenceBackCameraMode.setOnPreferenceChangeListener(this);

        key = getString(R.string.option_drive_reverse_key);
        mCheckBoxPreferenceDriveReverse = (CheckBoxPreference) this.findPreference(key);
        if (mCheckBoxPreferenceDriveReverse != null) {
            mCheckBoxPreferenceDriveReverse.setChecked(mDriveReverse);
            mCheckBoxPreferenceDriveReverse.setTitle(R.string.option_drive_reverse_title);
            mCheckBoxPreferenceDriveReverse.setSummary(R.string.option_drive_reverse_summary);
            mCheckBoxPreferenceDriveReverse.setOnPreferenceChangeListener(this);
        }

        key = getString(R.string.option_robobody_mac_key);
        mEditTextPreferenceRoboBodyMac = (EditTextPreference) this.findPreference(key);
        title = getString(R.string.option_robobody_mac_title) + ": " + mRoboBodyMac;
        mEditTextPreferenceRoboBodyMac.setTitle(title);
        mEditTextPreferenceRoboBodyMac.setOnPreferenceChangeListener(this);

        key = getString(R.string.option_logging_key);
        mCheckBoxPreferenceLogging = (CheckBoxPreference) this.findPreference(key);
        onPreferenceChange(mCheckBoxPreferenceLogging, Log.getEnabled());
        mCheckBoxPreferenceLogging.setOnPreferenceChangeListener(this);
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
        mIsPublicMaster = settings.getBoolean(key, true);

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

        // Loading static mCameraSizesSet:
        loadCameraSizesSet(context);

        final int numberOfCameras = Camera.getNumberOfCameras();

        key = context.getString(R.string.option_front_camera_mode_key);
        if (numberOfCameras == 0) {
            defaultValue = integerToHex(0xff, 0xff);
        } else {
            defaultValue = integerToHex(
                    mCameraSizesSet.get(mCameraSizesSet.length() - 1).getCameraIndex(),
                    0xff);
        }
        mFrontCameraMode = settings.getString(key, defaultValue);

        key = context.getString(R.string.option_back_camera_mode_key);
        if (numberOfCameras == 0) {
            defaultValue = integerToHex(0xff, 0xff);
        } else {
            defaultValue = integerToHex(
                    mCameraSizesSet.get(0).getCameraIndex(),
                    0xff);
        }
        mBackCameraMode = settings.getString(key, defaultValue);

        key = context.getString(R.string.option_camera_index_key);
        if (numberOfCameras == 0) {
            defaultValue = String.valueOf(AppConst.Common.Camera.DISABLED);
        } else {
            defaultValue = String.valueOf(AppConst.Common.Camera.FRONT);
        }
        mCameraIndex = Integer.valueOf(settings.getString(key, defaultValue));

        key = context.getString(R.string.option_drive_reverse_key);
        mDriveReverse = settings.getBoolean(key, true);

        key = context.getString(R.string.option_robobody_mac_key);
        defaultValue = context.getString(R.string.option_robobody_mac_default_value);
        mRoboBodyMac = settings.getString(key, defaultValue);

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

        Context context = getActivity();

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
        } else if (preference == mListPreferenceCamera) {
            mCameraIndex = Integer.valueOf((String) newValue);
            String title = getString(R.string.option_camera_index_title) +
                    ": " + getCameraValueDescription(mCameraIndex, context);
            mListPreferenceCamera.setTitle(title);
            sendCameraSettingsWereChangedBroadcast();
            return true;
        } else if (preference == mListPreferenceFrontCameraMode) {
            mFrontCameraMode = (String) newValue;
            String title = getString(R.string.option_front_camera_mode_title) +
                    ": " + getCameraModeValueDescription(mFrontCameraMode, mCameraSizesSet, context);
            mListPreferenceFrontCameraMode.setTitle(title);
            sendCameraSettingsWereChangedBroadcast();
            return true;
        } else if (preference == mListPreferenceBackCameraMode) {
            mBackCameraMode = (String) newValue;
            String title = getString(R.string.option_back_camera_mode_title) +
                    ": " + getCameraModeValueDescription(mBackCameraMode, mCameraSizesSet, context);
            mListPreferenceBackCameraMode.setTitle(title);
            sendCameraSettingsWereChangedBroadcast();
            return true;
        } else if (preference == mCheckBoxPreferenceDriveReverse) {
            mDriveReverse = (Boolean) newValue;
            return true;
        } else if (preference == mEditTextPreferenceRoboBodyMac) {
            mRoboBodyMac = (String) newValue;
            mEditTextPreferenceRoboBodyMac.setTitle(R.string.option_robobody_mac_title);
            mEditTextPreferenceRoboBodyMac.setTitle(mEditTextPreferenceRoboBodyMac.getTitle() + ": " + newValue);
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

    private void sendCameraSettingsWereChangedBroadcast() {
        Intent intent = new Intent(EyePreviewView.BROADCAST_CAMERA_SETTINGS_NAME);
        if ((getActivity() != null) && (getActivity().getApplicationContext() != null)) {
            LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).sendBroadcast(intent);
        }
    }

    @SuppressLint("CommitPrefEdits")
    private static void loadCameraSizesSet(final Context context) {
        mCameraSizesSet = new CameraSizesSet();

        // Load preference jsonCameraSizesSet only once after app's first launch.
        // Next time we'll read it from preference value.
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String jsonCameraSizesSet = settings.getString(context.getString(R.string.option_json_camera_sizes_set), "");
        if (jsonCameraSizesSet.equals("")) {
            Log.d(mCameraSizesSet, "First load");
            mCameraSizesSet.load();
            try {
                jsonCameraSizesSet = mCameraSizesSet.toJson();
            } catch (JSONException e) {
                Log.e(mCameraSizesSet, e.getMessage());
            }
            settings.edit().putString(context.getString(R.string.option_json_camera_sizes_set), jsonCameraSizesSet).commit();
        } else {
            try {
                mCameraSizesSet.fromJson(jsonCameraSizesSet);
            } catch (JSONException e) {
                Log.e(mCameraSizesSet, e.getMessage());
            }
        }

        Log.d(mCameraSizesSet, "jsonCameraSizesSet = " + jsonCameraSizesSet);
    }

    public static ArrayList<CharSequence> getCameraEntries(final Context context) {
        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> values = getCameraValues();
        for (CharSequence value : values) {
            entries.add(getCameraValueDescription(Integer.parseInt((String)value), context));
        }
        return entries;
    }

    public static ArrayList<CharSequence> getCameraValues() {
        ArrayList<CharSequence> values = new ArrayList<CharSequence>();
        values.add(String.valueOf(AppConst.Common.Camera.DISABLED));
        values.add(String.valueOf(AppConst.Common.Camera.FRONT));
        values.add(String.valueOf(AppConst.Common.Camera.BACK));
        return values;
    }

    public static ArrayList<CharSequence> getCameraModeEntries(final CameraSizesSet cameraSizesSet, final Context context) {
        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();

        ArrayList<CharSequence> values = getCameraModeValues(cameraSizesSet);
        for (CharSequence value : values) {
            entries.add(getCameraModeValueDescription(value.toString(), cameraSizesSet, context));
        }

        return entries;
    }

    public static ArrayList<CharSequence> getCameraModeValues(final CameraSizesSet cameraSizesSet) {
        ArrayList<CharSequence> values = new ArrayList<CharSequence>();

        values.add("FFFF");
        for (int i = 0; i < cameraSizesSet.length(); i++) {
            final CameraSizesSet.CameraSizes cameraSizes = cameraSizesSet.get(i);
            final int cameraNum = cameraSizes.getCameraIndex();
            values.add(integerToHex(cameraNum, 0xff));
            for (int j = 0; j < cameraSizes.getSizesLength(); j++) {
                values.add(integerToHex(cameraNum, j));
            }
        }

        return values;
    }

    private static String getCameraValueDescription(final int cameraIndex, final Context context) {
        switch (cameraIndex) {
            case AppConst.Common.Camera.FRONT:
                return context.getResources().getString(R.string.option_camera_front_entry);
            case AppConst.Common.Camera.BACK:
                return context.getResources().getString(R.string.option_camera_back_entry);
            default:
                return context.getResources().getString(R.string.option_camera_disabled_entry);
        }
    }

    private static String getCameraModeValueDescription(int value, CameraSizesSet cameraSizesSet, final Context context) {
        value &= 0xffff;
        if (value == 0xffff) {
            return context.getResources().getString(R.string.option_camera_disabled_entry);
        }
        // hiByte is the camera index in CameraSizesSet
        int hiByte = value & 0xff00;
        hiByte >>= 8;
        // loByte is the size index in CameraSizesSet for some cameraIndex
        final int loByte = value & 0x00ff;
        final int cameraIndex = cameraSizesSet.get(hiByte).getCameraIndex() + 1;
        final Resources resources = context.getResources();
        if (loByte == 0xff) {
            return String.format(resources.getString(R.string.option_camera_mode_default_entry),
                    cameraIndex);
        } else {
            CameraSizesSet.Size size = cameraSizesSet.get(hiByte).getSize(loByte);
            return String.format(resources.getString(R.string.option_camera_mode_size_entry),
                    cameraIndex, size.width, size.height);
        }
    }

    private static String getCameraModeValueDescription(String textValue, CameraSizesSet cameraSizesSet, final Context context) {
        int value = Integer.parseInt(textValue, 16);
        return getCameraModeValueDescription(value, cameraSizesSet, context);
    }

    private static String integerToHex(int hiByte, int loByte) {
        hiByte = hiByte & 0xff;
        hiByte = hiByte << 8;
        loByte = loByte & 0xff;
        int value = hiByte + loByte;
        return Integer.toHexString(0x10000 | value).substring(1).toUpperCase();
    }

    public static int cameraModeToCameraIndex(final int cameraMode) {
        final int hiByte = (cameraMode & 0xff00) >> 8;
        if (hiByte == 0xff) {
            return -1;
        }
        return mCameraSizesSet.get(hiByte).getCameraIndex();
    }
}