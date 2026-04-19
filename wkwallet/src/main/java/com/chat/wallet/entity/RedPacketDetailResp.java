package com.chat.wallet.entity;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

public class RedPacketDetailResp {
    public int status;
    public String packet_no;
    public String sender_uid;
    /** 红包形态：与 {@link com.chat.wallet.msg.WKRedPacketContent#packetType} 一致（1 个人 2 拼手气 3 普通群 4 专属）。 */
    @JSONField(name = "packet_type", alternateNames = {"type", "packetType", "redpacket_type"})
    public int type;
    public double total_amount;
    public int total_count;
    public double remaining_amount;
    public int remaining_count;
    public String remark;
    public int redpacket_status;
    public double my_amount;
    public List<RedPacketRecord> records;
    public String created_at;

    /** 会话 id；群红包时为群号，单聊为对方 uid 等，与 {@link #channel_type} 配合使用。 */
    @JSONField(name = "channel_id", alternateNames = {"channelId"})
    public String channel_id;
    @JSONField(name = "channel_type", alternateNames = {"channelType"})
    public Integer channel_type;
}
