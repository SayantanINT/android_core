package ru.robotmitya.robocommonlib;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import com.badlogic.gdx.math.Quaternion;

public class SensorGyroscopeMagneticOrientation extends SensorGyroscopeOrientation {
    private float[] mQuaternionArray = new float[4];

    private boolean mMagneticOrientationDetected = false; // gyroscope needs at least one magnetic orientation

    private Quaternion mMagneticQuaternion = new Quaternion(); // current magnetic orientation
    private Quaternion mGyroscopeQuaternion = new Quaternion(); // current gyroscope orientation
    private Quaternion mDeviceToWorldRaw = new Quaternion(); // final orientation = magnetic * (1 - mGyroscopeWeight) + gyroscope * mGyroscopeWeight;

    /**
     * mGyroscopeWeight = [0..1]. 0 - only TYPE_ROTATION_VECTOR, 1 - only TYPE_GYROSCOPE.
     * Default value is 0.98f.
     */
    private float mGyroscopeWeight;

    public SensorGyroscopeMagneticOrientation(Context context, float gyroscopeWeight) {
        super(context);
        mGyroscopeWeight = gyroscopeWeight;
    }

    public SensorGyroscopeMagneticOrientation(Context context) {
        this(context, 0.98f);
    }

    @SuppressWarnings("unused")
    public float getGyroscopeWeight() {
        return mGyroscopeWeight;
    }

    @SuppressWarnings("unused")
    public void setGyroscopeWeight(float gyroscopeWeight) {
        mGyroscopeWeight = gyroscopeWeight;
        if (mGyroscopeWeight < 0) {
            mGyroscopeWeight = 0;
        }
        if (mGyroscopeWeight > 1) {
            mGyroscopeWeight = 1;
        }
    }

    @Override
    public void start() {
        super.start();
        mMagneticOrientationDetected = false;
    }

    @Override
    protected void registerListener() {
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorOrientation.SENSOR_DELAY);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorOrientation.SENSOR_DELAY);
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ROTATION_VECTOR: {
                SensorManager.getQuaternionFromVector(mQuaternionArray, event.values);
                mMagneticQuaternion.set(mQuaternionArray[1], mQuaternionArray[2], mQuaternionArray[3], mQuaternionArray[0]);
                if (!mMagneticOrientationDetected) {
                    mGyroscopeQuaternion.set(mMagneticQuaternion);
                    mMagneticOrientationDetected = true;
                }

                break;
            }
            case Sensor.TYPE_GYROSCOPE: {
                if (!mMagneticOrientationDetected) {
                    return;
                }

                gyroscopeChanged(event, mGyroscopeQuaternion);

                break;
            }
            default:
                return;
        }

        mDeviceToWorldRaw.set(mMagneticQuaternion);
        mDeviceToWorldRaw.slerp(mGyroscopeQuaternion, mGyroscopeWeight);
        mGyroscopeQuaternion.set(mDeviceToWorldRaw);

        updateToCalibrated(mDeviceToWorldRaw);
        finalScreenRotation(mDeviceToWorldRaw);
        setDeviceToWorld(mDeviceToWorldRaw);
    }
}
