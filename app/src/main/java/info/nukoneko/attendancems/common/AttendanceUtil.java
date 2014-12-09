package info.nukoneko.attendancems.common;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;

import info.nukoneko.attendancems.R;

/**
 * Created by TEJNEK on 2014/11/05.
 */
public class AttendanceUtil {
    public static String unixTime2DateString(Context context, String unixTime){
        SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.entry_date_format));
        Date date = new Date(Long.parseLong(unixTime) * 1000);
        return sdf.format(date);
    }
    public static enum CommandKind{
        onStartUp,
        onReaderError,
        onResume,
        onRead,
        onAdminCardReading,
        onIdle,
        onHeartBeat
    }
    public static CommandKind getCommand(String command){
        for(CommandKind commandKind : CommandKind.values()){
            if(command.equals(commandKind.toString())) return commandKind;
        }
        System.out.println("Unknown Command : " + command);
        return null;
    }
}
