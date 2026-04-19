package com.chat.wallet.util;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.chat.base.msgitem.WKContentType;
import com.chat.wallet.msg.WKTransferContent;
import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.entity.WKMsg;
import com.xinbida.wukongim.manager.MsgManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * 确认收款等成功后：写回本地转账消息 {@link WKTransferContent#status} 并刷新聊天气泡 / 会话摘要。
 */
public final class TransferLocalSync {

    private TransferLocalSync() {
    }

    public static void applyStatus(
            @Nullable String transferNo,
            @Nullable String channelId,
            byte channelType,
            @Nullable String clientMsgNo,
            int status
    ) {
        if (TextUtils.isEmpty(transferNo)) {
            return;
        }
        Runnable job = () -> applyOnMainThread(transferNo, channelId, channelType, clientMsgNo, status);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            job.run();
        } else {
            new Handler(Looper.getMainLooper()).post(job);
        }
    }

    private static void applyOnMainThread(
            String transferNo,
            @Nullable String channelId,
            byte channelType,
            @Nullable String clientMsgNo,
            int status
    ) {
        MsgManager mm = WKIM.getInstance().getMsgManager();
        if (!TextUtils.isEmpty(clientMsgNo)) {
            WKMsg msg = mm.getWithClientMsgNO(clientMsgNo);
            if (msg != null && transferNo.equals(transferNoFromMsg(msg))) {
                WKTransferContent patch = copyWithStatus(msg, status);
                if (patch != null && mm.updateContentAndRefresh(clientMsgNo, patch, true)) {
                    return;
                }
            }
        }
        if (!TextUtils.isEmpty(channelId)) {
            if (patchByChannelSearch(mm, channelId, channelType, transferNo, status)) {
                return;
            }
        }
        patchByGlobalTransferScan(mm, transferNo, status);
    }

    private static boolean patchByChannelSearch(
            MsgManager mm,
            String channelId,
            byte channelType,
            String transferNo,
            int status
    ) {
        int[] types = new int[]{WKContentType.WK_TRANSFER};
        long cursor = 0;
        for (int round = 0; round < 50; round++) {
            @SuppressWarnings("unchecked")
            List<WKMsg> list = mm.searchMsgWithChannelAndContentTypes(
                    channelId, channelType, cursor, 100, types);
            if (list == null || list.isEmpty()) {
                break;
            }
            for (WKMsg msg : list) {
                if (transferNo.equals(transferNoFromMsg(msg))) {
                    WKTransferContent model = copyWithStatus(msg, status);
                    if (model != null && !TextUtils.isEmpty(msg.clientMsgNO)) {
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

    private static void patchByGlobalTransferScan(MsgManager mm, String transferNo, int status) {
        @SuppressWarnings("unchecked")
        List<WKMsg> list = mm.getWithContentType(WKContentType.WK_TRANSFER, 0, 400);
        if (list == null) {
            return;
        }
        for (WKMsg msg : list) {
            if (transferNo.equals(transferNoFromMsg(msg)) && !TextUtils.isEmpty(msg.clientMsgNO)) {
                WKTransferContent model = copyWithStatus(msg, status);
                if (model != null) {
                    mm.updateContentAndRefresh(msg.clientMsgNO, model, true);
                }
                return;
            }
        }
    }

    private static String transferNoFromMsg(@Nullable WKMsg msg) {
        if (msg == null) {
            return "";
        }
        WKTransferContent c = asTransferContent(msg);
        return c != null && !TextUtils.isEmpty(c.transferNo) ? c.transferNo : "";
    }

    @Nullable
    private static WKTransferContent copyWithStatus(@Nullable WKMsg msg, int status) {
        WKTransferContent src = asTransferContent(msg);
        if (src == null) {
            return null;
        }
        WKTransferContent model = new WKTransferContent();
        model.transferNo = src.transferNo;
        model.amount = src.amount;
        model.remark = src.remark != null ? src.remark : "";
        model.fromUid = src.fromUid != null ? src.fromUid : "";
        model.toUid = src.toUid != null ? src.toUid : "";
        model.status = status;
        return model;
    }

    @Nullable
    private static WKTransferContent asTransferContent(@Nullable WKMsg msg) {
        if (msg == null) {
            return null;
        }
        if (msg.baseContentMsgModel instanceof WKTransferContent) {
            return (WKTransferContent) msg.baseContentMsgModel;
        }
        if (TextUtils.isEmpty(msg.content)) {
            return null;
        }
        try {
            JSONObject jo = new JSONObject(msg.content);
            WKTransferContent c = new WKTransferContent();
            c.decodeMsg(jo);
            return c;
        } catch (JSONException ignored) {
            return null;
        }
    }
}
