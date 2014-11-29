package ru.robotmitya.robohead;

import android.content.Context;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import geometry_msgs.Twist;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import geometry_msgs.Vector3;
import ru.robotmitya.robocommonlib.*;

import java.lang.String;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Created by dmitrydzz on 4/12/14.
 *
 */
public class HeadAnalyzerNode implements NodeMain {
    private static final float SMOOTH_FACTOR = 0.9f;

    private Context mContext;

    private BluetoothBodyNode mBluetoothBodyNode; //todo #18 - delete this after fixing #18.

    private Publisher<std_msgs.String> mBodyPublisher;

    private final Vector2 mPosition = new Vector2();
    private Vector2 mTargetPosition;

    private int mControlMode = AppConst.Common.ControlMode.TWO_JOYSTICKS;

    private final boolean mHasOrientationSensors;
    private SensorOrientation mSensorOrientation;
    private ScheduledThreadPoolExecutor mPublisherExecutor;
    private boolean mStartingSensorOrientation;
    private volatile float mCurrentAzimuth = 0;
    private volatile float mCurrentPitch = 0;

    private volatile float mTargetAzimuth = 0;
    private volatile float mTargetPitch = 0;

    private volatile boolean mCalibrating = false;

    private final HeadAnalyzerTuner mHeadAnalyzerTuner = new HeadAnalyzerTuner();

    private PidController mHorizontalPid = new PidController("hor");
    private PidController mVerticalPid = new PidController("ver");

    private static final String HORIZONTAL_PID_PREFS_NAME = "HorizontalPid";
    private static final String VERTICAL_PID_PREFS_NAME = "VerticalPid";
    private static final String PID_KP_OPTION_NAME = "Kp";
    private static final String PID_KI_OPTION_NAME = "Ki";
    private static final String PID_KD_OPTION_NAME = "Kd";

    public HeadAnalyzerNode(Context context, int screenRotation, BluetoothBodyNode bluetoothBodyNode) {
        mContext = context;

        mHasOrientationSensors =
                SensorOrientation.hasGyroscopeSensor(mContext) && SensorOrientation.hasGravitySensor(mContext);

        mSensorOrientation = new SensorGyroscopeGravityOrientation(context, 0.98f);
        mSensorOrientation.setRotation(screenRotation);
        mSensorOrientation.setCalibrationEnabled(true);

        mHeadAnalyzerTuner.setHeadAnalyzerNode(this);

        mBluetoothBodyNode = bluetoothBodyNode; //todo #18 (delete this after fix)
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(AppConst.RoboHead.HEAD_ANALYZER_NODE);
    }

    private long mTimeTemp = 0;
    private static final long PID_PERIOD = 30;

    private class PidThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setPriority(Thread.MAX_PRIORITY);
            return thread;
        }
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        mSensorOrientation.start();
        mStartingSensorOrientation = true;

        mPublisherExecutor = new ScheduledThreadPoolExecutor(20);
        mPublisherExecutor.setThreadFactory(new PidThreadFactory());
        mPublisherExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                mainHandler();
            }
        }, 2000, PID_PERIOD, TimeUnit.MILLISECONDS);

        mBodyPublisher = connectedNode.newPublisher(AppConst.RoboHead.BODY_TOPIC, std_msgs.String._TYPE);

        Subscriber<geometry_msgs.Twist> twistSubscriber = connectedNode.newSubscriber(AppConst.RoboHead.HEAD_TOPIC, geometry_msgs.Twist._TYPE);
        twistSubscriber.addMessageListener(createTwistMessageListener());

        Subscriber<std_msgs.String> commandSubscriber = connectedNode.newSubscriber(AppConst.RoboHead.CONTROL_MODE_TOPIC, std_msgs.String._TYPE);
        commandSubscriber.addMessageListener(createCommandMessageListener());

        mHorizontalPid.setKp(SettingsFragment.getPidHeadHorizontalKp());
        mHorizontalPid.setKi(SettingsFragment.getPidHeadHorizontalKi());
        mHorizontalPid.setKd(SettingsFragment.getPidHeadHorizontalKd());
        mHorizontalPid.setInputRange(RoboState.getHeadHorizontalServoMinDegree(), RoboState.getHeadHorizontalServoMaxDegree());
        mHorizontalPid.setOutputRange(-1.0 / 128.0, 1.0 / 128.0);
        Log.d(this, String.format("Horizontal PID: Kp = %f  Ki = %f  Kd = %f",
                mHorizontalPid.getKp(), mHorizontalPid.getKi(), mHorizontalPid.getKd()));

        mVerticalPid.setKp(SettingsFragment.getPidHeadVerticalKp());
        mVerticalPid.setKi(SettingsFragment.getPidHeadVerticalKi());
        mVerticalPid.setKd(SettingsFragment.getPidHeadVerticalKd());
        mVerticalPid.setInputRange(RoboState.getHeadVerticalServoMinDegree(), RoboState.getHeadVerticalServoMaxDegree());
