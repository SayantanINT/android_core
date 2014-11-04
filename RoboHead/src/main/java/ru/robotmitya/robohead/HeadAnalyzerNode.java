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

/**
 * Created by dmitrydzz on 4/12/14.
 *
 */
public class HeadAnalyzerNode implements NodeMain {
    private static final float SMOOTH_FACTOR = 0.9f;

    private Publisher<std_msgs.String> mBodyPublisher;

    private final Vector2 mPosition = new Vector2();
    private Vector2 mTargetPosition;

    private int mControlMode = AppConst.Common.ControlMode.TWO_JOYSTICKS;

    private SensorOrientation mSensorOrientation;
    private Timer mPublisherTimer;
    private boolean mStartingSensorOrientation;
    private volatile float mCurrentAzimuth = 0;
    private volatile float mCurrentPitch = 0;

    private volatile float mTargetAzimuth = 0;
    private volatile float mTargetPitch = 0;

    private volatile boolean mCalibrating = false;

    public HeadAnalyzerNode(Context context, int screenRotation) {
        mSensorOrientation = new SensorGyroscopeGravityOrientation(context, 0.98f);
        mSensorOrientation.setRotation(screenRotation);
        mSensorOrientation.setCalibrationEnabled(true);
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(AppConst.RoboHead.HEAD_ANALYZER_NODE);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        mSensorOrientation.start();
        mStartingSensorOrientation = true;
        mPublisherTimer = new Timer();
        mPublisherTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mStartingSensorOrientation) {
                    calibrate();
                    mStartingSensorOrientation = false;
                }

                Quaternion q = mSensorOrientation.getDeviceToWorld();

                mCurrentAzimuth = q.getYaw() + RoboState.getHeadHorizontalZeroDegree();
                if (mCurrentAzimuth < RoboState.getHeadHorizontalServoMinDegree()) {
                    mCurrentAzimuth = RoboState.getHeadHorizontalServoMinDegree();
                } else if (mCurrentAzimuth > RoboState.getHeadHorizontalServoMaxDegree()) {
                    mCurrentAzimuth = RoboState.getHeadHorizontalServoMaxDegree();
                }

                mCurrentPitch = -q.getPitch() + RoboState.getHeadVerticalZeroDegree();
                if (mCurrentPitch < RoboState.getHeadVerticalServoMinDegree()) {
                    mCurrentPitch = RoboState.getHeadVerticalServoMinDegree();
                } else if (mCurrentPitch > RoboState.getHeadVerticalServoMaxDegree()) {
                    mCurrentPitch = RoboState.getHeadVerticalServoMaxDegree();
                }



                Vector2 t = new Vector2(mTargetAzimuth, mTargetPitch);
                Vector2 c = new Vector2(mCurrentAzimuth, mCurrentPitch);
//                Log.d(HeadAnalyzerNode.this, "+++ t: " + Log.fmt(t) + "   c: " + Log.fmt(c));


                if (mControlMode == AppConst.Common.ControlMode.ORIENTATION) {
                    // Zero delta means don't move.
                    final float azimuthDelta = mCalibrating ? 0 : mTargetAzimuth - mCurrentAzimuth;
                    final float pitchDelta = mCalibrating ? 0 : mTargetPitch - mCurrentPitch;

                    Vector2 delta = new Vector2(azimuthDelta, pitchDelta);
                    Log.d(HeadAnalyzerNode.this, String.format("+++ t: %s  c: %s  d: %s", Log.fmt(t), Log.fmt(c), Log.fmt(delta)));

                    if (!mCalibrating) {
                        moveHead(azimuthDelta, pitchDelta);
                    }
                }
            }
        }, 2000, 40);

        mBodyPublisher = connectedNode.newPublisher(AppConst.RoboHead.BODY_TOPIC, std_msgs.String._TYPE);

        Subscriber<geometry_msgs.Twist> twistSubscriber = connectedNode.newSubscriber(AppConst.RoboHead.HEAD_TOPIC, geometry_msgs.Twist._TYPE);
        twistSubscriber.addMessageListener(createTwistMessageListener());

        Subscriber<std_msgs.String> commandSubscriber = connectedNode.newSubscriber(AppConst.RoboHead.CONTROL_MODE_TOPIC, std_msgs.String._TYPE);
        commandSubscriber.addMessageListener(createCommandMessageListener());

//        for (int i = 0; i <= 200; i++) {
//            final short period = getRotatePeriod(i);
//            Log.d(this, "####\t" + i + "\t" + period);
//        }
    }

    @Override
    public void onShutdown(Node node) {
        mSensorOrientation.stop();

        mPublisherTimer.cancel();
        mPublisherTimer.purge();
    }

    @Override
    public void onShutdownComplete(Node node) {
    }

    @Override
    public void onError(Node node, Throwable throwable) {
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

                if (mControlMode == AppConst.Common.ControlMode.TWO_JOYSTICKS) {
                    positionHead(mTargetAzimuth, mTargetPitch);
                }
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
        std_msgs.String message = mBodyPublisher.newMessage();
        message.setData(command);
        mBodyPublisher.publish(message);
        Log.messagePublished(this, mBodyPublisher.getTopicName().toString(), command);
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

    private void calibrate() {
        mCalibrating = true;

        positionHead(RoboState.getHeadHorizontalZeroDegree(), RoboState.getHeadVerticalZeroDegree());

        Timer waitHeadServos = new Timer();
        waitHeadServos.schedule(new TimerTask() {
            @Override
            public void run() {
                SensorOrientationHelper.calibrate(mSensorOrientation);
            }
        }, 3000);

        Timer stopCalibrationDelay = new Timer();
        stopCalibrationDelay.schedule(new TimerTask() {
            @Override
            public void run() {
                mTargetAzimuth = mCurrentAzimuth;
                mTargetPitch = mCurrentPitch;
                mCalibrating = false;
            }
        }, 4000);
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
        final short horizontalPeriod = getRotatePeriod(horizontalDeltaDegree);
        final String horizontalCommand = MessageHelper.makeMessage(Rs.HeadHorizontalRotationPeriod.ID, horizontalPeriod);
        Log.d(this, "++ H ++ delta: " + MathUtils.round(horizontalDeltaDegree) + ", period: " + horizontalPeriod);

        final short verticalPeriod = getRotatePeriod(verticalDeltaDegree);
        final String verticalCommand = MessageHelper.makeMessage(Rs.HeadVerticalRotationPeriod.ID, verticalPeriod);
        Log.d(this, "++ V ++ delta: " + MathUtils.round(verticalDeltaDegree) + ", period: " + verticalPeriod);

        publishCommand(horizontalCommand);
        publishCommand(verticalCommand);
    }

    private static final float MIN_DEGREE = 2;
    private static final float MAX_DEGREE = 180;
//    private static final float MIN_PERIOD = 46080;
//    private static final float MAX_PERIOD = 1280;

    private static final float MIN_PERIOD = 22000;
    private static final float MAX_PERIOD = 1280;
    private static final float ELLIPSE_A = MAX_DEGREE - MIN_DEGREE;
    private static final float ELLIPSE_A2 = ELLIPSE_A * ELLIPSE_A;
    private static final float ELLIPSE_B = MIN_PERIOD - MAX_PERIOD;

    private short getRotatePeriod(float deltaDegree) {
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
}
