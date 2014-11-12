package ru.robotmitya.roboboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;
import org.ros.android.view.RosImageView;

/**
 *
 * Created by dmitrydzz on 12.11.14.
 */
public class MasterImageView <T> extends RosImageView {
    private ImageView mSecondaryImageView;

    public MasterImageView(Context context) {
        super(context);
    }

    public MasterImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MasterImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setSecondaryImageView(final ImageView imageView) {
        mSecondaryImageView = imageView;
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        if (mSecondaryImageView != null) {
            mSecondaryImageView.setImageBitmap(bm);
        }
    }
}
