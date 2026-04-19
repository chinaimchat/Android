package com.chat.wallet.redpacket;

import android.content.Context;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.wallet.R;
import com.chat.wallet.entity.TransactionRecord;
import com.chat.wallet.msg.WKRedPacketContent;

import org.jetbrains.annotations.NotNull;

/**
 * 红包记录页列表：基于钱包流水中的 {@code redpacket_receive} / {@code redpacket_send}。
 */
public class RedPacketHistoryListAdapter extends BaseQuickAdapter<TransactionRecord, BaseViewHolder> {

    public RedPacketHistoryListAdapter() {
        super(R.layout.item_redpacket_history_row);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder h, TransactionRecord item) {
        boolean received = "redpacket_receive".equals(item.type);
        int packetType = item.resolveRedPacketPacketType();
        String title;
        if (received) {
            String from = item.resolveFromName();
            if (TextUtils.isEmpty(from)) {
                from = item.resolvePeerName();
            }
            title = TextUtils.isEmpty(from) ? fallbackReceiveTitle(getContext()) : from;
        } else {
            String typeLabel = redPacketTypeLabel(getContext(), packetType);
            title = !TextUtils.isEmpty(typeLabel)
                    ? typeLabel
                    : getContext().getString(R.string.redpacket);
        }
        h.setText(R.id.titleTv, title);
        h.setText(R.id.timeTv, RedPacketRecordHistoryActivity.formatRowTime(item.created_at));

        String badge = resolveTypeBadgeText(getContext(), packetType, item);
        if (TextUtils.isEmpty(badge)) {
            h.setGone(R.id.rpTypeBadgeTv, true);
        } else {
            h.setGone(R.id.rpTypeBadgeTv, false);
            h.setText(R.id.rpTypeBadgeTv, badge);
        }

        double amt = received ? item.amount : Math.abs(item.amount);
        h.setText(R.id.amountTv, getContext().getString(R.string.redpacket_history_amount_fmt, amt));
        bindSendClaimStatus(h, item, received);
    }

    /** 「我发出的」：在金额下展示领取进度（与详情接口 total/remaining 一致）。 */
    private static void bindSendClaimStatus(BaseViewHolder h, TransactionRecord item, boolean received) {
        if (received) {
            h.setGone(R.id.sendClaimStatusTv, true);
            return;
        }
        Context ctx = h.itemView.getContext();
        if (!item.hasRedPacketClaimProgress()) {
            h.setGone(R.id.sendClaimStatusTv, true);
            return;
        }
        int total = item.resolveRedPacketTotalCount();
        int remaining = item.resolveRedPacketRemainingCount();
        if (total <= 0) {
            h.setGone(R.id.sendClaimStatusTv, true);
            return;
        }
        h.setGone(R.id.sendClaimStatusTv, false);
        TextView tv = h.getView(R.id.sendClaimStatusTv);
        int st = item.resolveRedPacketStatus();
        if (st == 2) {
            tv.setText(ctx.getString(R.string.redpacket_expired));
            tv.setTextColor(ContextCompat.getColor(ctx, com.chat.base.R.color.color999));
            return;
        }
        if (remaining == 0) {
            tv.setText(ctx.getString(R.string.redpacket_history_claim_all_done));
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.wallet_redpacket_claim_done));
        } else {
            int claimed = total - remaining;
            if (claimed < 0) {
                claimed = 0;
            }
            tv.setText(ctx.getString(R.string.redpacket_received_count, claimed, total));
            tv.setTextColor(ContextCompat.getColor(ctx, com.chat.base.R.color.red));
        }
    }

    /**
     * 流水里的 {@link TransactionRecord#remark} 多为服务端摘要（如「领取红包」），不是发送方昵称；
     * 若用它作主标题会在补全昵称后「像变魔术一样」换掉，故此处只用固定占位，昵称靠 {@link com.chat.wallet.util.TransactionRecordDetailEnricher} 写入后再展示。
     */
    private static String fallbackReceiveTitle(Context ctx) {
        return ctx.getString(R.string.redpacket_history_fallback_receive);
    }

    /**
     * 角标一字：拼 / 普 / 专 / 个。未补全 {@code packet_type} 且能判断为群会话时仍显示「拼」（与旧逻辑兼容）。
     */
    private static String resolveTypeBadgeText(Context ctx, int packetType, TransactionRecord item) {
        switch (packetType) {
            case WKRedPacketContent.TYPE_GROUP_RANDOM:
                return ctx.getString(R.string.redpacket_history_badge_pin);
            case WKRedPacketContent.TYPE_GROUP_NORMAL:
                return ctx.getString(R.string.redpacket_history_badge_normal);
            case WKRedPacketContent.TYPE_EXCLUSIVE:
                return ctx.getString(R.string.redpacket_history_badge_exclusive);
            case WKRedPacketContent.TYPE_INDIVIDUAL:
                return ctx.getString(R.string.redpacket_history_badge_individual);
            default:
                if (!TextUtils.isEmpty(item.resolveGroupName())) {
                    return ctx.getString(R.string.redpacket_history_badge_pin);
                }
                return null;
        }
    }

    private static String redPacketTypeLabel(Context ctx, int packetType) {
        switch (packetType) {
            case WKRedPacketContent.TYPE_INDIVIDUAL:
                return ctx.getString(R.string.redpacket_type_individual);
            case WKRedPacketContent.TYPE_GROUP_RANDOM:
                return ctx.getString(R.string.redpacket_type_group_random);
            case WKRedPacketContent.TYPE_GROUP_NORMAL:
                return ctx.getString(R.string.redpacket_type_group_normal);
            case WKRedPacketContent.TYPE_EXCLUSIVE:
                return ctx.getString(R.string.redpacket_type_exclusive);
            default:
                return "";
        }
    }
}
