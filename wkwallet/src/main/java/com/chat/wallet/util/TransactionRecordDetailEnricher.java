package com.chat.wallet.util;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSONObject;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.base.common.WKCommonModel;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.net.IRequestResultListener;
import com.chat.uikit.user.service.UserModel;
import com.chat.wallet.api.WalletModel;
import com.chat.wallet.entity.RedPacketDetailResp;
import com.chat.wallet.entity.TransactionRecord;
import com.chat.wallet.entity.TransferDetailResp;
import com.xinbida.wukongim.entity.WKChannelType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 流水列表仅含 {@link TransactionRecord#related_id}，需组合：
 * {@code GET /v1/redpacket/:packet_no}、{@code GET /v1/transfer/:transfer_no}，
 * 再拉会话名（群）与 {@link UserModel#getUserInfo} 解析对端昵称。
 */
public final class TransactionRecordDetailEnricher {

    private TransactionRecordDetailEnricher() {
    }

    /** 写入 context，便于从流水/红包记录进详情时携带会话，与群内禁止互加等规则对齐。 */
    private static void patchContextChannel(TransactionRecord r, String channelId, Integer channelType) {
        if (r == null || (TextUtils.isEmpty(channelId) && channelType == null)) {
            return;
        }
        if (r.context == null) {
            r.context = new JSONObject();
        }
        if (!TextUtils.isEmpty(channelId)) {
            r.context.put("channel_id", channelId);
        }
        if (channelType != null) {
            r.context.put("channel_type", channelType);
        }
    }

    private static void patchPacketType(TransactionRecord r, RedPacketDetailResp d) {
        if (r == null || d == null) {
            return;
        }
        int pt = d.type;
        if (pt <= 0) {
            return;
        }
        if (r.context == null) {
            r.context = new JSONObject();
        }
        r.context.put("packet_type", pt);
    }

    /** 「我发出的」列表展示领取进度：总人数、剩余份数、过期等状态。 */
    private static void patchRedPacketClaimStats(TransactionRecord r, RedPacketDetailResp d) {
        if (r == null || d == null) {
            return;
        }
        if (r.context == null) {
            r.context = new JSONObject();
        }
        r.context.put("redpacket_total_count", d.total_count);
        r.context.put("redpacket_remaining_count", d.remaining_count);
        r.context.put("redpacket_status", d.redpacket_status);
    }

    private static boolean isActivityOk(Activity a) {
        if (a == null || a.isFinishing()) {
            return false;
        }
        return Build.VERSION.SDK_INT < 17 || !a.isDestroyed();
    }

    private static boolean isGroupChannel(Integer channelType) {
        return channelType != null && channelType.byteValue() == WKChannelType.GROUP;
    }

    public static void scheduleSequentialEnrich(Activity activity,
                                                List<TransactionRecord> batch,
                                                BaseQuickAdapter<TransactionRecord, BaseViewHolder> adapter) {
        if (!isActivityOk(activity) || batch == null || batch.isEmpty() || adapter == null) {
            return;
        }
        List<TransactionRecord> work = new ArrayList<>();
        for (TransactionRecord r : batch) {
            if (needsEnrich(r)) {
                work.add(r);
            }
        }
        if (work.isEmpty()) {
            return;
        }
        Handler h = new Handler(Looper.getMainLooper());
        runNext(activity, work, 0, adapter, h);
    }

    /**
     * 与 {@link #scheduleSequentialEnrich} 相同的数据补全，但多条并行请求，全部结束后只 {@link BaseQuickAdapter#notifyDataSetChanged()} 一次，
     * 避免列表昵称逐行「蹦出来」的不适感（如红包记录页）。
     */
    public static void scheduleParallelEnrichOnce(Activity activity,
                                                  List<TransactionRecord> batch,
                                                  BaseQuickAdapter<TransactionRecord, BaseViewHolder> adapter) {
        scheduleParallelEnrichOnce(activity, batch, adapter, null);
    }

    /**
     * @param adapter                可为 null；为 null 时不刷新列表，仅执行 {@code onCompleteMainThread}（用于补全后再 {@code setList}）。
     * @param onCompleteMainThread   全部结束（含无需补全、列表为空、Activity 不可用等）时主线程回调；与 {@code adapter} 独立。
     */
    public static void scheduleParallelEnrichOnce(Activity activity,
                                                  List<TransactionRecord> batch,
                                                  @Nullable BaseQuickAdapter<TransactionRecord, BaseViewHolder> adapter,
                                                  @Nullable Runnable onCompleteMainThread) {
        Handler main = new Handler(Looper.getMainLooper());
        Runnable dispatchComplete = () -> main.post(() -> {
            if (!isActivityOk(activity)) {
                return;
            }
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            if (onCompleteMainThread != null) {
                onCompleteMainThread.run();
            }
        });

        if (!isActivityOk(activity) || batch == null) {
            dispatchComplete.run();
            return;
        }
        if (batch.isEmpty()) {
            dispatchComplete.run();
            return;
        }

        List<TransactionRecord> work = new ArrayList<>();
        for (TransactionRecord r : batch) {
            if (needsEnrich(r)) {
                work.add(r);
            }
        }
        if (work.isEmpty()) {
            dispatchComplete.run();
            return;
        }

        AtomicInteger remaining = new AtomicInteger(work.size());
        Runnable onOneDone = () -> {
            if (remaining.decrementAndGet() != 0) {
                return;
            }
            dispatchComplete.run();
        };
        for (TransactionRecord r : work) {
            enrichOne(r, onOneDone);
        }
    }

    private static boolean needsEnrich(TransactionRecord r) {
        if (r == null || TextUtils.isEmpty(r.related_id)) {
            return false;
        }
        String t = r.type;
        return "redpacket_send".equals(t) || "redpacket_receive".equals(t)
                || "transfer_in".equals(t) || "transfer_out".equals(t);
    }

    private static void runNext(Activity activity, List<TransactionRecord> work, int index,
                                BaseQuickAdapter<TransactionRecord, BaseViewHolder> adapter, Handler h) {
        if (!isActivityOk(activity) || index >= work.size()) {
            return;
        }
        TransactionRecord r = work.get(index);
        enrichOne(r, () -> {
            if (!isActivityOk(activity)) {
                return;
            }
            int pos = adapter.getItemPosition(r);
            if (pos >= 0) {
                adapter.notifyItemChanged(pos);
            }
            h.post(() -> runNext(activity, work, index + 1, adapter, h));
        });
    }

    private static void enrichOne(TransactionRecord r, Runnable done) {
        String t = r.type;
        if ("redpacket_send".equals(t) || "redpacket_receive".equals(t)) {
            enrichRedPacket(r, t, done);
        } else if ("transfer_in".equals(t) || "transfer_out".equals(t)) {
            enrichTransfer(r, t, done);
        } else {
            done.run();
        }
    }

    private static void enrichRedPacket(TransactionRecord r, String type, Runnable done) {
        WalletModel.getInstance().getRedPacketDetail(r.related_id, new IRequestResultListener<RedPacketDetailResp>() {
            @Override
            public void onSuccess(RedPacketDetailResp d) {
                if (d == null) {
                    done.run();
                    return;
                }
                patchContextChannel(r, d.channel_id, d.channel_type);
                patchPacketType(r, d);
                patchRedPacketClaimStats(r, d);
                if ("redpacket_receive".equals(type) && d.my_amount > 0) {
                    r.amount = d.my_amount;
                }
                if ("redpacket_receive".equals(type)) {
                    loadGroupThenUser(r, d.channel_id, d.channel_type, d.sender_uid, done);
                } else {
                    loadGroupThenUser(r, d.channel_id, d.channel_type, null, done);
                }
            }

            @Override
            public void onFail(int c, String m) {
                done.run();
            }
        });
    }

    private static void enrichTransfer(TransactionRecord r, String type, Runnable done) {
        WalletModel.getInstance().getTransferDetail(r.related_id, new IRequestResultListener<TransferDetailResp>() {
            @Override
            public void onSuccess(TransferDetailResp d) {
                if (d == null) {
                    done.run();
                    return;
                }
                String counterUid = "transfer_in".equals(type) ? d.from_uid : d.to_uid;
                loadGroupThenPeer(r, d.channel_id, d.channel_type, type, counterUid, done);
            }

            @Override
            public void onFail(int c, String m) {
                done.run();
            }
        });
    }

    /**
     * 先拉群会话名（仅群），再拉用户昵称写入 {@link TransactionRecord#from_user_name}（红包领取场景）。
     */
    private static void loadGroupThenUser(TransactionRecord r, String channelId, Integer channelType,
                                          String uidForUserFetch, Runnable done) {
        Runnable fetchUser = () -> {
            if (TextUtils.isEmpty(uidForUserFetch)) {
                done.run();
                return;
            }
            String grpNo = isGroupChannel(channelType) && !TextUtils.isEmpty(channelId) ? channelId : null;
            UserModel.getInstance().getUserInfo(uidForUserFetch, grpNo, (code, msg, info) -> {
                if (code == HttpResponseCode.success && info != null && !TextUtils.isEmpty(info.name)) {
                    r.from_user_name = info.name;
                }
                done.run();
            });
        };
        if (isGroupChannel(channelType) && !TextUtils.isEmpty(channelId)) {
            WKCommonModel.getInstance().getChannel(channelId, WKChannelType.GROUP, (code, msg, entity) -> {
                if (code == HttpResponseCode.success && entity != null && !TextUtils.isEmpty(entity.name)) {
                    r.group_name = entity.name;
                }
                fetchUser.run();
            });
        } else {
            fetchUser.run();
        }
    }

    private static void loadGroupThenPeer(TransactionRecord r, String channelId, Integer channelType,
                                          String txType, String counterUid, Runnable done) {
        Runnable fetchPeer = () -> {
            if (TextUtils.isEmpty(counterUid)) {
                done.run();
                return;
            }
            String grpNo = isGroupChannel(channelType) && !TextUtils.isEmpty(channelId) ? channelId : null;
            UserModel.getInstance().getUserInfo(counterUid, grpNo, (code, msg, info) -> {
                if (code == HttpResponseCode.success && info != null && !TextUtils.isEmpty(info.name)) {
                    if ("transfer_in".equals(txType)) {
                        r.from_user_name = info.name;
                    } else {
                        r.to_user_name = info.name;
                    }
                }
                done.run();
            });
        };
        if (isGroupChannel(channelType) && !TextUtils.isEmpty(channelId)) {
            WKCommonModel.getInstance().getChannel(channelId, WKChannelType.GROUP, (code, msg, entity) -> {
                if (code == HttpResponseCode.success && entity != null && !TextUtils.isEmpty(entity.name)) {
                    r.group_name = entity.name;
                }
                fetchPeer.run();
            });
        } else {
            fetchPeer.run();
        }
    }
}
