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
    public static String BROADCAST_BOARD_ORIENTATION_CALIBRATE = "ru.robotmitya.roboboard.BOARD-ORIENTATION-CALIBRATE";

    private static float PI_DIV_2 = MathUtils.PI;

    private Context mContext;

    private BroadcastReceiver mBroadcastReceiverCalibrate;

    private Publisher<Twist> mPublisher;
    private SensorOrientation mSensorOrientation;
    private Timer mPublisherTimer;
    private boolean mStarting;

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

                Vector3 x = new Vector3(1, 0, 0);
                Vector3 y = new Vector3(0, 1, 0);
                Vector3 z = new Vector3(0, 0, 1);
                Quaternion q = mSensorOrientation.getDeviceToWorld();
                q.transform(x);
                q.transform(y);
                q.transform(z);
                Log.d(this, "==== " +
                        "x(" + Log.fmt(x) + ") " +
                        "y(" + Log.fmt(y) + ") " +
                        "z(" + Log.fmt(z) + ")");

                @SuppressWarnings("SuspiciousNameCombination")
                float azimuthValue = MathUtils.atan2(z.x, z.z);
                azimuthValue = azimuthValue > PI_DIV_2 ? PI_DIV_2 : azimuthValue;
                float pitchValue = -MathUtils.atan2(z.y, z.z);
                pitchValue = pitchValue > PI_DIV_2 ? PI_DIV_2 : pitchValue;
                Log.d(this, "*** azimuth: " + azimuthValue + "   pitch: " + pitchValue);

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
        }, 2000, 80);

        mBroadcastReceiverCalibrate = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(BoardOrientationNode.this, "broadcast received: calibrate orientation ===");
                calibrate();
            }
        };

        LocalBroadcastManager.getInstance(mContext).registerReceiver(
                mBroadcastReceiverCalibrate, new IntentFilter(BoardOrientationNode.BROADCAST_BOARD_ORIENTATION_CALIBRATE));
    }

    @Override
    public void onShutdown(Node node) {
        mSensorOrientation.stop();

        mPublisherTimer.cancel();
        mPublisherTimer.purge();

        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mBroadcastReceiverCalibrate);
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
