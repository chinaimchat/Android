package com.chat.wallet.util;

import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.EndpointSID;
import com.chat.base.endpoint.entity.ChatViewMenu;
import com.xinbida.wukongim.entity.WKChannelType;

/**
 * 钱包模块内打开单聊（通过 Endpoint，避免直接依赖 wkuikit）。
 */
public final class WalletChatRouter {

    private WalletChatRouter() {
    }

    /**
     * 与通讯录「客服」一致：由 {@code wkcustomerservice} 注册 {@code show_customer_service}，
     * 内部请求 {@code POST hotline/visitor/topic/channel}（{@code topic_id=0}、{@code appid}），
     * 用返回的 {@code channel_id}/{@code channel_type} 打开 {@link EndpointSID#chatView}。
     */
    public static void openOfficialCustomerService(AppCompatActivity activity) {
        if (activity == null) {
            return;
        }
        EndpointManager.getInstance().invoke("show_customer_service", activity);
    }

    public static void openP2PChat(AppCompatActivity activity, String uid) {
        if (activity == null || TextUtils.isEmpty(uid)) {
            return;
        }
        String channelId = uid.trim();
        byte channelType = (byte) WKChannelType.PERSONAL;
        EndpointManager.getInstance().invoke(EndpointSID.chatView, new ChatViewMenu(activity, channelId, channelType, 0, true));
    }
}
