package com.chat.wallet.ui;

import android.text.TextUtils;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.wallet.R;
import com.chat.wallet.entity.TransactionRecord;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TransactionRecordAdapter extends BaseQuickAdapter<TransactionRecord, BaseViewHolder> {
    public TransactionRecordAdapter() {
        super(R.layout.item_transaction_record);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder h, TransactionRecord item) {
        String title = TextUtils.isEmpty(item.remark) ? typeLabel(item.type) : item.remark;
        h.setText(R.id.remarkTv, title);

        String detail = buildDetail(item);
        if (TextUtils.isEmpty(detail)) {
            h.setGone(R.id.detailTv, true);
        } else {
            h.setGone(R.id.detailTv, false);
            h.setText(R.id.detailTv, detail);
        }

        h.setText(R.id.timeTv, item.created_at != null ? item.created_at : "");
        if (item.amount >= 0) {
            h.setText(R.id.amountTv, String.format("+%.2f", item.amount));
            h.setTextColor(R.id.amountTv, 0xFF4CAF50);
        } else {
            h.setText(R.id.amountTv, String.format("%.2f", item.amount));
            h.setTextColor(R.id.amountTv, 0xFFF44336);
        }
    }

    private String buildDetail(TransactionRecord item) {
        List<String> parts = new ArrayList<>();
        String group = item.resolveGroupName();
        if (!TextUtils.isEmpty(group)) {
            parts.add(getContext().getString(R.string.wallet_tx_group_fmt, group));
        }
        String type = item.type;
        if ("redpacket_receive".equals(type) || "transfer_in".equals(type)) {
            String from = item.resolveFromName();
            if (TextUtils.isEmpty(from)) {
                from = item.resolvePeerName();
            }
            if (!TextUtils.isEmpty(from)) {
                parts.add(getContext().getString(R.string.wallet_tx_from_fmt, from));
            }
        } else if ("redpacket_send".equals(type) || "transfer_out".equals(type)) {
            String to = item.resolveToName();
            if (TextUtils.isEmpty(to)) {
                to = item.resolvePeerName();
            }
            if (!TextUtils.isEmpty(to)) {
                parts.add(getContext().getString(R.string.wallet_tx_to_fmt, to));
            }
        }
        if (parts.isEmpty()) {
            String peer = item.resolvePeerName();
            if (!TextUtils.isEmpty(peer)) {
                parts.add(getContext().getString(R.string.wallet_tx_peer_fmt, peer));
            }
        }
        if ("redpacket_receive".equals(type)) {
            parts.add(getContext().getString(R.string.wallet_tx_redpacket_got, item.amount));
        } else if ("transfer_in".equals(type)) {
            parts.add(getContext().getString(R.string.wallet_tx_transfer_got, item.amount));
        } else if ("redpacket_send".equals(type)) {
            parts.add(getContext().getString(R.string.wallet_tx_redpacket_sent, Math.abs(item.amount)));
        } else if ("transfer_out".equals(type)) {
            parts.add(getContext().getString(R.string.wallet_tx_transfer_sent, Math.abs(item.amount)));
        }
        return parts.isEmpty() ? "" : TextUtils.join(" · ", parts);
    }

    private String typeLabel(String t) {
        if (t == null) {
            return "";
        }
        switch (t) {
            case "recharge":
                return "充值";
            case "admin_recharge":
                return "管理员充值";
            case "admin_adjust":
                return "管理员调整";
            case "redpacket_send":
                return "红包发出";
            case "redpacket_receive":
                return "红包收入";
            case "transfer_out":
                return "转账发出";
            case "transfer_in":
                return "转账收入";
            case "refund":
                return "退款";
            case "withdrawal":
                return "提现";
            case "withdrawal_refund":
                return "提现退款";
            case "fee":
                return "手续费";
            case "transfer_refund":
                return "转账退回";
            default:
                return t;
        }
    }
}