//        mVerticalPid.setInputRange(0, 180);
        mVerticalPid.setOutputRange(-1.0 / 128.0, 1.0 / 128.0);
        Log.d(this, String.format("Vertical PID: Kp = %f  Ki = %f  Kd = %f",
                mVerticalPid.getKp(), mVerticalPid.getKi(), mVerticalPid.getKd()));

        mHeadAnalyzerTuner.start(mContext);
    }

    @Override
    public void onShutdown(Node node) {
        mHeadAnalyzerTuner.stop(mContext);

        mSensorOrientation.stop();

        mPublisherExecutor.shutdownNow();

        stopMoving();
    }

    @Override
    public void onShutdownComplete(Node node) {
    }

    @Override
    public void onError(Node node, Throwable throwable) {
    }

    private void mainHandler() {
        if (!mHasOrientationSensors) {
            positionHead(mTargetAzimuth, mTargetPitch);
            return;
        }

        if (mStartingSensorOrientation) {
            calibrate();
            mStartingSensorOrientation = false;
        }

        Quaternion q = mSensorOrientation.getDeviceToWorld();

        float currentAzimuth = q.getYaw() + RoboState.getHeadHorizontalZeroDegree();
        if (currentAzimuth < RoboState.getHeadHorizontalServoMinDegree()) {
            currentAzimuth = RoboState.getHeadHorizontalServoMinDegree();
        } else if (currentAzimuth > RoboState.getHeadHorizontalServoMaxDegree()) {
            currentAzimuth = RoboState.getHeadHorizontalServoMaxDegree();
        }
        mCurrentAzimuth = currentAzimuth;

        float currentPitch = -q.getPitch() + SettingsFragment.getStraightAheadAngle();
        if (currentPitch < RoboState.getHeadVerticalServoMinDegree()) {
            currentPitch = RoboState.getHeadVerticalServoMinDegree();
        } else if (currentPitch > RoboState.getHeadVerticalServoMaxDegree()) {
            currentPitch = RoboState.getHeadVerticalServoMaxDegree();
        }
        mCurrentPitch = currentPitch;

        final boolean isPidEnabled = mControlMode == AppConst.Common.ControlMode.ORIENTATION;
        mHorizontalPid.setEnabled(isPidEnabled);
        mVerticalPid.setEnabled(isPidEnabled);
        if (isPidEnabled) {
            // Zero delta means don't move.
            final float azimuthDelta = mCalibrating ? 0 : mTargetAzimuth - mCurrentAzimuth;
            final float pitchDelta = mCalibrating ? 0 : mTargetPitch - mCurrentPitch;

            if (!mCalibrating) {
                moveHead(azimuthDelta, pitchDelta);
            }
        }

        final float time = (float) mTimeTemp / 1000f;
        Plot.send("headpos", time, mTargetAzimuth, mCurrentAzimuth, mTargetPitch, mCurrentPitch);

        mTimeTemp += PID_PERIOD;
    }

    private MessageListener<Twist> createTwistMessageListener() {
        return new MessageListener<geometry_msgs.Twist>() {
            @Override
            public void onNewMessage(geometry_msgs.Twist message) {
                // Ignore incoming messages when calibrating:
                if (mCalibrating) {
                    return;
                }

                Vector3 linear = message.getLinear();
                Vector3 angular = message.getAngular();
                float x = (float) -angular.getZ();
                float y = (float) -linear.getX();
                if (RoboState.getIsReverse()) {
                    y = -y;
                }
                if (x < -1) x = -1;
                else if (x > 1) x = 1;
                if (y < -1) y = -1;
                else if (y > 1) y = 1;
                mPosition.set(x, y);
                if (mTargetPosition == null) {
                    mTargetPosition = new Vector2(mPosition);
                } else {
                    if (mControlMode == AppConst.Common.ControlMode.TWO_JOYSTICKS) {
                        mTargetPosition.lerp(mPosition, SMOOTH_FACTOR);
                    } else {
                        mTargetPosition.set(mPosition);
                    }
                }

                Log.messageReceived(HeadAnalyzerNode.this, String.format("x=%.3f, y=%.3f", mTargetPosition.x, mTargetPosition.y));

                mTargetAzimuth = getHorizontalDegree(mTargetPosition);
                mTargetPitch = getVerticalDegree(mTargetPosition);
            }
        };
    }

    private MessageListener<std_msgs.String> createCommandMessageListener() {
        return new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {
                String messageBody = message.getData();

                Log.messageReceived(HeadAnalyzerNode.this, messageBody);

                final String identifier = MessageHelper.getMessageIdentifier(messageBody);
                final int value = MessageHelper.getMessageIntegerValue(messageBody);
                if (identifier.equals(Rs.Instruction.ID)) {
                    switch (value) {
                        case Rs.Instruction.CONTROL_MODE_TWO_JOYSTICKS:
                            mControlMode = AppConst.Common.ControlMode.TWO_JOYSTICKS;
                            break;
                        case Rs.Instruction.CONTROL_MODE_ORIENTATION:
                            mControlMode = AppConst.Common.ControlMode.ORIENTATION;
                            break;
                        case Rs.Instruction.CONTROL_MODE_CALIBRATE:
                            mControlMode = AppConst.Common.ControlMode.ORIENTATION;
                            calibrate();
                            break;
                    }
                }
            }
        };
    }

    private void publishCommand(final String command) {
/*
        std_msgs.String message = mBodyPublisher.newMessage();
        message.setData(command);
        mBodyPublisher.publish(message);
        Log.messagePublished(this, mBodyPublisher.getTopicName().toString(), command);
*/
        //todo #18 - Loss of ROS messages. So this is my _temporary_ solution:
        if (mBluetoothBodyNode != null) {
            mBluetoothBodyNode.sendViaBluetooth(command);
        }
    }

    private float getHorizontalDegree(final Vector2 pos) {
        final float min = RoboState.getHeadHorizontalServoMinDegree();
        final float max = RoboState.getHeadHorizontalServoMaxDegree();
        return ((1 - pos.x) * ((max - min) / 2)) + min;
    }

    private float getVerticalDegree(final Vector2 pos) {
        final float min = RoboState.getHeadVerticalServoMinDegree();
        final float max = RoboState.getHeadVerticalServoMaxDegree();
        return ((1 - pos.y) * ((max - min) / 2)) + min;
    }

    public void calibrate() {
        if (mCalibrating) {
            return;
        }

        Log.d(this, "Start calibrate " + System.currentTimeMillis());
        mCalibrating = true;

        stopMoving();
        positionHead(RoboState.getHeadHorizontalZeroDegree(), SettingsFragment.getStraightAheadAngle());

        Timer waitHeadServos = new Timer();
        waitHeadServos.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d(this, "3000 calibrate " + System.currentTimeMillis());
                SensorOrientationHelper.calibrate(mSensorOrientation);
            }
        }, 3000);

        Timer moveServosToZeroPosition = new Timer();
        moveServosToZeroPosition.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d(this, "4000 calibrate " + System.currentTimeMillis());
                positionHead(RoboState.getHeadHorizontalZeroDegree(), RoboState.getHeadVerticalZeroDegree());
            }
        }, 4000);

        Timer stopCalibrationDelay = new Timer();
        stopCalibrationDelay.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d(this, "5000 calibrate " + System.currentTimeMillis());
                mTargetAzimuth = mCurrentAzimuth;
                mTargetPitch = mCurrentPitch;
                mCalibrating = false;
                Log.d(HeadAnalyzerNode.this, "Stop calibrate");
            }
        }, 5000);
    }

    private void positionHead(final float horizontalDegree, final float verticalDegree) {
        final String horizontalCommand = MessageHelper.makeMessage(
                Rs.HeadHorizontalPosition.ID,
                (short) MathUtils.round(horizontalDegree));
        final String verticalCommand = MessageHelper.makeMessage(
                Rs.HeadVerticalPosition.ID,
                (short) MathUtils.round(verticalDegree));
        publishCommand(horizontalCommand);
        publishCommand(verticalCommand);
    }

    private void moveHead(final float horizontalDeltaDegree, final float verticalDeltaDegree) {
        mHorizontalPid.setTarget(mTargetAzimuth);
        mHorizontalPid.setInput(mCurrentAzimuth);
        final double horizontalSpeed = mHorizontalPid.performPid();
        final short horizontalPeriod = Math.abs(horizontalSpeed) < MathUtils.FLOAT_ROUNDING_ERROR ? 0 : (short) Math.round(1.0 / horizontalSpeed);
        final String horizontalCommand = MessageHelper.makeMessage(Rs.HeadHorizontalRotationPeriod.ID, horizontalPeriod);
        publishCommand(horizontalCommand);

        mVerticalPid.setTarget(mTargetPitch);
        mVerticalPid.setInput(mCurrentPitch);
        final double verticalSpeed = mVerticalPid.performPid();
        final short verticalPeriod = Math.abs(verticalSpeed) < MathUtils.FLOAT_ROUNDING_ERROR ? 0 : (short) Math.round(1.0 / verticalSpeed);
        final String verticalCommand = MessageHelper.makeMessage(Rs.HeadVerticalRotationPeriod.ID, verticalPeriod);
        publishCommand(verticalCommand);
    }

    private void stopMoving() {
        final String horizontalCommand = MessageHelper.makeMessage(Rs.HeadHorizontalRotationPeriod.ID, (short) 0);
        publishCommand(horizontalCommand);
        final String verticalCommand = MessageHelper.makeMessage(Rs.HeadVerticalRotationPeriod.ID, (short) 0);
        publishCommand(verticalCommand);
    }

