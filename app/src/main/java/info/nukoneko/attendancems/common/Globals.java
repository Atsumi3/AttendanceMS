package info.nukoneko.attendancems.common;

import android.app.Application;

/**
 * Created by Telneko on 2014/12/04.
 */
public class Globals extends Application {
    public static String firstHash = "";
    public static Integer nonce = 0;

    public static String hash = "";
    public static Long lectureID = 0L;
    public static boolean isAuthEnable = false;
    public static boolean readingMode = false;

    public static String targetIP = "";
    public static Long sessionKey = 0L;
    public static String targetPort = "8888";
    public static String targetWSPort = "8889";

}
