package ru.robotmitya.robocommonlib;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * Created by dmitrydzz on 3/14/14.
 *
 */
public class MessageHelperTest extends TestCase {
    @Test
    public void testCorrectLength() {
        String valueToTest = MessageHelper.correctLength("DIMA", 6, '-');
        assertEquals("--DIMA", valueToTest);

        valueToTest = MessageHelper.correctLength("DIMA", 2, '-');
        assertEquals("MA", valueToTest);

        valueToTest = MessageHelper.correctLength("DIMA", 0, '-');
        assertEquals("", valueToTest);

        valueToTest = MessageHelper.correctLength("DIMA", 4, '-');
        assertEquals("DIMA", valueToTest);
    }

    @Test
    public void testMakeMessage() {
        String command = MessageHelper.makeMessage("G", (short)0);
        assertEquals("G0000", command);

        command = MessageHelper.makeMessage("G", (short)192);
        assertEquals("G00C0", command);

        command = MessageHelper.makeMessage("G", (short)-192);
        assertEquals("GFF40", command);

        command = MessageHelper.makeMessage("G", (short)-1);
        assertEquals("GFFFF", command);

        command = MessageHelper.makeMessage("G", (short)65535);
        assertEquals("GFFFF", command);
    }

    @Test
    public void testMakeStringMessage() {
        String message = MessageHelper.makeMessage("L", "74");
        assertEquals("L0074", message);

        message = MessageHelper.makeMessage("MOVE", "01101974");
        assertEquals("E1974", message);
    }

    @Test
    public void testGetMessageIdentifier() {
        String identifier = MessageHelper.getMessageIdentifier("DI123");
        assertEquals("D", identifier);

        identifier = MessageHelper.getMessageIdentifier("D");
        assertEquals("D", identifier);

        identifier = MessageHelper.getMessageIdentifier("");
        assertEquals(" ", identifier);
    }

    @Test
    public void testGetMessageStringValue() {
        String value = MessageHelper.getMessageStringValue("G1234");
        assertEquals("1234", value);

        value = MessageHelper.getMessageStringValue("G123");
        assertEquals("0123", value);

        value = MessageHelper.getMessageStringValue("G");
        assertEquals("0000", value);

        value = MessageHelper.getMessageStringValue("");
        assertEquals("0000", value);

        value = MessageHelper.getMessageStringValue("G12345");
        assertEquals("2345", value);
    }

    @Test
    public void testSkipFirstBrokenMessage() {
        String value = MessageHelper.skipFirstBrokenMessage("G1234I00");
        assertEquals("G1234I00", value);

        value = MessageHelper.skipFirstBrokenMessage("1234I00");
        assertEquals("I00", value);

        value = MessageHelper.skipFirstBrokenMessage("1234I");
        assertEquals("I", value);

        value = MessageHelper.skipFirstBrokenMessage("1234");
        assertEquals("", value);

        value = MessageHelper.skipFirstBrokenMessage("");
        assertEquals("", value);

        value = MessageHelper.skipFirstBrokenMessage("I");
        assertEquals("I", value);

        value = MessageHelper.skipFirstBrokenMessage("A");
        assertEquals("", value);
    }

    @Test
    public void testGetFirstMessagePosition() {
        int value = MessageHelper.getFirstMessagePosition("G1234I00");
        assertEquals(0, value);

        value = MessageHelper.getFirstMessagePosition("1234I00");
        assertEquals(4, value);

        value = MessageHelper.getFirstMessagePosition("1234I");
        assertEquals(4, value);

        value = MessageHelper.getFirstMessagePosition("1234");
        assertEquals(-1, value);

        value = MessageHelper.getFirstMessagePosition("");
        assertEquals(-1, value);

        value = MessageHelper.getFirstMessagePosition("I");
        assertEquals(0, value);

        value = MessageHelper.getFirstMessagePosition("A");
        assertEquals(-1, value);
    }
}
