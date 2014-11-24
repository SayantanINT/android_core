package ru.robotmitya.robocommonlib;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

/**
 * Output to feedgnuplot.
 * Created by dzakhovds on 14.11.14.
 */
public class Plot {
    private static boolean ENABLE_PLOT;

    static {
        ENABLE_PLOT = true;
    }

    private static final String PLOT_SEPARATOR = " ";

    @SuppressWarnings("UnusedDeclaration")
    public static void send(final String tag, final float x, float... yArgs) {
        if (!ENABLE_PLOT) {
            return;
        }

        if (yArgs.length == 0) {
            return;
        }

        String line = "" + x;
        for (float yArg : yArgs) {
            line += PLOT_SEPARATOR + yArg;
        }

        android.util.Log.d(tag, line);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void send(final String tag, final long x, float... yArgs) {
        if (!ENABLE_PLOT) {
            return;
        }

        if (yArgs.length == 0) {
            return;
        }

        String line = "" + x;
        for (float yArg : yArgs) {
            line += PLOT_SEPARATOR + yArg;
        }

        android.util.Log.d(tag, line);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void send(final String tag, final float x, final Vector2 vector) {
        if (vector == null) {
            return;
        }
        //noinspection SuspiciousNameCombination
        send(tag, x, vector.x, vector.y);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void send(final String tag, final long x, final Vector2 vector) {
        if (vector == null) {
            return;
        }
        //noinspection SuspiciousNameCombination
        send(tag, x, vector.x, vector.y);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void send(final String tag, final float x, final Vector3 vector) {
        if (vector == null) {
            return;
        }
        //noinspection SuspiciousNameCombination
        send(tag, x, vector.x, vector.y, vector.z);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void send(final String tag, final long x, final Vector3 vector) {
        if (vector == null) {
            return;
        }
        //noinspection SuspiciousNameCombination
        send(tag, x, vector.x, vector.y, vector.z);
    }
}
