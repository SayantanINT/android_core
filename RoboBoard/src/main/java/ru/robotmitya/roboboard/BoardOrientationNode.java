package ru.robotmitya.roboboard;

import android.content.BroadcastReceiver;
import android.content.Context;

import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import com.badlogic.gdx.math.MathUtils;
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
    private static float PI_DIV_2 = MathUtils.PI / 2f;
    private static float MIN_VALUE = -1f;
    private static float MAX_VALUE = 1f;

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
                    Log.d(this, "================");
                    calibrate();
                    mStarting = false;
                }

                Quaternion q = mSensorOrientation.getDeviceToWorld();

                float azimuthValue = q.getYawRad();
                if (azimuthValue > PI_DIV_2) {
                    azimuthValue = PI_DIV_2;
                } else if (azimuthValue < -PI_DIV_2) {
                    azimuthValue = -PI_DIV_2;
                }
                azimuthValue /= PI_DIV_2;

                float pitchValue = q.getPitchRad();
                if (pitchValue > PI_DIV_2) {
                    pitchValue = PI_DIV_2;
                } else if (pitchValue < -PI_DIV_2) {
                    pitchValue = -PI_DIV_2;
                }
                pitchValue /= PI_DIV_2;

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
                calibrate();
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

    private void calibrate() {
        mSensorOrientation.calibrate(new Quaternion(new Vector3(0, 0, 1), 90));
    }
}
