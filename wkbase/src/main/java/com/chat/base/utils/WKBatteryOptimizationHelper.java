package com.chat.base.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.annotation.NonNull;

/**
 * 打开应用详情/电池相关系统页，便于用户关闭省电限制（无法替代 FGS/推送架构）。
 */
public final class WKBatteryOptimizationHelper {

    private WKBatteryOptimizationHelper() {
    }

    public static boolean isIgnoringBatteryOptimizations(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    /**
     * 打开本应用「应用信息」页，用户可在其中找到电池/后台限制（各厂商入口不同）。
     */
    public static void openAppDetailSettings(@NonNull Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
