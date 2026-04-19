package com.chat.wallet.entity;

import com.alibaba.fastjson.annotation.JSONField;

public class TransferDetailResp {
    public int status;
    public String transfer_no;
    public String from_uid;
    public String to_uid;
    public double amount;
    public String remark;
    public int status_code;
    public String created_at;

    @JSONField(name = "channel_id", alternateNames = {"channelId"})
    public String channel_id;
    @JSONField(name = "channel_type", alternateNames = {"channelType"})
    public Integer channel_type;
}
