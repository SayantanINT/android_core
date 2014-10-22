package ru.robotmitya.roboboard;

import android.content.BroadcastReceiver;
import android.content.Context;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import java.util.Timer;
import java.util.TimerTask;

import geometry_msgs.Twist;
import ru.robotmitya.robocommonlib.*;

/**
 * Created by dmitrydzz on 5/25/14.
 *
 */
public class BoardOrientationNode implements NodeMain {
    private Context mContext;

    private BroadcastReceiver mBroadcastReceiverCalibrate;
    private BroadcastReceiver mBroadcastReceiverActivate;
    private Intent mPointerPositionIntent = new Intent(AppConst.RoboBoard.Broadcast.ORIENTATION_POINTER_POSITION);

    private Publisher<Twist> mPublisher;
    private SensorOrientation mSensorOrientation;
    private Timer mPublisherTimer;
    private boolean mStarting;
    private boolean mEnabled;

    /**
     * @param screenRotation - 0, 90, 180 or 270.
     */
    public BoardOrientationNode(Context context, int screenRotation) {
        mContext = context;
        mSensorOrientation = new SensorGyroscopeMagneticOrientation(context, 0.98f);
        mSensorOrientation.setRotation(screenRotation);
        mSensorOrientation.setCalibrationEnabled(true);
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(AppConst.RoboBoard.ORIENTATION_NODE);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        mEnabled = false;

        mPublisher = connectedNode.newPublisher(AppConst.RoboHead.HEAD_JOYSTICK_TOPIC, Twist._TYPE);

        mSensorOrientation.start();
        mStarting = true;

        mPublisherTimer = new Timer();
        mPublisherTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mStarting) {
                    calibrate(false);
                    mStarting = false;
                }

                Quaternion q = mSensorOrientation.getDeviceToWorld();

                final float horizontalRange = RoboState.getHeadHorizontalServoMaxRad() - RoboState.getHeadHorizontalServoMinRad();
                final float verticalRange = RoboState.getHeadVerticalServoMaxRad() - RoboState.getHeadVerticalServoMinRad();

                float azimuthValue = q.getYawRad() * 2 / horizontalRange;
                if (azimuthValue > 1) {
                    azimuthValue = 1;
                } else if (azimuthValue < -1) {
                    azimuthValue = -1;
                }

                float pitchValue = q.getPitchRad() * 2 / verticalRange;
                if (pitchValue > 1) {
                    pitchValue = 1;
                } else if (pitchValue < -1) {
                    pitchValue = -1;
                }

                mPointerPositionIntent.putExtra(AppConst.RoboBoard.Broadcast.ORIENTATION_POINTER_POSITION_EXTRA_AZIMUTH, azimuthValue);
                mPointerPositionIntent.putExtra(AppConst.RoboBoard.Broadcast.ORIENTATION_POINTER_POSITION_EXTRA_PITCH, pitchValue);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(mPointerPositionIntent);


                if (mEnabled) {
                    try {
                        Twist message = mPublisher.newMessage();
                        message.getAngular().setX(0);
                        message.getAngular().setY(0);
                        message.getAngular().setZ(azimuthValue);
                        message.getLinear().setX(pitchValue);
                        message.getLinear().setY(0);
                        message.getLinear().setZ(0);
                        mPublisher.publish(message);
                        Log.messagePublished(BoardOrientationNode.this, mPublisher.getTopicName().toString(), message.toString());
                    } catch (NullPointerException e) {
                        Log.e(this, e.getMessage());
                    }
                }
            }
        }, 2000, 40);

        mBroadcastReceiverCalibrate = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(BoardOrientationNode.this, "broadcast received: calibrate");
                calibrate(true);
            }
        };

        mBroadcastReceiverActivate = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(BoardOrientationNode.this, "broadcast received: activate");
                mEnabled = intent.getBooleanExtra(AppConst.RoboBoard.Broadcast.ORIENTATION_ACTIVATE_EXTRA_ENABLED, false);
            }
        };

        LocalBroadcastManager.getInstance(mContext).registerReceiver(
                mBroadcastReceiverCalibrate, new IntentFilter(AppConst.RoboBoard.Broadcast.ORIENTATION_CALIBRATE));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(
                mBroadcastReceiverActivate, new IntentFilter(AppConst.RoboBoard.Broadcast.ORIENTATION_ACTIVATE));
    }

    @Override
    public void onShutdown(Node node) {
        mEnabled = false;

        mSensorOrientation.stop();

        mPublisherTimer.cancel();
        mPublisherTimer.purge();

        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mBroadcastReceiverCalibrate);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mBroadcastReceiverActivate);
    }

    @Override
    public void onShutdownComplete(Node node) {
    }

    @Override
    public void onError(Node node, Throwable throwable) {
    }

    private void calibrate(boolean vibrate) {
        if (vibrate) {
            Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(50);
        }
        mSensorOrientation.calibrate(new Quaternion(new Vector3(0, 0, 1), 90));
    }
}
