package ru.robotmitya.robohead;

import android.content.Context;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.lang.String;
import java.util.ArrayList;

import ru.robotmitya.robocommonlib.AppConst;
import ru.robotmitya.robocommonlib.Log;
import ru.robotmitya.robocommonlib.MessageHelper;
import ru.robotmitya.robocommonlib.Rs;

/**
 * Created by dmitrydzz on 3/6/14.
 *
 */
public final class ReflexNode implements NodeMain {
    private Context mContext;

    private Publisher<std_msgs.String> mBodyPublisher;
    private Publisher<std_msgs.String> mFacePublisher;

    private ArrayList<String> mHappyReflex; // M0101
    private ArrayList<String> mPlayReflex;  // M0102
    private ArrayList<String> mSadReflex;   // M0103
    private ArrayList<String> mAngryReflex; // M0104
    private ArrayList<String> mDanceReflex; // M0105 Nina Simone "Ain't Got No – I Got Life"
    private ArrayList<String> mNoseReflex;  // M0106

    public ReflexNode(final Context context) {
        mContext = context;
        initHappyReflex();
        initPlayReflex();
        initSadReflex();
        initAngryReflex();
        initDanceReflex();
        initNoseReflex();
    }

    private void initHappyReflex() {
        mHappyReflex = new ArrayList<String>();
        mHappyReflex.add(MessageHelper.makeMessage(Rs.Mood.ID, Rs.Mood.ACTION_HAPPY)); // smile, wag the tail
        mHappyReflex.add(MessageHelper.makeMessage(Rs.Wait.ID, (short) 4000)); // 4 seconds delay
        mHappyReflex.add(MessageHelper.makeMessage(Rs.Mood.ID, Rs.Mood.FACE_OK)); // normal face
    }

    private void initPlayReflex() {
        mPlayReflex = new ArrayList<String>();
        mPlayReflex.add(MessageHelper.makeMessage(Rs.Mood.ID, Rs.Mood.ACTION_PLAY)); // want to play face, jump
        mPlayReflex.add(MessageHelper.makeMessage(Rs.Wait.ID, (short) 4000)); // 4 seconds delay
        mPlayReflex.add(MessageHelper.makeMessage(Rs.Mood.ID, Rs.Mood.FACE_OK)); // normal face
    }

    private void initSadReflex() {
        mSadReflex = new ArrayList<String>();
        mSadReflex.add(MessageHelper.makeMessage(Rs.Mood.ID, Rs.Mood.ACTION_SAD)); // sad face, hang down his head
        mSadReflex.add(MessageHelper.makeMessage(Rs.Wait.ID, (short)5000)); // 5 seconds delay
        mSadReflex.add(MessageHelper.makeMessage(Rs.Mood.ID, Rs.Mood.FACE_OK)); // normal face
    }

    private void initAngryReflex() {
        mAngryReflex = new ArrayList<String>();
        mAngryReflex.add(MessageHelper.makeMessage(Rs.Mood.ID, Rs.Mood.ACTION_ANGRY));
        mAngryReflex.add(MessageHelper.makeMessage(Rs.Wait.ID, (short)4000)); // 4 seconds delay
        mAngryReflex.add(MessageHelper.makeMessage(Rs.Mood.ID, Rs.Mood.FACE_OK)); // normal face
    }

    private void initDanceReflex() {
        mDanceReflex = new ArrayList<String>();
        mDanceReflex.add(MessageHelper.makeMessage(Rs.Mood.ID, Rs.Mood.ACTION_DANCE));
        mDanceReflex.add(MessageHelper.makeMessage(Rs.Wait.ID, (short)6500)); // 6.5 seconds delay
        mDanceReflex.add(MessageHelper.makeMessage(Rs.Mood.ID, Rs.Mood.FACE_OK)); // normal face
    }

    private void initNoseReflex() {
        mNoseReflex = new ArrayList<String>();
        mNoseReflex.add(MessageHelper.makeMessage(Rs.Mood.ID, Rs.Mood.ACTION_NOSE));
        mNoseReflex.add(MessageHelper.makeMessage(Rs.Wait.ID, (short) 3000)); // 3 seconds delay
        mNoseReflex.add(MessageHelper.makeMessage(Rs.Mood.ID, Rs.Mood.FACE_OK)); // normal face
    }

    private boolean mIsExecutingReflex = false;

    private void executeReflex(final ArrayList<String> reflex) {
        if (mIsExecutingReflex) {
            return;
        }

        mIsExecutingReflex = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (String command : reflex) {
                    String messageIdentifier = MessageHelper.getMessageIdentifier(command);
                    int messageValue = MessageHelper.getMessageIntegerValue(command);

                    if (messageIdentifier.equals(Rs.Wait.ID)) {
                        try {
                            Thread.sleep(messageValue);
                        } catch (InterruptedException e) {
                            Log.e(ReflexNode.this, String.format(mContext.getResources().getString(R.string.error_command_failed), command));
                        }
                    } else {
                        if (messageIdentifier.equals(Rs.Mood.ID)) {
                            publishCommand(mFacePublisher, command);
                        }
                        publishCommand(mBodyPublisher, command);
                    }
                }

                mIsExecutingReflex = false;
            }
        }).start();
    }

    private void publishCommand(final Publisher<std_msgs.String> publisher, final String command) {
        std_msgs.String message = publisher.newMessage();
        message.setData(command);
        publisher.publish(message);
        Log.messagePublished(this, publisher.getTopicName().toString(), command);
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(AppConst.RoboHead.REFLEX_NODE);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        mBodyPublisher = connectedNode.newPublisher(AppConst.RoboHead.BODY_TOPIC, std_msgs.String._TYPE);
        mFacePublisher = connectedNode.newPublisher(AppConst.RoboHead.FACE_TOPIC, std_msgs.String._TYPE);

        Subscriber<std_msgs.String> subscriber = connectedNode.newSubscriber(AppConst.RoboHead.REFLEX_TOPIC, std_msgs.String._TYPE);
        subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {
                String messageBody = message.getData();
                String identifier = MessageHelper.getMessageIdentifier(messageBody);
                int value = MessageHelper.getMessageIntegerValue(messageBody);

                Log.messageReceived(ReflexNode.this, messageBody);
                if (identifier.contentEquals(Rs.Mood.ID)) {
                    if (value == Rs.Mood.ACTION_HAPPY) {
                        executeReflex(mHappyReflex);
                    } else if (value == Rs.Mood.ACTION_PLAY) {
                        executeReflex(mPlayReflex);
                    } else if (value == Rs.Mood.ACTION_SAD) {
                        executeReflex(mSadReflex);
                    } else if (value == Rs.Mood.ACTION_ANGRY) {
                        executeReflex(mAngryReflex);
                    } else if (value == Rs.Mood.ACTION_DANCE) {
                        executeReflex(mDanceReflex);
                    } else if (value == Rs.Mood.ACTION_NOSE) {
                        executeReflex(mNoseReflex);
                    }
                }
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
}
