package com.chat.wallet.entity;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * {@code GET /v1/wallet/withdrawal/fee-config} 解析结果：页面初始化用的最小提币、参考费率/说明（实际按金额的手续费以 {@code fee-preview} 为准）。
 * <p>兼容根对象、{@code data}/{@code result} 包装，以及按 {@code channel_id} 分条的列表（{@code list/channels/items/configs}）。</p>
 */
public final class WithdrawalFeeConfig {

    public static final double DEFAULT_MIN_WITHDRAW = 4.0;
    public static final double DEFAULT_SERVICE_FEE = 2.0;

    public final double minWithdrawUsdt;
    public final double serviceFeeUsdt;
    /** 费率规则说明，展示在「？」或弹窗；无则客户端用默认文案 */
    @Nullable
    public final String feeDescription;

    public WithdrawalFeeConfig(double minWithdrawUsdt, double serviceFeeUsdt, @Nullable String feeDescription) {
        this.minWithdrawUsdt = normalizeMin(minWithdrawUsdt);
        this.serviceFeeUsdt = normalizeFee(serviceFeeUsdt);
        this.feeDescription = feeDescription;
    }

    public static WithdrawalFeeConfig fallbackDefaults() {
        return new WithdrawalFeeConfig(DEFAULT_MIN_WITHDRAW, DEFAULT_SERVICE_FEE, null);
    }

    private static double normalizeMin(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v <= 0) {
            return DEFAULT_MIN_WITHDRAW;
        }
        return v;
    }

    private static double normalizeFee(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v < 0) {
            return DEFAULT_SERVICE_FEE;
        }
        return v;
    }

    /**
     * @param forChannelId 当前选中的链上渠道 id；&gt;0 时优先匹配列表中与 {@code channel_id} 一致的项
     */
    public static WithdrawalFeeConfig parse(String json, long forChannelId) {
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        JSONObject root = JSON.parseObject(json.trim());
        if (root == null) {
            return null;
        }
        if (root.containsKey("status")) {
            int st = root.getIntValue("status");
            if (st != 0 && st != 200) {
                return null;
            }
        }
        JSONObject base = unwrapPayload(root);
        if (base == null) {
            return null;
        }

        String[] arrayKeys = {"list", "channels", "items", "configs", "fee_configs"};
        if (forChannelId > 0) {
            for (String k : arrayKeys) {
                JSONArray arr = base.getJSONArray(k);
                if (arr == null || arr.isEmpty()) {
                    continue;
                }
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject it = arr.getJSONObject(i);
                    if (it == null) {
                        continue;
                    }
                    long cid = it.getLongValue("channel_id");
                    if (cid <= 0) {
                        cid = it.getLongValue("id");
                    }
                    if (cid != forChannelId) {
                        continue;
                    }
                    Double min = readDouble(it, "min_amount", "min_withdraw", "min_withdraw_amount", "min_usdt");
                    Double fee = readDouble(it, "service_fee", "withdraw_fee", "fee", "fee_usdt", "network_fee");
                    if (min != null || fee != null) {
                        String desc = readString(it, "hint", "fee_hint", "fee_desc", "fee_description", "fee_tip",
                                "fee_rule", "remark");
                        return new WithdrawalFeeConfig(
                                min != null ? min : DEFAULT_MIN_WITHDRAW,
                                fee != null ? fee : DEFAULT_SERVICE_FEE,
                                desc);
                    }
                }
            }
        }

        Double min = readDouble(base, "min_amount", "min_withdraw", "min_withdraw_amount", "min_usdt");
        Double fee = readDouble(base, "service_fee", "withdraw_fee", "fee", "fee_usdt", "network_fee");
        if (min == null && fee == null) {
            return null;
        }
        String desc = readString(base, "hint", "fee_hint", "fee_desc", "fee_description", "fee_tip", "fee_rule",
                "remark");
        return new WithdrawalFeeConfig(
                min != null ? min : DEFAULT_MIN_WITHDRAW,
                fee != null ? fee : DEFAULT_SERVICE_FEE,
                desc);
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
