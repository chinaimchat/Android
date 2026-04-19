package com.chat.wallet.msg;

import android.content.Context;
import android.os.Parcel;
import androidx.annotation.NonNull;
import com.chat.base.WKBaseApplication;
import com.chat.base.msgitem.WKContentType;
import com.chat.wallet.R;
import com.xinbida.wukongim.msgmodel.WKMessageContent;
import org.json.JSONException;
import org.json.JSONObject;

public class WKRedPacketContent extends WKMessageContent {
    public static final int TYPE_INDIVIDUAL = 1;
    public static final int TYPE_GROUP_RANDOM = 2;
    public static final int TYPE_GROUP_NORMAL = 3;
    public static final int TYPE_EXCLUSIVE = 4;

    public String packetNo;
    public int packetType;
    public String remark;
    public String senderName;
    public int status;

    public WKRedPacketContent() { type = WKContentType.WK_REDPACKET; }

    @NonNull @Override
    public JSONObject encodeMsg() {
        JSONObject j = new JSONObject();
        try {
            j.put("packet_no", packetNo); j.put("packet_type", packetType);
            j.put("remark", remark); j.put("sender_name", senderName); j.put("status", status);
        } catch (JSONException ignored) {}
        return j;
    }

    @Override
    public WKMessageContent decodeMsg(JSONObject j) {
        packetNo = j.optString("packet_no");
        packetType = j.optInt("packet_type");
        remark = j.optString("remark");
        senderName = j.optString("sender_name");
        if (j.has("status")) {
            status = j.optInt("status");
        } else if (j.has("packet_status")) {
            status = j.optInt("packet_status");
        } else if (j.has("redpacket_status")) {
            status = j.optInt("redpacket_status");
        } else {
            status = 0;
        }
        // 部分接口用独立字段表示「已领」
        if (status == 0 && (j.optInt("received", 0) == 1 || j.optInt("is_received", 0) == 1
                || j.optBoolean("opened", false) || j.optInt("opened", 0) == 1)) {
            status = 1;
        }
        return this;
    }

    protected WKRedPacketContent(Parcel in) {
        super(in); packetNo=in.readString(); packetType=in.readInt();
        remark=in.readString(); senderName=in.readString(); status=in.readInt();
    }
    @Override public void writeToParcel(Parcel d, int f) {
        super.writeToParcel(d,f); d.writeString(packetNo); d.writeInt(packetType);
        d.writeString(remark); d.writeString(senderName); d.writeInt(status);
    }
    public static final Creator<WKRedPacketContent> CREATOR = new Creator<WKRedPacketContent>() {
        @Override public WKRedPacketContent createFromParcel(Parcel in) { return new WKRedPacketContent(in); }
        @Override public WKRedPacketContent[] newArray(int s) { return new WKRedPacketContent[s]; }
    };
    @Override public int describeContents() { return 0; }
    @Override public String getDisplayContent() {
        Context ctx = WKBaseApplication.getInstance().getContext();
        String remarkPart = remark != null && !remark.isEmpty() ? remark : null;
        if (ctx != null) {
            if (remarkPart == null) {
                remarkPart = ctx.getString(R.string.redpacket_remark);
            }
            if (status == 0) {
                return "[红包] " + remarkPart;
            }
            int statusLabel = R.string.redpacket_received;
            if (status == 1) {
                statusLabel = R.string.redpacket_finished;
            } else if (status == 2) {
                statusLabel = R.string.redpacket_expired;
            }
            return ctx.getString(R.string.last_msg_redpacket_with_status, remarkPart, ctx.getString(statusLabel));
        }
        String r = remarkPart != null ? remarkPart : "恭喜发财";
        if (status == 0) {
            return "[红包] " + r;
        }
        if (status == 1) {
            return "[红包] " + r + " · 红包已领完";
        }
        if (status == 2) {
            return "[红包] " + r + " · 已过期";
        }
        return "[红包] " + r + " · 已领取";
    }
}
