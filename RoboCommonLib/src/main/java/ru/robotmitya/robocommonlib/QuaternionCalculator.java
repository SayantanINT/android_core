package ru.robotmitya.robocommonlib;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

/**
 * Вычислитель некоторых потокоопасных функций в Quaternion.
 * Каждый экземпляр этого класса можно использовать только в одном потоке.
 */
public class QuaternionCalculator {
    private final Quaternion tmp1 = new Quaternion(0, 0, 0, 0);
    private final Quaternion tmp2 = new Quaternion(0, 0, 0, 0);

    public void transform(final Quaternion quaternion, final Vector3 v) {
        tmp2.set(quaternion);
        tmp2.conjugate();
        tmp2.mulLeft(tmp1.set(v.x, v.y, v.z, 0)).mulLeft(quaternion);

        v.x = tmp2.x;
        v.y = tmp2.y;
        v.z = tmp2.z;
    }

    public void slerp(Quaternion quaternion, Quaternion end, float alpha) {
        final float dot = quaternion.dot(end);
        float absDot = dot < 0.f ? -dot : dot;

        // Set the first and second scale for the interpolation
        float scale0 = 1 - alpha;
        float scale1 = alpha;

        // Check if the angle between the 2 quaternions was big enough to
        // warrant such calculations
        if ((1 - absDot) > 0.1) {// Get the angle between the 2 quaternions,
            // and then store the sin() of that angle
            final double angle = Math.acos(absDot);
            final double invSinTheta = 1f / Math.sin(angle);

            // Calculate the scale for q1 and q2, according to the angle and
            // it's sine value
            scale0 = (float)(Math.sin((1 - alpha) * angle) * invSinTheta);
            scale1 = (float)(Math.sin((alpha * angle)) * invSinTheta);
        }

        if (dot < 0.f) scale1 = -scale1;

        // Calculate the x, y, z and w values for the quaternion by using a
        // special form of linear interpolation for quaternions.
        quaternion.x = (scale0 * quaternion.x) + (scale1 * end.x);
        quaternion.y = (scale0 * quaternion.y) + (scale1 * end.y);
        quaternion.z = (scale0 * quaternion.z) + (scale1 * end.z);
        quaternion.w = (scale0 * quaternion.w) + (scale1 * end.w);
    }
}
