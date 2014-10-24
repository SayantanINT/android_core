package ru.robotmitya.roboboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
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

import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * Created by dmitrydzz on 24.10.14.
 */
public class RoboJoystickView extends View implements NodeMain {
    private static final String DEFAULT_NODE_NAME = "virtual_joystick_view";
    private static final String DEFAULT_TOPIC_NAME = "joystick";

    private static final float POINTER_SCALE = 1f / 10f;
    private static final float POINTER_BORDER_SCALE = 1f / 30f;
    private static final float POINTER_ROUND_SCALE = 1f / 20f;

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

    private boolean mIsInTouch;

    private String mTopicName;
    private boolean mIsConnected = false;
    private Timer mPublisherTimer;
    private Publisher<Twist> mPublisher;
    private geometry_msgs.Twist mCurrentMessage;


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
                mPointerCenter.set(e.getX(), e.getY());
                screenToJoystick(mPointerCenter);
                setPosition(mPointerCenter.x, mPointerCenter.y);
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                setPosition(0, 0);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                setPosition(0, 0);
                return true;
            }
        });
    }

    private void setPosition(final float x, final float y) {
        mX = x;
        mY = y;
        mPreviousPointerCenter = null;
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
                break;
            case MotionEvent.ACTION_UP:
                mIsInTouch = mIsInTouch && (event.getPointerCount() == 0);
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

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        final float width = right - left + 1;
        final float height = bottom - top + 1;
        final float radius = width / 2;
        mPointerRadius = width * POINTER_SCALE;
        final float strokeWidth = width * POINTER_BORDER_SCALE;
        mRoundRadius = width * POINTER_ROUND_SCALE;
        mScale = radius - mPointerRadius - strokeWidth;

        mViewCenter.set(width / 2f, height / 2f);
        mBackgroundRect.set(
                mViewCenter.x - radius, mViewCenter.y - radius,
                mViewCenter.x + radius, mViewCenter.y + radius);

        mPaintPointer.setStrokeWidth(strokeWidth);
        mPaintPointer.setColor(getContext().getResources().getColor(R.color.joystick_color));

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRoundRect(mBackgroundRect, mRoundRadius, mRoundRadius, mPaintBackground);

        mPointerCenter.set(mX, mY);
        joystickToScreen(mPointerCenter);

        if (mPreviousPointerCenter == null) {
            mPreviousPointerCenter = new Vector2(mPointerCenter);
        } else {
            mPreviousPointerCenter.lerp(mPointerCenter, 0.4f);
        }

        mPointerRect.set(mPreviousPointerCenter.x - mPointerRadius, mPreviousPointerCenter.y - mPointerRadius, mPreviousPointerCenter.x + mPointerRadius, mPreviousPointerCenter.y + mPointerRadius);
        canvas.drawRoundRect(mPointerRect, mRoundRadius, mRoundRadius, mPaintPointer);
    }

    public void setTopicName(String topicName) {
        mTopicName = topicName;
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
                mPublisher.publish(mCurrentMessage);
            }
        }, 0, 80);

        mIsConnected = true;
    }

    @Override
    public void onShutdown(Node node) {
    }

    @Override
    public void onShutdownComplete(Node node) {
        mPublisherTimer.cancel();
        mPublisherTimer.purge();
    }

    @Override
    public void onError(Node node, Throwable throwable) {
    }
    //**************            end          ***************
    //************** NodeMain implementation ***************
}
