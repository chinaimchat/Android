package com.chat.uikit.utils;

import android.text.TextUtils;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 前台 IM 通知与离线推送通知共用去重：同一条消息短时间仅弹一次。
 */
public final class PushNotifyDedupHelper {
    private static final int MAX_CACHE = 256;
    private static final long EXPIRE_MS = 2 * 60 * 1000L;

    private static final LinkedHashMap<String, Long> RECENT = new LinkedHashMap<>(MAX_CACHE + 1, 0.75f, true);

    private PushNotifyDedupHelper() {
    }

    public static synchronized boolean shouldNotify(String channelId, byte channelType, long messageSeq, String fallbackId) {
        String key = buildKey(channelId, channelType, messageSeq, fallbackId);
        if (TextUtils.isEmpty(key)) {
            return true;
        }
        long now = System.currentTimeMillis();
        prune(now);
        if (RECENT.containsKey(key)) {
            return false;
        }
        RECENT.put(key, now);
        if (RECENT.size() > MAX_CACHE) {
            Iterator<String> it = RECENT.keySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
        return true;
    }

    private static String buildKey(String channelId, byte channelType, long messageSeq, String fallbackId) {
        if (!TextUtils.isEmpty(channelId) && messageSeq > 0) {
            return channelId + "|" + channelType + "|" + messageSeq;
        }
        if (!TextUtils.isEmpty(channelId) && !TextUtils.isEmpty(fallbackId)) {
            return channelId + "|" + channelType + "|" + fallbackId;
        }
        if (!TextUtils.isEmpty(fallbackId)) {
            return fallbackId;
        }
        return null;
    }

    private static void prune(long now) {
        Iterator<Map.Entry<String, Long>> it = RECENT.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            if (now - e.getValue() > EXPIRE_MS) {
                it.remove();
            }
        }
    }
}
