package com.chat.wallet.util;

import androidx.annotation.Nullable;

import com.chat.wallet.entity.RechargeChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 充值渠道列表内存缓存：配合 {@link com.chat.wallet.api.WalletModel#getRechargeChannels} 做 stale-while-revalidate。
 * <p>正常退出与 HTTP 401 时由 {@code wk_wallet_logout_cleanup} endpoint 调用 {@link #clear()}；
 * 其它场景可手动 {@link #clear()}。</p>
 */
public final class RechargeChannelsCache {

    private static final Object LOCK = new Object();
    private static volatile List<RechargeChannel> snapshot;
    private static volatile boolean hasSnapshot;

    private RechargeChannelsCache() {}

    /** 是否已有至少一次成功写入（含空列表）。 */
    public static boolean hasSnapshot() {
        return hasSnapshot;
    }

    /**
     * 返回快照副本；从未成功拉取过时为 {@code null}（与空列表区分：空列表也会返回 new ArrayList）。
     */
    @Nullable
    public static List<RechargeChannel> getCopy() {
        if (!hasSnapshot) {
            return null;
        }
        synchronized (LOCK) {
            return new ArrayList<>(snapshot);
        }
    }

    public static void put(@Nullable List<RechargeChannel> fresh) {
        List<RechargeChannel> copy = fresh != null ? new ArrayList<>(fresh) : new ArrayList<>();
        synchronized (LOCK) {
            snapshot = copy;
            hasSnapshot = true;
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            snapshot = null;
            hasSnapshot = false;
        }
    }

    /**
     * 将当前快照与即将写入的列表做业务字段比对（忽略列表顺序）。
     *
     * @return {@code true} 表示无快照或与 {@code fresh} 内容不一致
     */
    public static boolean differsFromSnapshot(@Nullable List<RechargeChannel> fresh) {
        List<RechargeChannel> f = fresh != null ? fresh : new ArrayList<>();
        synchronized (LOCK) {
            if (!hasSnapshot) {
                return true;
            }
            return !contentEquals(snapshot, f);
        }
    }

    static boolean contentEquals(@Nullable List<RechargeChannel> a, @Nullable List<RechargeChannel> b) {
        List<RechargeChannel> la = a != null ? a : new ArrayList<>();
        List<RechargeChannel> lb = b != null ? b : new ArrayList<>();
        if (la.size() != lb.size()) {
            return false;
        }
        return listFingerprint(la).equals(listFingerprint(lb));
    }

    private static String listFingerprint(List<RechargeChannel> list) {
        List<RechargeChannel> sorted = new ArrayList<>(list);
        Collections.sort(sorted, Comparator.comparingLong(c -> c != null ? c.id : 0L));
        StringBuilder all = new StringBuilder();
        for (RechargeChannel c : sorted) {
            all.append(channelFingerprint(c)).append('\n');
        }
        return all.toString();
    }

    private static String channelFingerprint(@Nullable RechargeChannel c) {
        if (c == null) {
            return "";
        }
        String payTypeStr = c.pay_type != null ? String.valueOf(c.pay_type) : "";
        String wf = c.withdraw_fee != null ? String.valueOf(c.withdraw_fee) : "";
        return c.id + "\u001f" + c.status + "\u001f" + c.min_amount + "\u001f" + c.max_amount + "\u001f"
                + c.getPayTypeInt() + "\u001f" + payTypeStr + "\u001f"
                + nz(c.getDepositAddressForDisplay()) + "\u001f" + nz(c.getDepositQrImageUrlOrEmpty()) + "\u001f"
                + nz(c.install_key) + "\u001f" + nz(c.exchange_rate) + "\u001f" + nz(c.icon) + "\u001f"
                + nz(c.getDisplayName()) + "\u001f" + nz(c.customer_service_uid) + "\u001f" + nz(c.customer_service_name)
                + "\u001f" + nz(c.customer_service_desc) + "\u001f" + wf + "\u001f" + nz(c.type) + "\u001f"
                + nz(c.remark) + "\u001f" + nz(c.pay_address) + "\u001f" + nz(c.recharge_deposit_address) + "\u001f"
                + nz(c.description) + "\u001f" + nz(c.title) + "\u001f" + nz(c.name) + "\u001f" + nz(c.channel_name)
                + "\u001f" + nz(c.pay_type_name) + "\u001f" + nz(c.qr_code);
    }

    private static String nz(@Nullable String s) {
        return s == null ? "" : s;
    }
}
