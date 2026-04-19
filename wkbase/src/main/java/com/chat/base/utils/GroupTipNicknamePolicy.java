package com.chat.base.utils;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.chat.base.config.WKConfig;
import com.chat.base.msgitem.WKChannelMemberRole;

import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.entity.WKChannel;
import com.xinbida.wukongim.entity.WKChannelExtras;
import com.xinbida.wukongim.entity.WKChannelMember;
import com.xinbida.wukongim.entity.WKChannelType;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 群「禁止互加好友」与群管理、IM 消息 payload（如 1011/1012）及 {@link WKChannelExtras#forbiddenAddFriend} 一致。
 * 普通成员不可经系统提示昵称、红包详情领取列表等入口跳转群成员资料卡（避免绕开 vercode）；群主/管理员不限。
 */
public final class GroupTipNicknamePolicy {

    private GroupTipNicknamePolicy() {
    }

    public static int parseForbiddenAddFriendFlag(@Nullable String contentJson) {
        if (TextUtils.isEmpty(contentJson)) {
            return 0;
        }
        try {
            JSONObject jo = new JSONObject(contentJson);
            return jo.optInt("forbidden_add_friend", 0);
        } catch (JSONException e) {
            return 0;
        }
    }

    /**
     * 是否应禁止「点昵称进资料卡」：群 + 禁止互开 + 当前用户为普通成员（非群主/管理员）。
     * 单聊或未带标记视为不限制。
     */
    public static boolean shouldBlockNicknameProfileJump(
            @Nullable String channelId,
            byte channelType,
            int forbiddenAddFriendFlag
    ) {
        if (forbiddenAddFriendFlag != 1) {
            return false;
        }
        if (channelType != WKChannelType.GROUP || TextUtils.isEmpty(channelId)) {
            return false;
        }
        String me = WKConfig.getInstance().getUid();
        if (TextUtils.isEmpty(me)) {
            return true;
        }
        WKChannelMember self = WKIM.getInstance().getChannelMembersManager()
                .getMember(channelId, WKChannelType.GROUP, me);
        if (self == null) {
            return true;
        }
        return self.role == WKChannelMemberRole.normal;
    }

    /**
     * 从本地已缓存的群频道信息读取「群内禁止互加」开关（与 {@link WKChannelExtras#forbiddenAddFriend} 同步）。
     */
    public static int forbiddenAddFriendFlagFromLocalGroupChannel(
            @Nullable String channelId,
            byte channelType
    ) {
        if (channelType != WKChannelType.GROUP || TextUtils.isEmpty(channelId)) {
            return 0;
        }
        WKChannel ch = WKIM.getInstance().getChannelManager().getChannel(channelId, WKChannelType.GROUP);
        if (ch == null || ch.remoteExtraMap == null) {
            return 0;
        }
        Object o = ch.remoteExtraMap.get(WKChannelExtras.forbiddenAddFriend);
        if (!(o instanceof Number)) {
            return 0;
        }
        return ((Number) o).intValue() == 1 ? 1 : 0;
    }
}
