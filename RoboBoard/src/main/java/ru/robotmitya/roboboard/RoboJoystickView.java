package ru.robotmitya.roboboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import com.badlogic.gdx.math.Vector2;
import geometry_msgs.Twist;
import org.jetbrains.annotations.NotNull;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import ru.robotmitya.robocommonlib.AppConst;
import ru.robotmitya.robocommonlib.Log;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * Created by dmitrydzz on 24.10.14.
 */
public class RoboJoystickView extends View implements NodeMain {
    private static final String DEFAULT_NODE_NAME = "virtual_joystick_view";
    private static final String DEFAULT_TOPIC_NAME = "joystick";

    private static final float ZERO_DELTA = 0.05f;

    private static final float POINTER_SCALE = 1f / 10f;
    private static final float POINTER_BORDER_SCALE = 1f / 30f;

    private GestureDetector mGestureDetector;

    private float mPointerRadius;
    private float mPointerCenterRadius;
    private float mRoundRadius;
    private float mScale;

    private float mX = 0f;
    private float mY = 0f;

    private RectF mBackgroundRect = new RectF();
    private RectF mPointerRect = new RectF();

    private Vector2 mViewCenter = new Vector2();
    private Vector2 mPointerCenter = new Vector2();
    private Vector2 mScreenPointerCenter;

    private Paint mPaintBackground;
    private Paint mPaintEnabledHintLines;
    private Paint mPaintDisabledHintLines;
    private Paint mPaintEnabledPointer;
    private Paint mPaintDisabledPointer;
    private Paint mPaintEnabledPointerCenter;
    private Paint mPaintDisabledPointerCenter;

    private boolean mIsInTouch;
    private boolean mMoveToZeroWhenReleased;

    private String mTopicName;
    private boolean mIsConnected = false;
    private Timer mPublisherTimer;
    private Publisher<Twist> mPublisher;
    private geometry_msgs.Twist mCurrentMessage;
    private final ReentrantReadWriteLock mReadWriteLock = new ReentrantReadWriteLock();

    private BroadcastReceiver mBroadcastReceiverActivate;


    @SuppressWarnings("UnusedDeclaration")
    public RoboJoystickView(Context context) {
        super(context);
        init();
    }

