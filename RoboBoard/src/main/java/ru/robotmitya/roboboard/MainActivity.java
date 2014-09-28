package ru.robotmitya.roboboard;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Surface;
import android.view.WindowManager;

import com.badlogic.gdx.Gdx;
import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import ru.robotmitya.robocommonlib.AppConst;

public class MainActivity extends RosActivity {

    private VideoFragment mVideoFragment;
    private BoardFragment mBoardFragment;
    private BoardNode mBoardNode;
    private BoardOrientationNode mBoardOrientationNode;

    public MainActivity() {
        super("Robot Mitya\'s ticker", "RoboBoard");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mBoardNode = new BoardNode(this);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        mVideoFragment = new VideoFragment();
        fragmentTransaction.add(R.id.video_fragment, mVideoFragment);
        mBoardFragment = new BoardFragment();
        fragmentTransaction.add(R.id.board_fragment, mBoardFragment);
        fragmentTransaction.commit();

        mBoardOrientationNode = new BoardOrientationNode(this, getRotation(this));

        SettingsFragment.initialize(this);
    }

    @Override
    public void startMasterChooser() {
        Intent data = new Intent();
        data.putExtra("ROS_MASTER_URI", SettingsFragment.getMasterUri());
        data.putExtra("NEW_MASTER", false);
        data.putExtra("ROS_MASTER_PRIVATE", false);
        onActivityResult(0, RESULT_OK, data);
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
/*
        if (mBoardNode != null) {
            nodeMainExecutor.shutdownNodeMain(mBoardNode);
        }
        if (mVideoFragment.getImageView() != null) {
            nodeMainExecutor.shutdownNodeMain(mVideoFragment.getImageView());
        }
        if (mBoardFragment.getDriveJoystick() != null) {
            nodeMainExecutor.shutdownNodeMain(mBoardFragment.getDriveJoystick());
        }
        if (mBoardFragment.getHeadJoystick() != null) {
            nodeMainExecutor.shutdownNodeMain(mBoardFragment.getHeadJoystick());
        }
*/

        NodeConfiguration nodeConfiguration =
                NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(), getMasterUri());

        nodeMainExecutor.execute(mBoardNode, nodeConfiguration);
        nodeMainExecutor.execute(mVideoFragment.getImageView(), nodeConfiguration.setNodeName(AppConst.RoboBoard.VIDEO_NODE));
        nodeMainExecutor.execute(mBoardFragment.getDriveJoystick(), nodeConfiguration.setNodeName(AppConst.RoboBoard.DRIVE_JOYSTICK_NODE));
//        nodeMainExecutor.execute(mBoardFragment.getHeadJoystick(), nodeConfiguration.setNodeName(AppConst.RoboBoard.HEAD_JOYSTICK_NODE));
        nodeMainExecutor.execute(mBoardOrientationNode, nodeConfiguration.setNodeName(AppConst.RoboBoard.ORIENTATION_NODE));
    }

    private static int getRotation (Context context) {
        int orientation = 0;

        if (context instanceof Activity) {
            orientation = ((Activity)context).getWindowManager().getDefaultDisplay().getRotation();
        } else {
            orientation = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        }

        switch (orientation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                return 0;
        }
    }
}
