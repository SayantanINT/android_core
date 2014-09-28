package ru.robotmitya.robocommonlib;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Quaternion;

public class SensorMagneticOrientation extends SensorOrientation {
    private static final float LOW_PASS_CONST = 0.05f; // [0, 1] -> 1 - как есть, 0.01 - меняется ооочень медленно.

    private float mLowPassFactor = LOW_PASS_CONST;

    private float[] mAccelerometerValues = null;
    private float[] mMagneticValues = null;

    private float[] mRotationMatrixArray = new float[9];
    private Matrix3 mRotationMatrix = new Matrix3();

    private Quaternion mDeviceToWorldRaw = new Quaternion();
    private Quaternion mDeviceToWorldRawPrev = new Quaternion();
    private boolean mHasDeviceToWorldRawPrev = false;

    public SensorMagneticOrientation(Context context, float lowPassFactor) {
        super(context);
        setLowPassFactor(lowPassFactor);
    }

    public SensorMagneticOrientation(Context context) {
        this(context, LOW_PASS_CONST);
    }

    @Override
    protected void registerListener() {
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorOrientation.SENSOR_DELAY);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorOrientation.SENSOR_DELAY);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                if (mMagneticValues == null) {
                    mMagneticValues = new float[3];
                }
                System.arraycopy(event.values, 0, mMagneticValues, 0, 3);
                break;
            case Sensor.TYPE_ACCELEROMETER:
                if (mAccelerometerValues == null) {
                    mAccelerometerValues = new float[3];
                }
                System.arraycopy(event.values, 0, mAccelerometerValues, 0, 3);
                break;
            default:
                return;
        }

        if ((mAccelerometerValues == null) || (mMagneticValues == null)) {
            return;
        }

        SensorManager.getRotationMatrix(mRotationMatrixArray, null, event.values, mMagneticValues);
        mRotationMatrix.set(mRotationMatrixArray).transpose();
        mDeviceToWorldRaw.setFromMatrix(mRotationMatrix);

        if (mHasDeviceToWorldRawPrev) {
            mDeviceToWorldRawPrev.slerp(mDeviceToWorldRaw, mLowPassFactor);
        } else {
            mDeviceToWorldRawPrev.set(mDeviceToWorldRaw);
            mHasDeviceToWorldRawPrev = true;
        }

        mDeviceToWorldRaw.set(mDeviceToWorldRawPrev);

        updateToCalibrated(mDeviceToWorldRaw);
        finalScreenRotation(mDeviceToWorldRaw);
        setDeviceToWorld(mDeviceToWorldRaw);
    }

    @SuppressWarnings("unused")
    public float getLowPassFactor() {
        return mLowPassFactor;
    }

    @SuppressWarnings("unused")
    public void setLowPassFactor(float lowPassFactor) {
        mLowPassFactor = lowPassFactor;
        if (mLowPassFactor < 0) {
            mLowPassFactor = 0;
        }
        if (mLowPassFactor > 1) {
            mLowPassFactor = 1;
        }
    }
}
