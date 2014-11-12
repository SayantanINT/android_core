package ru.robotmitya.roboboard;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.view.RosImageView;

import ru.robotmitya.robocommonlib.AppConst;

public class VideoFragment extends Fragment {

    private MasterImageView<sensor_msgs.CompressedImage> mMasterImageView;
    private ImageView mSecondaryView;

    public VideoFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.video_fragment, container, false);
        if (result == null) {
            return null;
        }

        mSecondaryView = (ImageView) result.findViewById(R.id.imageSecondaryViewVideo);

        mMasterImageView = (MasterImageView<sensor_msgs.CompressedImage>) result.findViewById(R.id.imageViewVideo);
        mMasterImageView.setTopicName(AppConst.RoboBoard.CAMERA_TOPIC);
        mMasterImageView.setMessageType(sensor_msgs.CompressedImage._TYPE);
        mMasterImageView.setMessageToBitmapCallable(new BitmapFromCompressedImage());
        mMasterImageView.setSecondaryImageView(mSecondaryView);

        return result;
    }

    public RosImageView<sensor_msgs.CompressedImage> getImageView() {
        return mMasterImageView;
    }
}
