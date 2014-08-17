package ru.robotmitya.robohead;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import ru.robotmitya.robocommonlib.AppConst;
import ru.robotmitya.robocommonlib.Log;

public class MainActivity extends RosActivity {
    private EyePreviewView mEyePreviewView;
    private BluetoothAdapter mBluetoothAdapter;
    private HeadStateNode mHeadStateNode;

    private Handler mEyeNodeHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (mEyePreviewView == null) {
                return true;
            }
            if (msg.arg1 == EyePreviewView.VIDEO_STARTED) {
                mEyePreviewView.setVisibility(View.VISIBLE);
            } else if (msg.arg1 == EyePreviewView.VIDEO_STOPPED) {
                mEyePreviewView.setVisibility(View.GONE);
            }

            return true;
        }
    });

    public MainActivity() {
        super("Robot Mitya\'s ticker", "RoboHead");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mHeadStateNode = new HeadStateNode(this);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        FaceFragment faceFragment = new FaceFragment();
        fragmentTransaction.add(R.id.face_fragment, faceFragment);
        fragmentTransaction.commit();

        SettingsFragment.initialize(this);

        mEyePreviewView = (EyePreviewView) findViewById(R.id.eye_preview_view);
        mEyePreviewView.setHandler(mEyeNodeHandler);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    protected void onStart() {
        super.onStart();

        final short selectedCamera = (short)SettingsFragment.getCameraIndex();
        if (selectedCamera == AppConst.Common.Camera.FRONT) {
            mEyePreviewView.startVideoStreaming(SettingsFragment.getFrontCameraMode());
        } else if (selectedCamera == AppConst.Common.Camera.BACK) {
            mEyePreviewView.startVideoStreaming(SettingsFragment.getBackCameraMode());
        } else {
            mEyePreviewView.stopVideoStreaming();
        }
    }

    @Override
    protected void onStop() {
        mEyePreviewView.stopVideoStreaming();
        super.onStop();
    }

    @Override
    public void startMasterChooser() {
        Intent data = new Intent();
        data.putExtra("ROS_MASTER_URI", SettingsFragment.getMasterUri());
        data.putExtra("NEW_MASTER", true);
        data.putExtra("ROS_MASTER_PRIVATE", false);
        onActivityResult(0, RESULT_OK, data);
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        NodeConfiguration nodeConfiguration =
                NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
        nodeConfiguration.setMasterUri(getMasterUri());

        nodeMainExecutor.execute(mHeadStateNode, nodeConfiguration);

        nodeMainExecutor.execute(mEyePreviewView, nodeConfiguration);

        initBluetoothBodyNode(nodeMainExecutor, nodeConfiguration);

        FaceNode faceNode = new FaceNode(this);
        nodeMainExecutor.execute(faceNode, nodeConfiguration);

        ReflexNode reflexNode = new ReflexNode(this);
        nodeMainExecutor.execute(reflexNode, nodeConfiguration);

        DriveJoystickAnalyzerNode driveJoystickAnalyzerNode = new DriveJoystickAnalyzerNode();
        nodeMainExecutor.execute(driveJoystickAnalyzerNode, nodeConfiguration);

        HeadJoystickAnalyzerNode headJoystickAnalyzerNode = new HeadJoystickAnalyzerNode();
        nodeMainExecutor.execute(headJoystickAnalyzerNode, nodeConfiguration);
    }

    private void initBluetoothBodyNode(final NodeMainExecutor nodeMainExecutor,
                                       final NodeConfiguration nodeConfiguration) {
        if (mBluetoothAdapter == null) {
            Log.e(this, getString(R.string.error_no_bluetooth_adapter));
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(
                            MainActivity.this,
                            MainActivity.this.getString(R.string.error_no_bluetooth_adapter),
                            Toast.LENGTH_LONG)
                            .show();
                }
            });
        } else if (!mBluetoothAdapter.isEnabled()) {
            Log.e(this, getString(R.string.error_bluetooth_adapter_is_not_activated));
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(
                            MainActivity.this,
                            MainActivity.this.getString(R.string.error_bluetooth_adapter_is_not_activated),
                            Toast.LENGTH_LONG)
                            .show();
                }
            });
        } else {
            BluetoothBodyNode bluetoothBodyNode = new BluetoothBodyNode(this, mBluetoothAdapter);
            nodeMainExecutor.execute(bluetoothBodyNode, nodeConfiguration);
        }
    }
}
