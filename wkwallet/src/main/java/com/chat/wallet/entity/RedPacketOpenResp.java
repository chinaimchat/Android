package com.chat.wallet.entity;

import com.alibaba.fastjson.JSONObject;

/**
 * POST 开红包接口响应；业务字段在 {@code data} 内。
 * <p>
 * 注意：全局 {@code redpacket_status==0}（他人还可领）≠ 本地气泡「我还能点」；领取成功后换肤见 {@link #getLocalMessageStatusAfterOpenSuccess()}。
 */
public class RedPacketOpenResp {
    public int status;
    /** 部分接口用 code 表示业务码 */
    public Integer code;
    public String msg;
    /** 根级金额（若有） */
    public Double amount;
    public JSONObject data;

    /**
     * 解析实际领取金额：优先 {@code data} 内常见字段，其次根级 {@link #amount}。
     */
    public double getResolvedAmount() {
        if (data != null) {
            String[] keys = {"my_amount", "amount", "received_amount", "money", "receive_amount", "lucky_amount"};
            for (String k : keys) {
                if (data.containsKey(k)) {
                    double v = data.getDoubleValue(k);
                    if (!Double.isNaN(v) && v > 0) {
                        return v;
                    }
                }
            }
        }
        if (amount != null && !amount.isNaN() && amount > 0) {
            return amount;
        }
        return Double.NaN;
    }

    /**
     * 服务端「红包业务」状态（与后台约定一致）：0 进行中仍可领、1 已领完、2 过期。
     * 字段在 {@code data} 内；优先读 {@code redpacket_status}，与 {@code packet_status}/{@code status} 同值时可任读其一。
     */
    public int getResolvedRedPacketStatus() {
        if (data != null) {
            String[] keys = {"redpacket_status", "packet_status", "status"};
            for (String k : keys) {
                if (data.containsKey(k)) {
                    return data.getIntValue(k);
                }
            }
        }
        return 1;
    }

    /**
     * 领取成功回调里写入会话消息的 {@link com.chat.wallet.msg.WKRedPacketContent#status}。
     * <p>
     * 服务端 {@code redpacket_status==0} 表示「红包仍在进行、他人还可领」，而本地 {@code 0} 表示「当前用户仍可点开抢」；
     * 当前请求已成功拆开时，若仍为 0，本地应显示「已领取」浅色气泡，故映射为 {@code 3}（与 {@link com.chat.wallet.msg.WKRedPacketProvider} 中「已领取」分支一致）。
     */
    public int getLocalMessageStatusAfterOpenSuccess() {
        int s = getResolvedRedPacketStatus();
        return s == 0 ? 3 : s;
    }
}