    @SuppressWarnings("UnusedDeclaration")
    public RoboJoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @SuppressWarnings("UnusedDeclaration")
    public RoboJoystickView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mTopicName = DEFAULT_TOPIC_NAME;
        initPaint();
        initGestureDetector();
    }

    private void initPaint() {
        float size = getContext().getResources().getDimension(R.dimen.rounded_corner);
        mRoundRadius = convertDpToPixel(getContext(), size);

        mPaintBackground = new Paint();
        mPaintBackground.setStyle(Paint.Style.FILL);
        mPaintBackground.setColor(getContext().getResources().getColor(R.color.board_button_background_color));

        final int colorEnabled = getContext().getResources().getColor(R.color.joystick_pointer_enabled_color);
        final int colorDisabled = getContext().getResources().getColor(R.color.joystick_pointer_disabled_color);

        mPaintEnabledHintLines = new Paint();
        mPaintEnabledHintLines.setStyle(Paint.Style.STROKE);
        mPaintEnabledHintLines.setStrokeWidth(convertDpToPixel(getContext(), 1));
        mPaintEnabledHintLines.setColor(colorEnabled);

        mPaintDisabledHintLines = new Paint(mPaintEnabledHintLines);
        mPaintDisabledHintLines.setColor(colorDisabled);

        mPaintEnabledPointer = new Paint();
        mPaintEnabledPointer.setStyle(Paint.Style.STROKE);
        mPaintEnabledPointer.setColor(colorEnabled);

        mPaintDisabledPointer = new Paint(mPaintEnabledPointer);
        mPaintDisabledPointer.setColor(colorDisabled);

        mPaintEnabledPointerCenter = new Paint();
        mPaintEnabledPointerCenter.setStyle(Paint.Style.FILL);
        mPaintEnabledPointerCenter.setColor(colorEnabled);

        mPaintDisabledPointerCenter = new Paint(mPaintEnabledPointerCenter);
        mPaintDisabledPointerCenter.setColor(colorDisabled);
    }

    private void initGestureDetector() {
        mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (mMoveToZeroWhenReleased) {
                    return true;
                }

                mPointerCenter.set(e.getX(), e.getY());
                screenToJoystick(mPointerCenter);

                // Near zero is zero
                if (isZero(mPointerCenter)) {
                    mPointerCenter.setZero();
                }

                setPosition(mPointerCenter.x, mPointerCenter.y);
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                setPosition(0, 0);
                return true;
            }
        });
    }

    private boolean isZero(final Vector2 p) {
        return (Math.abs(p.x) < ZERO_DELTA) && (Math.abs(p.y) < ZERO_DELTA);
    }

    private void setPosition(final float x, final float y) {
        mX = x;
        mY = y;
        mScreenPointerCenter = null;
        invalidate();
    }

    private void screenToJoystick(final Vector2 p) {
        p.add(-mViewCenter.x, -mViewCenter.y);
        p.scl(1f / mScale);
        if (p.x < -1) p.x = -1;
        else if (p.x > 1) p.x = 1;
        p.y = -p.y;
        if (p.y < -1) p.y = -1;
        else if (p.y > 1) p.y = 1;
    }

    private void joystickToScreen(final Vector2 p) {
        p.y = -p.y;
        p.scl(mScale);
        p.add(mViewCenter);
    }

    @Override
    public boolean onTouchEvent(@NotNull MotionEvent event) {
        if (!mIsConnected) {
            return true;
        }

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mIsInTouch = event.getPointerCount() == 1;
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                mIsInTouch = mIsInTouch && (event.getPointerCount() == 0);
                if (mMoveToZeroWhenReleased && !mIsInTouch) {
                    setPosition(0, 0);
                }
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsInTouch) {
                    mPointerCenter.set(event.getX(), event.getY());
                    screenToJoystick(mPointerCenter);
                    mX = mPointerCenter.x;
                    mY = mPointerCenter.y;
                    invalidate();
                }
                break;
        }

        return mGestureDetector.onTouchEvent(event);
    }

    private static float convertDpToPixel(Context context, float size) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, context.getResources().getDisplayMetrics());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        final float width = right - left + 1;
        final float height = bottom - top + 1;
        final float radius = width / 2;
        mPointerRadius = width * POINTER_SCALE;
        mPointerCenterRadius = mPointerRadius / 2;
        final float strokeWidth = width * POINTER_BORDER_SCALE;
        mScale = radius - mPointerRadius - strokeWidth;

        mViewCenter.set(width / 2f, height / 2f);
        mBackgroundRect.set(
                mViewCenter.x - radius, mViewCenter.y - radius,
                mViewCenter.x + radius, mViewCenter.y + radius);

        mPaintEnabledPointer.setStrokeWidth(strokeWidth);
        mPaintDisabledPointer.setStrokeWidth(strokeWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRoundRect(mBackgroundRect, mRoundRadius, mRoundRadius, mPaintBackground);

        mPointerCenter.set(mX, mY);
        final boolean isZero = isZero(mPointerCenter);
        joystickToScreen(mPointerCenter);

        if (mScreenPointerCenter == null) {
            mScreenPointerCenter = new Vector2(mPointerCenter);
        } else {
            mScreenPointerCenter.lerp(mPointerCenter, 0.9f); //0.4f);
        }

        mPointerCenter.set(mScreenPointerCenter);
        screenToJoystick(mPointerCenter);
        setCurrentMessage(mPointerCenter.x, mPointerCenter.y);

        if (mIsInTouch) {
            Paint paintHintLines = isZero ? mPaintDisabledHintLines : mPaintEnabledHintLines;
            canvas.drawLine(0, mScreenPointerCenter.y, getRight(), mScreenPointerCenter.y, paintHintLines);
            canvas.drawLine(mScreenPointerCenter.x, 0, mScreenPointerCenter.x, getBottom(), paintHintLines);
        }

        Paint paintPointerBorder = isZero ? mPaintDisabledPointer : mPaintEnabledPointer;
        mPointerRect.set(
                mScreenPointerCenter.x - mPointerRadius, mScreenPointerCenter.y - mPointerRadius,
                mScreenPointerCenter.x + mPointerRadius, mScreenPointerCenter.y + mPointerRadius);
        canvas.drawRoundRect(mPointerRect, mRoundRadius, mRoundRadius, paintPointerBorder);

        Paint paintPointerCenter = isZero ? mPaintDisabledPointerCenter : mPaintEnabledPointerCenter;
        mPointerRect.set(
                mScreenPointerCenter.x - mPointerCenterRadius, mScreenPointerCenter.y - mPointerCenterRadius,
                mScreenPointerCenter.x + mPointerCenterRadius, mScreenPointerCenter.y + mPointerCenterRadius);
        canvas.drawRoundRect(mPointerRect, mRoundRadius, mRoundRadius, paintPointerCenter);
    }

    public void setTopicName(String topicName) {
        mTopicName = topicName;
    }

    public void setMoveToZeroWhenReleased(boolean value) {
        mMoveToZeroWhenReleased = value;
    }

    private geometry_msgs.Twist getCurrentMessage() {
        mReadWriteLock.readLock().lock();
        try {
            return mCurrentMessage;
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    private void setCurrentMessage(final float x, final float y) {
        mReadWriteLock.writeLock().lock();
        try {
            if (mCurrentMessage == null) {
                return;
            }
            mCurrentMessage.getAngular().setX(0);
            mCurrentMessage.getAngular().setY(0);
            mCurrentMessage.getAngular().setZ(-x);
            mCurrentMessage.getLinear().setX(y);
            mCurrentMessage.getLinear().setY(0);
            mCurrentMessage.getLinear().setZ(0);
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }


    //************** NodeMain implementation ***************
    //**************           begin         ***************
    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(DEFAULT_NODE_NAME);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        mPublisher = connectedNode.newPublisher(mTopicName, geometry_msgs.Twist._TYPE);
        mCurrentMessage = mPublisher.newMessage();

        mPublisherTimer = new Timer();
        mPublisherTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (RoboJoystickView.this.isEnabled()) {
                    mPublisher.publish(getCurrentMessage());
                }
            }
        }, 0, 80);

        mBroadcastReceiverActivate = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String topicName = intent.getStringExtra(AppConst.RoboBoard.Broadcast.JOYSTICK_ACTIVATE_EXTRA_TOPIC);
                if (topicName == null) topicName = "";
                if (topicName.contentEquals(mTopicName)) {
                    Log.d(RoboJoystickView.this, "broadcast received: activate");
                    setEnabled(intent.getBooleanExtra(AppConst.RoboBoard.Broadcast.JOYSTICK_ACTIVATE_EXTRA_ENABLED, false));
                }
            }
        };

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(
                mBroadcastReceiverActivate, new IntentFilter(AppConst.RoboBoard.Broadcast.JOYSTICK_ACTIVATE));

        mIsConnected = true;
    }

    @Override
    public void onShutdown(Node node) {
    }

    @Override
    public void onShutdownComplete(Node node) {
        mPublisherTimer.cancel();
        mPublisherTimer.purge();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mBroadcastReceiverActivate);
    }

    @Override
    public void onError(Node node, Throwable throwable) {
    }
    //**************            end          ***************
    //************** NodeMain implementation ***************


//    @Override
//    public void setEnabled(boolean enabled) {
//        super.setEnabled(enabled);
//        Log.d(this, "======== " + mTopicName + " " + enabled);
//    }
}
