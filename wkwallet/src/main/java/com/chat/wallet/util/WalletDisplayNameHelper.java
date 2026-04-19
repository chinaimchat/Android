package com.chat.wallet.util;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.entity.WKChannel;
import com.xinbida.wukongim.entity.WKChannelMember;
import com.xinbida.wukongim.entity.WKChannelType;

import java.util.List;

/**
 * 将钱包接口返回的 uid 解析为客户端可读的昵称：群聊优先成员备注/群名片，否则回退单聊频道名。
 */
public final class WalletDisplayNameHelper {

    private WalletDisplayNameHelper() {
    }

    /**
     * @param chatChannelId 当前会话 channel_id（单聊为对方 uid，群聊为群 id），可为 null
     * @param chatChannelType {@link WKChannelType}
     */
    public static String displayNameForUid(@Nullable String uid,
                                           @Nullable String chatChannelId,
                                           int chatChannelType) {
        if (uid == null || uid.isEmpty()) {
            return "";
        }
        if (chatChannelType == WKChannelType.GROUP && !TextUtils.isEmpty(chatChannelId)) {
            List<WKChannelMember> members = WKIM.getInstance().getChannelMembersManager()
                    .getMembers(chatChannelId, WKChannelType.GROUP);
            if (members != null) {
                for (WKChannelMember m : members) {
                    if (uid.equals(m.memberUID)) {
                        if (!TextUtils.isEmpty(m.memberRemark)) {
                            return m.memberRemark;
                        }
                        if (!TextUtils.isEmpty(m.memberName)) {
                            return m.memberName;
                        }
                        break;
                    }
                }
            }
        }
        WKChannel ch = WKIM.getInstance().getChannelManager().getChannel(uid, WKChannelType.PERSONAL);
        if (ch != null && !TextUtils.isEmpty(ch.channelName)) {
            return ch.channelName;
        }
        return uid;
    }
}