/*
    private short getRotatePeriod(float deltaDegree) {
        final float MIN_DEGREE = 2;
        final float MAX_DEGREE = 180;
//        final float MIN_PERIOD = 46080;
//        final float MAX_PERIOD = 1280;
        final float MIN_PERIOD = 22000;
        final float MAX_PERIOD = 1280;
        final float ELLIPSE_A = MAX_DEGREE - MIN_DEGREE;
        final float ELLIPSE_A2 = ELLIPSE_A * ELLIPSE_A;
        final float ELLIPSE_B = MIN_PERIOD - MAX_PERIOD;

        final float sign = Math.signum(deltaDegree);

        deltaDegree = Math.abs(deltaDegree);
        if (deltaDegree < MIN_DEGREE) {
            return 0;
        } else if (deltaDegree > MAX_DEGREE) {
            deltaDegree = MAX_DEGREE;
        }

        final float delta = deltaDegree - MAX_DEGREE;

        float result = -ELLIPSE_B / ELLIPSE_A * (float) Math.sqrt(ELLIPSE_A2 - delta * delta) + MIN_PERIOD;
        result *= sign;
        result /= 10;
        return (short) MathUtils.round(result);
    }
*/

    public void setHorizontalKp(final double kp) {
        mHorizontalPid.setKp(kp);
        SettingsFragment.setPidHeadHorizontalKp(mContext, kp);
        Log.d(this, String.format("Horizontal Kp = %f", mHorizontalPid.getKp()));
    }

    public void setHorizontalKi(final double ki) {
        mHorizontalPid.setKi(ki);
        SettingsFragment.setPidHeadHorizontalKi(mContext, ki);
        Log.d(this, String.format("Horizontal Ki = %f", mHorizontalPid.getKi()));
    }

    public void setHorizontalKd(final double kd) {
        mHorizontalPid.setKd(kd);
        SettingsFragment.setPidHeadHorizontalKd(mContext, kd);
        Log.d(this, String.format("Horizontal Kd = %f", mHorizontalPid.getKd()));
    }

    public void addHorizontalKp(final double deltaKp) {
        mHorizontalPid.addKp(deltaKp);
        SettingsFragment.setPidHeadHorizontalKp(mContext, mHorizontalPid.getKp());
        Log.d(this, String.format("Horizontal Kp = %f", mHorizontalPid.getKp()));
    }

    public void addHorizontalKi(final double deltaKi) {
        mHorizontalPid.addKi(deltaKi);
        SettingsFragment.setPidHeadHorizontalKi(mContext, mHorizontalPid.getKi());
        Log.d(this, String.format("Horizontal Ki = %f", mHorizontalPid.getKi()));
    }

    public void addHorizontalKd(final double deltaKd) {
        mHorizontalPid.addKd(deltaKd);
        SettingsFragment.setPidHeadHorizontalKd(mContext, mHorizontalPid.getKd());
        Log.d(this, String.format("Horizontal Kd = %f", mHorizontalPid.getKd()));
    }

