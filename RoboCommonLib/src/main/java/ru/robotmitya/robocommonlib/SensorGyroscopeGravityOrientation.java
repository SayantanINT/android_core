package ru.robotmitya.robocommonlib;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

/**
 * Getting orientation of Android device. Class uses gravity sensor (TYPE_GRAVITY) and gyroscope (TYPE_GYROSCOPE).
 * The output returns in getDeviceToWorld() method.
 */
public class SensorGyroscopeGravityOrientation extends SensorGyroscopeOrientation {
    private static final float DEFAULT_GYROSCOPE_WEIGHT = 0.8f;
    private static final float GRAVITY_LOW_PASS_FACTOR = 0.6f;

    private final Vector3 mGravityDeviceVectorRaw = new Vector3(); // temp vector for mGravityDeviceVector calculation
    private final Vector3 mGravityDeviceVector = new Vector3(); // gravity vector in device's coords
    private final Vector3 mGravityWorldVector = new Vector3(); // gravity vector in world coords
    private final Vector3 mDesiredRotateVector = new Vector3(Vector3.X); // used when gravity and Z are opposite collinear

    private final Vector3 mTmpVector = new Vector3();
    private final Quaternion mTmpQuaternion = new Quaternion();

    private boolean mGravityDetected = false; // gyroscope needs gravity to be detected first

    private final Quaternion mGyroscopeQuaternion = new Quaternion(); // current gyroscope orientation
    private final Quaternion mDeviceToWorldRaw = new Quaternion(); // temp quaternion for mDeviceToWorld calculation

    private final QuaternionCalculator mCalculator = new QuaternionCalculator();

    /**
     * mGyroscopeWeight = [0..1]
     *      0 - gyroscope's quaternion fully corrected to gravity vector,
     *      1 - gyroscope's quaternion is not corrected to gravity vector.
     * Default value is DEFAULT_GYROSCOPE_WEIGHT.
     */
    private float mGyroscopeWeight;

    public SensorGyroscopeGravityOrientation(Context context, float gyroscopeWeight) {
        super(context);
        setGyroscopeWeight(gyroscopeWeight);
    }

    public SensorGyroscopeGravityOrientation(Context context) {
        this(context, DEFAULT_GYROSCOPE_WEIGHT);
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
        mGravityDetected = false;
    }

    @Override
    protected void registerListener() {
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorOrientation.SENSOR_DELAY);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorOrientation.SENSOR_DELAY);
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_GRAVITY: {
                // Getting gravity vector mGravityDeviceVector:
                mGravityDeviceVectorRaw.set(event.values[0], event.values[1], event.values[2]).nor();
                if (!mGravityDetected) {
                    mGravityDeviceVector.set(mGravityDeviceVectorRaw);
                    // Первоначальная инициализация кватерниона поворота:
                    mGyroscopeQuaternion.idt();
                    // Ось Z мировой системы координат должна быть сонаправлена с вектором гравитации:
                    addRotation(mGyroscopeQuaternion, Vector3.Z, mGravityDeviceVector, mDesiredRotateVector);
                    mGravityDetected = true;
                } else {
                    mGravityDeviceVector.slerp(mGravityDeviceVectorRaw, GRAVITY_LOW_PASS_FACTOR);
                }

                break;
            }
            case Sensor.TYPE_GYROSCOPE: {
                if (!mGravityDetected) {
                    return;
                }

                gyroscopeChanged(event, mGyroscopeQuaternion);

                // Ось Z со временем может отклониться от вектора гравитации, поэтому её необходимо корректировать:
                mGyroscopeQuaternion.nor(); // correction
                mDeviceToWorldRaw.set(mGyroscopeQuaternion);

                mGravityWorldVector.set(mGravityDeviceVector);
                mCalculator.transform(mDeviceToWorldRaw, mGravityWorldVector);

                addRotation(mDeviceToWorldRaw, Vector3.Z, mGravityWorldVector, mDesiredRotateVector); // correct Z to gravity
                mCalculator.slerp(mDeviceToWorldRaw, mGyroscopeQuaternion, mGyroscopeWeight); // not exactly to gravity
                mGyroscopeQuaternion.set(mDeviceToWorldRaw); // save new mGyroscopeQuaternion for next iteration

                updateToCalibrated(mDeviceToWorldRaw);
                finalScreenRotation(mDeviceToWorldRaw);
                setDeviceToWorld(mDeviceToWorldRaw);

                break;
            }
        }
    }

    // Дополнительный поворот для:
    // 1. Первоначальной устаноки мировой системы координат так, чтобы ось Z была сонаправлена с вектором графитации (вверх).
    // 2. Постоянная корректировка ошибки интегрирования показаний гироскопа. Полученная новая ориентация оси Z
    //    мировой системы координат постоянно подтягивается к вектору гравитации функцией lerp.
    private void addRotation(final Quaternion transformation, final Vector3 source, final Vector3 target,
                             final Vector3 rotateVectorIfCollinearOpposite) {
        if (source.isOnLine(target)) {
            // Если source и target сонаправлены, ничего не делать.
            if (source.hasSameDirection(target)) {
                return;
            }
            // Если source и target противонаправлены, повернуть на 180 вокруг rotateVectorIfCollinearOpposite.
            mTmpQuaternion.set(rotateVectorIfCollinearOpposite, 180);
        } else {
            // В остальных случаях, находим ось вращения (векторное произведение source и target.
            // И поворачиваем на угол между source и target.
            mTmpVector.set(source);
            mTmpVector.crs(target);
            final float angle = - MathUtils.radiansToDegrees * MathUtils.atan2(mTmpVector.len(), source.dot(target));
            mTmpQuaternion.set(mTmpVector, angle);

//            //todo #45 убрать
//            if (Math.abs(angle) > 10) {
//                Log.d(this, "+++ angle=" + angle +
//                        "\t source=" + Log.fmt(source) +
//                        "\t target=" + Log.fmt(target) +
//                        "\t curr=" + Log.fmt(mGravityDeviceVectorRaw));
//            }
        }
        // Дополняем трансформацию этим поворотом.
        transformation.mulLeft(mTmpQuaternion);
    }
}
