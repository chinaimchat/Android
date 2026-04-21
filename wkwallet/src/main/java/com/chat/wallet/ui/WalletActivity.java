package com.chat.wallet.ui;

import android.content.Intent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.net.IRequestResultListener;
import com.chat.base.utils.systembar.WKStatusBarUtils;
import com.chat.wallet.R;
import com.chat.wallet.receive.WalletReceiveQrActivity;
import com.chat.wallet.api.WalletModel;
import com.chat.wallet.databinding.ActivityWalletBinding;
import com.chat.wallet.entity.RechargeChannel;
import com.chat.wallet.entity.WalletBalanceResp;
import com.chat.wallet.util.WalletBalanceSyncNotifier;
import com.chat.wallet.util.WalletCnyPerUsdtRates;
import com.chat.wallet.util.WalletChatRouter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WalletActivity extends WKBaseActivity<ActivityWalletBinding> {
    private boolean hasPassword = false;
    /** 展示为人民币（与接口 balance 一致） */
    private double cnyBalance = 0;
    /**
     * 每 1 USDT 折合多少元人民币，来自 {@code GET /v1/wallet/recharge/channels}（与快捷买币页一致）。
     * 首页 USDT 展示为 {@code cnyBalance / cnyPerUsdtRate}。
     */
    private double cnyPerUsdtRate = Double.NaN;
    private boolean balanceVisible = true;
    /** false：¥ + CNY；true：USDT 数量 + USDT */
    private boolean balanceShowUsdt = false;
    private WalletMarketAdapter marketAdapter;

    private final WalletBalanceSyncNotifier.Listener walletBalanceSyncListener = resp -> {
        if (resp == null) {
            return;
        }
        applyCnyBalanceFromResp(resp);
        applyBalanceText();
    };

    @Override
    protected ActivityWalletBinding getViewBinding() {
        return ActivityWalletBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView t) {
        // 无系统标题栏，渐变区内自建 Toolbar
    }

    @Override
    protected void toggleStatusBarMode() {
        Window window = getWindow();
        if (window == null) {
            return;
        }
        WKStatusBarUtils.transparentStatusBar(window);
        WKStatusBarUtils.setLightMode(window);
    }

    @Override
    protected void initView() {
        RecyclerView rv = wkVBinding.marketRecyclerView;
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        marketAdapter = new WalletMarketAdapter();
        rv.setAdapter(marketAdapter);
        marketAdapter.setList(buildDemoMarket());
    }

    private static List<WalletMarketAdapter.Item> buildDemoMarket() {
        List<WalletMarketAdapter.Item> list = new ArrayList<>();
        int usdt = 0xFF26A17B;
        int bnb = 0xFFF3BA2F;
        int btc = 0xFFF7931A;
        int eth = 0xFF627EEA;
        list.add(new WalletMarketAdapter.Item("USDT-TRC20", "$1", 0.02, usdt, "₮"));
        list.add(new WalletMarketAdapter.Item("USDT-ERC20", "$1", -0.06, usdt, "₮"));
        list.add(new WalletMarketAdapter.Item("USDT-BSC", "$1", -1.92, usdt, "₮"));
        list.add(new WalletMarketAdapter.Item("BNB", "$624.92", 0.35, bnb, "B"));
        list.add(new WalletMarketAdapter.Item("BTC", "$68287.19", -0.12, btc, "₿"));
        list.add(new WalletMarketAdapter.Item("ETH", "$2043.41", 0.08, eth, "Ξ"));
        return list;
    }

    @Override
    protected void initListener() {
        wkVBinding.walletBackBtn.setOnClickListener(v -> finish());

        wkVBinding.walletSettingsBtn.setOnClickListener(this::showWalletMenu);

        wkVBinding.balanceEyeBtn.setOnClickListener(v -> toggleBalanceVisibility());

        wkVBinding.balanceAmountClickArea.setOnClickListener(v -> toggleBalanceCurrency());
        wkVBinding.balanceAmountClickArea.setContentDescription(getString(R.string.wallet_balance_switch_currency_cd));

        wkVBinding.quickScanLayout.setOnClickListener(v ->
                EndpointManager.getInstance().invoke("wk_scan_show", null));

        wkVBinding.quickReceiveLayout.setOnClickListener(v -> {
            startActivity(new Intent(this, WalletReceiveQrActivity.class));
            overridePendingTransition(R.anim.wallet_buy_usdt_open_enter, R.anim.wallet_buy_usdt_open_exit);
        });

        wkVBinding.quickRechargeLayout.setOnClickListener(v ->
                startActivity(new Intent(this, RechargeActivity.class)));

        wkVBinding.quickWithdrawLayout.setOnClickListener(v ->
                startActivity(new Intent(this, WithdrawActivity.class)));

        wkVBinding.quickBuyBanner.setOnClickListener(v -> onQuickBuyBannerClick());

        wkVBinding.marketMoreTv.setOnClickListener(v ->
                showToast(R.string.wallet_market_more_toast));
    }

    /**
     * 「快捷购买 USDT」横幅：暂改为打开官方客服会话（与 {@link BuyUsdtActivity} 底部「联系客服」相同）。
     * 若要恢复进买币页，将方法体改为调用 {@link #openBuyUsdtQuickEntry()}。
     */
    private void onQuickBuyBannerClick() {
        WalletChatRouter.openOfficialCustomerService((AppCompatActivity) this);
    }

    /**
     * 保留：原横幅进入快捷买 USDT 全屏页（当前点击未使用）。
     */
    @SuppressWarnings("unused")
    private void openBuyUsdtQuickEntry() {
        startActivity(new Intent(this, BuyUsdtActivity.class));
        overridePendingTransition(R.anim.wallet_buy_usdt_open_enter, R.anim.wallet_buy_usdt_open_exit);
    }

    private void toggleBalanceVisibility() {
        balanceVisible = !balanceVisible;
        ImageButton eye = wkVBinding.balanceEyeBtn;
        eye.setImageResource(balanceVisible
                ? R.drawable.ic_wallet_visibility_white
                : R.drawable.ic_wallet_visibility_off_white);
        applyBalanceText();
    }

    private void toggleBalanceCurrency() {
        if (!balanceVisible) {
            return;
        }
        balanceShowUsdt = !balanceShowUsdt;
        applyBalanceText();
    }

    private void applyCnyBalanceFromResp(WalletBalanceResp r) {
        cnyBalance = r.balance;
    }

    /** 人民币余额 ÷ 渠道汇率（元/USDT）；无有效汇率时为 NaN。 */
    private double computeUsdtEquivalentFromCny() {
        if (!Double.isNaN(cnyPerUsdtRate) && cnyPerUsdtRate > 0 && !Double.isInfinite(cnyPerUsdtRate)
                && cnyBalance >= 0 && !Double.isNaN(cnyBalance)) {
            return cnyBalance / cnyPerUsdtRate;
        }
        return Double.NaN;
    }

    private void applyBalanceText() {
        wkVBinding.balanceCurrencyTv.setVisibility(balanceVisible ? View.VISIBLE : View.GONE);
        if (balanceVisible) {
            if (balanceShowUsdt) {
                double u = computeUsdtEquivalentFromCny();
                if (Double.isNaN(u) || Double.isInfinite(u)) {
                    wkVBinding.balanceTv.setText(R.string.wallet_balance_usdt_unavailable);
                } else {
                    wkVBinding.balanceTv.setText(formatUsdtMainAmount(u));
                }
                wkVBinding.balanceCurrencyTv.setText(R.string.wallet_currency_usdt);
            } else {
                wkVBinding.balanceTv.setText(String.format(Locale.getDefault(), "¥%.2f", cnyBalance));
                wkVBinding.balanceCurrencyTv.setText(R.string.wallet_currency_unit);
            }
        } else {
            wkVBinding.balanceTv.setText("******");
        }
    }

    /** 首页 USDT 展示：最多 6 位小数，去掉末尾无意义的 0 */
    private static String formatUsdtMainAmount(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return "0";
        }
        String s = String.format(Locale.US, "%.6f", v);
        int dot = s.indexOf('.');
        if (dot < 0) {
            return s;
        }
        int end = s.length();
        while (end > dot + 1 && s.charAt(end - 1) == '0') {
            end--;
        }
        if (end > dot && s.charAt(end - 1) == '.') {
            end--;
        }
        return s.substring(0, end);
    }

    private void showWalletMenu(View anchor) {
        PopupMenu pm = new PopupMenu(this, anchor);
        pm.getMenu().add(0, 1, 0, R.string.wallet_transaction_record);
        if (hasPassword) {
            pm.getMenu().add(0, 2, 0, R.string.wallet_change_pay_password);
        } else {
            pm.getMenu().add(0, 3, 0, R.string.wallet_set_pay_password);
        }
        pm.getMenu().add(0, 4, 0, R.string.wallet_customer_service);
        pm.setOnMenuItemClickListener(this::onWalletMenuItem);
        pm.show();
    }

    private boolean onWalletMenuItem(MenuItem item) {
        int id = item.getItemId();
        if (id == 1) {
            startActivity(new Intent(this, TransactionRecordActivity.class));
            return true;
        }
        if (id == 2) {
            startActivity(new Intent(this, SetPayPasswordActivity.class).putExtra("mode", "change"));
            return true;
        }
        if (id == 3) {
            startActivity(new Intent(this, SetPayPasswordActivity.class).putExtra("mode", "set"));
            return true;
        }
        if (id == 4) {
            startActivity(new Intent(this, CustomerServiceActivity.class));
            return true;
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        WalletBalanceSyncNotifier.register(walletBalanceSyncListener);
    }

    @Override
    protected void onStop() {
        WalletBalanceSyncNotifier.unregister(walletBalanceSyncListener);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBalance();
        loadCnyPerUsdtRateFromChannels();
    }

    private void loadCnyPerUsdtRateFromChannels() {
        WalletModel.getInstance().getRechargeChannels(new IRequestResultListener<List<RechargeChannel>>() {
            @Override
            public void onSuccess(List<RechargeChannel> list) {
                cnyPerUsdtRate = WalletCnyPerUsdtRates.resolveCnyPerUsdtFromRawList(list, 0);
                applyBalanceText();
            }

            @Override
            public void onFail(int c, String m) {
                cnyPerUsdtRate = Double.NaN;
                applyBalanceText();
            }
        });
    }

    private void loadBalance() {
        WalletModel.getInstance().getBalance(new IRequestResultListener<WalletBalanceResp>() {
            @Override
            public void onSuccess(WalletBalanceResp r) {
                applyCnyBalanceFromResp(r);
                hasPassword = r.has_password;
                applyBalanceText();
            }

            @Override
            public void onFail(int c, String m) {
            }
        });
    }
}
