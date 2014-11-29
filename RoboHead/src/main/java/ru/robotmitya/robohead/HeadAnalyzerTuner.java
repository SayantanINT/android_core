package ru.robotmitya.robohead;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import ru.robotmitya.robocommonlib.Log;

/**
 *
 *  Command line sample:
 *  >_  adb shell am broadcast -a ru.robotmitya.robohead.HeadAnalyzerTuner.Calibrate
 *  >_  adb shell am broadcast -a ru.robotmitya.robohead.HeadAnalyzerTuner.PositionHead --ei h 19 --ei v 74
 *  >_  adb shell am broadcast -a ru.robotmitya.robohead.HeadAnalyzerTuner.SetTarget --es h "19.74" --es v "10.01"
 *  >_  adb shell am broadcast -a ru.robotmitya.robohead.HeadAnalyzerTuner.ActivatePidTest --ez e true
 *  >_  adb shell am broadcast -a ru.robotmitya.robohead.HeadAnalyzerTuner.SetHorizontalPidFactors --es Kp "197.4" --es Ki "19.74" --es Kd "1.974"
 *  >_  adb shell am broadcast -a ru.robotmitya.robohead.HeadAnalyzerTuner.SetHorizontalPidFactors --es addKp "0.1" --es addKi "0.2" --es addKd "0.3"
 *  >_  adb shell am broadcast -a ru.robotmitya.robohead.HeadAnalyzerTuner.SetVerticalPidFactors --es Kp "197.4" --es Ki "19.74" --es Kd "1.974"
 *  >_  adb shell am broadcast -a ru.robotmitya.robohead.HeadAnalyzerTuner.SetVerticalPidFactors --es addKp "0.1" --es addKi "0.2" --es addKd "0.3"
 *
 *  Note: --ef doesn't work in Android 4.0.3. I guess it was added in 4.2. So I have to use --es with string typed values.
 *
 * Created by dmitrydzz on 07.11.14.
 */
public class HeadAnalyzerTuner extends BroadcastReceiver {

    public static class Intent {
        public static class Calibrate {
            public static final String ACTION = "ru.robotmitya.robohead.HeadAnalyzerTuner.Calibrate";
        }

        public static class PositionHead {
            public static final String ACTION = "ru.robotmitya.robohead.HeadAnalyzerTuner.PositionHead";
            public static final String EXTRA_HORIZONTAL = "h";
            public static final String EXTRA_VERTICAL = "v";
        }

        public static class SetTarget {
            public static final String ACTION = "ru.robotmitya.robohead.HeadAnalyzerTuner.SetTarget";
            public static final String EXTRA_HORIZONTAL = "h";
            public static final String EXTRA_VERTICAL = "v";
        }

        public static class ActivatePidTest {
            public static final String ACTION = "ru.robotmitya.robohead.HeadAnalyzerTuner.ActivatePidTest";
            public static final String EXTRA_ENABLED = "e";
        }

        public static class SetHorizontalPidFactors {
            public static final String ACTION = "ru.robotmitya.robohead.HeadAnalyzerTuner.SetHorizontalPidFactors";
            public static final String EXTRA_KP = "Kp";
            public static final String EXTRA_KI = "Ki";
            public static final String EXTRA_KD = "Kd";
            public static final String EXTRA_ADD_KP = "addKp";
            public static final String EXTRA_ADD_KI = "addKi";
            public static final String EXTRA_ADD_KD = "addKd";
        }

        public static class SetVerticalPidFactors {
            public static final String ACTION = "ru.robotmitya.robohead.HeadAnalyzerTuner.SetVerticalPidFactors";
            public static final String EXTRA_KP = "Kp";
            public static final String EXTRA_KI = "Ki";
            public static final String EXTRA_KD = "Kd";
            public static final String EXTRA_ADD_KP = "addKp";
            public static final String EXTRA_ADD_KI = "addKi";
            public static final String EXTRA_ADD_KD = "addKd";
        }
    }

    private HeadAnalyzerNode mHeadAnalyzerNode;

    public void setHeadAnalyzerNode(final HeadAnalyzerNode headAnalyzerNode) {
        mHeadAnalyzerNode = headAnalyzerNode;
    }

