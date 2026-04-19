package com.chat.wallet.util;

import com.chat.wallet.entity.WalletBalanceResp;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 钱包余额 IM 推送（1020）或 HTTP 校验回调后，通知已打开的界面刷新。
 */
public final class WalletBalanceSyncNotifier {

    public interface Listener {
        void onWalletBalanceUpdated(WalletBalanceResp resp);
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private WalletBalanceSyncNotifier() {
    }

    public static void register(Listener l) {
        if (l != null && !LISTENERS.contains(l)) {
            LISTENERS.add(l);
        }
    }

    public static void unregister(Listener l) {
        LISTENERS.remove(l);
    }

    public static void dispatch(WalletBalanceResp resp) {
        if (resp == null) {
            return;
        }
        for (Listener l : LISTENERS) {
            l.onWalletBalanceUpdated(resp);
        }
    }
}
