package com.chat.wallet.receive;

import android.net.Uri;
import android.text.TextUtils;

/**
 * 钱包「收款码」二维码载荷：同平台用户扫码后进入转账页向对方付款。
 * <p>
 * 格式：{@code mtp://wallet/receive?uid=<收款方UID>}（与加好友等其它 mtp 路径区分）。
 * </p>
 */
public final class WalletReceiveQrContract {

    public static final String SCHEME = "mtp";
    public static final String HOST = "wallet";
    public static final String SEGMENT_RECEIVE = "receive";
    public static final String QUERY_UID = "uid";

    private WalletReceiveQrContract() {
    }

    public static String buildReceiveUri(String uid) {
        if (TextUtils.isEmpty(uid)) {
            return "";
        }
        return new Uri.Builder()
                .scheme(SCHEME)
                .authority(HOST)
                .appendPath(SEGMENT_RECEIVE)
                .appendQueryParameter(QUERY_UID, uid.trim())
                .build()
                .toString();
    }

    /**
     * @return 收款方 uid；无法解析时返回 null
     */
    public static String parseRecipientUid(String scanned) {
        if (TextUtils.isEmpty(scanned)) {
            return null;
        }
        Uri u = Uri.parse(scanned.trim());
        if (!SCHEME.equalsIgnoreCase(u.getScheme())) {
            return null;
        }
        if (!HOST.equalsIgnoreCase(u.getHost())) {
            return null;
        }
        if (!pathEndsWithReceive(u.getPath())) {
            return null;
        }
        String uid = u.getQueryParameter(QUERY_UID);
        if (TextUtils.isEmpty(uid)) {
            uid = u.getQueryParameter("to_uid");
        }
        if (TextUtils.isEmpty(uid)) {
            return null;
        }
        return uid.trim();
    }

    private static boolean pathEndsWithReceive(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        String p = path;
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        while (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return SEGMENT_RECEIVE.equalsIgnoreCase(p) || p.toLowerCase().endsWith("/" + SEGMENT_RECEIVE);
    }
}
