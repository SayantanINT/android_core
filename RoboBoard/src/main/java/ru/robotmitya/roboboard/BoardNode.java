package ru.robotmitya.roboboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.util.Timer;
import java.util.TimerTask;

import ru.robotmitya.robocommonlib.AppConst;
import ru.robotmitya.robocommonlib.Log;
import ru.robotmitya.robocommonlib.MessageHelper;
import ru.robotmitya.robocommonlib.RoboState;
import ru.robotmitya.robocommonlib.Rs;

/**
 * Created by dmitrydzz on 3/23/14.
 *
 */
public class BoardNode implements NodeMain {
    // No matter what real index is. These are internal indexes to identify phone's cameras.
    private static final short NO_CAM = (short)-1;
    private static final short FRONT_CAM_INDEX = (short)1;
    private static final short BACK_CAM_INDEX = (short)0;

    private Context mContext;

    private Publisher<std_msgs.String> mEyePublisher;
    private Publisher<std_msgs.String> mFacePublisher;
    private Publisher<std_msgs.String> mReflexPublisher;
    private Publisher<std_msgs.String> mBodyPublisher;
    private Publisher<std_msgs.String> mHeadStatePublisher;

    private BroadcastReceiver mBroadcastReceiverForBodyTopic;
    private BroadcastReceiver mBroadcastReceiverForEyeTopic;
    private BroadcastReceiver mBroadcastReceiverForFaceTopic;
    private BroadcastReceiver mBroadcastReceiverForReflexTopic;

