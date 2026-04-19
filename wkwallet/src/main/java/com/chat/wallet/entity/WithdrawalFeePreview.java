package com.chat.wallet.entity;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * {@code GET /v1/wallet/withdrawal/fee-preview?amount=…} 解析结果：手续费与到账金额（{@code actual_amount} = amount - fee，旧 {@code total_freeze} 兼容）。
 */
public final class WithdrawalFeePreview {

    /** 业务是否成功（HTTP 200 且体可解析、无明确失败码时 true） */
    public final boolean ok;
    @Nullable
    public final String msg;
    /** 用户输入的提币数量回显（若有） */
    public final double amount;
    /** 手续费（USDT） */
    public final double fee;
    /** 预计到账（USDT） */
    public final double arrival;
    /** 服务端若以字符串下发金额类字段，优先用于展示（保留精度） */
    @Nullable
    public final String feeText;
    @Nullable
    public final String arrivalText;

    public WithdrawalFeePreview(boolean ok, @Nullable String msg, double amount, double fee, double arrival,
                                @Nullable String feeText, @Nullable String arrivalText) {
        this.ok = ok;
        this.msg = msg;
        this.amount = amount;
        this.fee = fee;
        this.arrival = arrival;
        this.feeText = feeText;
        this.arrivalText = arrivalText;
    }

    public static WithdrawalFeePreview failed(@Nullable String msg) {
        return new WithdrawalFeePreview(false, msg, Double.NaN, Double.NaN, Double.NaN, null, null);
    }

    public static WithdrawalFeePreview parse(String json) {
        if (TextUtils.isEmpty(json)) {
            return emptyFail(null);
        }
        JSONObject root = JSON.parseObject(json.trim());
        if (root == null) {
            return emptyFail(null);
        }
        if (root.containsKey("status")) {
            int st = root.getIntValue("status");
            if (st != 0 && st != 200) {
                String m = readString(root, "msg", "message", "error");
                return emptyFail(m);
            }
        }
        JSONObject base = unwrapPayload(root);
        if (base == null) {
            return emptyFail(readString(root, "msg", "message"));
        }

        Double feeNum = readDouble(base, "fee", "service_fee", "withdraw_fee", "fee_usdt", "network_fee");
        // 到账 = amount - fee；后台统一为 actual_amount（旧 arrival_* / total_freeze 兼容）
        Double arrNum = readDouble(base, "actual_amount", "actualAmount", "arrival_amount", "receive_amount",
                "net_amount", "arrived_amount", "amount_received", "real_amount", "total_freeze", "totalFreeze");
        String feeStr = readString(base, "fee_str", "service_fee_str", "fee_text");
        String arrStr = readString(base, "actual_amount_str", "actualAmountStr", "arrival_str", "receive_amount_str",
                "net_amount_str");
        Double amtEcho = readDouble(base, "amount", "withdraw_amount", "request_amount");

        boolean hasNumbers = feeNum != null || arrNum != null;
        if (!hasNumbers) {
            String m = readString(base, "msg", "message", "error");
            return emptyFail(m);
        }

        double fee = feeNum != null ? feeNum : Double.NaN;
        double arrival = arrNum != null ? arrNum : Double.NaN;
        double amount = amtEcho != null ? amtEcho : Double.NaN;

        return new WithdrawalFeePreview(true, null, amount, fee, arrival, feeStr, arrStr);
    }

    private static WithdrawalFeePreview emptyFail(@Nullable String msg) {
        return new WithdrawalFeePreview(false, msg, Double.NaN, Double.NaN, Double.NaN, null, null);
    }

    private static JSONObject unwrapPayload(JSONObject root) {
        if (root.containsKey("data")) {
            Object d = root.get("data");
            if (d instanceof JSONObject) {
                return (JSONObject) d;
            }
            if (d instanceof String) {
                String s = ((String) d).trim();
                if (s.startsWith("{")) {
                    return JSON.parseObject(s);
                }
            }
        }
        if (root.containsKey("result") && root.get("result") instanceof JSONObject) {
            return root.getJSONObject("result");
        }
        return root;
    }

    private static Double readDouble(JSONObject o, String... keys) {
        for (String k : keys) {
            if (o == null || !o.containsKey(k)) {
                continue;
            }
            Object v = o.get(k);
            if (v == null) {
                continue;
            }
            if (v instanceof Number) {
                return ((Number) v).doubleValue();
            }
            if (v instanceof String) {
                String s = ((String) v).trim();
                if (s.isEmpty()) {
                    continue;
                }
                try {
                    return Double.parseDouble(s.replace(",", "."));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    @Nullable
    private static String readString(JSONObject o, String... keys) {
        for (String k : keys) {
            if (o == null || !o.containsKey(k)) {
                continue;
            }
            Object v = o.get(k);
            if (v == null) {
                continue;
            }
            String s = String.valueOf(v).trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        return null;
    }
}
