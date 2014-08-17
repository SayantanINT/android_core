package ru.robotmitya.robohead;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.lang.String;

import geometry_msgs.Twist;
import geometry_msgs.Vector3;
import ru.robotmitya.robocommonlib.AppConst;
import ru.robotmitya.robocommonlib.Log;
import ru.robotmitya.robocommonlib.MessageHelper;
import ru.robotmitya.robocommonlib.MotorsSpeed;
import ru.robotmitya.robocommonlib.RoboState;
import ru.robotmitya.robocommonlib.Rs;

/**
 * Created by dmitrydzz on 4/12/14.
 *
 */
public class DriveJoystickAnalyzerNode implements NodeMain {

    private Publisher<std_msgs.String> mBodyPublisher;

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(AppConst.RoboHead.DRIVE_JOYSTICK_ANALYZER_NODE);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        mBodyPublisher = connectedNode.newPublisher(AppConst.RoboHead.BODY_TOPIC, std_msgs.String._TYPE);

        Subscriber<Twist> subscriber = connectedNode.newSubscriber(AppConst.RoboHead.DRIVE_JOYSTICK_TOPIC, geometry_msgs.Twist._TYPE);
        subscriber.addMessageListener(new MessageListener<Twist>() {
            @Override
            public void onNewMessage(geometry_msgs.Twist message) {
                Vector3 linear = message.getLinear();
                Vector3 angular = message.getAngular();
                double x = -angular.getZ();
                double y = linear.getX();
                Log.messageReceived(DriveJoystickAnalyzerNode.this, String.format("x=%.3f, y=%.3f", x, y));

                MotorsSpeed motorsSpeed = new MotorsSpeed();
                calculateMotorsSpeed(x, y, motorsSpeed);
                publishCommand(MessageHelper.makeMessage(Rs.DriveLeft.ID, motorsSpeed.mLeft));
                publishCommand(MessageHelper.makeMessage(Rs.DriveRight.ID, motorsSpeed.mRight));
            }
        });
    }

    @Override
    public void onShutdown(Node node) {
    }

    @Override
    public void onShutdownComplete(Node node) {
    }

    @Override
    public void onError(Node node, Throwable throwable) {
    }

    private void publishCommand(final String command) {
        std_msgs.String message = mBodyPublisher.newMessage();
        message.setData(command);
        mBodyPublisher.publish(message);
        Log.messagePublished(this, mBodyPublisher.getTopicName().toString(), command);
    }

    public static final short MAX_SPEED = 255;

    public static void calculateMotorsSpeed(double x, double y, final MotorsSpeed motorsSpeed) {
        if (RoboState.getIsReverse() && SettingsFragment.getDriveReverse()) {
            y = -y;
        }

        if ((x >= 0) && (y >= 0)) {
            calculateMotorsSpeedInFirstQuadrant(x, y, motorsSpeed);
        } else if ((x < 0) && (y >= 0)) {
            x = -x;
            calculateMotorsSpeedInFirstQuadrant(x, y, motorsSpeed);
            short temp = motorsSpeed.mLeft;
            motorsSpeed.mLeft = motorsSpeed.mRight;
            motorsSpeed.mRight = temp;
        } else if ((x < 0) && (y < 0)) {
            x = -x;
            y = -y;
            calculateMotorsSpeedInFirstQuadrant(x, y, motorsSpeed);
            motorsSpeed.mLeft = (short) -motorsSpeed.mLeft;
            motorsSpeed.mRight = (short) -motorsSpeed.mRight;
        } else if ((x >= 0) && (y < 0)) {
            y = -y;
            calculateMotorsSpeedInFirstQuadrant(x, y, motorsSpeed);
            short temp = motorsSpeed.mLeft;
            motorsSpeed.mLeft = (short) -motorsSpeed.mRight;
            motorsSpeed.mRight = (short) -temp;
        }
    }

    private static void calculateMotorsSpeedInFirstQuadrant(double x, double y, final MotorsSpeed motorsSpeed) {
        if ((x >= 0) && (y >= 0)) {
            double r = Math.sqrt(x * x + y * y);
            double angle = Math.asin(y / r);

            double rightCoef = r * (4 * angle / Math.PI - 1);

            motorsSpeed.mLeft = (short) (Math.round(MAX_SPEED * r));
            motorsSpeed.mRight = (short) (Math.round(MAX_SPEED * rightCoef));
        }
    }
}
