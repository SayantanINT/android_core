package ru.robotmitya.roboboard;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

//import org.ros.android.view.VirtualJoystickView;

import ru.robotmitya.robocommonlib.AppConst;
import ru.robotmitya.robocommonlib.Log;
import ru.robotmitya.robocommonlib.MessageHelper;
import ru.robotmitya.robocommonlib.Rs;

public class BoardFragment extends Fragment {

    private BroadcastReceiver mBatteryBroadcastReceiver;
    private BroadcastReceiver mButtonStateBroadcastReceiver;

    private CheckableImageView mButtonFaceOk;
    private CheckableImageView mButtonFaceIll;
    private CheckableImageView mButtonFaceAngry;
    private CheckableImageView mButtonFaceBlue;
    private CheckableImageView mButtonFaceHappy;

    private CheckableImageView mButtonActionPlay;
    private CheckableImageView mButtonActionDance;
    private CheckableImageView mButtonActionAngry;
    private CheckableImageView mButtonActionBlue;
    private CheckableImageView mButtonActionHappy;

    private CheckableImageView mButtonHeadlights;

    private CheckableImageView mButtonSwitchCamera;

    private BoardJoystickView mDriveJoystick;
    private BoardJoystickView mHeadJoystick;

    private ImageView mImageViewRobotBattery;
    private ImageView mImageViewRobotPlugged;
    private TextView mTextViewRobotBatteryVoltage;

    private ImageView mImageViewHeadBattery;
    private ImageView mImageViewHeadPlugged;
    private TextView mTextViewHeadBatteryPercent;

    private ImageView mImageViewBoardBattery;
    private ImageView mImageViewBoardPlugged;
    private TextView mTextViewBoardBatteryPercent;

    public BoardFragment() {
        super();

        mBatteryBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mTextViewBoardBatteryPercent == null) {
                    return;
                }

