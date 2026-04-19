package rtc.utils;

import android.util.Log;

/**
 * 2020-09-09 12:09
 * view点击事件
 */
public class BtnClickUtils {

    private BtnClickUtils() {
    }

    private static class ViewClickUtilsBinder {
        final static BtnClickUtils viewClick = new BtnClickUtils();
    }

    public static BtnClickUtils getInstance() {
        return ViewClickUtilsBinder.viewClick;
    }

    private static long lastClickTime = 0;
    private static int lastButtonId = -1;

    /**
     * 判断两次点击的间隔，如果小于1000，则认为是多次无效点击
     */
    public boolean isCanClick(int buttonId) {
        return isFastDoubleClick(buttonId, 500);
    }

    /**
     * 判断两次点击的间隔，如果小于diff，则认为是多次无效点击
     */
    public boolean isFastDoubleClick(int buttonId, long diff) {
        long time = System.currentTimeMillis();
        long timeD = time - lastClickTime;
        if (lastButtonId == buttonId && lastClickTime > 0 && timeD < diff) {
            Log.v("isFastDoubleClick", "短时间内按钮多次触发");
            return false;
        }
        lastClickTime = time;
        lastButtonId = buttonId;
        return true;
    }
}