/////////////////////
    public void setVerticalKp(final double kp) {
        mVerticalPid.setKp(kp);
        SettingsFragment.setPidHeadVerticalKp(mContext, kp);
        Log.d(this, String.format("Vertical Kp = %f", mVerticalPid.getKp()));
    }

    public void setVerticalKi(final double ki) {
        mVerticalPid.setKi(ki);
        SettingsFragment.setPidHeadVerticalKi(mContext, ki);
        Log.d(this, String.format("Vertical Ki = %f", mVerticalPid.getKi()));
    }

    public void setVerticalKd(final double kd) {
        mVerticalPid.setKd(kd);
        SettingsFragment.setPidHeadVerticalKd(mContext, kd);
        Log.d(this, String.format("Vertical Kd = %f", mVerticalPid.getKd()));
    }

    public void addVerticalKp(final double deltaKp) {
        mVerticalPid.addKp(deltaKp);
        SettingsFragment.setPidHeadVerticalKp(mContext, mVerticalPid.getKp());
        Log.d(this, String.format("Vertical Kp = %f", mVerticalPid.getKp()));
    }

    public void addVerticalKi(final double deltaKi) {
        mVerticalPid.addKi(deltaKi);
        SettingsFragment.setPidHeadVerticalKi(mContext, mVerticalPid.getKi());
        Log.d(this, String.format("Vertical Ki = %f", mVerticalPid.getKi()));
    }

    public void addVerticalKd(final double deltaKd) {
        mVerticalPid.addKd(deltaKd);
        SettingsFragment.setPidHeadVerticalKd(mContext, mVerticalPid.getKd());
        Log.d(this, String.format("Vertical Kd = %f", mVerticalPid.getKd()));
    }
