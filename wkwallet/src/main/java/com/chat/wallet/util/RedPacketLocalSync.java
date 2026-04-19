package com.chat.wallet.util;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.chat.base.config.WKConfig;
import com.chat.base.msgitem.WKContentType;
import com.chat.wallet.entity.RedPacketDetailResp;
import com.chat.wallet.entity.RedPacketRecord;
import com.chat.wallet.msg.WKRedPacketContent;
import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.entity.WKMsg;
import com.xinbida.wukongim.manager.MsgManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * 开红包成功后：写回本地消息库并触发 UI 刷新（聊天列表 / 会话摘要）。
 * <p>
 * 优先用 {@code clientMsgNo}；缺失时在指定会话内按 {@code packet_no} 搜索红包消息再更新。
 * <p>
 * <b>全局 status 与气泡 status</b>：服务端 {@code redpacket_status==0} 且 {@code remaining_count>0} 只表示「红包仍在进行、他人还可领」；
 * 本地 {@link WKRedPacketContent#status}{@code ==0} 表示「当前用户仍可点开抢」。因此「我已领过但全局未领完」时不能单靠等业务变为 1 才变浅，
 * 须在 open 成功（及详情校准）时按「本人是否已领」映射为浅色气泡（如 {@code 3}），见 {@link com.chat.wallet.entity.RedPacketOpenResp#getLocalMessageStatusAfterOpenSuccess()}。
 */
public final class RedPacketLocalSync {

    private RedPacketLocalSync() {
    }

    /**
     * 将详情接口的红包状态映射为 {@link WKRedPacketContent#status}（与 {@link WKRedPacketContent#decodeMsg} 语义对齐）。
     * <p>
     * 与 open 响应一致：业务仍为 0 时若当前用户已领过，本地不能用 0（大红可点），应映射为 {@code 3}（浅色已领取）。
     */
    public static int statusFromDetail(@Nullable RedPacketDetailResp d) {
        if (d == null) {
            return 1;
        }
        int s = d.redpacket_status;
        boolean meClaimed = claimedByCurrentUserInDetail(d);
        if (s == 0 && meClaimed) {
            if (d.total_count > 0 && d.remaining_count <= 0) {
                return 1;
            }
            return 3;
        }
        if (s >= 0 && s <= 2) {
            return s;
        }
        if (d.total_count > 0 && d.remaining_count <= 0) {
            return 1;
        }
        return 1;
    }

    /** 详情里是否已体现「当前用户领过」（金额或领取记录）。 */
    private static boolean claimedByCurrentUserInDetail(RedPacketDetailResp d) {
        if (d.my_amount > 0) {
            return true;
        }
        String me = WKConfig.getInstance().getUid();
        if (TextUtils.isEmpty(me) || d.records == null) {
            return false;
        }
        for (RedPacketRecord r : d.records) {
            if (r != null && me.equals(r.uid)) {
                return true;
            }
        }
        return false;
    }

    public static void applyOpened(
            @Nullable String packetNo,
            @Nullable String channelId,
            byte channelType,
            @Nullable String clientMsgNo,
            @Nullable String senderName,
            @Nullable String remark,
            int packetType,
            int status
    ) {
        if (TextUtils.isEmpty(packetNo)) {
            return;
        }
        Runnable job = () -> applyOpenedOnMainThread(
                packetNo, channelId, channelType, clientMsgNo, senderName, remark, packetType, status);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            job.run();
        } else {
            new Handler(Looper.getMainLooper()).post(job);
        }
    }

    private static void applyOpenedOnMainThread(
            String packetNo,
            @Nullable String channelId,
            byte channelType,
            @Nullable String clientMsgNo,
            @Nullable String senderName,
            @Nullable String remark,
            int packetType,
            int status
    ) {
        WKRedPacketContent model = buildContent(packetNo, senderName, remark, packetType, status);
        MsgManager mm = WKIM.getInstance().getMsgManager();
        boolean updated = false;
        if (!TextUtils.isEmpty(clientMsgNo)) {
            updated = mm.updateContentAndRefresh(clientMsgNo, model, true);
        }
        if (!updated && !TextUtils.isEmpty(channelId)) {
            updated = patchByChannelSearch(mm, channelId, channelType, packetNo, model);
        }
        if (!updated && TextUtils.isEmpty(channelId)) {
            patchByGlobalRedPacketScan(mm, packetNo, model);
        }
    }

    private static WKRedPacketContent buildContent(
            String packetNo,
            @Nullable String senderName,
            @Nullable String remark,
            int packetType,
            int status
    ) {
        WKRedPacketContent c = new WKRedPacketContent();
        c.packetNo = packetNo;
        c.packetType = packetType;
        c.remark = remark != null ? remark : "";
        c.senderName = senderName != null ? senderName : "";
        c.status = status;
        return c;
    }

    private static boolean patchByChannelSearch(
            MsgManager mm,
            String channelId,
            byte channelType,
            String packetNo,
            WKRedPacketContent model
    ) {
        int[] types = new int[]{WKContentType.WK_REDPACKET};
        long cursor = 0;
        for (int round = 0; round < 50; round++) {
            @SuppressWarnings("unchecked")
            List<WKMsg> list = mm.searchMsgWithChannelAndContentTypes(
                    channelId, channelType, cursor, 100, types);
            if (list == null || list.isEmpty()) {
                break;
            }
            for (WKMsg msg : list) {
                if (packetNo.equals(packetNoFromMsg(msg))) {
                    if (!TextUtils.isEmpty(msg.clientMsgNO)) {
                        return mm.updateContentAndRefresh(msg.clientMsgNO, model, true);
                    }
                }
            }
            if (list.size() < 100) {
                break;
            }
            long lastOrderSeq = list.get(list.size() - 1).orderSeq;
            if (lastOrderSeq <= 0 || lastOrderSeq == cursor) {
                break;
            }
            cursor = lastOrderSeq;
        }
        return false;
    }

    /** 无 channel 时的兜底：按类型扫本地库（较慢，尽量不依赖）。 */
    private static void patchByGlobalRedPacketScan(MsgManager mm, String packetNo, WKRedPacketContent model) {
        @SuppressWarnings("unchecked")
        List<WKMsg> list = mm.getWithContentType(WKContentType.WK_REDPACKET, 0, 400);
        if (list == null) {
            return;
        }
        for (WKMsg msg : list) {
            if (packetNo.equals(packetNoFromMsg(msg)) && !TextUtils.isEmpty(msg.clientMsgNO)) {
                mm.updateContentAndRefresh(msg.clientMsgNO, model, true);
                return;
            }
        }
    }

    private static String packetNoFromMsg(@Nullable WKMsg msg) {
        if (msg == null) {
            return "";
        }
        WKRedPacketContent c = asRedPacketContent(msg);
        if (c != null && !TextUtils.isEmpty(c.packetNo)) {
            return c.packetNo;
        }
        return "";
    }

    @Nullable
    private static WKRedPacketContent asRedPacketContent(WKMsg msg) {
        if (msg.baseContentMsgModel instanceof WKRedPacketContent) {
            return (WKRedPacketContent) msg.baseContentMsgModel;
        }
        if (TextUtils.isEmpty(msg.content)) {
            return null;
        }
        try {
            JSONObject jo = new JSONObject(msg.content);
            WKRedPacketContent c = new WKRedPacketContent();
            c.decodeMsg(jo);
            return c;
        } catch (JSONException ignored) {
            return null;
        }
    }
}
