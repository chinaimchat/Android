package com.chat.wallet.entity;

import com.alibaba.fastjson.JSONObject;

public class TransferSendResp {
    public int status;
    public String msg;
    public String transfer_no;
    public JSONObject data;

    /**
     * 与 {@link com.chat.wallet.entity.TransferDetailResp#status_code} 一致：0 待确认、1 已收款、2 已退回。
     * 优先读 {@code data.status_code}，其次 {@code data.transfer_status}。
     */
    public int getResolvedStatusCode() {
        if (data != null) {
            if (data.containsKey("status_code")) {
                return data.getIntValue("status_code");
            }
            if (data.containsKey("transfer_status")) {
                return data.getIntValue("transfer_status");
            }
        }
        return 0;
    }
}
