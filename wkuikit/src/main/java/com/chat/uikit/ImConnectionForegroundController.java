package com.chat.uikit;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.chat.base.config.WKConfig;
import com.chat.base.config.WKSharedPreferencesUtil;

/**
 * 登录且用户开启「后台保持消息连接」时，以前台服务提高 IM 进程优先级（非 100% 可靠）。
 */
public final class ImConnectionForegroundController {

    public static final String PREF_IM_FGS_KEEP_ALIVE = "wk_im_fgs_keep_alive";

    private ImConnectionForegroundController() {
    }

    public static void startIfEnabled(@NonNull Context context) {
        if (TextUtils.isEmpty(WKConfig.getInstance().getToken())) {
            return;
        }
        if (!WKSharedPreferencesUtil.getInstance().getBoolean(PREF_IM_FGS_KEEP_ALIVE, true)) {
            return;
        }
        Intent i = new Intent(context.getApplicationContext(), ImConnectionForegroundService.class);
        ContextCompat.startForegroundService(context.getApplicationContext(), i);
    }

    public static void stop(@NonNull Context context) {
        context.getApplicationContext().stopService(new Intent(context.getApplicationContext(), ImConnectionForegroundService.class));
    }
}
