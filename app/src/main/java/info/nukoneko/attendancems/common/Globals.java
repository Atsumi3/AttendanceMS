package info.nukoneko.attendancems.common;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiManager;

import info.nukoneko.attendancems.R;

/**
 * Created by Telneko on 2014/12/04.
 */
public class Globals extends Application {

    public static boolean isDebug = true;

    public static Uri serverURI = null;

    public static String targetWSPort = "8889";

    public static String sessionToken = "";
    public static Long lectureID = 0L;
    public static boolean isAuthEnable = false;
    public static boolean readingMode = false;

    public static String macAddress = "";

    public static boolean isIgnoreDuplicatedEntry = false;

    /**
     * CommonSetting Load, Save, Delete **
     */
    public boolean loadSettingPreference() {
        if(Globals.isDebug) return true;
        SharedPreferences preferences = this.getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
        String sessionURL = preferences.getString(getString(R.string.PREF_SESSION_URL), null);
        if(sessionURL == null)return false;
        Globals.serverURI = Uri.parse(sessionURL);
        String sessionTOKEN = preferences.getString(getString(R.string.PREF_SESSION_TOKEN), null);
        if(sessionTOKEN != null){
            Globals.sessionToken = sessionTOKEN;
        }
        return true;
    }

    public void saveSettingPreference() {
        SharedPreferences preferences = this.getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(getString(R.string.PREF_SESSION_URL), serverURI.toString());
        editor.putString(getString(R.string.PREF_SESSION_TOKEN), sessionToken);
        editor.apply();
    }
}
