package com.chat.wallet.ui;

import com.chat.wallet.R;
import com.chat.wallet.entity.RechargeChannel;

/**
 * 微信 / 支付宝等内置支付渠道：无后台 icon 时使用本地品牌色矢量图。
 */
final class RechargePayBrandIcons {

    private RechargePayBrandIcons() {
    }

    /**
     * @return drawable 资源 id，非品牌渠道返回 0
     */
    static int localBrandDrawableRes(RechargeChannel ch) {
        if (ch == null) {
            return 0;
        }
        if (ch.getPayTypeInt() == 2) {
            return R.drawable.ic_recharge_pay_alipay;
        }
        if (ch.getPayTypeInt() == 3) {
            return R.drawable.ic_recharge_pay_wechat;
        }
        String n = ch.getDisplayName();
        if (n == null) {
            return 0;
        }
        if (n.contains("支付宝")) {
            return R.drawable.ic_recharge_pay_alipay;
        }
        if (n.contains("微信")) {
            return R.drawable.ic_recharge_pay_wechat;
        }
        return 0;
    }
}