    public void start(final Context context) {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.Calibrate.ACTION);
        intentFilter.addAction(Intent.PositionHead.ACTION);
        intentFilter.addAction(Intent.SetTarget.ACTION);
        intentFilter.addAction(Intent.ActivatePidTest.ACTION);
        intentFilter.addAction(Intent.SetHorizontalPidFactors.ACTION);
        intentFilter.addAction(Intent.SetVerticalPidFactors.ACTION);
        context.registerReceiver(this, intentFilter);
    }

    public void stop(final Context context) {
        try {
            context.unregisterReceiver(this);
        } catch (Exception e) {
            Log.e(this, e.getMessage(), e);
        }
    }

    @Override
    public void onReceive(Context context, android.content.Intent intent) {
        final String action = intent.getAction();
        if (action.equals(Intent.Calibrate.ACTION)) {
            mHeadAnalyzerNode.calibrate();
        } else if (action.equals(Intent.PositionHead.ACTION)) {
            final int azimuthDegree = intent.getIntExtra(Intent.PositionHead.EXTRA_HORIZONTAL, 90);
            final int pitchDegree = intent.getIntExtra(Intent.PositionHead.EXTRA_VERTICAL, 45);
            mHeadAnalyzerNode.setHead(azimuthDegree, pitchDegree);
        } else if (action.equals(Intent.SetTarget.ACTION)) {
            float azimuthDegree = (float) textToDouble(intent.getStringExtra(Intent.SetTarget.EXTRA_HORIZONTAL), 90);
            final float pitchDegree = (float) textToDouble(intent.getStringExtra(Intent.SetTarget.EXTRA_VERTICAL), 45);
            mHeadAnalyzerNode.setTarget(azimuthDegree, pitchDegree);
        } else if (action.equals(Intent.ActivatePidTest.ACTION)) {
            final boolean enabled = intent.getBooleanExtra(Intent.ActivatePidTest.EXTRA_ENABLED, false);
            mHeadAnalyzerNode.activatePidTest(enabled);
        } else if (action.equals(Intent.SetHorizontalPidFactors.ACTION)) {
            if (intent.hasExtra(Intent.SetHorizontalPidFactors.EXTRA_KP)) {
                final double kp = textToDouble(intent.getStringExtra(Intent.SetHorizontalPidFactors.EXTRA_KP), 0);
                mHeadAnalyzerNode.setHorizontalKp(kp);
            }
            if (intent.hasExtra(Intent.SetHorizontalPidFactors.EXTRA_KI)) {
                final double ki = textToDouble(intent.getStringExtra(Intent.SetHorizontalPidFactors.EXTRA_KI), 0);
                mHeadAnalyzerNode.setHorizontalKi(ki);
            }
            if (intent.hasExtra(Intent.SetHorizontalPidFactors.EXTRA_KD)) {
                final double kd = textToDouble(intent.getStringExtra(Intent.SetHorizontalPidFactors.EXTRA_KD), 0);
                mHeadAnalyzerNode.setHorizontalKd(kd);
            }

            if (intent.hasExtra(Intent.SetHorizontalPidFactors.EXTRA_ADD_KP)) {
                final double deltaKp = textToDouble(intent.getStringExtra(Intent.SetHorizontalPidFactors.EXTRA_ADD_KP), 0);
                mHeadAnalyzerNode.addHorizontalKp(deltaKp);
            }
            if (intent.hasExtra(Intent.SetHorizontalPidFactors.EXTRA_ADD_KI)) {
                final double deltaKi = textToDouble(intent.getStringExtra(Intent.SetHorizontalPidFactors.EXTRA_ADD_KI), 0);
                mHeadAnalyzerNode.addHorizontalKi(deltaKi);
            }
            if (intent.hasExtra(Intent.SetHorizontalPidFactors.EXTRA_ADD_KD)) {
                final double deltaKd = textToDouble(intent.getStringExtra(Intent.SetHorizontalPidFactors.EXTRA_ADD_KD), 0);
                mHeadAnalyzerNode.addHorizontalKd(deltaKd);
            }
        } else if (action.equals(Intent.SetVerticalPidFactors.ACTION)) {
            if (intent.hasExtra(Intent.SetVerticalPidFactors.EXTRA_KP)) {
                final double kp = textToDouble(intent.getStringExtra(Intent.SetVerticalPidFactors.EXTRA_KP), 0);
                mHeadAnalyzerNode.setVerticalKp(kp);
            }
            if (intent.hasExtra(Intent.SetVerticalPidFactors.EXTRA_KI)) {
                final double ki = textToDouble(intent.getStringExtra(Intent.SetVerticalPidFactors.EXTRA_KI), 0);
                mHeadAnalyzerNode.setVerticalKi(ki);
            }
            if (intent.hasExtra(Intent.SetVerticalPidFactors.EXTRA_KD)) {
                final double kd = textToDouble(intent.getStringExtra(Intent.SetVerticalPidFactors.EXTRA_KD), 0);
                mHeadAnalyzerNode.setVerticalKd(kd);
            }

            if (intent.hasExtra(Intent.SetVerticalPidFactors.EXTRA_ADD_KP)) {
                final double deltaKp = textToDouble(intent.getStringExtra(Intent.SetVerticalPidFactors.EXTRA_ADD_KP), 0);
                mHeadAnalyzerNode.addVerticalKp(deltaKp);
            }
            if (intent.hasExtra(Intent.SetVerticalPidFactors.EXTRA_ADD_KI)) {
                final double deltaKi = textToDouble(intent.getStringExtra(Intent.SetVerticalPidFactors.EXTRA_ADD_KI), 0);
                mHeadAnalyzerNode.addVerticalKi(deltaKi);
            }
            if (intent.hasExtra(Intent.SetVerticalPidFactors.EXTRA_ADD_KD)) {
                final double deltaKd = textToDouble(intent.getStringExtra(Intent.SetVerticalPidFactors.EXTRA_ADD_KD), 0);
                mHeadAnalyzerNode.addVerticalKd(deltaKd);
            }
        }
    }

    /**
     *
     * @param value dot separated float number text.
     * @return parsed float value of the default value.
     */
    private double textToDouble(final String value, final double defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
