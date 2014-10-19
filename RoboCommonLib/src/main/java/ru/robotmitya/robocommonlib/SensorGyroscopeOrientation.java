package ru.robotmitya.robocommonlib;

import android.content.Context;
import android.hardware.SensorEvent;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;

public abstract class SensorGyroscopeOrientation extends SensorOrientation {
    // Fields to detect gyroscope angles:
    private long mLastTimestamp = 0;
    private static final double NANOSECONDS_PER_SECOND = 1.0f / 1000000000.0f;
    private final float[] mGyroscopeSpeed = new float[3];
    private final Quaternion mGyroscopeDeltaQuaternion = new Quaternion();

    public SensorGyroscopeOrientation(Context context) {
        super(context);
    }

    protected void gyroscopeChanged(final SensorEvent event, final Quaternion gyroscopeQuaternion) {
        if (mLastTimestamp != 0) {
            final double dT = (event.timestamp - mLastTimestamp) * NANOSECONDS_PER_SECOND;
            System.arraycopy(event.values, 0, mGyroscopeSpeed, 0, 3);
            getDeltaRotationFromGyroscope(mGyroscopeSpeed, mGyroscopeDeltaQuaternion, dT);
            gyroscopeQuaternion.mul(mGyroscopeDeltaQuaternion);
        }

        mLastTimestamp = event.timestamp;
    }

    /**
     * Calculates quaternion extra transformation based on gyroscope's angular velocities.
     * @param gyroValues angular velocities.
     * @param deltaRotation transformation.
     * @param timePassed time has passed.
     */
    private void getDeltaRotationFromGyroscope(float[] gyroValues, Quaternion deltaRotation, double timePassed) {
        double normValue0 = 0;
        double normValue1 = 0;
        double normValue2 = 0;

        // Calculate the angular speed of the sample
        final double omegaMagnitude =
                Math.sqrt(gyroValues[0] * gyroValues[0] +
                        gyroValues[1] * gyroValues[1] +
                        gyroValues[2] * gyroValues[2]);

        // Normalize the rotation vector if it's big enough to get the axis
        if (omegaMagnitude > MathUtils.FLOAT_ROUNDING_ERROR) {
            normValue0 = gyroValues[0] / omegaMagnitude;
            normValue1 = gyroValues[1] / omegaMagnitude;
            normValue2 = gyroValues[2] / omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        final double thetaOverTwo = omegaMagnitude * timePassed / 2.0;
        final double sinThetaOverTwo = Math.sin(thetaOverTwo);
        final double cosThetaOverTwo = Math.cos(thetaOverTwo);
        deltaRotation.set(
                (float) (sinThetaOverTwo * normValue0),
                (float) (sinThetaOverTwo * normValue1),
                (float) (sinThetaOverTwo * normValue2),
                (float) cosThetaOverTwo);
    }
}
