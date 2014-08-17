package ru.robotmitya.robocommonlib;

import junit.framework.Assert;
import junit.framework.TestCase;

//import org.junit.Test;

/**
 * Created by dmitrydzz on 4/17/14.
 *
 */
public class SensorValueAdapterTest extends TestCase {

    public void testNoPut() {
        SensorValueAdapter sensorValueAdapter = new SensorValueAdapter(5, 0);
        Assert.assertNull(sensorValueAdapter.get());
        Assert.assertEquals(-1000, sensorValueAdapter.get(-1000), 0.00001);
    }

    public void testOnePut() {
        SensorValueAdapter sensorValueAdapter = new SensorValueAdapter(5, 0);
        sensorValueAdapter.put(0.1974);
        Assert.assertEquals(0.1974, sensorValueAdapter.get(0), 0.00001);
    }

    public void testManySamePut() {
        SensorValueAdapter sensorValueAdapter = new SensorValueAdapter(5, 0);
        for (int i = 0; i < 100; i++) {
            sensorValueAdapter.put(0.1974);
            Assert.assertEquals(0.1974, sensorValueAdapter.get(0), 0.00001);
        }
    }

    public void testManyPut() {
        SensorValueAdapter sensorValueAdapter = new SensorValueAdapter(2, 0);

        sensorValueAdapter.put(1974);
        Assert.assertEquals(1974, sensorValueAdapter.get(0), 0.00001);

        sensorValueAdapter.put(2014);
        Assert.assertEquals(1974, sensorValueAdapter.get(0), 0.00001);

        sensorValueAdapter.put(1972);
        Assert.assertEquals(1974, sensorValueAdapter.get(0), 0.00001);

        sensorValueAdapter.put(1973);
        Assert.assertEquals(1973.66667, sensorValueAdapter.get(0), 0.00001);
    }
}
