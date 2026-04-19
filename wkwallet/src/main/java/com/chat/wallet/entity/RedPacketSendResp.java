package com.chat.wallet.entity;

import com.alibaba.fastjson.JSONObject;

/**
 * 发红包接口响应。部分服务端把单号放在 data 内，或与 status/msg 组合表示业务结果。
 */
public class RedPacketSendResp {
    public int status;
    public String msg;
    public String packet_no;
    public JSONObject data;
}