                try {
                    int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                    int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                    int percent = (scale > 0) && (level > 0) ? level * 100 / scale : 0;

                    final int NOT_PLUGGED = 0;
                    if (plugged == NOT_PLUGGED) {
                        mImageViewBoardBattery.setVisibility(View.VISIBLE);
                        mImageViewBoardPlugged.setVisibility(View.INVISIBLE);
                    } else {
                        mImageViewBoardBattery.setVisibility(View.INVISIBLE);
                        mImageViewBoardPlugged.setVisibility(View.VISIBLE);
                    }
                    mTextViewBoardBatteryPercent.setText(percent + "%");
                } catch (Exception e) {
                    Log.e(this, e.getMessage());
                }
            }
        };

        mButtonStateBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = intent.getStringExtra(AppConst.RoboBoard.Broadcast.BROADCAST_MESSAGE_TO_GUI_EXTRA_NAME);
                Log.d(BoardFragment.this, "message received: " + message);

                String identifier = MessageHelper.getMessageIdentifier(message);
                int value = MessageHelper.getMessageIntegerValue(message);
                if (identifier.contentEquals(Rs.Mood.ID)) {
                    mButtonFaceOk.setChecked(false);
                    mButtonFaceIll.setChecked(false);
                    mButtonFaceAngry.setChecked(false);
                    mButtonFaceBlue.setChecked(false);
                    mButtonFaceHappy.setChecked(false);

                    mButtonActionPlay.setChecked(false);
                    mButtonActionDance.setChecked(false);
                    mButtonActionAngry.setChecked(false);
                    mButtonActionBlue.setChecked(false);
                    mButtonActionHappy.setChecked(false);

                    switch (value) {
                        case Rs.Mood.ACTION_NOSE:
                        case Rs.Mood.FACE_OK:
                            mButtonFaceOk.setChecked(true);
                            break;
                        case Rs.Mood.FACE_ILL:
                            mButtonFaceIll.setChecked(true);
                            break;
                        case Rs.Mood.FACE_ANGRY:
                            mButtonFaceAngry.setChecked(true);
                            break;
                        case Rs.Mood.FACE_BLUE:
                            mButtonFaceBlue.setChecked(true);
                            break;
                        case Rs.Mood.FACE_HAPPY:
                            mButtonFaceHappy.setChecked(true);
                            break;

                        case Rs.Mood.ACTION_PLAY:
                            mButtonActionPlay.setChecked(true);
                            break;
                        case Rs.Mood.ACTION_DANCE:
                            mButtonActionDance.setChecked(true);
                            break;
                        case Rs.Mood.ACTION_ANGRY:
                            mButtonActionAngry.setChecked(true);
                            break;
                        case Rs.Mood.ACTION_SAD:
                            mButtonActionBlue.setChecked(true);
                            break;
                        case Rs.Mood.ACTION_HAPPY:
                            mButtonActionHappy.setChecked(true);
                            break;

                    }
                } else if (identifier.contentEquals(Rs.Instruction.ID)) {
                    switch (value) {
                        case Rs.Instruction.HEADLIGHTS_OFF:
                            mButtonHeadlights.setChecked(false);
                            break;
                        case Rs.Instruction.HEADLIGHTS_ON:
                            mButtonHeadlights.setChecked(true);
                            break;
                        case Rs.Instruction.CAMERA_OFF:
                        case Rs.Instruction.CAMERA_FRONT_ON:
                            mButtonSwitchCamera.setChecked(false);
                            break;
                        case Rs.Instruction.CAMERA_BACK_ON:
                            mButtonSwitchCamera.setChecked(true);
                            break;
                    }
                } else if (identifier.contentEquals(Rs.BatteryResponse.ID)) {
                    final int battery = value & 0xF000;
                    switch ((short)battery) {
                        case Rs.BatteryResponse.ROBOT_BATTERY_VOLTAGE: {
                            final int centivolts = value & 0x0FFF;
                            final float voltage = centivolts / 100F;
                            mTextViewRobotBatteryVoltage.setText(
                                    String.format("%.2f " + getString(R.string.board_volts), voltage));
                            break;
                        }
                        case Rs.BatteryResponse.ROBOT_POWER_SUPPLY_VOLTAGE: {
                            final int voltage = value & 0x0FFF;
                            // Let's think that the robot is plugged in power supply
                            // if ~1XXX-message voltage is greater then 5 volts (500 centivolts).
                            if (voltage > 500) {
                                mImageViewRobotBattery.setVisibility(View.INVISIBLE);
                                mImageViewRobotPlugged.setVisibility(View.VISIBLE);
                            } else {
                                mImageViewRobotBattery.setVisibility(View.VISIBLE);
                                mImageViewRobotPlugged.setVisibility(View.INVISIBLE);
                            }
                            break;
                        }
                        case Rs.BatteryResponse.ROBOHEAD_BATTERY: {
                            final int plugged = value & 0x0F00;
                            final int percent = value & 0x00FF;
                            final int NOT_PLUGGED = 0;
                            if (plugged == NOT_PLUGGED) {
                                mImageViewHeadBattery.setVisibility(View.VISIBLE);
                                mImageViewHeadPlugged.setVisibility(View.INVISIBLE);
                            } else {
                                mImageViewHeadBattery.setVisibility(View.INVISIBLE);
                                mImageViewHeadPlugged.setVisibility(View.VISIBLE);
                            }
                            mTextViewHeadBatteryPercent.setText(percent + "%");
                            break;
                        }
                    }
                }
            }
        };
    }

    private void setHeadJoystickVisibility() {
        int visibility;
        if (SettingsFragment.getRemoteControlMode() == SettingsFragment.REMOTE_CONTROL_MODE.TWO_JOYSTICKS) {
            visibility = View.VISIBLE;
        } else {
            visibility = View.GONE;
        }
        mHeadJoystick.setVisibility(visibility);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.board_fragment, container, false);
        if (result == null) {
            return null;
        }

        // Face buttons:
        mButtonFaceOk = (CheckableImageView) result.findViewById(R.id.buttonFaceOk);
        mButtonFaceOk.setTag(Rs.Mood.FACE_OK);
        mButtonFaceOk.setOnClickListener(faceButtonListener);

        mButtonFaceIll = (CheckableImageView) result.findViewById(R.id.buttonFaceIll);
        mButtonFaceIll.setTag(Rs.Mood.FACE_ILL);
        mButtonFaceIll.setOnClickListener(faceButtonListener);

        mButtonFaceAngry = (CheckableImageView) result.findViewById(R.id.buttonFaceAngry);
        mButtonFaceAngry.setTag(Rs.Mood.FACE_ANGRY);
        mButtonFaceAngry.setOnClickListener(faceButtonListener);

        mButtonFaceBlue = (CheckableImageView) result.findViewById(R.id.buttonFaceBlue);
        mButtonFaceBlue.setTag(Rs.Mood.FACE_BLUE);
        mButtonFaceBlue.setOnClickListener(faceButtonListener);

        mButtonFaceHappy = (CheckableImageView) result.findViewById(R.id.buttonFaceHappy);
        mButtonFaceHappy.setTag(Rs.Mood.FACE_HAPPY);
        mButtonFaceHappy.setOnClickListener(faceButtonListener);

        // Action buttons:
        mButtonActionPlay = (CheckableImageView) result.findViewById(R.id.buttonActionPlay);
        mButtonActionPlay.setTag(Rs.Mood.ACTION_PLAY);
        mButtonActionPlay.setOnClickListener(reflexButtonListener);

        mButtonActionDance = (CheckableImageView) result.findViewById(R.id.buttonActionDance);
        mButtonActionDance.setTag(Rs.Mood.ACTION_DANCE);
        mButtonActionDance.setOnClickListener(reflexButtonListener);

        mButtonActionAngry = (CheckableImageView) result.findViewById(R.id.buttonActionAngry);
        mButtonActionAngry.setTag(Rs.Mood.ACTION_ANGRY);
        mButtonActionAngry.setOnClickListener(reflexButtonListener);

        mButtonActionBlue = (CheckableImageView) result.findViewById(R.id.buttonActionBlue);
        mButtonActionBlue.setTag(Rs.Mood.ACTION_SAD);
        mButtonActionBlue.setOnClickListener(reflexButtonListener);

        mButtonActionHappy = (CheckableImageView) result.findViewById(R.id.buttonActionHappy);
        mButtonActionHappy.setTag(Rs.Mood.ACTION_HAPPY);
        mButtonActionHappy.setOnClickListener(reflexButtonListener);

        // Other buttons:
        ImageView buttonSettings = (ImageView) result.findViewById(R.id.buttonSettings);
        buttonSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
            }
        });

        ImageView buttonActionLike = (ImageView) result.findViewById(R.id.buttonActionLike);
        buttonActionLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String command = MessageHelper.makeMessage(Rs.Tail.ID, Rs.Tail.WAG_1);
                sendMessageToBoardNodeForBodyTopic(v.getContext(), command);
            }
        });

        mButtonHeadlights = (CheckableImageView) result.findViewById(R.id.buttonHeadlights);
        mButtonHeadlights.setOnClickListener(headlightsButtonListener);

        ImageView buttonActionNo = (ImageView) result.findViewById(R.id.buttonActionNo);
        buttonActionNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String command = MessageHelper.makeMessage(Rs.No.ID, Rs.No.SHAKE_1);
                sendMessageToBoardNodeForBodyTopic(v.getContext(), command);
            }
        });

        ImageView buttonActionYes = (ImageView) result.findViewById(R.id.buttonActionYes);
        buttonActionYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String command = MessageHelper.makeMessage(Rs.Yes.ID, Rs.Yes.SHAKE_1);
                sendMessageToBoardNodeForBodyTopic(v.getContext(), command);
            }
        });

        ImageView buttonStateRequest = (ImageView) result.findViewById(R.id.buttonStateRequest);
        buttonStateRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String command = MessageHelper.makeMessage(Rs.Instruction.ID, Rs.Instruction.STATE_REQUEST);
                sendMessageToBoardNodeForFaceTopic(v.getContext(), command);
                sendMessageToBoardNodeForEyeTopic(v.getContext(), command);
                sendMessageToBoardNodeForBodyTopic(v.getContext(), command);
            }
        });

        mButtonSwitchCamera = (CheckableImageView) result.findViewById(R.id.buttonSwitchCam);
        mButtonSwitchCamera.setOnClickListener(switchCamButtonListener);

        // Joysticks:
        mDriveJoystick = (BoardJoystickView) result.findViewById(R.id.drive_joystick);
        mDriveJoystick.setTopicName(AppConst.RoboHead.DRIVE_JOYSTICK_TOPIC);
        mHeadJoystick = (BoardJoystickView) result.findViewById(R.id.head_joystick);
        mHeadJoystick.setTopicName(AppConst.RoboHead.HEAD_JOYSTICK_TOPIC);
        setHeadJoystickVisibility();


        // Batteries status:
        mImageViewRobotBattery = (ImageView) result.findViewById(R.id.imageRobotBattery);
        mImageViewRobotPlugged = (ImageView) result.findViewById(R.id.imageRobotPlugged);
        mTextViewRobotBatteryVoltage = (TextView) result.findViewById(R.id.textRobotCharged);

        mImageViewHeadBattery = (ImageView) result.findViewById(R.id.imageHeadBattery);
        mImageViewHeadPlugged = (ImageView) result.findViewById(R.id.imageHeadPlugged);
        mTextViewHeadBatteryPercent = (TextView) result.findViewById(R.id.textHeadCharged);

        mImageViewBoardBattery = (ImageView) result.findViewById(R.id.imageBoardBattery);
        mImageViewBoardPlugged = (ImageView) result.findViewById(R.id.imageBoardPlugged);
        mTextViewBoardBatteryPercent = (TextView) result.findViewById(R.id.textBoardCharged);

        return result;
    }

    private View.OnClickListener faceButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            CheckableImageView button = (CheckableImageView)v;
            short value = button.isChecked() ? Rs.Mood.FACE_OK : (Short)v.getTag();
            String command = MessageHelper.makeMessage(Rs.Mood.ID, value);
            sendMessageToBoardNodeForFaceTopic(v.getContext(), command);
        }
    };

    private View.OnClickListener reflexButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            short value = (Short)v.getTag();
            String command = MessageHelper.makeMessage(Rs.Mood.ID, value);
            sendMessageToBoardNodeForReflexTopic(v.getContext(), command);
        }
    };

    private View.OnClickListener headlightsButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            short value = mButtonHeadlights.isChecked() ? Rs.Instruction.HEADLIGHTS_OFF : Rs.Instruction.HEADLIGHTS_ON;
            String command = MessageHelper.makeMessage(Rs.Instruction.ID, value);
            sendMessageToBoardNodeForBodyTopic(v.getContext(), command);
        }
    };

    private View.OnClickListener switchCamButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            short value = mButtonSwitchCamera.isChecked() ? Rs.Instruction.CAMERA_FRONT_ON : Rs.Instruction.CAMERA_BACK_ON;
            String command = MessageHelper.makeMessage(Rs.Instruction.ID, value);
            sendMessageToBoardNodeForEyeTopic(v.getContext(), command);
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        final Context context = getActivity();
        if (context != null) {
            context.registerReceiver(mBatteryBroadcastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            LocalBroadcastManager.getInstance(context).registerReceiver(
                    mButtonStateBroadcastReceiver, new IntentFilter(AppConst.RoboBoard.Broadcast.BROADCAST_MESSAGE_TO_GUI_NAME));
        }

        setHeadJoystickVisibility();
    }

    @Override
    public void onPause() {
        final Context context = getActivity();
        if (context != null) {
            context.unregisterReceiver(mBatteryBroadcastReceiver);
            LocalBroadcastManager.getInstance(context).unregisterReceiver(mButtonStateBroadcastReceiver);
        }
        super.onPause();
    }

    public BoardJoystickView getDriveJoystick() {
        return mDriveJoystick;
    }

    public BoardJoystickView getHeadJoystick() {
        return mHeadJoystick;
    }

    private void sendMessageToBoardNodeForBodyTopic(final Context context, final String command) {
        Intent intent = new Intent(AppConst.RoboBoard.Broadcast.BROADCAST_MESSAGE_TO_BODY_NAME);
        intent.putExtra(AppConst.RoboBoard.Broadcast.BROADCAST_MESSAGE_TO_BODY_EXTRA_NAME, command);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void sendMessageToBoardNodeForEyeTopic(final Context context, final String command) {
        Intent intent = new Intent(AppConst.RoboBoard.Broadcast.BROADCAST_MESSAGE_TO_EYE_NAME);
        intent.putExtra(AppConst.RoboBoard.Broadcast.BROADCAST_MESSAGE_TO_EYE_EXTRA_NAME, command);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void sendMessageToBoardNodeForFaceTopic(final Context context, final String command) {
        Intent intent = new Intent(AppConst.RoboBoard.Broadcast.BROADCAST_MESSAGE_TO_FACE_NAME);
        intent.putExtra(AppConst.RoboBoard.Broadcast.BROADCAST_MESSAGE_TO_FACE_EXTRA_NAME, command);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void sendMessageToBoardNodeForReflexTopic(final Context context, final String command) {
        Intent intent = new Intent(AppConst.RoboBoard.Broadcast.BROADCAST_MESSAGE_TO_REFLEX_NAME);
        intent.putExtra(AppConst.RoboBoard.Broadcast.BROADCAST_MESSAGE_TO_REFLEX_EXTRA_NAME, command);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

}
