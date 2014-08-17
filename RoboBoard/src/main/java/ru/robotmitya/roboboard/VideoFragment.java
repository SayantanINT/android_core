package ru.robotmitya.roboboard;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.view.RosImageView;

import ru.robotmitya.robocommonlib.AppConst;

public class VideoFragment extends Fragment {

    private RosImageView<sensor_msgs.CompressedImage> mImageView;

    public VideoFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.video_fragment, container, false);
        if (result == null) {
            return null;
        }

        mImageView = (RosImageView<sensor_msgs.CompressedImage>) result.findViewById(R.id.imageViewVideo);
        mImageView.setTopicName(AppConst.RoboBoard.CAMERA_TOPIC);
        mImageView.setMessageType(sensor_msgs.CompressedImage._TYPE);
        mImageView.setMessageToBitmapCallable(new BitmapFromCompressedImage());

        return result;
    }

    public RosImageView<sensor_msgs.CompressedImage> getImageView() {
        return mImageView;
    }
}
