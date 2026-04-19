package com.chat.wallet.msg;

import android.os.Parcel;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.chat.base.msgitem.WKContentType;
import com.chat.wallet.entity.WalletBalanceResp;
import com.xinbida.wukongim.msgmodel.WKMessageContent;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * IM 下行 type=1020 钱包余额同步；与 {@code GET /v1/wallet/balance} 字段语义对齐。
 */
public class WKWalletBalanceSyncContent extends WKMessageContent {

    public int walletSyncVersion;
    public double usdtAvailable;
    public double usdtFrozen;
    public double usdtBalance;
    public double balance;
    public int walletStatus;
    public String reason;
    public String relatedId;
    public long ts;

    public WKWalletBalanceSyncContent() {
        type = WKContentType.WK_WALLET_BALANCE_SYNC;
    }

    @NonNull
    @Override
    public JSONObject encodeMsg() {
        JSONObject j = new JSONObject();
        try {
            j.put("type", WKContentType.WK_WALLET_BALANCE_SYNC);
            j.put("wallet_sync_version", walletSyncVersion);
            j.put("usdt_available", usdtAvailable);
            j.put("usdt_frozen", usdtFrozen);
            j.put("usdt_balance", usdtBalance);
            j.put("balance", balance);
            j.put("wallet_status", walletStatus);
            j.put("reason", reason);
            j.put("related_id", relatedId);
            j.put("ts", ts);
        } catch (JSONException ignored) {
        }
        return j;
    }

    @Override
    public WKMessageContent decodeMsg(JSONObject j) {
        walletSyncVersion = j.optInt("wallet_sync_version", j.optInt("walletSyncVersion", 1));
        usdtAvailable = j.optDouble("usdt_available", j.optDouble("usdtAvailable", Double.NaN));
        if (Double.isNaN(usdtAvailable)) {
            usdtAvailable = 0;
        }
        usdtFrozen = j.optDouble("usdt_frozen", j.optDouble("usdtFrozen", 0));
        usdtBalance = j.optDouble("usdt_balance", j.optDouble("usdtBalance", Double.NaN));
        if (Double.isNaN(usdtBalance)) {
            usdtBalance = usdtAvailable + usdtFrozen;
        }
        balance = j.optDouble("balance", Double.NaN);
        if (Double.isNaN(balance)) {
            balance = usdtAvailable;
        }
        walletStatus = j.optInt("wallet_status", j.optInt("walletStatus", 1));
        reason = j.optString("reason", null);
        relatedId = j.optString("related_id", j.optString("relatedId", null));
        ts = j.optLong("ts", 0L);
        return this;
    }

    /**
     * 转为与钱包 HTTP 接口共用的模型（不含 has_password，勿用其覆盖本地「是否设密」状态）。
     */
    public WalletBalanceResp toWalletBalanceResp() {
        WalletBalanceResp r = new WalletBalanceResp();
        r.status = 0;
        r.balance = balance;
        r.usdt_available = usdtAvailable;
        r.usdt_frozen = usdtFrozen > 0 ? usdtFrozen : null;
        r.usdt_balance = !Double.isNaN(usdtBalance) && usdtBalance >= 0 ? usdtBalance : null;
        return r;
    }

    @Override
    public String getDisplayContent() {
        return "";
    }

    protected WKWalletBalanceSyncContent(Parcel in) {
        super(in);
        walletSyncVersion = in.readInt();
        usdtAvailable = in.readDouble();
        usdtFrozen = in.readDouble();
        usdtBalance = in.readDouble();
        balance = in.readDouble();
        walletStatus = in.readInt();
        reason = in.readString();
        relatedId = in.readString();
        ts = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(walletSyncVersion);
        dest.writeDouble(usdtAvailable);
        dest.writeDouble(usdtFrozen);
        dest.writeDouble(usdtBalance);
        dest.writeDouble(balance);
        dest.writeInt(walletStatus);
        dest.writeString(reason);
        dest.writeString(relatedId);
        dest.writeLong(ts);
    }

    public static final Creator<WKWalletBalanceSyncContent> CREATOR = new Creator<WKWalletBalanceSyncContent>() {
        @Override
        public WKWalletBalanceSyncContent createFromParcel(Parcel in) {
            return new WKWalletBalanceSyncContent(in);
        }

        @Override
        public WKWalletBalanceSyncContent[] newArray(int size) {
            return new WKWalletBalanceSyncContent[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * 从消息的 content 字符串解析（SDK 未绑出 model 时的兜底）。
     */
    public static WKWalletBalanceSyncContent fromContentJson(String contentJson) {
        if (TextUtils.isEmpty(contentJson)) {
            return null;
        }
        try {
            JSONObject j = new JSONObject(contentJson.trim());
            int t = j.optInt("type", -1);
            if (t != WKContentType.WK_WALLET_BALANCE_SYNC && !j.has("usdt_available") && !j.has("balance")) {
                return null;
            }
            return (WKWalletBalanceSyncContent) new WKWalletBalanceSyncContent().decodeMsg(j);
        } catch (JSONException e) {
            return null;
        }
    }
}
