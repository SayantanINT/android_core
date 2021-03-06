package ru.robotmitya.robocommonlib;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import org.ros.node.NodeMain;

/**
 * Created by dmitrydzz on 1/28/14.
 * @author dmitrydzz
 *
 */
@SuppressWarnings("UnusedDeclaration")
public final class Log {
    /**
     * Flag to enable/disable logging.
     */
    private static boolean ENABLE_LOG;

    static {
        ENABLE_LOG = false;
    }

    /**
     * Tag to filter.
     */
    public static final String LOG_TAG = "Mitya";

    private Log() { }

    public static void setEnabled(final boolean enabled) {
        ENABLE_LOG = enabled;
    }

    public static boolean getEnabled() {
        return ENABLE_LOG;
    }

    /**
     * Log details.
     * @param source object - the source of event.
     * @param msg message text.
     */
    public static void v(final Object source, final String msg) {
        if (ENABLE_LOG) {
            android.util.Log.v(LOG_TAG, source.getClass().getName() + " => " + msg);
        }
    }

    /**
     * Log warning.
     * @param source object - the source of event.
     * @param msg message text.
     */
    public static void w(final Object source, final String msg) {
        if (ENABLE_LOG) {
            android.util.Log.w(LOG_TAG, source.getClass().getName() + " => " + msg);
        }
    }

    /**
     * Log info.
     * @param source object - the source of event.
     * @param msg message text.
     */
    public static void i(final Object source, final String msg) {
        if (ENABLE_LOG) {
            android.util.Log.i(LOG_TAG, source.getClass().getName() + " => " + msg);
        }
    }

    /**
     * Log debug info.
     * @param source object - the source of event.
     * @param msg message text.
     */
    public static void d(final Object source, final String msg) {
        if (ENABLE_LOG) {
            android.util.Log.d(LOG_TAG, source.getClass().getName() + " => " + msg);
        }
    }

    public static String fmt(final float v) {
        return String.format("%+3.1f", v);
    }

    public static String fmt(final Vector2 v) {
        return String.format("%+3.1f, %+3.1f", v.x, v.y);
    }

    public static String fmt(final Vector3 v) {
        return String.format("%+3.1f, %+3.1f, %+3.1f", v.x, v.y, v.z);
    }

    public static String fmt(final Quaternion q) {
        Vector3 v = new Vector3();
        float a = q.getAxisAngle(v);
        return String.format("%+3.1f, %+3.1f, %+3.1f", v.x, v.y, v.z) + ", " + String.format("%+3.1f", a);
    }

    /**
     * Log error.
     * @param source object - the source of event.
     * @param msg message text.
     */
    public static void e(final Object source, final String msg) {
        android.util.Log.e(LOG_TAG, source.getClass().getName() + " => " + msg);
    }

    public static void messagePublished(final NodeMain node, final String topic, final String message) {
        if (ENABLE_LOG) {
            android.util.Log.d(LOG_TAG, node.getDefaultNodeName() + " => published to " + topic + ": " + message);
        }
    }

    public static void messageReceived(final NodeMain node, final String message) {
        if (ENABLE_LOG) {
            android.util.Log.d(LOG_TAG, node.getDefaultNodeName() + " => received: " + message);
        }
    }

    public static void messageReceived(final NodeMain node, final String from, final String message) {
        if (ENABLE_LOG) {
            android.util.Log.d(LOG_TAG, node.getDefaultNodeName() + " => received from " + from + ": " + message);
        }
    }
}