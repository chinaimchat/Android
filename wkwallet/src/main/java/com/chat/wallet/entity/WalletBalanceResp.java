package com.chat.wallet.entity;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * {@code GET /v1/wallet/balance}。
 * <p>与「提币先冻结」对齐时，建议服务端返回：{@code usdt_available}（可再发起提币的 USDT，已不含冻结中额度）；
 * 可选 {@code usdt_frozen} 仅用于展示。若只下发 {@code usdt_balance}，则应表示与可用口径一致（冻结已从可用中扣除）。</p>
 */
public class WalletBalanceResp {
    public int status;
    public double balance;
    public boolean has_password;

    /** 链上 USDT 可用额（兼容旧接口；语义上宜为「可提/可用」，不含审核中冻结）。 */
    @JSONField(name = "usdt_balance")
    public Double usdt_balance;

    /** 明确可用 USDT（优先于 {@link #usdt_balance}）。 */
    @JSONField(name = "usdt_available", alternateNames = {"available_usdt"})
    public Double usdt_available;

    /** 审核中/冻结中的 USDT 提币额度，可选，用于展示。 */
    @JSONField(name = "usdt_frozen", alternateNames = {"frozen_usdt", "pending_withdraw_usdt"})
    public Double usdt_frozen;

    public double getAvailableUsdtOrFallback() {
        if (usdt_available != null && usdt_available >= 0 && !usdt_available.isNaN()) {
            return usdt_available;
        }
        if (usdt_balance != null && usdt_balance >= 0 && !usdt_balance.isNaN()) {
            return usdt_balance;
        }
        return balance;
    }

    /** 有冻结字段且大于 0 时用于 UI 提示。 */
    public double getFrozenUsdtOrZero() {
        if (usdt_frozen == null || usdt_frozen.isNaN() || usdt_frozen <= 0) {
            return 0;
        }
        return usdt_frozen;
    }
}
