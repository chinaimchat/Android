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

public class WKTransferContent extends WKMessageContent {
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_ACCEPTED = 1;
    public static final int STATUS_REFUNDED = 2;

    /** 接口 {@code status_code} / {@code transfer_status} → 本地 {@link #status} */
    public static int statusFromApiStatusCode(int apiCode) {
        if (apiCode == 1) {
            return STATUS_ACCEPTED;
        }
        if (apiCode == 2) {
            return STATUS_REFUNDED;
        }
        return STATUS_PENDING;
    }

    public String transferNo;
    public double amount;
    public String remark;
    public String fromUid;
    public String toUid;
    public int status;

    public WKTransferContent() { type = WKContentType.WK_TRANSFER; }

    @NonNull @Override
    public JSONObject encodeMsg() {
        JSONObject j = new JSONObject();
        try {
            j.put("transfer_no",transferNo); j.put("amount",amount); j.put("remark",remark);
            j.put("from_uid",fromUid); j.put("to_uid",toUid); j.put("status",status);
        } catch (JSONException ignored) {}
        return j;
    }

    @Override
    public WKMessageContent decodeMsg(JSONObject j) {
        transferNo = j.optString("transfer_no");
        amount = j.optDouble("amount", 0);
        remark = j.optString("remark");
        fromUid = j.optString("from_uid");
        toUid = j.optString("to_uid");
        if (j.has("status")) {
            status = j.optInt("status");
        } else if (j.has("transfer_status")) {
            status = j.optInt("transfer_status");
        } else {
            status = STATUS_PENDING;
        }
        if (status == STATUS_PENDING && (j.optInt("accepted", 0) == 1 || j.optBoolean("is_accepted", false))) {
            status = STATUS_ACCEPTED;
        }
        return this;
    }

    protected WKTransferContent(Parcel in) {
        super(in); transferNo=in.readString(); amount=in.readDouble();
        remark=in.readString(); fromUid=in.readString(); toUid=in.readString(); status=in.readInt();
    }
    @Override public void writeToParcel(Parcel d, int f) {
        super.writeToParcel(d,f); d.writeString(transferNo); d.writeDouble(amount);
        d.writeString(remark); d.writeString(fromUid); d.writeString(toUid); d.writeInt(status);
    }
    public static final Creator<WKTransferContent> CREATOR = new Creator<WKTransferContent>() {
        @Override public WKTransferContent createFromParcel(Parcel in) { return new WKTransferContent(in); }
        @Override public WKTransferContent[] newArray(int s) { return new WKTransferContent[s]; }
    };
    @Override public int describeContents() { return 0; }
    @Override public String getDisplayContent() {
        Context ctx = WKBaseApplication.getInstance().getContext();
        if (ctx != null) {
            int statusLabel = R.string.transfer_pending;
            if (status == STATUS_ACCEPTED) {
                statusLabel = R.string.transfer_accepted;
            } else if (status == STATUS_REFUNDED) {
                statusLabel = R.string.transfer_refunded;
            }
            return ctx.getString(R.string.last_msg_transfer_with_status, amount, ctx.getString(statusLabel));
        }
        String st = "待确认";
        if (status == STATUS_ACCEPTED) st = "已收款";
        else if (status == STATUS_REFUNDED) st = "已退回";
        return "[转账] " + String.format(java.util.Locale.getDefault(), "%.2f元 · %s", amount, st);
    }
}