/////////////////////

//    private void saveFloatOption(final String preferencesName, final String optionName, final float value) {
//        SharedPreferences settings = mContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = settings.edit();
//        editor.putFloat(optionName, value);
//        editor.apply();
//    }

//    private float loadFloatOption(final String preferencesName, final String optionName, final float defaultValue) {
//        SharedPreferences settings = mContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
//        return settings.getFloat(optionName, defaultValue);
//    }

    public void setTarget(final float azimuthDegree, final float pitchDegree) {
        mTargetAzimuth = azimuthDegree;
        mHorizontalPid.setTarget(mTargetAzimuth);
        mTargetPitch = pitchDegree;
        mVerticalPid.setTarget(mTargetPitch);
        Log.d(this, String.format("Target azimuth: %f  Target pitch: %f", mTargetAzimuth, mTargetPitch));
    }

    public void setHead(final int azimuthDegree, final int pitchDegree) {
        positionHead(azimuthDegree, pitchDegree);
        Log.d(this, String.format("Head positioned to azimuth: %d  pitch: %d", azimuthDegree, pitchDegree));
    }

    public void activatePidTest(final boolean enabled) {
        if (enabled) {
            mControlMode = AppConst.Common.ControlMode.ORIENTATION;
        }
        mHorizontalPid.setEnabled(enabled);
        mVerticalPid.setEnabled(enabled);
        Log.d(this, "PID test " + (enabled ? "started" : "stopped"));
    }
}
