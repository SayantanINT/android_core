package ru.robotmitya.robohead;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.lang.String;

import ru.robotmitya.robocommonlib.AppConst;
import ru.robotmitya.robocommonlib.Log;
import ru.robotmitya.robocommonlib.MessageHelper;
import ru.robotmitya.robocommonlib.RoboState;
import ru.robotmitya.robocommonlib.Rs;

/**
 * Created by dmitrydzz on 3/26/14.
 *
 */
public class HeadStateNode implements NodeMain {
    private final Context mContext;
    private Publisher<std_msgs.String> mBoardPublisher;
    private BroadcastReceiver mBatteryBroadcastReceiver;

    public HeadStateNode(Context context) {
        mContext = context;

        mBatteryBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                    int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                    int percent = (scale > 0) && (level > 0) ? level * 100 / scale : 0;

                    short messageValue = Rs.BatteryResponse.ROBOHEAD_BATTERY;
                    messageValue |= (short)percent;
                    final int NOT_PLUGGED = 0;
                    if (plugged != NOT_PLUGGED) {
                        messageValue |= (short)0x0100;
                    }

                    RoboState.setRoboHeadBatteryState(messageValue);
                    publishToBoard(MessageHelper.makeMessage(Rs.BatteryResponse.ID, messageValue));
                } catch (Exception e) {
                    Log.e(this, e.getMessage());
                }
            }
        };
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(AppConst.RoboHead.HEAD_STATE_NODE);
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        RoboState.setSelectedCamIndex((short)SettingsFragment.getCameraIndex());

        mContext.registerReceiver(mBatteryBroadcastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        mBoardPublisher = connectedNode.newPublisher(AppConst.RoboBoard.BOARD_TOPIC, std_msgs.String._TYPE);

        Subscriber<std_msgs.String> subscriber = connectedNode.newSubscriber(AppConst.RoboHead.HEAD_STATE_TOPIC, std_msgs.String._TYPE);
        subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {
                try {
                    String messageBody = message.getData();
                    String identifier = MessageHelper.getMessageIdentifier(messageBody);
                    short value = (short) MessageHelper.getMessageIntegerValue(messageBody);

                    Log.messageReceived(HeadStateNode.this, messageBody);

                    if (identifier.contentEquals(Rs.Instruction.ID)) {
                        switch (value) {
                            case Rs.Instruction.HEADLIGHTS_OFF:
                            case Rs.Instruction.HEADLIGHTS_ON:
                                RoboState.setHeadlights(value);
                                publishToBoard(messageBody);
                                break;
                            case Rs.Instruction.CAMERA_OFF:
                                SettingsFragment.setCameraIndex(mContext, AppConst.Common.Camera.DISABLED);
                                RoboState.setSelectedCamIndex((short) SettingsFragment.getCameraIndex());
                                publishToBoard(messageBody);
                                break;
                            case Rs.Instruction.CAMERA_BACK_ON:
                                SettingsFragment.setCameraIndex(mContext, AppConst.Common.Camera.BACK);
                                RoboState.setSelectedCamIndex((short) SettingsFragment.getCameraIndex());
                                publishToBoard(messageBody);
                                break;
                            case Rs.Instruction.CAMERA_FRONT_ON:
                                SettingsFragment.setCameraIndex(mContext, AppConst.Common.Camera.FRONT);
                                RoboState.setSelectedCamIndex((short) SettingsFragment.getCameraIndex());
                                publishToBoard(messageBody);
                                break;
                            case Rs.Instruction.ACCUMULATOR_MAIN_CHARGING_STOP:
                            case Rs.Instruction.ACCUMULATOR_MAIN_CHARGING_START:
                                RoboState.setMainAccumulatorCharging(value);
                                publishToBoard(messageBody);
                                break;
                            case Rs.Instruction.ACCUMULATOR_ROBOHEAD_CHARGING_STOP:
                            case Rs.Instruction.ACCUMULATOR_ROBOHEAD_CHARGING_START:
                                RoboState.setRoboHeadAccumulatorCharging(value);
                                publishToBoard(messageBody);
                                break;
                        }
                    } else if (identifier.equals(Rs.Mood.ID)) {
                        RoboState.setMood(value);
                        publishToBoard(messageBody);
                    } else if (identifier.equals(Rs.BatteryRequest.ID)) {
                        if (value == Rs.BatteryRequest.ROBOHEAD_BATTERY) {
                            publishToBoard(MessageHelper.makeMessage(Rs.BatteryResponse.ID, RoboState.getRoboHeadBatteryState()));
                        }
                    } else if (identifier.equals(Rs.BatteryResponse.ID)) {
                        publishToBoard(messageBody);
                    }
                } catch (Exception e) {
                    Log.e(this, e.getMessage());
                }
            }
        });
    }

    @Override
    public void onShutdown(Node node) {
        try {
            mContext.unregisterReceiver(mBatteryBroadcastReceiver);
        } catch (IllegalArgumentException e) {
            // Got strange error: java.lang.IllegalArgumentException: Receiver not registered
            // That couldn't be true %-\ So just catch it!
        }
    }

    @Override
    public void onShutdownComplete(Node node) {
    }

    @Override
    public void onError(Node node, Throwable throwable) {
    }

    private void publishToBoard(final String command) {
        std_msgs.String message = mBoardPublisher.newMessage();
        message.setData(command);
        mBoardPublisher.publish(message);

        Log.messagePublished(this, mBoardPublisher.getTopicName().toString(), command);
    }
}
