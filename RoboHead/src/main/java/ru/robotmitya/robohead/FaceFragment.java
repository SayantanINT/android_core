package ru.robotmitya.robohead;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import ru.robotmitya.robocommonlib.Log;

public class FaceFragment extends Fragment {
    private FaceHelper mFaceHelper;
    private BroadcastReceiver mMessageReceiver;

    public FaceFragment() {
        super();

        mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = intent.getStringExtra(FaceNode.BROADCAST_FACE_CHANGE_EXTRA_NAME);
                Log.d(FaceFragment.this, "message received: " + message);
                FaceType faceType = FaceHelper.messageToFaceType(message);
                if ((mFaceHelper != null) && (faceType != FaceType.ftUnknown)) {
                    mFaceHelper.setFace(faceType);
                }
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.face_fragment, container, false);

        if (result == null) {
            return null;
        }

        ImageView faceImageView = (ImageView) result.findViewById(R.id.imageViewFace);
        mFaceHelper = new FaceHelper(faceImageView);
        mFaceHelper.setFace(FaceType.ftOk);

        new FaceTouchHelper(getActivity(), faceImageView, mFaceHelper);

        ImageButton settingsButton = (ImageButton) result.findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
            }
        });

        return result;
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mMessageReceiver, new IntentFilter(FaceNode.BROADCAST_FACE_CHANGE_NAME));
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }
}
