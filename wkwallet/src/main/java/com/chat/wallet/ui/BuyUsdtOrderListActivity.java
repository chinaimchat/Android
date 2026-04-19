package com.chat.wallet.ui;

import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.base.base.WKBaseActivity;
import com.chat.base.net.IRequestResultListener;
import com.chat.base.ui.Theme;
import com.chat.base.utils.WKToastUtils;
import com.chat.base.utils.systembar.WKStatusBarUtils;
import com.chat.wallet.R;
import com.chat.wallet.api.WalletModel;
import com.chat.wallet.databinding.ActivityBuyUsdtOrderListBinding;
import com.chat.wallet.entity.RechargeApplicationRecord;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 买币 USDT 充值申请订单列表（Material 卡片列表，日夜间随 {@link R.style#Theme_Wallet_BuyUsdt} 资源）。
 */
public class BuyUsdtOrderListActivity extends WKBaseActivity<ActivityBuyUsdtOrderListBinding> {

    private BaseQuickAdapter<RechargeApplicationRecord, BaseViewHolder> adapter;

    @Override
    protected ActivityBuyUsdtOrderListBinding getViewBinding() {
        return ActivityBuyUsdtOrderListBinding.inflate(getLayoutInflater());
    }

    @Override
    protected boolean supportSlideBack() {
        return false;
    }

    @Override
    protected void toggleStatusBarMode() {
        Window window = getWindow();
        if (window == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }
        int bar = ContextCompat.getColor(this, R.color.buy_usdt_appbar_bg);
        WKStatusBarUtils.setStatusBarColor(window, bar, 0);
        int nav = ContextCompat.getColor(this, R.color.buy_usdt_page_bg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setNavigationBarColor(nav);
        }
        boolean darkTheme = Theme.getDarkModeStatus(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = window.getDecorView();
            int vis = decor.getSystemUiVisibility();
            if (!darkTheme) {
                vis |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                vis &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decor.setSystemUiVisibility(vis);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.wallet_buy_usdt_close_enter, R.anim.wallet_buy_usdt_close_exit);
    }

    @Override
    protected void initView() {
        int tint = ContextCompat.getColor(this, R.color.buy_usdt_text_primary);
        wkVBinding.orderListToolbar.setTitleTextColor(tint);
        wkVBinding.orderListToolbar.setNavigationOnClickListener(v -> finish());
        if (wkVBinding.orderListToolbar.getNavigationIcon() != null) {
            DrawableCompat.setTint(Objects.requireNonNull(wkVBinding.orderListToolbar.getNavigationIcon().mutate()), tint);
        }

        wkVBinding.orderRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        adapter = new BaseQuickAdapter<RechargeApplicationRecord, BaseViewHolder>(R.layout.item_buy_usdt_order) {
            @Override
            protected void convert(@NotNull BaseViewHolder h, RechargeApplicationRecord item) {
                BuyUsdtOrderListActivity.this.bindOrderItem(h, item);
            }
        };
        wkVBinding.orderRecyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener((a, view, position) -> {
            RechargeApplicationRecord r = adapter.getItem(position);
            if (r == null) {
                return;
            }
            String no = r.getApplicationNo();
            if (no.isEmpty()) {
                return;
            }
            Intent i = new Intent(this, RechargeApplyDetailActivity.class);
            i.putExtra("application_no", no);
            startActivity(i);
        });

        wkVBinding.orderSwipeRefresh.setColorSchemeResources(R.color.buy_usdt_primary);
        wkVBinding.orderSwipeRefresh.setOnRefreshListener(this::loadOrders);
    }

    @Override
    protected void initData() {
        loadOrders();
    }

    private void loadOrders() {
        wkVBinding.orderSwipeRefresh.setRefreshing(true);
        WalletModel.getInstance().getRechargeApplicationsList(1, 50,
                new IRequestResultListener<List<RechargeApplicationRecord>>() {
                    @Override
                    public void onSuccess(List<RechargeApplicationRecord> list) {
                        wkVBinding.orderSwipeRefresh.setRefreshing(false);
                        List<RechargeApplicationRecord> data = list != null ? list : new ArrayList<>();
                        adapter.setList(data);
                        boolean empty = data.isEmpty();
                        wkVBinding.orderEmptyTv.setVisibility(empty ? View.VISIBLE : View.GONE);
                        wkVBinding.orderRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void onFail(int c, String m) {
                        wkVBinding.orderSwipeRefresh.setRefreshing(false);
                        WKToastUtils.getInstance().showToastNormal(
                                m != null ? m : getString(R.string.wallet_load_fail));
                        if (adapter.getData().isEmpty()) {
                            wkVBinding.orderEmptyTv.setVisibility(View.VISIBLE);
                            wkVBinding.orderRecyclerView.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void bindOrderItem(@NotNull BaseViewHolder h, RechargeApplicationRecord r) {
        h.setText(R.id.orderCardTimeTv, WalletModel.formatRechargeApplicationTimeForDisplay(r.createdAt));
        h.setText(R.id.orderCardQtyTv, formatQty(r));
        h.setText(R.id.orderCardTotalCnyTv, formatCnyTotal(r));

        TextView statusTv = h.getView(R.id.orderCardStatusTv);
        int processing = ContextCompat.getColor(this, R.color.buy_usdt_order_processing);
        int ok = 0xFF4CAF50;
        int rej = ContextCompat.getColor(this, R.color.buy_usdt_error);
        int def = ContextCompat.getColor(this, R.color.buy_usdt_text_secondary);
        switch (r.resolveAuditStatus()) {
            case 0:
                statusTv.setText(R.string.buy_usdt_order_status_processing);
                statusTv.setTextColor(processing);
                break;
            case 1:
                statusTv.setText(R.string.wallet_withdrawal_approved);
                statusTv.setTextColor(ok);
                break;
            case 2:
                statusTv.setText(R.string.wallet_withdrawal_rejected);
                statusTv.setTextColor(rej);
                break;
            default:
                statusTv.setText(String.valueOf(r.resolveAuditStatus()));
                statusTv.setTextColor(def);
                break;
        }
    }

    private static String formatQty(RechargeApplicationRecord r) {
        if (r == null) {
            return "—";
        }
        if (r.amountU != null && !r.amountU.isNaN() && r.amountU > 0) {
            return String.format(Locale.getDefault(), "%.4f", r.amountU);
        }
        return "—";
    }

    private static String formatCnyTotal(RechargeApplicationRecord r) {
        if (r == null || r.amount == null || r.amount.isNaN()) {
            return "—";
        }
        double v = r.amount;
        if (v == Math.floor(v)) {
            return String.format(Locale.US, "%d", (long) v);
        }
        return String.format(Locale.getDefault(), "%.2f", v);
    }
}
