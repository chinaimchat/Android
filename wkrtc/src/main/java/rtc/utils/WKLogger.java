package rtc.utils;

import android.util.Log;

public class WKLogger {
    public static boolean isDebug = true;

    public static void e(String tag, String msg) {
        if (isDebug) {
            Log.e(tag, msg);
        }
    }
}
