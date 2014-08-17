package ru.robotmitya.robocommonlib;

/**
 * Created by dmitrydzz on 3/23/14.
 *
 */
public class RoboState {
    private static short mHeadlights;

    private static short mSelectedCamIndex;

    private static boolean mIsReverse;

    private static short mMainAccumulatorCharging;
    private static short mRoboHeadAccumulatorCharging;

    private static short mRoboHeadBatteryState;

    private final static short mHeadHorizontalServoMinDegree = 0;
    private final static short mHeadHorizontalServoMaxDegree = 180;
    private final static short mHeadVerticalServoMinDegree = 0;
    private final static short mHeadVerticalServoMaxDegree = 90;

    private static short mMood;

    private static double mHorizontalZeroOrientation;
    private static double mVerticalZeroOrientation;

    static {
        mHeadlights = Rs.Instruction.HEADLIGHTS_OFF;

        mSelectedCamIndex = -1;

        mIsReverse = false;

        mMainAccumulatorCharging = Rs.Instruction.ACCUMULATOR_MAIN_CHARGING_STOP;
        mRoboHeadAccumulatorCharging = Rs.Instruction.ACCUMULATOR_ROBOHEAD_CHARGING_STOP;

        mRoboHeadBatteryState = Rs.BatteryResponse.ROBOHEAD_BATTERY;

        mMood = Rs.Mood.FACE_OK;

        double horizontalZeroOrientationInRadians = 0.0;
        mHorizontalZeroOrientation = horizontalZeroOrientationInRadians * 1.0 / Math.PI;
        double verticalZeroOrientationInRadians = Math.PI / 2.0;
        mVerticalZeroOrientation = verticalZeroOrientationInRadians * 1.0 / Math.PI;
    }


    // Headlights

    public static boolean setHeadlights(final short value) {
        if (value != mHeadlights) {
            mHeadlights = value;
            return true;
        }
        return false;
    }

    public static short getHeadlights() {
        return mHeadlights;
    }


    // Selected camera index

    public static boolean setSelectedCamIndex(final short value) {
        if (value != mSelectedCamIndex) {
            mSelectedCamIndex = value;
            mIsReverse = mSelectedCamIndex == AppConst.Common.Camera.BACK;
            return true;
        }
        return false;
    }

    public static short getSelectedCamIndex() {
        return mSelectedCamIndex;
    }

    public static boolean getIsReverse() {
        return mIsReverse;
    }


    // Main accumulator charge

    public static boolean setMainAccumulatorCharging(final short value) {
        if (value != mMainAccumulatorCharging) {
            mMainAccumulatorCharging = value;
            return true;
        }
        return false;
    }

    public static short getMainAccumulatorCharging() {
        return mMainAccumulatorCharging;
    }


    // Phone accumulator charge

    public static boolean setRoboHeadAccumulatorCharging(final short value) {
        if (value != mRoboHeadAccumulatorCharging) {
            mRoboHeadAccumulatorCharging = value;
            return true;
        }
        return false;
    }

    public static short getRoboHeadAccumulatorCharging() {
        return mRoboHeadAccumulatorCharging;
    }


    // RoboHead battery state for ~2XYY response

    public static boolean setRoboHeadBatteryState(final short value) {
        if (value != mRoboHeadBatteryState) {
            mRoboHeadBatteryState = value;
            return true;
        }
        return false;
    }

    public static short getRoboHeadBatteryState() {
        return mRoboHeadBatteryState;
    }

    // Mood

    public static boolean setMood(final short value) {
        if (value != mMood) {
            mMood = value;
            return true;
        }
        return false;
    }

    public static short getMood() {
        return mMood;
    }

    public static short getHeadHorizontalServoMinDegree() {
        return mHeadHorizontalServoMinDegree;
    }

    public static short getHeadHorizontalServoMaxDegree() {
        return mHeadHorizontalServoMaxDegree;
    }

    public static short getHeadVerticalServoMinDegree() {
        return mHeadVerticalServoMinDegree;
    }

    public static short getHeadVerticalServoMaxDegree() {
        return mHeadVerticalServoMaxDegree;
    }


    // Zero orientation

    public static double getHorizontalZeroOrientation() {
        return mHorizontalZeroOrientation;
    }

    public static double getVerticalZeroOrientation() {
        return mVerticalZeroOrientation;
    }

}
