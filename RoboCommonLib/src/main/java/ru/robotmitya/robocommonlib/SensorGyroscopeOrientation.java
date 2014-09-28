package ru.robotmitya.robocommonlib;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

public class SensorGyroscopeOrientation extends SensorOrientation {
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

    private long mLastTimestamp = 0;
    private static final float EPSILON = 0.000000001f;
    private static final float NANOSECONDS_PER_SECOND = 1.0f / 1000000000.0f;
    private float[] mGyroscopeSpeed = new float[3];
    private Quaternion mGyroscopeDeltaQuaternion = new Quaternion();

    public SensorGyroscopeOrientation(Context context, float gyroscopeWeight) {
        super(context);
        mGyroscopeWeight = gyroscopeWeight;
    }

    public SensorGyroscopeOrientation(Context context) {
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

                if (mLastTimestamp != 0) {
                    final float dT = (event.timestamp - mLastTimestamp) * NANOSECONDS_PER_SECOND;
                    System.arraycopy(event.values, 0, mGyroscopeSpeed, 0, 3);
                    getDeltaRotationFromGyroscope(mGyroscopeSpeed, mGyroscopeDeltaQuaternion, dT / 2.0f);
                    mGyroscopeQuaternion.mul(mGyroscopeDeltaQuaternion);
                }

                mLastTimestamp = event.timestamp;

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

    private void getDeltaRotationFromGyroscope(float[] gyroValues, Quaternion deltaRotation, float timeFactor) {
        float normValue0 = 0;
        float normValue1 = 0;
        float normValue2 = 0;

        // Calculate the angular speed of the sample
        final float omegaMagnitude =
                (float) Math.sqrt(gyroValues[0] * gyroValues[0] +
                        gyroValues[1] * gyroValues[1] +
                        gyroValues[2] * gyroValues[2]);

        // Normalize the rotation vector if it's big enough to get the axis
        if (omegaMagnitude > EPSILON) {
            normValue0 = gyroValues[0] / omegaMagnitude;
            normValue1 = gyroValues[1] / omegaMagnitude;
            normValue2 = gyroValues[2] / omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        final float thetaOverTwo = omegaMagnitude * timeFactor;
        final float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
        final float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
        deltaRotation.set(
                sinThetaOverTwo * normValue0,
                sinThetaOverTwo * normValue1,
                sinThetaOverTwo * normValue2,
                cosThetaOverTwo);
    }
}
