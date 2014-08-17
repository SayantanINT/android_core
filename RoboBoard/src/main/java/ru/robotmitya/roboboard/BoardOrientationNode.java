package ru.robotmitya.roboboard;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import java.util.Timer;
import java.util.TimerTask;

import geometry_msgs.Twist;
import ru.robotmitya.robocommonlib.AppConst;
import ru.robotmitya.robocommonlib.Log;
import ru.robotmitya.robocommonlib.RoboState;
import ru.robotmitya.robocommonlib.SensorFusion;

/**
 * Created by dmitrydzz on 5/25/14.
 *
 */
public class BoardOrientationNode implements NodeMain, SensorEventListener {
    private Publisher<Twist> mPublisher;
    private SensorManager mSensorManager;
    private SensorFusion mSensorFusion;
    private Timer mPublisherTimer;

    public BoardOrientationNode(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(AppConst.RoboBoard.ORIENTATION_NODE);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        mPublisher = connectedNode.newPublisher(AppConst.RoboHead.HEAD_JOYSTICK_TOPIC, Twist._TYPE);

        mSensorFusion = new SensorFusion();
        mSensorFusion.setMode(SensorFusion.Mode.FUSION);

        registerSensorManagerListeners();

        mPublisherTimer = new Timer();
        mPublisherTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                double azimuthValue = mSensorFusion.getAzimuth(); // [-PI, PI]
                azimuthValue /= Math.PI; // zoom to interval [-1, 1]
                azimuthValue += RoboState.getHorizontalZeroOrientation(); // change zero point and break interval [-1, 1]
                if (azimuthValue > 1) azimuthValue -= 2; // make interval [-1, 1]
                azimuthValue *= 2; // zoom to interval [-2, 2]
                if (azimuthValue < -1) azimuthValue = -1; // ignore back positions
                else if (azimuthValue > 1) azimuthValue = 1; // ignore back positions

                double rollValue =  mSensorFusion.getRoll(); // [-PI, PI]
                rollValue /= Math.PI; // zoom to interval [-1, 1]
                rollValue += RoboState.getVerticalZeroOrientation(); // change zero point and break interval [-1, 1]
                if (rollValue > 1) rollValue -= 2; // make interval [-1, 1]
                rollValue *= 4; // zoom to interval [-4, 4]
                if (rollValue < -1) rollValue = -1; // ignore back positions
                else if (rollValue > 1) rollValue = 1; // ignore back positions

                Log.d(this, "azimuth: " + azimuthValue);
                Log.d(this, "roll: " + rollValue);

                try {
                    Twist message = mPublisher.newMessage();
                    message.getAngular().setX(0);
                    message.getAngular().setY(0);
                    message.getAngular().setZ(-azimuthValue);
                    message.getLinear().setX(-rollValue);
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
        unregisterSensorManagerListeners();

        mPublisherTimer.cancel();
        mPublisherTimer.purge();
    }

    @Override
    public void onShutdownComplete(Node node) {
    }

    @Override
    public void onError(Node node, Throwable throwable) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                mSensorFusion.setAccel(event.values);
                mSensorFusion.calculateAccMagOrientation();
                break;

            case Sensor.TYPE_GYROSCOPE:
                mSensorFusion.gyroFunction(event);
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                mSensorFusion.setMagnet(event.values);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void registerSensorManagerListeners() {
        final int rate = SensorManager.SENSOR_DELAY_GAME;
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), rate);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), rate);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), rate);
    }

    public void unregisterSensorManagerListeners() {
        mSensorManager.unregisterListener(this);
    }
}
