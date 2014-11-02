package ru.robotmitya.robocommonlib;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Surface;
import android.view.WindowManager;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class SensorOrientation implements SensorEventListener {
    protected static final int SENSOR_DELAY = SensorManager.SENSOR_DELAY_GAME;

    protected final SensorManager mSensorManager;

    private Boolean started = false;
    private Boolean stopped = false;

    private final ReentrantReadWriteLock mReadWriteLock = new ReentrantReadWriteLock();
    private Quaternion mDeviceToWorld = new Quaternion();

    private boolean mCalibrationEnabled = false;
    private Quaternion mCalibration = new Quaternion().idt();
    private Quaternion mCalibrationExtraRotation = new Quaternion().idt();
    private boolean mInitNewCalibration = false;

    /**
     * The rotation of the device with respect to its native orientation: 0, 90, 180 or 270 degrees.
     */
    protected int mRotation;
    private Vector3 mOrientationRotationAxis = new Vector3(Vector3.Z).scl(-1);
    private Quaternion mOrientationRotation = new Quaternion();//final rotation according to device's screen orientation

    public SensorOrientation(final Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }
    protected abstract void registerListener();

    public void start() {
        if (started) {
            return;
        }

        registerListener();

        started = true;
        stopped = false;
    }

    public void stop() {
        if (stopped) {
            return;
        }

        mSensorManager.unregisterListener(this);

        stopped = true;
        started = false;
    }

    public Quaternion getDeviceToWorld() {
        mReadWriteLock.readLock().lock();
        try {
            return mDeviceToWorld;
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    protected void setDeviceToWorld(final Quaternion deviceToWorld) {
        mReadWriteLock.writeLock().lock();
        try {
            mDeviceToWorld.set(deviceToWorld);
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Sets the rotation of the device with respect to its native orientation.
     * @param rotation in degrees. Should be 0, 90, 180 or 270.
     */
    public void setRotation(final int rotation) {
        mRotation = rotation;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    protected void finalScreenRotation(final Quaternion deviceToWorld) {
        if (mRotation != 0) {
            mOrientationRotation.set(mOrientationRotationAxis, mRotation);
            deviceToWorld.mul(mOrientationRotation);
        }
    }

    protected void updateToCalibrated(final Quaternion deviceToWorld) {
        if (!mCalibrationEnabled) {
            return;
        }

        if (mInitNewCalibration) {
            mCalibration.set(deviceToWorld).nor().conjugate();
            mInitNewCalibration = false;
        }

        deviceToWorld.mulLeft(mCalibration);
        deviceToWorld.mulLeft(mCalibrationExtraRotation);
    }

    @SuppressWarnings("unused")
    public void setCalibrationEnabled(boolean calibrationEnabled) {
        mCalibrationEnabled = calibrationEnabled;
    }

    @SuppressWarnings("unused")
    public void calibrate(Quaternion extraRotation) {
        if (!mCalibrationEnabled) {
            return;
        }

        mCalibrationExtraRotation.set(extraRotation);
        mInitNewCalibration = true;
    }

    @SuppressWarnings("unused")
    public void calibrate() {
        if (!mCalibrationEnabled) {
            return;
        }

        mCalibrationExtraRotation.idt();
        mInitNewCalibration = true;
    }

    @SuppressWarnings("unused")
    public static boolean hasMagneticSensor(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        return (magneticSensor != null) && (accelerometerSensor != null);
    }

    @SuppressWarnings("unused")
    public static boolean hasGyroscopeSensor(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null;
    }

    @SuppressWarnings("unused")
    public static boolean hasGravitySensor(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null;
    }

    @SuppressWarnings("unused")
    public static int getRotation (Context context) {
        int orientation;

        if (context instanceof Activity) {
            orientation = ((Activity)context).getWindowManager().getDefaultDisplay().getRotation();
        } else {
            orientation = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        }

        switch (orientation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                return 0;
        }
    }
}
