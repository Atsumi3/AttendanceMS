package info.nukoneko.attendancems.common;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by TEJNEK on 2014/11/05.
 */
public class AttendanceUtil {
    public static String unixTime2DateString(String unixTime){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年 MM月 dd日 EEE曜日 HH:mm:ss");
        Date date = new Date(Long.parseLong(unixTime) * 1000);
        return sdf.format(date);
    }
}
