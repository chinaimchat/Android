package rtc.utils;

import android.os.Build;
import android.view.Window;

import androidx.annotation.ColorInt;

import java.util.List;


/**
 * 5/6/21 4:39 PM
 */
public class WKRTCCommonUtils {
    //设置状态栏颜色和透明度
    public static void setStatusBarColor(Window window, @ColorInt int color, int alpha) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(calculateStatusColor(color, alpha));
        }
    }

    /**
     * 计算状态栏颜色
     *
     * @param color color值
     * @param alpha alpha值
     * @return 最终的状态栏颜色
     */
    private static int calculateStatusColor(@ColorInt int color, int alpha) {
        if (alpha == 0) {
            return color;
        }
        float a = 1 - alpha / 255f;
        int red = color >> 16 & 0xff;
        int green = color >> 8 & 0xff;
        int blue = color & 0xff;
        red = (int) (red * a + 0.5);
        green = (int) (green * a + 0.5);
        blue = (int) (blue * a + 0.5);
        return 0xff << 24 | red << 16 | green << 8 | blue;
    }

    public static <T> boolean isNotEmpty(List<T> list) {
        return list != null && !list.isEmpty();
    }

    public static <T> boolean isEmpty(List<T> list) {
        return list == null || list.isEmpty();
    }


}
