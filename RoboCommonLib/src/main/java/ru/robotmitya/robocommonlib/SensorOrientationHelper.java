package ru.robotmitya.robocommonlib;

import com.badlogic.gdx.math.Quaternion;

/**
 * Don't want to mix these functions with SensorOrientation.
 * Created by dmitrydzz on 02.11.14.
 */
public class SensorOrientationHelper {

    public static void calibrate(final SensorOrientation sensorOrientation) {
        sensorOrientation.calibrate(new Quaternion(new com.badlogic.gdx.math.Vector3(0, 0, 1), 90));
    }
}