    public BoardNode(final Context context) {
        mContext = context;

        mBroadcastReceiverForBodyTopic = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String command = intent.getStringExtra(AppConst.RoboBoard.Broadcast.BROADCAST_MESSAGE_TO_BODY_EXTRA_NAME);
                publishToBodyTopic(command);
            }
        };

        mBroadcastReceiverForEyeTopic = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String command = intent.getStringExtra(AppConst.RoboBoard.Broadcast.BROADCAST_MESSAGE_TO_EYE_EXTRA_NAME);
                publishToEyeTopic(command);
            }
        };

        mBroadcastReceiverForFaceTopic = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String command = intent.getStringExtra(AppConst.RoboBoard.Broadcast.BROADCAST_MESSAGE_TO_FACE_EXTRA_NAME);
                publishToFaceTopic(command);
            }
        };

        mBroadcastReceiverForReflexTopic = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String command = intent.getStringExtra(AppConst.RoboBoard.Broadcast.BROADCAST_MESSAGE_TO_REFLEX_EXTRA_NAME);
                publishToReflexTopic(command);
            }
        };
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(AppConst.RoboBoard.BOARD_NODE);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        mEyePublisher = connectedNode.newPublisher(AppConst.RoboHead.EYE_TOPIC, std_msgs.String._TYPE);
        mFacePublisher = connectedNode.newPublisher(AppConst.RoboHead.FACE_TOPIC, std_msgs.String._TYPE);
        mReflexPublisher = connectedNode.newPublisher(AppConst.RoboHead.REFLEX_TOPIC, std_msgs.String._TYPE);
        mBodyPublisher = connectedNode.newPublisher(AppConst.RoboHead.BODY_TOPIC, std_msgs.String._TYPE);
        mHeadStatePublisher = connectedNode.newPublisher(AppConst.RoboHead.HEAD_STATE_TOPIC, std_msgs.String._TYPE);

        RoboState.setSelectedCamIndex(NO_CAM);

        initializeBroadcasts();

        Subscriber<std_msgs.String> subscriber = connectedNode.newSubscriber(AppConst.RoboBoard.BOARD_TOPIC, std_msgs.String._TYPE);
        subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {
                String messageBody = message.getData();

                Log.messageReceived(BoardNode.this, messageBody);

                String identifier = MessageHelper.getMessageIdentifier(messageBody);
                int value = MessageHelper.getMessageIntegerValue(messageBody);
                if (identifier.contentEquals(Rs.Instruction.ID)) {
                    switch (value) {
                        case Rs.Instruction.CAMERA_OFF:
                            RoboState.setSelectedCamIndex(NO_CAM);
                            break;
                        case Rs.Instruction.CAMERA_FRONT_ON:
                            RoboState.setSelectedCamIndex(FRONT_CAM_INDEX);
                            break;
                        case Rs.Instruction.CAMERA_BACK_ON:
                            RoboState.setSelectedCamIndex(BACK_CAM_INDEX);
                            break;
                    }
                }

                Intent intent = new Intent(AppConst.RoboBoard.Broadcast.BROADCAST_MESSAGE_TO_GUI_NAME);
                intent.putExtra(AppConst.RoboBoard.Broadcast.BROADCAST_MESSAGE_TO_GUI_EXTRA_NAME, messageBody);
                LocalBroadcastManager.getInstance(BoardNode.this.mContext).sendBroadcast(intent);
            }
        });

        // This is a strange patch. After publisher is created I don't no why but it is not ready to publish messages.
        // So I wait for 1 second and then send "IFFFF" message 6 times with 1 second period.
        for (int i = 0; i < 6; i++) {
            int delay = 1000 * (i + 1);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    sendRoboStateRequest();
                }
            }, delay);
        }
    }

    private void initializeBroadcasts() {
        LocalBroadcastManager.getInstance(mContext).registerReceiver(
                mBroadcastReceiverForBodyTopic, new IntentFilter(AppConst.RoboBoard.Broadcast.BROADCAST_MESSAGE_TO_BODY_NAME));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(
                mBroadcastReceiverForEyeTopic, new IntentFilter(AppConst.RoboBoard.Broadcast.BROADCAST_MESSAGE_TO_EYE_NAME));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(
                mBroadcastReceiverForFaceTopic, new IntentFilter(AppConst.RoboBoard.Broadcast.BROADCAST_MESSAGE_TO_FACE_NAME));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(
                mBroadcastReceiverForReflexTopic, new IntentFilter(AppConst.RoboBoard.Broadcast.BROADCAST_MESSAGE_TO_REFLEX_NAME));
    }

    private void finalizeBroadcasts() {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mBroadcastReceiverForBodyTopic);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mBroadcastReceiverForEyeTopic);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mBroadcastReceiverForFaceTopic);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mBroadcastReceiverForReflexTopic);
    }

    public void sendRoboStateRequest() {
        String stateRequestCommand = MessageHelper.makeMessage(Rs.Instruction.ID, Rs.Instruction.STATE_REQUEST);
        publishToFaceTopic(stateRequestCommand);
        publishToEyeTopic(stateRequestCommand);
        publishToBodyTopic(stateRequestCommand);
        publishToHeadStateTopic(MessageHelper.makeMessage(Rs.BatteryRequest.ID, Rs.BatteryRequest.ROBOHEAD_BATTERY));
    }

    @Override
    public void onShutdown(Node node) {
        finalizeBroadcasts();
    }

    @Override
    public void onShutdownComplete(Node node) {
    }

    @Override
    public void onError(Node node, Throwable throwable) {
        Log.e(this, throwable.getMessage());
    }

    private void publishCommand(final Publisher<std_msgs.String> publisher, final String command) {
        try {
            std_msgs.String message = publisher.newMessage();
            message.setData(command);
            publisher.publish(message);
            Log.messagePublished(this, publisher.getTopicName().toString(), command);
        } catch (NullPointerException e) {
            Log.e(this, e.getMessage());
        }
    }

    public void publishToEyeTopic(final String message) {
        publishCommand(mEyePublisher, message);
    }

    public void publishToFaceTopic(final String message) {
        publishCommand(mFacePublisher, message);
    }

    public void publishToReflexTopic(final String message) {
        publishCommand(mReflexPublisher, message);
    }

    public void publishToBodyTopic(final String message) {
        publishCommand(mBodyPublisher, message);
    }

    public void publishToHeadStateTopic(final String message) {
        publishCommand(mHeadStatePublisher, message);
    }
}
