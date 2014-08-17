package ru.robotmitya.robohead;

import junit.framework.TestCase;

import org.json.JSONException;

import java.util.ArrayList;

/**
 * Created by dmitrydzz on 5/19/14.
 *
 */
public class SettingsFragmentTest extends TestCase {
    private final static String CAMERAS_SIZES =
        "[{\"Sizes\":[{\"Height\":10,\"Width\":20},{\"Height\":11,\"Width\":21}],\"CameraIndex\":0}," +
         "{\"Sizes\":[{\"Height\":100,\"Width\":200},{\"Height\":101,\"Width\":201}],\"CameraIndex\":1}," +
         "{\"Sizes\":[{\"Height\":1000,\"Width\":2000},{\"Height\":1001,\"Width\":2001}],\"CameraIndex\":2}]";

    public void testGetCameraEntries() throws JSONException {
        CameraSizesSet cameraSizesSet = new CameraSizesSet();
        cameraSizesSet.fromJson(CAMERAS_SIZES);

        ArrayList<CharSequence> entries = SettingsFragment.getCameraModeEntries(cameraSizesSet);

        assertEquals(10, entries.size());
        assertEquals("Disabled", entries.get(0));
        assertEquals("Camera 1 [default]", entries.get(1));
        assertEquals("Camera 1 [20x10]", entries.get(2));
        assertEquals("Camera 1 [21x11]", entries.get(3));
        assertEquals("Camera 2 [default]", entries.get(4));
        assertEquals("Camera 2 [200x100]", entries.get(5));
        assertEquals("Camera 2 [201x101]", entries.get(6));
        assertEquals("Camera 3 [default]", entries.get(7));
        assertEquals("Camera 3 [2000x1000]", entries.get(8));
        assertEquals("Camera 3 [2001x1001]", entries.get(9));
    }

    public void testGetCameraValues() throws JSONException {
        CameraSizesSet cameraSizesSet = new CameraSizesSet();
        cameraSizesSet.fromJson(CAMERAS_SIZES);

        ArrayList<CharSequence> values = SettingsFragment.getCameraModeValues(cameraSizesSet);

        assertEquals(10, values.size());
        assertEquals("FFFF", values.get(0));
        assertEquals("00FF", values.get(1));
        assertEquals("0000", values.get(2));
        assertEquals("0001", values.get(3));
        assertEquals("01FF", values.get(4));
        assertEquals("0100", values.get(5));
        assertEquals("0101", values.get(6));
        assertEquals("02FF", values.get(7));
        assertEquals("0200", values.get(8));
        assertEquals("0201", values.get(9));
    }
}
