package ru.robotmitya.robohead;

import junit.framework.TestCase;

import org.json.JSONException;

/**
 * Created by dmitrydzz on 5/18/14.
 *
 */
public class CameraSizesSetTest extends TestCase {
    private final static String CAMERAS_SIZES =
        "[{\"Sizes\":[{\"Height\":10,\"Width\":100},{\"Height\":11,\"Width\":101}],\"CameraIndex\":0}," +
         "{\"Sizes\":[{\"Height\":10,\"Width\":100},{\"Height\":11,\"Width\":101}],\"CameraIndex\":1}," +
         "{\"Sizes\":[{\"Height\":10,\"Width\":100},{\"Height\":11,\"Width\":101}],\"CameraIndex\":2}]";

    private void load(CameraSizesSet mCameraSizesSet) {
        mCameraSizesSet.clear();
        for (int i = 0; i < 3; i++) {
            CameraSizesSet.CameraSizes cameraSizes = new CameraSizesSet.CameraSizes();
            cameraSizes.setCameraIndex(i);
            cameraSizes.clear();
            for (int j = 0; j < 2; j++) {
                CameraSizesSet.Size size = new CameraSizesSet.Size();
                size.width = 100 + j;
                size.height = 10 + j;
                cameraSizes.add(size);
            }
            mCameraSizesSet.add(cameraSizes);
        }
    }

    public void testToJson() throws JSONException {
        CameraSizesSet cameraSizesSet = new CameraSizesSet();
        assertEquals("[]", cameraSizesSet.toJson());
        load(cameraSizesSet);
        assertEquals(CAMERAS_SIZES, cameraSizesSet.toJson());
    }

    public void testFromJson() throws JSONException {
        CameraSizesSet cameraSizesSet = new CameraSizesSet();
        cameraSizesSet.fromJson(CAMERAS_SIZES);
        assertEquals(3, cameraSizesSet.length());


        CameraSizesSet.CameraSizes cameraSizes = cameraSizesSet.get(0);
        assertEquals(0, cameraSizes.getCameraIndex());
        assertEquals(2, cameraSizes.getSizesLength());

        CameraSizesSet.Size size = cameraSizes.getSize(0);
        assertEquals(100, size.width);
        assertEquals(10, size.height);

        size = cameraSizes.getSize(1);
        assertEquals(101, size.width);
        assertEquals(11, size.height);


        cameraSizes = cameraSizesSet.get(1);
        assertEquals(1, cameraSizes.getCameraIndex());
        assertEquals(2, cameraSizes.getSizesLength());

        size = cameraSizes.getSize(0);
        assertEquals(100, size.width);
        assertEquals(10, size.height);

        size = cameraSizes.getSize(1);
        assertEquals(101, size.width);
        assertEquals(11, size.height);


        cameraSizes = cameraSizesSet.get(2);
        assertEquals(2, cameraSizes.getCameraIndex());
        assertEquals(2, cameraSizes.getSizesLength());

        size = cameraSizes.getSize(0);
        assertEquals(100, size.width);
        assertEquals(10, size.height);

        size = cameraSizes.getSize(1);
        assertEquals(101, size.width);
        assertEquals(11, size.height);
    }
}
