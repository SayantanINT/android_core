package ru.robotmitya.robohead;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import geometry_msgs.Vector3;
import ru.robotmitya.robocommonlib.AppConst;
import ru.robotmitya.robocommonlib.Log;
import ru.robotmitya.robocommonlib.MessageHelper;
import ru.robotmitya.robocommonlib.RoboState;
import ru.robotmitya.robocommonlib.Rs;
import ru.robotmitya.robocommonlib.SensorValueAdapter;

/**
 * Created by dmitrydzz on 4/12/14.
 *
 */
public class HeadJoystickAnalyzerNode implements NodeMain {
    private Publisher<std_msgs.String> mBodyPublisher;
    private final SensorValueAdapter mHorizontalValueAdapter = new SensorValueAdapter(0.01, 0.4);
    private final SensorValueAdapter mVerticalValueAdapter = new SensorValueAdapter(0.005, 0.4);

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(AppConst.RoboHead.HEAD_JOYSTICK_ANALYZER_NODE);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        mBodyPublisher = connectedNode.newPublisher(AppConst.RoboHead.BODY_TOPIC, std_msgs.String._TYPE);

        Subscriber<geometry_msgs.Twist> subscriber = connectedNode.newSubscriber(AppConst.RoboHead.HEAD_JOYSTICK_TOPIC, geometry_msgs.Twist._TYPE);
        subscriber.addMessageListener(new MessageListener<geometry_msgs.Twist>() {
            @Override
            public void onNewMessage(geometry_msgs.Twist message) {
                Vector3 linear = message.getLinear();
                Vector3 angular = message.getAngular();
                double x = -angular.getZ();
                double y = linear.getX();
                if (RoboState.getIsReverse()) {
                    y = -y;
                }
                JoystickPosition joystickPosition = new JoystickPosition(x, y);
                correctCoordinatesFromCycleToSquareArea(joystickPosition);
                // Round:
//                x = (double)Math.round(joystickPosition.mX * 100) / 100F;
//                y = (double)Math.round(joystickPosition.mY * 50) / 50F;
//                joystickPosition.mX = x;
//                joystickPosition.mY = y;
                mHorizontalValueAdapter.put(joystickPosition.mX);
                mVerticalValueAdapter.put(joystickPosition.mY);
                joystickPosition.mX = mHorizontalValueAdapter.get();
                joystickPosition.mY = mVerticalValueAdapter.get();

                Log.messageReceived(HeadJoystickAnalyzerNode.this, String.format("x=%.3f, y=%.3f", joystickPosition.mX, joystickPosition.mY));

                // Reverse vertical:
                joystickPosition.mY = -joystickPosition.mY;

                short horizontalDegree = getHorizontalDegree(joystickPosition);
                short verticalDegree = getVerticalDegree(joystickPosition);
                String horizontalCommand = MessageHelper.makeMessage(Rs.HeadHorizontalPosition.ID, horizontalDegree);
                String verticalCommand = MessageHelper.makeMessage(Rs.HeadVerticalPosition.ID, verticalDegree);
                publishCommand(horizontalCommand);
                publishCommand(verticalCommand);
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

    private final class JoystickPosition {
        public double mX;
        public double mY;
        public JoystickPosition(final double x, final double y) {
            mX = x;
            mY = y;
        }
    }

    private static void correctCoordinatesFromCycleToSquareArea(final JoystickPosition pos) {
        if ((pos.mX >= 0) && (pos.mY >= 0)) {
            correctCoordinatesFromCycleToSquareAreaForFirstQuadrant(pos);
        } else if ((pos.mX < 0) && (pos.mY >= 0)) {
            pos.mX = -pos.mX;
            correctCoordinatesFromCycleToSquareAreaForFirstQuadrant(pos);
            pos.mX = -pos.mX;
        } else if ((pos.mX < 0) && (pos.mY < 0)) {
            pos.mX = -pos.mX;
            pos.mY = -pos.mY;
            correctCoordinatesFromCycleToSquareAreaForFirstQuadrant(pos);
            pos.mX = -pos.mX;
            pos.mY = -pos.mY;
        } else if ((pos.mX >= 0) && (pos.mY < 0)) {
            pos.mY = -pos.mY;
            correctCoordinatesFromCycleToSquareAreaForFirstQuadrant(pos);
            pos.mY = -pos.mY;
        }

        pos.mX = pos.mX < -1 ? -1 : pos.mX;
        pos.mX = pos.mX > 1 ? 1 : pos.mX;
        pos.mY = pos.mY < -1 ? -1 : pos.mY;
        pos.mY = pos.mY > 1 ? 1 : pos.mY;
    }

    private static void correctCoordinatesFromCycleToSquareAreaForFirstQuadrant(final JoystickPosition pos) {
        // To avoid div 0:
        if (pos.mX == 0) {
            return;
        }

        if ((pos.mX >= 0) && (pos.mY >= 0)) {
            boolean firstSectorInOctet = pos.mX >= pos.mY;
            if (!firstSectorInOctet) {
                double temp = pos.mX;
                pos.mX = pos.mY;
                pos.mY = temp;
            }

            double resultX = Math.sqrt((pos.mX * pos.mX) + (pos.mY * pos.mY));
            double resultY = pos.mY * resultX / pos.mX;
            pos.mX = resultX;
            pos.mY = resultY;

            if (!firstSectorInOctet) {
                double temp = pos.mX;
                pos.mX = pos.mY;
                pos.mY = temp;
            }
        }
    }

    private void publishCommand(final String command) {
        std_msgs.String message = mBodyPublisher.newMessage();
        message.setData(command);
        mBodyPublisher.publish(message);
        Log.messagePublished(this, mBodyPublisher.getTopicName().toString(), command);
    }

    private short getHorizontalDegree(final JoystickPosition pos) {
        double min = RoboState.getHeadHorizontalServoMinDegree();
        double max = RoboState.getHeadHorizontalServoMaxDegree();
        double result = ((1 - pos.mX) * ((max - min) / 2)) + min;
        return (short)result;
    }

    private short getVerticalDegree(final JoystickPosition pos) {
        double min = RoboState.getHeadVerticalServoMinDegree();
        double max = RoboState.getHeadVerticalServoMaxDegree();
        double result = ((1 - pos.mY) * ((max - min) / 2)) + min;
        return (short)result;
    }
}
