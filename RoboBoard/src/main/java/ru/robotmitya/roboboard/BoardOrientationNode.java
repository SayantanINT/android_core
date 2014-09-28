package ru.robotmitya.roboboard;

import android.content.Context;

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
    private Publisher<Twist> mPublisher;
    private SensorOrientation mSensorOrientation;
    private Timer mPublisherTimer;

    /**
     * @param screenRotation - 0, 90, 180 or 270.
     */
    public BoardOrientationNode(Context context, int screenRotation) {
        mSensorOrientation = new SensorGyroscopeOrientation(context, 0.98f);
        mSensorOrientation.setCalibrationEnabled(true);
        mSensorOrientation.setRotation(screenRotation);
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(AppConst.RoboBoard.ORIENTATION_NODE);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        mPublisher = connectedNode.newPublisher(AppConst.RoboHead.HEAD_JOYSTICK_TOPIC, Twist._TYPE);

        mSensorOrientation.start();

        mPublisherTimer = new Timer();
        mPublisherTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Vector3 forward = new Vector3(1, 0, 0);
                mSensorOrientation.getDeviceToWorld().conjugate().transform(forward);
                Log.d(this, "forward: " + String.format("%+3.2f, %+3.2f, %+3.2f", forward.x, forward.y, forward.z));

                double azimuthValue = 0;
                double pitchValue = 0;
                Log.d(this, "azimuth: " + azimuthValue);
                Log.d(this, "roll: " + pitchValue);

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
        }, 0, 80);
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
}
