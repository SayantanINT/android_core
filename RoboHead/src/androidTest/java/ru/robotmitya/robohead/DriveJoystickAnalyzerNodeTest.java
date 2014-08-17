package ru.robotmitya.robohead;

import junit.framework.TestCase;

import org.junit.Test;

import ru.robotmitya.robocommonlib.MotorsSpeed;

/**
 * Created by dmitrydzz on 4/16/14.
 *
 */
public class DriveJoystickAnalyzerNodeTest extends TestCase {
    private double getX(final double r, final double angle) {
        return r * Math.cos(angle);
    }

    private double getY(final double r, final double angle) {
        return r * Math.sin(angle);
    }

    @Test
    public void testCalculateMotorsSpeedInQuadrant1() {
        double x;
        double y;
        MotorsSpeed motorsSpeed = new MotorsSpeed();
        double angle;

        // Angle = 0
        x = 1;
        y = 0;
        DriveJoystickAnalyzerNode.calculateMotorsSpeed(x, y, motorsSpeed);
        assertEquals(255, motorsSpeed.mLeft);
        assertEquals(-255, motorsSpeed.mRight);

        // Angle = Pi/8
        angle = Math.PI / 8;
        x = getX(1, angle);
        y = getY(1, angle);
        DriveJoystickAnalyzerNode.calculateMotorsSpeed(x, y, motorsSpeed);
        assertEquals(255, motorsSpeed.mLeft);
        assertEquals(-127, motorsSpeed.mRight);

        // Angle = Pi/4
        angle = Math.PI / 4;
        x = getX(1, angle);
        y = getY(1, angle);
        DriveJoystickAnalyzerNode.calculateMotorsSpeed(x, y, motorsSpeed);
        assertEquals(255, motorsSpeed.mLeft);
        assertEquals(0, motorsSpeed.mRight);

        // Angle = 3*Pi/8
        angle = 3 * Math.PI / 8;
        x = getX(1, angle);
        y = getY(1, angle);
        DriveJoystickAnalyzerNode.calculateMotorsSpeed(x, y, motorsSpeed);
        assertEquals(255, motorsSpeed.mLeft);
        assertEquals(128, motorsSpeed.mRight);
    }

    @Test
    public void testCalculateMotorsSpeedInQuadrant2() {
        double x;
        double y;
        MotorsSpeed motorsSpeed = new MotorsSpeed();
        double angle;

        // Angle = Pi/2
        x = 0;
        y = 1;
        DriveJoystickAnalyzerNode.calculateMotorsSpeed(x, y, motorsSpeed);
        assertEquals(255, motorsSpeed.mLeft);
        assertEquals(255, motorsSpeed.mRight);

        // Angle = 5*Pi/8
        angle = 5 * Math.PI / 8;
        x = getX(1, angle);
        y = getY(1, angle);
        DriveJoystickAnalyzerNode.calculateMotorsSpeed(x, y, motorsSpeed);
        assertEquals(128, motorsSpeed.mLeft);
        assertEquals(255, motorsSpeed.mRight);

        // Angle = 3*Pi/4
        angle = 3 * Math.PI / 4;
        x = getX(1, angle);
        y = getY(1, angle);
        DriveJoystickAnalyzerNode.calculateMotorsSpeed(x, y, motorsSpeed);
        assertEquals(0, motorsSpeed.mLeft);
        assertEquals(255, motorsSpeed.mRight);

        // Angle = 7*Pi/8
        angle = 7 * Math.PI / 8;
        x = getX(1, angle);
        y = getY(1, angle);
        DriveJoystickAnalyzerNode.calculateMotorsSpeed(x, y, motorsSpeed);
        assertEquals(-127, motorsSpeed.mLeft);
        assertEquals(255, motorsSpeed.mRight);
    }

    @Test
    public void testCalculateMotorsSpeedInQuadrant3() {
        double x;
        double y;
        MotorsSpeed motorsSpeed = new MotorsSpeed();
        double angle;

        // Angle = Pi
        x = -1;
        y = 0;
        DriveJoystickAnalyzerNode.calculateMotorsSpeed(x, y, motorsSpeed);
        assertEquals(-255, motorsSpeed.mLeft);
        assertEquals(255, motorsSpeed.mRight);

        // Angle = 9*Pi/8
        angle = 9 * Math.PI / 8;
        x = getX(1, angle);
        y = getY(1, angle);
        DriveJoystickAnalyzerNode.calculateMotorsSpeed(x, y, motorsSpeed);
        assertEquals(-255, motorsSpeed.mLeft);
        assertEquals(128, motorsSpeed.mRight);

        // Angle = 5*Pi/4
        angle = 5 * Math.PI / 4;
        x = getX(1, angle);
        y = getY(1, angle);
        DriveJoystickAnalyzerNode.calculateMotorsSpeed(x, y, motorsSpeed);
        assertEquals(-255, motorsSpeed.mLeft);
        assertEquals(0, motorsSpeed.mRight);

        // Angle = 11*Pi/8
        angle = 11 * Math.PI / 8;
        x = getX(1, angle);
        y = getY(1, angle);
        DriveJoystickAnalyzerNode.calculateMotorsSpeed(x, y, motorsSpeed);
        assertEquals(-255, motorsSpeed.mLeft);
        assertEquals(-127, motorsSpeed.mRight);
    }

    @Test
    public void testCalculateMotorsSpeedInQuadrant4() {
        double x;
        double y;
        MotorsSpeed motorsSpeed = new MotorsSpeed();
        double angle;

        // Angle = 3*Pi/2
        x = 0;
        y = -1;
        DriveJoystickAnalyzerNode.calculateMotorsSpeed(x, y, motorsSpeed);
        assertEquals(-255, motorsSpeed.mLeft);
        assertEquals(-255, motorsSpeed.mRight);

        // Angle = 13*Pi/8
        angle = 13 * Math.PI / 8;
        x = getX(1, angle);
        y = getY(1, angle);
        DriveJoystickAnalyzerNode.calculateMotorsSpeed(x, y, motorsSpeed);
        assertEquals(-127, motorsSpeed.mLeft);
        assertEquals(-255, motorsSpeed.mRight);

        // Angle = 7*Pi/4
        angle = 7 * Math.PI / 4;
        x = getX(1, angle);
        y = getY(1, angle);
        DriveJoystickAnalyzerNode.calculateMotorsSpeed(x, y, motorsSpeed);
        assertEquals(0, motorsSpeed.mLeft);
        assertEquals(-255, motorsSpeed.mRight);

        // Angle = 15*Pi/8
        angle = 15 * Math.PI / 8;
        x = getX(1, angle);
        y = getY(1, angle);
        DriveJoystickAnalyzerNode.calculateMotorsSpeed(x, y, motorsSpeed);
        assertEquals(127, motorsSpeed.mLeft);
        assertEquals(-255, motorsSpeed.mRight);
    }
}
