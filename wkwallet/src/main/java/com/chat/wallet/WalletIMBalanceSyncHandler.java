package com.chat.wallet;

import android.text.TextUtils;

import com.chat.base.msgitem.WKContentType;
import com.chat.base.utils.AndroidUtilities;
import com.chat.wallet.api.WalletModel;
import com.chat.wallet.entity.WalletBalanceResp;
import com.chat.wallet.msg.WKWalletBalanceSyncContent;
import com.chat.wallet.util.WalletBalanceSyncNotifier;
import com.chat.base.net.IRequestResultListener;
import com.xinbida.wukongim.entity.WKMsg;

/**
 * 处理 IM 下发的钱包余额同步（type=1020）：先应用推送，再静默拉取 HTTP 校验。
 */
public final class WalletIMBalanceSyncHandler {

    private WalletIMBalanceSyncHandler() {
    }

    public static void handle(WKMsg msg) {
        if (msg == null || msg.type != WKContentType.WK_WALLET_BALANCE_SYNC) {
            return;
        }
        WalletBalanceResp fromPush = extractResp(msg);
        if (fromPush != null) {
            AndroidUtilities.runOnUIThread(() -> WalletBalanceSyncNotifier.dispatch(fromPush));
        }
        WalletModel.getInstance().getBalance(new IRequestResultListener<WalletBalanceResp>() {
            @Override
            public void onSuccess(WalletBalanceResp r) {
                AndroidUtilities.runOnUIThread(() -> WalletBalanceSyncNotifier.dispatch(r));
            }

            @Override
            public void onFail(int c, String m) {
            }
        });
    }

    private static WalletBalanceResp extractResp(WKMsg msg) {
        if (msg.baseContentMsgModel instanceof WKWalletBalanceSyncContent) {
            return ((WKWalletBalanceSyncContent) msg.baseContentMsgModel).toWalletBalanceResp();
        }
        WKWalletBalanceSyncContent c = WKWalletBalanceSyncContent.fromContentJson(msg.content);
        if (c != null) {
            return c.toWalletBalanceResp();
        }
        return null;
    }
}
