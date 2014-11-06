package ru.robotmitya.robohead;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import geometry_msgs.Twist;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import ru.robotmitya.robocommonlib.AppConst;
import ru.robotmitya.robocommonlib.Log;

public class PidNode extends BroadcastReceiver implements NodeMain {
    private Publisher<std_msgs.String> mControlModePublisher;
    private Publisher<Twist> mHeadPublisher;

    @Override
    public void onReceive(Context context, Intent intent) {
        // adb shell am broadcast -a ru.robotmitya.robohead.PidNode.test --es command "dsd" --ef x 0.123 --ef y 0.321
        float x = 0;
        float y = 0;
        boolean hasXY = false;
        if (intent.hasExtra("x")) {
            x = intent.getFloatExtra("x", 0);
            hasXY = true;
        }
        if (intent.hasExtra("y")) {
            y = intent.getFloatExtra("y", 0);
            hasXY = true;
        }
        if (hasXY) {
            Log.d(this, "+++ action: " + intent.getAction() + "    x = " + x + "    y = " + y);
        }

        if (intent.hasExtra("command")) {
            final String command = intent.getStringExtra("command");
            Log.d(this, "+++ action: " + intent.getAction() + "    command = " + command);
        }
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("robot_mitya/pid_node");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        mControlModePublisher = connectedNode.newPublisher(AppConst.RoboHead.CONTROL_MODE_TOPIC, std_msgs.String._TYPE);
        mHeadPublisher = connectedNode.newPublisher(AppConst.RoboHead.HEAD_TOPIC, Twist._TYPE);

//        Subscriber<std_msgs.String> twistSubscriber = connectedNode.newSubscriber("robot_mitya/pid", std_msgs.String._TYPE);
//        twistSubscriber.addMessageListener(new MessageListener<std_msgs.String>() {
//            @Override
//            public void onNewMessage(std_msgs.String message) {
//
//                mPidController.reset();
//
//                try {
///*
//                    rostopic pub -1 /robot_mitya/pid std_msgs/String -- "{'data': '{"command": "H", "value": 0}'}"
//*/
//                    JSONObject json = new JSONObject(message.getData());
//                    final String command = json.getString("command");
//                    final Double value = json.getDouble("value");
//                    if (command.equals("go")) {
//                        mTargetValue = (short) Math.round(value);
//                        stopTimer();
//                        startTimer();
//                    } else if (command.equals("H") || command.equals("V")) {
//                        mCurrentValue = (short) Math.round(value);
//                        publishCommand(MessageHelper.makeMessage(command, mCurrentValue));
//                    } else if (command.equals("Kp")) {
//                        mKp = value;
//                    } else if (command.equals("Ki")) {
//                        mKi = value;
//                    } else if (command.equals("Kd")) {
//                        mKd = value;
//                    }
//                } catch (JSONException e) {
//                    Log.e(PidNode.this, "+++ " + e.getMessage());
//                }
//            }
//        });
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

    private void publishControlModeCommand(final String command) {
        std_msgs.String message = mControlModePublisher.newMessage();
        message.setData(command);
        mControlModePublisher.publish(message);
        Log.messagePublished(this, mControlModePublisher.getTopicName().toString(), command);
    }

    private void publishHeadMessage(final float x, final float y) {
        try {
            Twist message = mHeadPublisher.newMessage();
            message.getAngular().setX(0);
            message.getAngular().setY(0);
            message.getAngular().setZ(x);
            message.getLinear().setX(y);
            message.getLinear().setY(0);
            message.getLinear().setZ(0);
            mHeadPublisher.publish(message);
            Log.messagePublished(this, mHeadPublisher.getTopicName().toString(), message.toString());
        } catch (NullPointerException e) {
            Log.e(this, e.getMessage());
        }
    }
}
