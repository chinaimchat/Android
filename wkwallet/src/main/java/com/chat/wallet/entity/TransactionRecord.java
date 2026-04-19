package com.chat.wallet.entity;

import android.text.TextUtils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;

/**
 * GET /v1/wallet/transactions 列表单项；字段名兼容 snake_case、camelCase 及常见嵌套 {@code context/extra}。
 */
public class TransactionRecord {
    public long id;
    public String type;
    public double amount;
    @JSONField(name = "related_id", alternateNames = {"relatedId"})
    public String related_id;
    public String remark;
    @JSONField(name = "created_at", alternateNames = {"createdAt"})
    public String created_at;

    /** 群名称 / 会话名称 */
    @JSONField(name = "group_name", alternateNames = {
            "groupName", "channel_name", "channelName", "room_name", "roomName", "group_title", "groupTitle"
    })
    public String group_name;

    /** 付款方 / 发红包方 / 转账转出方（对「收入」类记录） */
    @JSONField(name = "from_user_name", alternateNames = {
            "fromName", "from_name", "sender_name", "senderName", "from_nickname", "fromNickname",
            "from_user", "fromUser", "payer_name", "payerName"
    })
    public String from_user_name;

    /** 收款方 / 收红包方 / 转账接收方（对「支出」类记录） */
    @JSONField(name = "to_user_name", alternateNames = {
            "toName", "to_name", "receiver_name", "receiverName", "to_nickname", "toNickname",
            "to_user", "toUser", "payee_name", "payeeName"
    })
    public String to_user_name;

    /** 对端用户昵称（部分后台统一字段） */
    @JSONField(name = "peer_name", alternateNames = {
            "peerName", "counterparty_name", "counterpartyName", "opposite_name", "oppositeName", "target_name"
    })
    public String peer_name;

    /**
     * 扩展信息（群、对方昵称等也可能只出现在此对象内）。
     */
    @JSONField(name = "context", alternateNames = {"extra", "meta", "detail_info", "extend", "payload"})
    public JSONObject context;

    private static String strFromContext(JSONObject ctx, String... keys) {
        if (ctx == null || keys == null) {
            return null;
        }
        for (String k : keys) {
            if (!ctx.containsKey(k)) {
                continue;
            }
            Object o = ctx.get(k);
            if (o == null) {
                continue;
            }
            String s = String.valueOf(o).trim();
            if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) {
                return s;
            }
        }
        return null;
    }

    public String resolveGroupName() {
        if (!TextUtils.isEmpty(group_name)) {
            return group_name;
        }
        return strFromContext(context,
                "group_name", "groupName", "channel_name", "channelName", "room_name", "room_title", "channel_title");
    }

    public String resolveFromName() {
        if (!TextUtils.isEmpty(from_user_name)) {
            return from_user_name;
        }
        return strFromContext(context,
                "from_user_name", "fromName", "from_name", "sender_name", "senderName",
                "from_nickname", "from_user", "payer_name");
    }

    public String resolveToName() {
        if (!TextUtils.isEmpty(to_user_name)) {
            return to_user_name;
        }
        return strFromContext(context,
                "to_user_name", "toName", "to_name", "receiver_name", "receiverName",
                "to_nickname", "to_user", "payee_name");
    }

    public String resolvePeerName() {
        if (!TextUtils.isEmpty(peer_name)) {
            return peer_name;
        }
        return strFromContext(context, "peer_name", "peerName", "counterparty_name", "target_name");
    }

    /** 红包/转账所在会话 id（群号等），用于打开红包详情时带上上下文以遵守群内禁止互加等规则。 */
    public String resolveChannelId() {
        return strFromContext(context,
                "channel_id", "channelId", "group_no", "groupNo");
    }

    public Integer resolveChannelType() {
        if (context == null) {
            return null;
        }
        String[] keys = {"channel_type", "channelType"};
        for (String k : keys) {
            if (!context.containsKey(k)) {
                continue;
            }
            Object o = context.get(k);
            if (o instanceof Number) {
                return ((Number) o).intValue();
            }
            if (o != null) {
                String s = String.valueOf(o).trim();
                if (!s.isEmpty()) {
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return null;
    }

    /**
     * 红包详情补全后写入 context 的 {@code packet_type}；与 {@link com.chat.wallet.msg.WKRedPacketContent} 取值一致。
     */
    public int resolveRedPacketPacketType() {
        if (context == null) {
            return 0;
        }
        Object o = context.get("packet_type");
        if (o == null) {
            o = context.get("packetType");
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        if (o != null) {
            String s = String.valueOf(o).trim();
            if (!s.isEmpty()) {
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0;
    }

    private static int intFromContext(JSONObject ctx, String key, int missing) {
        if (ctx == null || !ctx.containsKey(key)) {
            return missing;
        }
        Object o = ctx.get(key);
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        if (o != null) {
            try {
                return Integer.parseInt(String.valueOf(o).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return missing;
    }

    /**
     * 红包详情补全后写入的领取进度；未补全时为 -1。
     */
    public int resolveRedPacketTotalCount() {
        return intFromContext(context, "redpacket_total_count", -1);
    }

    public int resolveRedPacketRemainingCount() {
        return intFromContext(context, "redpacket_remaining_count", -1);
    }

    /** 与 {@link com.chat.wallet.entity.RedPacketDetailResp#redpacket_status} 一致，如 2 表示过期。 */
    public int resolveRedPacketStatus() {
        return intFromContext(context, "redpacket_status", 0);
    }

    /** 是否已写入领取进度字段（用于「我发出的」列表展示）。 */
    public boolean hasRedPacketClaimProgress() {
        int t = resolveRedPacketTotalCount();
        int r = resolveRedPacketRemainingCount();
        return t > 0 && r >= 0;
    }
}
