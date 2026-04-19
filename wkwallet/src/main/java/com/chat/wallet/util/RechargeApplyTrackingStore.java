package com.chat.wallet.util;

import android.content.Context;
import android.text.TextUtils;

import com.chat.base.config.WKConfig;
import com.chat.base.config.WKSharedPreferencesUtil;
import com.chat.wallet.R;

/**
 * 当前用户最近一次充值申请单号及缓存的审核状态（用于充值页标题角标）。
 */
public final class RechargeApplyTrackingStore {

    private static final String K_NO = "wallet_recharge_track_application_no";
    private static final String K_STATUS = "wallet_recharge_track_audit_status";

    private RechargeApplyTrackingStore() {
    }

    public static void trackNewApplication(String applicationNo) {
        if (TextUtils.isEmpty(applicationNo)) {
            return;
        }
        WKSharedPreferencesUtil sp = WKSharedPreferencesUtil.getInstance();
        sp.putSPWithUID(K_NO, applicationNo);
        sp.putIntWithUID(K_STATUS, 0);
    }

    public static String getTrackedApplicationNo() {
        return WKSharedPreferencesUtil.getInstance().getSPWithUID(K_NO);
    }

    public static boolean hasTrackedApplication() {
        return !TextUtils.isEmpty(getTrackedApplicationNo());
    }

    public static void updateCachedAuditStatus(int auditStatus) {
        WKSharedPreferencesUtil.getInstance().putIntWithUID(K_STATUS, auditStatus);
    }

    public static int getCachedAuditStatus() {
        String uid = WKConfig.getInstance().getUid();
        if (TextUtils.isEmpty(uid)) {
            return -1;
        }
        return WKSharedPreferencesUtil.getInstance().getInt(uid + "_" + K_STATUS, -1);
    }

    /**
     * 标题角标文案：与详情页状态一致。
     */
    public static String getTitleLabel(Context ctx) {
        int s = getCachedAuditStatus();
        if (s == 1) {
            return ctx.getString(R.string.wallet_withdrawal_approved);
        }
        if (s == 2) {
            return ctx.getString(R.string.wallet_withdrawal_rejected);
        }
        return ctx.getString(R.string.wallet_withdrawal_pending);
    }
}
