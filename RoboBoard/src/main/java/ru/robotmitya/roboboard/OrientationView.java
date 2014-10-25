package ru.robotmitya.roboboard;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import com.badlogic.gdx.math.Vector2;
import org.jetbrains.annotations.NotNull;
import ru.robotmitya.robocommonlib.AppConst;

public class OrientationView extends View {
    private static final float POINTER_SCALE = 1f / 10f;
    private static final float POINTER_BORDER_SCALE = 1f / 30f;

    private GestureDetector mGestureDetector;

    private float mPointerRadius;
    private float mRoundRadius;
    private float mScale;

    private float mX = 0f;
    private float mY = 0f;

    private RectF mBackgroundRect = new RectF();
    private RectF mPointerRect = new RectF();

    private Vector2 mViewCenter = new Vector2();
    private Vector2 mPointerCenter = new Vector2();
    private Vector2 mPreviousPointerCenter;

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
        final float radius = width / 2;
        final float strokeWidth = width * POINTER_BORDER_SCALE;
        mScale = radius - mPointerRadius - strokeWidth;
        mPaintPointer.setStrokeWidth(strokeWidth);
        mPointerRadius = width * POINTER_SCALE;
        mViewCenter.set(width / 2f, height / 2f);
        mBackgroundRect.set(
                mViewCenter.x - radius, mViewCenter.y - radius,
                mViewCenter.x + radius, mViewCenter.y + radius);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRoundRect(mBackgroundRect, mRoundRadius, mRoundRadius, mPaintBackground);

        final int colorId = isEnabled() ? R.color.orientation_pointer_enabled_color : R.color.orientation_pointer_disabled_color;
        mPaintPointer.setColor(getContext().getResources().getColor(colorId));

        mPointerCenter.set(mX, mY);
        mPointerCenter.scl(mScale);
        mPointerCenter.add(mViewCenter);

        if (mPreviousPointerCenter == null) {
            mPreviousPointerCenter = new Vector2(mPointerCenter);
        } else {
            mPreviousPointerCenter.lerp(mPointerCenter, 0.4f);
        }

        mPointerRect.set(
                mPreviousPointerCenter.x - mPointerRadius, mPreviousPointerCenter.y - mPointerRadius,
                mPreviousPointerCenter.x + mPointerRadius, mPreviousPointerCenter.y + mPointerRadius);
        canvas.drawRoundRect(mPointerRect, mRoundRadius, mRoundRadius, mPaintPointer);
    }

    private void init() {
        setEnabled(false);

        initPaint();
        initGestureDetector();
    }

    private static float convertDpToPixel(Context context, float size) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, context.getResources().getDisplayMetrics());
    }

    private void initPaint() {
        float size = getContext().getResources().getDimension(R.dimen.rounded_corner);
        mRoundRadius = convertDpToPixel(getContext(), size);

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
                calibrate();
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                calibrate();
                return true;
            }
        });

    }

    @Override
    public boolean onTouchEvent(@NotNull MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    private void calibrate() {
        Toast.makeText(getContext(), R.string.board_orientation_calibrate_hint, Toast.LENGTH_SHORT).show();
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(mCommandCalibrateIntent);
    }
}
