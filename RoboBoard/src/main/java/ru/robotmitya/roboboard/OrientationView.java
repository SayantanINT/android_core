package ru.robotmitya.roboboard;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import org.jetbrains.annotations.NotNull;
import ru.robotmitya.robocommonlib.AppConst;

public class OrientationView extends View {
    private static final float POINTER_SCALE = 1f / 10f;
    private static final float POINTER_BORDER_SCALE = 1f / 30f;

    private GestureDetector mGestureDetector;

    private float mRadius;
    private float mPointerRadius;
    private float mStrokeWidth;
    private float mScale;

    private float mX = 0f;
    private float mY = 0f;

    private Vector2 mViewCenter = new Vector2();
    private Vector2 mPointerCenter = new Vector2();
    private Vector2 mPreviousPointerCenter = new Vector2();

    private Paint mPaintBackground;
    private Paint mPaintPointer;

    private Intent mCommandCalibrateIntent = new Intent(AppConst.RoboBoard.Broadcast.ORIENTATION_CALIBRATE);
    private Intent mCommandActivateIntent = new Intent(AppConst.RoboBoard.Broadcast.ORIENTATION_ACTIVATE);

    @SuppressWarnings("UnusedDeclaration")
    public OrientationView(Context context) {
        super(context);
        init();
    }

    @SuppressWarnings("UnusedDeclaration")
    public OrientationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @SuppressWarnings("UnusedDeclaration")
    public OrientationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setPosition(float x, float y) {
        x = -x;
        if (x < -1) x = -1;
        else if (x > 1) x = 1;
        mX = x;

        y = -y;
        if (y < -1) y = -1;
        else if (y > 1) y = 1;
        mY = y;

        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        final float width = right - left + 1;
        final float height = bottom - top + 1;
        mRadius = width / 2;
        mPointerRadius = width * POINTER_SCALE;
        mStrokeWidth = width * POINTER_BORDER_SCALE;
        mScale = mRadius - mPointerRadius - mStrokeWidth;
        mViewCenter.set(width / 2f, height / 2f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawCircle(mViewCenter.x, mViewCenter.y, mRadius, mPaintBackground);

        mPaintPointer.setStrokeWidth(mStrokeWidth);
        final int colorId = isEnabled() ? R.color.orientation_pointer_enabled_color : R.color.orientation_pointer_disabled_color;
        mPaintPointer.setColor(getContext().getResources().getColor(colorId));

        mPointerCenter.set(mX, mY);
        transformSquareToCircle(mPointerCenter);
        mPointerCenter.scl(mScale);
        mPointerCenter.add(mViewCenter);

        if (mPreviousPointerCenter == null) {
            mPreviousPointerCenter = new Vector2(mPointerCenter);
        } else {
            mPreviousPointerCenter.lerp(mPointerCenter, 0.4f);
        }

        canvas.drawCircle(mPreviousPointerCenter.x, mPreviousPointerCenter.y, mPointerRadius, mPaintPointer);
    }

    private void transformSquareToCircle(Vector2 p) {
        final float x = p.x;
        final float y = p.y;
        final float r = (float) Math.sqrt(x * x + y * y);
        if (r > MathUtils.FLOAT_ROUNDING_ERROR) {
            p.x = Math.signum(x) * x * x / r;
            p.y = Math.signum(y) * y * y / r;
        } else {
            p.x = 0f;
            p.y = 0f;
        }
    }

    private void init() {
        setEnabled(false);

        initPaint();
        initGestureDetector();
    }

    private void initPaint() {
        mPaintBackground = new Paint();
        mPaintBackground.setStyle(Paint.Style.FILL);
        mPaintBackground.setColor(getContext().getResources().getColor(R.color.board_button_background_color));

        mPaintPointer = new Paint();
        mPaintPointer.setStyle(Paint.Style.STROKE);
    }

    private void initGestureDetector() {
        mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                final boolean enabled = !isEnabled();
                setEnabled(enabled);
                mCommandActivateIntent.putExtra(AppConst.RoboBoard.Broadcast.ORIENTATION_ACTIVATE_EXTRA_ENABLED, enabled);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(mCommandActivateIntent);
                final int hintId = enabled ? R.string.board_orientation_sensor_enabled_hint : R.string.board_orientation_sensor_disabled_hint;
                Toast.makeText(getContext(), hintId, Toast.LENGTH_SHORT).show();
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                Toast.makeText(getContext(), R.string.board_orientation_calibrate_hint, Toast.LENGTH_SHORT).show();
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(mCommandCalibrateIntent);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
//                Toast.makeText(getContext(), "onDoubleTap", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

    }

    @Override
    public boolean onTouchEvent(@NotNull MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }
}
