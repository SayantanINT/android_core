package ru.robotmitya.robocommonlib;

import org.ros.address.InetAddressFactory;

import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * Created by dmitrydzz on 30.10.14.
 */
public class SettingsHelper {
    private static final int PORT = 11311;

    public static String getNewPublicMasterUri() throws MalformedURLException {
        final String host = InetAddressFactory.newNonLoopback().getHostAddress();
        final URL url = new URL("http", host, PORT, "");
        return url.toString();
    }

    public static String getMasterUri(String masterUriIp) throws MalformedURLException {
        masterUriIp = fixUrl(masterUriIp);
        return masterUriIp + ":" + PORT;
    }

    public static String fixUrl(String url) throws MalformedURLException {
        if (url == null) {
            return "";
        }

        url = url.trim();

        if (url.equals("")) {
            return "";
        }

        if (!url.startsWith("http://")) {
            url = "http://" + url;
        }

        final URL u = new URL(url);
        return u.getProtocol() + "://" + u.getHost();
    }
}
