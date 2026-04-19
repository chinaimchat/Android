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
import com.chat.wallet.databinding.ActivityWithdrawalOrderListBinding;
import com.chat.wallet.entity.WithdrawalListItem;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 用户提币申请订单列表（与 {@link BuyUsdtOrderListActivity} 同款 Material 列表）。
 */
public class WithdrawalOrderListActivity extends WKBaseActivity<ActivityWithdrawalOrderListBinding> {

    private BaseQuickAdapter<WithdrawalListItem, BaseViewHolder> adapter;

    @Override
    protected ActivityWithdrawalOrderListBinding getViewBinding() {
        return ActivityWithdrawalOrderListBinding.inflate(getLayoutInflater());
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
        adapter = new BaseQuickAdapter<WithdrawalListItem, BaseViewHolder>(R.layout.item_withdrawal_order) {
            @Override
            protected void convert(@NotNull BaseViewHolder h, WithdrawalListItem item) {
                WithdrawalOrderListActivity.this.bindOrderItem(h, item);
            }
        };
        wkVBinding.orderRecyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener((a, view, position) -> {
            WithdrawalListItem r = adapter.getItem(position);
            if (r == null) {
                return;
            }
            String no = r.withdrawalNo;
            if (no == null || no.isEmpty()) {
                return;
            }
            Intent i = new Intent(this, WithdrawalDetailActivity.class);
            i.putExtra("withdrawal_no", no);
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
        WalletModel.getInstance().getWithdrawalList(1, 50, new IRequestResultListener<List<WithdrawalListItem>>() {
            @Override
            public void onSuccess(List<WithdrawalListItem> list) {
                wkVBinding.orderSwipeRefresh.setRefreshing(false);
                List<WithdrawalListItem> data = list != null ? list : new ArrayList<>();
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

    private void bindOrderItem(@NotNull BaseViewHolder h, WithdrawalListItem r) {
        h.setText(R.id.orderCardTimeTv, WalletModel.formatRechargeApplicationTimeForDisplay(r.createdAt));
        h.setText(R.id.orderCardQtyTv, formatAmount(r.amount));
        h.setText(R.id.orderCardTotalCnyTv, formatFee(r.fee));
        h.setText(R.id.orderCardActualTv, formatAmount(r.actual_amount));

        TextView statusTv = h.getView(R.id.orderCardStatusTv);
        int processing = ContextCompat.getColor(this, R.color.buy_usdt_order_processing);
        int ok = 0xFF4CAF50;
        int rej = ContextCompat.getColor(this, R.color.buy_usdt_error);
        int def = ContextCompat.getColor(this, R.color.buy_usdt_text_secondary);

        switch (r.resolveAuditStatus()) {
            case 0:
                statusTv.setText(R.string.wallet_withdrawal_pending);
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
                if (r.statusText != null && !r.statusText.isEmpty()) {
                    statusTv.setText(r.statusText);
                } else {
                    statusTv.setText(String.valueOf(r.resolveAuditStatus()));
                }
                statusTv.setTextColor(def);
                break;
        }
    }

    private static String formatAmount(Double v) {
        if (v == null || v.isNaN() || v < 0) {
            return "—";
        }
        return String.format(Locale.US, "%.4f", v);
    }

    private static String formatFee(Double v) {
        if (v == null || v.isNaN() || v < 0) {
            return "—";
        }
        if (v == 0) {
            return "0";
        }
        if (v == Math.floor(v)) {
            return String.format(Locale.US, "%d", (long) v.doubleValue());
        }
        return String.format(Locale.US, "%.4f", v);
    }
}
