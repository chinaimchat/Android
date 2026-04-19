package com.chat.wallet.entity;

import android.text.TextUtils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;

/**
 * POST /v1/wallet/recharge/apply 响应；最终以服务端返回的到账金额为准。
 * 字段与文档对齐：status、application_no、amount（入账金额）、amount_u、exchange_rate（U 换算时有效）。
 */
public class RechargeApplyResp {
    public int status;
    public String msg;
    /** 根级到账金额（若接口放在 data 内见 {@link #data}） */
    public Double amount;
    @JSONField(name = "application_no")
    public String applicationNo;
    @JSONField(name = "amount_u")
    public Double amountU;
    @JSONField(name = "exchange_rate")
    public Double exchangeRate;
    public JSONObject data;

    public double getResolvedCreditedAmount() {
        if (amount != null && !amount.isNaN()) {
            return amount;
        }
        if (data != null) {
            if (data.containsKey("amount")) {
                return data.getDoubleValue("amount");
            }
            if (data.containsKey("credited_amount")) {
                return data.getDoubleValue("credited_amount");
            }
        }
        return Double.NaN;
    }

    /** 申请单号：根级或 data 内 */
    public String getResolvedApplicationNo() {
        if (!TextUtils.isEmpty(applicationNo)) {
            return applicationNo;
        }
        if (data != null && data.containsKey("application_no")) {
            return data.getString("application_no");
        }
        return null;
    }
}
