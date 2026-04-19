package com.chat.wallet.entity;

import android.text.TextUtils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;

/**
 * {@code POST /v1/wallet/withdrawal/apply} 响应。
 * <p><b>服务端资金约定（需在后台实现）</b>：提交成功即冻结本单对应额度（含手续费）；管理端<b>拒绝</b>或<b>超时未审</b>应解冻并退回可用余额；<b>通过</b>后再做最终扣减并完成出账。客户端仅展示余额与文案，不做本地加减。</p>
 */
public class WithdrawApplyResp {
    public int status;
    public String msg;
    @JSONField(name = "withdrawal_no", alternateNames = {"withdrawalNo"})
    public String withdrawal_no;
    /**
     * 到账金额（USDT）≈ {@code amount - fee}；旧字段名 {@code total_freeze} 兼容解析。
     */
    @JSONField(name = "actual_amount", alternateNames = {"actualAmount", "total_freeze", "totalFreeze"})
    public Double actual_amount;
    public JSONObject data;

    /** 到账金额，优先根字段，其次 {@link #data}。 */
    public Double getResolvedActualAmountUsdt() {
        if (actual_amount != null && !actual_amount.isNaN()) {
            return actual_amount;
        }
        if (data != null) {
            Double d = readAmountFromData(data, "actual_amount", "actualAmount", "total_freeze", "totalFreeze");
            if (d != null) {
                return d;
            }
        }
        return null;
    }

    private static Double readAmountFromData(JSONObject data, String... keys) {
        for (String k : keys) {
            if (!data.containsKey(k)) {
                continue;
            }
            Object v = data.get(k);
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

    public String getResolvedWithdrawalNo() {
        if (!TextUtils.isEmpty(withdrawal_no)) {
            return withdrawal_no.trim();
        }
        if (data != null) {
            String n = data.getString("withdrawal_no");
            if (TextUtils.isEmpty(n)) {
                n = data.getString("withdrawalNo");
            }
            if (!TextUtils.isEmpty(n)) {
                return n.trim();
            }
        }
        return null;
    }
}
