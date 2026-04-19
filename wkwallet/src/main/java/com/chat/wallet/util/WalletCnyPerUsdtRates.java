package com.chat.wallet.util;

import androidx.annotation.Nullable;

import com.chat.wallet.entity.RechargeChannel;

import java.util.ArrayList;
import java.util.List;

/**
 * 与 {@code GET /v1/wallet/recharge/channels} 及快捷买币页一致：解析「每 1 USDT 折合人民币（元）」，
 * 用于钱包首页 {@code CNY / 汇率 → USDT} 展示。
 */
public final class WalletCnyPerUsdtRates {

    private WalletCnyPerUsdtRates() {
    }

    /**
     * 将启用渠道划为微信/支付宝等 CNY 渠道与 U 盾（pay_type=4）；规则与 {@link com.chat.wallet.ui.BuyUsdtActivity} 一致。
     */
    public static void partitionEnabledChannels(@Nullable List<RechargeChannel> src,
                                                List<RechargeChannel> outCny,
                                                List<RechargeChannel> outUCoin) {
        outCny.clear();
        outUCoin.clear();
        if (src == null) {
            return;
        }
        for (RechargeChannel ch : src) {
            if (ch == null || !ch.isChannelEnabled()) {
                continue;
            }
            int pt = ch.getPayTypeInt();
            if (pt == 4) {
                outUCoin.add(ch);
            } else if (pt == 2 || pt == 3) {
                outCny.add(ch);
            }
        }
        if (outCny.isEmpty()) {
            for (RechargeChannel ch : src) {
                if (ch == null || !ch.isChannelEnabled()) {
                    continue;
                }
                if (ch.getPayTypeInt() != 4) {
                    outCny.add(ch);
                }
            }
        }
    }

    /**
     * 快捷买币页 / 钱包首页共用：优先当前选中的 CNY 渠道，再 U 盾，再其余 CNY 渠道的 {@link RechargeChannel#getCnyPerUsdtForBuyPageOrNaN()}。
     *
     * @param selectedCnyIndex 钱包首页传 0 即可；买币页为当前选中支付方式下标
     */
    public static double resolveCnyPerUsdt(List<RechargeChannel> cnyChannels,
                                           List<RechargeChannel> uCoinChannels,
                                           int selectedCnyIndex) {
        RechargeChannel sel = null;
        if (!cnyChannels.isEmpty()) {
            int i = Math.min(Math.max(selectedCnyIndex, 0), cnyChannels.size() - 1);
            sel = cnyChannels.get(i);
        }
        if (sel != null) {
            double r = sel.getCnyPerUsdtForBuyPageOrNaN();
            if (!Double.isNaN(r) && r > 0) {
                return r;
            }
        }
        for (RechargeChannel ch : uCoinChannels) {
            if (ch == null) {
                continue;
            }
            double r = ch.getCnyPerUsdtForBuyPageOrNaN();
            if (!Double.isNaN(r) && r > 0) {
                return r;
            }
        }
        for (RechargeChannel ch : cnyChannels) {
            if (ch == null || ch == sel) {
                continue;
            }
            double r = ch.getCnyPerUsdtForBuyPageOrNaN();
            if (!Double.isNaN(r) && r > 0) {
                return r;
            }
        }
        return Double.NaN;
    }

    /**
     * 从原始渠道列表直接得到「元/USDT」；内部会先 {@link #partitionEnabledChannels}。
     */
    public static double resolveCnyPerUsdtFromRawList(@Nullable List<RechargeChannel> list, int selectedCnyIndex) {
        List<RechargeChannel> cny = new ArrayList<>();
        List<RechargeChannel> ucoin = new ArrayList<>();
        partitionEnabledChannels(list, cny, ucoin);
        return resolveCnyPerUsdt(cny, ucoin, selectedCnyIndex);
    }
}
