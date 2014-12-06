package info.nukoneko.attendancems.common;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiManager;

/**
 * Created by Telneko on 2014/12/04.
 */
public class Globals extends Application {

    public static Uri serverURI = null;

    public static String targetWSPort = "8889";

    public static String hash = "";
    public static Long lectureID = 0L;
    public static boolean isAuthEnable = false;
    public static boolean readingMode = false;

    public static String macAddress = "";

    public static void setMacAddress(Application application){
        macAddress = ((WifiManager)application.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getMacAddress();
    }
}
