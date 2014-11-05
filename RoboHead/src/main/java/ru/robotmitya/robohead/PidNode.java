package ru.robotmitya.robohead;

import org.json.JSONException;
import org.json.JSONObject;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;
import ru.robotmitya.robocommonlib.AppConst;
import ru.robotmitya.robocommonlib.Log;
import ru.robotmitya.robocommonlib.MessageHelper;
import ru.robotmitya.robocommonlib.PidController;

import java.util.Timer;
import java.util.TimerTask;

public class PidNode implements NodeMain {
    private short mCurrentValue = 0;
    private short mTargetValue = 0;

    private PidController mPidController = new PidController(0, 0, 0);
    private double mKp = 0;
    private double mKi = 0;
    private double mKd = 0;

    private Timer mTimer;

    private Publisher<std_msgs.String> mBodyPublisher;

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("robot_mitya/pid_node");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        mBodyPublisher = connectedNode.newPublisher(AppConst.RoboHead.BODY_TOPIC, std_msgs.String._TYPE);

        Subscriber<std_msgs.String> twistSubscriber = connectedNode.newSubscriber("robot_mitya/pid", std_msgs.String._TYPE);
        twistSubscriber.addMessageListener(new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {

                mPidController.reset();

                try {
/*
                    rostopic pub -1 /robot_mitya/pid std_msgs/String -- "{'data': '{"command": "H", "value": 0}'}"
*/
                    JSONObject json = new JSONObject(message.getData());
                    final String command = json.getString("command");
                    final Double value = json.getDouble("value");
                    if (command.equals("go")) {
                        mTargetValue = (short) Math.round(value);
                        stopTimer();
                        startTimer();
                    } else if (command.equals("H") || command.equals("V")) {
                        mCurrentValue = (short) Math.round(value);
                        publishCommand(MessageHelper.makeMessage(command, mCurrentValue));
                    } else if (command.equals("Kp")) {
                        mKp = value;
                    } else if (command.equals("Ki")) {
                        mKi = value;
                    } else if (command.equals("Kd")) {
                        mKd = value;
                    }
                } catch (JSONException e) {
                    Log.e(PidNode.this, "+++ " + e.getMessage());
                }
            }
        });

        mPidController.setInputRange(0, 180);
        mPidController.setOutputRange(1 / 46080, 1 / 1280);
        mPidController.setTolerance(1.8);
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

    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
        }
    }

    private void startTimer() {
        mPidController.setPID(mKp, mKi, mKd);
        mPidController.enable();

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mPidController.performPID();
                if (mPidController.onTarget()) {
                    mPidController.disable();
                }
            }
        }, 30);
    }
}
