/**
 * This node implements connection and communication functions with robo_body via Bluetooth.
 */
package ru.robotmitya.robohead;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import ru.robotmitya.robocommonlib.AppConst;
import ru.robotmitya.robocommonlib.Log;
import ru.robotmitya.robocommonlib.MessageHelper;
import ru.robotmitya.robocommonlib.Rs;

/**
 * Created by dmitrydzz on 1/27/14.
 *
 */
public class BluetoothBodyNode implements NodeMain {
    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;

    private Publisher<std_msgs.String> mHeadStatePublisher;

    private BluetoothSocket mBluetoothSocket = null;
    private InputStream mInputStream = null;
    private OutputStream mOutputStream = null;
    private boolean mConnected;

    public BluetoothBodyNode(final Context context, final BluetoothAdapter bluetoothAdapter) {
        super();
        mContext = context;
        mBluetoothAdapter = bluetoothAdapter;
        mConnected = false;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(AppConst.RoboHead.BLUETOOTH_BODY_NODE);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        connectedNode.executeCancellableLoop(new CancellableLoop() {
            private String mPreviousMessagesRest = "";

            @Override
            protected void loop() throws InterruptedException {
                if (mConnected) {
                    List<String> messageList = null;
                    try {
                        // Получить список принятых на данный момент команд:
                        messageList = getMessagesFromStream(mInputStream, MessageHelper.MESSAGE_LENGTH);
                    } catch (Exception e) {
                        Log.e(BluetoothBodyNode.this.getClass(), "input error: " + e.getMessage());
                        cancel();
                    }

                    if (messageList != null) {
                        // Выполнить каждую принятую команду:
                        for (String messageText : messageList) {
                            Log.messageReceived(BluetoothBodyNode.this, "body", messageText);
                            publishToHeadState(messageText);
                        }
                    }
                } else {
                    // Это - единственный метод подключиться напрямую, не используя поиска
                    // всех устройств в округе.
                    // createRfcommSocketToServiceRecord(), к сожалению, не работает
                    try {
                        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(SettingsFragment.getRoboBodyMac());
                        Method method = bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                        mBluetoothSocket = (BluetoothSocket) method.invoke(bluetoothDevice, 1 /*Integer.valueOf(1)*/);

                        // Если контроллер робота недоступен, connect() вызывает исключение и тормозит работу
                        // приложения, несмотря на отдельный поток!
                        mBluetoothSocket.connect();

                        mInputStream = mBluetoothSocket.getInputStream();
                        mOutputStream = mBluetoothSocket.getOutputStream();
                        mConnected = true;
                        Log.d(BluetoothBodyNode.this, "started");

                        startVoltageRequests();
                    } catch (Exception e) {
                        Log.e(BluetoothBodyNode.this, "connection error: " + e.getMessage());
                        cancel();
                    }
                }
            }

            private List<String> getMessagesFromStream(final InputStream inputStream, final int messageLength) throws IOException {
                List<String> result = new ArrayList<String>();

                DataInputStream dataInputStream = new DataInputStream(inputStream);

                int bytesAvailable = dataInputStream.available();
                if (bytesAvailable <= 0) {
                    return result;
                }

                byte[] buffer = new byte[bytesAvailable];
                dataInputStream.readFully(buffer);
                String messages = new String(buffer);

                // Если на предыдущей итерации часть команды была прочитана, дочитываю команду:
                if (!mPreviousMessagesRest.equals("")) {
                    messages = mPreviousMessagesRest + messages;
                }

                messages = MessageHelper.skipFirstBrokenMessage(messages);
                while (messages.length() >= messageLength) {
                    String newMessage = messages.substring(0, messageLength);
                    result.add(newMessage);
                    messages = messages.substring(messageLength);
                    messages = MessageHelper.skipFirstBrokenMessage(messages);
                }
                mPreviousMessagesRest = messages;

                return result;
            }
        });

        mHeadStatePublisher = connectedNode.newPublisher(AppConst.RoboHead.HEAD_STATE_TOPIC, std_msgs.String._TYPE);

        Subscriber<std_msgs.String> subscriber = connectedNode.newSubscriber(AppConst.RoboHead.BODY_TOPIC, std_msgs.String._TYPE);
        subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(final std_msgs.String message) {
                String messageBody = message.getData();
                Log.messageReceived(BluetoothBodyNode.this, messageBody);

                sendToBody(messageBody);
            }
        });
    }

    @Override
    public void onShutdown(Node node) {
        if (mConnected) {
            try {
                stopVoltageRequests();
                if (mBluetoothSocket != null) {
                    mBluetoothSocket.close();
                    mBluetoothSocket = null;
                }
            } catch (IOException e) {
                mBluetoothSocket = null;
            }
        }
        mConnected = false;
        Log.d(this, "stopped");
    }

    @Override
    public void onShutdownComplete(Node node) {
    }

    @Override
    public void onError(Node node, Throwable throwable) {
    }

    private void publishToHeadState(final String command) {
        std_msgs.String message = mHeadStatePublisher.newMessage();
        message.setData(command);
        mHeadStatePublisher.publish(message);

        Log.messagePublished(this, mHeadStatePublisher.getTopicName().toString(), command);
    }

    private void sendToBody(String messageBody) {
        if (mConnected) {
            if (mBluetoothSocket != null) {
                try {
                    mOutputStream.write(messageBody.getBytes());
                    Log.d(BluetoothBodyNode.this, "sent to body via Bluetooth: " + messageBody);
                } catch (IOException e) {
                    String errorText = String.format(
                            mContext.getResources().getString(R.string.error_sending_message_through_bluetooth),
                            messageBody);
                    Log.e(BluetoothBodyNode.this, errorText);
                }
            }
        }
    }

    private void startVoltageRequests() {
        final short period = 0x012c; // 300 centiseconds = 3 seconds
        sendToBody(MessageHelper.makeMessage(Rs.BatteryRequest.ID, (short)(Rs.BatteryRequest.ROBOT_BATTERY_VOLTAGE | period)));
        sendToBody(MessageHelper.makeMessage(Rs.BatteryRequest.ID, (short)(Rs.BatteryRequest.ROBOT_POWER_SUPPLY_VOLTAGE | period)));
    }

    private void stopVoltageRequests() {
        sendToBody(MessageHelper.makeMessage(Rs.BatteryRequest.ID, Rs.BatteryRequest.ROBOT_BATTERY_VOLTAGE));
        sendToBody(MessageHelper.makeMessage(Rs.BatteryRequest.ID, Rs.BatteryRequest.ROBOT_POWER_SUPPLY_VOLTAGE));
    }
}
