package com.chat.push.push;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.chat.base.config.WKConfig;
import com.chat.base.endpoint.EndpointManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 离线推送统一业务字段（data / 透传 JSON）：唤醒 IM + 交由 UI 层本地 notify，不依赖各厂商系统栏展示。
 */
public final class OfflinePushPayloadHelper {
    private static final String TAG = "OfflinePushPayload";

    private OfflinePushPayloadHelper() {
    }

    public static Map<String, String> normalizeFlatMap(Map<String, String> raw) {
        HashMap<String, String> out = new HashMap<>();
        if (raw == null) {
            return out;
        }
        for (Map.Entry<String, String> e : raw.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            if (TextUtils.isEmpty(v)) {
                continue;
            }
            String t = v.trim();
            if (t.startsWith("{") && t.endsWith("}")) {
                try {
                    JSONObject jo = new JSONObject(t);
                    for (Iterator<String> it = jo.keys(); it.hasNext(); ) {
                        String ik = it.next();
                        Object iv = jo.opt(ik);
                        out.put(ik, iv != null ? String.valueOf(iv) : "");
                    }
                } catch (JSONException ex) {
                    out.put(k, v);
                }
            } else {
                out.put(k, v);
            }
        }
        return out;
    }

    public static Map<String, String> fromJsonString(String json) {
        HashMap<String, String> out = new HashMap<>();
        if (TextUtils.isEmpty(json)) {
            return out;
        }
        try {
            JSONObject jo = new JSONObject(json.trim());
            for (Iterator<String> it = jo.keys(); it.hasNext(); ) {
                String k = it.next();
                Object v = jo.opt(k);
                out.put(k, v != null ? String.valueOf(v) : "");
            }
        } catch (JSONException e) {
            Log.w(TAG, "fromJsonString parse fail", e);
        }
        return out;
    }

    public static void dispatch(Context context, Map<String, String> payload) {
        if (context == null || payload == null || payload.isEmpty()) {
            return;
        }
        String channelId = payload.get("channel_id");
        if (TextUtils.isEmpty(channelId)) {
            return;
        }
        if (!TextUtils.isEmpty(WKConfig.getInstance().getToken())
                && "1".equals(payload.get("wk_fcm_wake_im"))) {
            EndpointManager.getInstance().invoke("wk_fcm_wake_im", null);
        }
        EndpointManager.getInstance().invoke("wk_offline_push_notify", payload);
    }
}
