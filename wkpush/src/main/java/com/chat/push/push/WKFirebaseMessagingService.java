package com.chat.push.push;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.chat.base.config.WKConfig;
import com.chat.base.endpoint.EndpointManager;
import com.chat.push.WKPushApplication;
import com.chat.push.service.PushModel;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class WKFirebaseMessagingService extends FirebaseMessagingService {

    //监控令牌的生成
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.e("获取到FCM令牌111", token);
        if (!TextUtils.isEmpty(token)) {
            PushModel.getInstance().registerDeviceToken(token, WKPushApplication.getInstance().pushBundleID, "FIREBASE");
        }
    }

    //监控推送的消息
    @Override
    public void onMessageReceived(@NonNull RemoteMessage msg) {
        super.onMessageReceived(msg);
        Log.e("收到Firebase推送消息", msg.getFrom());
        // 服务端宜发 data payload / 高优先级，以便在后台触发重连；仅 notification 时系统可能直接展示而不回调此处。
        if (!TextUtils.isEmpty(WKConfig.getInstance().getToken())) {
            EndpointManager.getInstance().invoke("wk_fcm_wake_im", null);
        }
    }

}
