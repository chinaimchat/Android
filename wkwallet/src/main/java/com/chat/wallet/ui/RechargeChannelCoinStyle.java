package com.chat.wallet.ui;

import com.chat.wallet.entity.RechargeChannel;

import java.util.Locale;

/**
 * 无网络图标时，根据渠道展示名推断列表项圆形底色与字母（与行情列表风格一致）。
 */
final class RechargeChannelCoinStyle {

    final int circleColorArgb;
    final String letter;

    private RechargeChannelCoinStyle(int rgbNoAlpha, String letter) {
        this.circleColorArgb = 0xFF000000 | (rgbNoAlpha & 0xFFFFFF);
        this.letter = letter;
    }

    static RechargeChannelCoinStyle fromChannel(RechargeChannel ch) {
        if (ch == null) {
            return fromLabel("");
        }
        if (ch.getPayTypeInt() == 2) {
            return new RechargeChannelCoinStyle(0x1677FF, "支");
        }
        if (ch.getPayTypeInt() == 3) {
            return new RechargeChannelCoinStyle(0x07C160, "微");
        }
        return fromLabel(ch.getDisplayName());
    }

    static RechargeChannelCoinStyle fromLabel(String raw) {
        if (raw == null) {
            raw = "";
        }
        if (raw.contains("支付宝")) {
            return new RechargeChannelCoinStyle(0x1677FF, "支");
        }
        if (raw.contains("微信")) {
            return new RechargeChannelCoinStyle(0x07C160, "微");
        }
        String u = raw.toUpperCase(Locale.US);
        if (u.contains("USDT") || u.contains("TETHER")) {
            return new RechargeChannelCoinStyle(0x26A17B, "₮");
        }
        if (u.contains("BTC") || u.contains("BITCOIN")) {
            return new RechargeChannelCoinStyle(0xF7931A, "₿");
        }
        if (u.contains("ETH") || u.contains("ETHEREUM")) {
            return new RechargeChannelCoinStyle(0x627EEA, "Ξ");
        }
        if (u.contains("BNB") || u.contains("BINANCE")) {
            return new RechargeChannelCoinStyle(0xF3BA2F, "B");
        }
        if (u.contains("DOGE")) {
            return new RechargeChannelCoinStyle(0xC2A633, "Ð");
        }
        if (u.contains("LTC") || u.contains("LITECOIN")) {
            return new RechargeChannelCoinStyle(0x345D9D, "Ł");
        }
        if (u.contains("TRX") || u.contains("TRON")) {
            return new RechargeChannelCoinStyle(0xEB0029, "T");
        }
        String letter = "?";
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (!Character.isWhitespace(c)) {
                letter = String.valueOf(c).toUpperCase(Locale.getDefault());
                break;
            }
        }
        return new RechargeChannelCoinStyle(0x757575, letter);
    }
}
