package ru.robotmitya.roboboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import org.jetbrains.annotations.NotNull;

public class OrientationView extends View {
    private static final float POINTER_SCALE = 1f / 10f;
    private static final float POINTER_BORDER_SCALE = 1f / 30f;

    private GestureDetector mGestureDetector;

//    private float mX = 0;
//    private float mY = 0;

    private float mPointerRadius;

    private Paint mPaintBackground;
    private Paint mPaintPointer;

    @SuppressWarnings("UnusedDeclaration")
    public OrientationView(Context context) {
        super(context);
        initGestureDetector();
    }

    @SuppressWarnings("UnusedDeclaration")
    public OrientationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initGestureDetector();
    }

    @SuppressWarnings("UnusedDeclaration")
    public OrientationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initGestureDetector();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mPointerRadius = (right - left) * POINTER_SCALE;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final float width = getWidth();
        final float height = getHeight();

        canvas.drawCircle(width / 2f, height / 2f, width / 2f, mPaintBackground);

        final float strokeWidth = width * POINTER_BORDER_SCALE;
        mPaintPointer.setStrokeWidth(strokeWidth);
        final int colorId = isEnabled() ? R.color.orientation_pointer_enabled_color : R.color.orientation_pointer_disabled_color;
        mPaintPointer.setColor(getContext().getResources().getColor(colorId));

        float pointerCenterX = width / 2f + 0; //...
        float pointerCenterY = height / 2f + 0; //...
        canvas.drawCircle(pointerCenterX, pointerCenterY, mPointerRadius, mPaintPointer);
    }

    private void initGestureDetector() {
        mPaintBackground = new Paint();
        mPaintBackground.setStyle(Paint.Style.FILL);
        mPaintBackground.setColor(getContext().getResources().getColor(R.color.board_button_background_color));

        mPaintPointer = new Paint();
        mPaintPointer.setStyle(Paint.Style.STROKE);

        mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                setEnabled(!isEnabled());
                Toast.makeText(getContext(), "onSingleTapConfirmed", Toast.LENGTH_SHORT).show();
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                Toast.makeText(getContext(), "onLongPress", Toast.LENGTH_SHORT).show();
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                Toast.makeText(getContext(), "onDoubleTap", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

    }

    @Override
    public boolean onTouchEvent(@NotNull MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }
}
