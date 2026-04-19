package com.chat.wallet.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
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
import com.chat.wallet.databinding.ActivityBuyUsdtBinding;
import com.chat.wallet.entity.RechargeApplyResp;
import com.chat.wallet.entity.RechargeChannel;
import com.chat.wallet.util.RechargeApplyTrackingStore;
import com.chat.wallet.util.WalletChatRouter;
import com.chat.wallet.util.WalletCnyPerUsdtRates;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 快捷购买 USDT：Material 全屏页，日夜间资源分离；进入/退出带动画（由 {@link WalletActivity} 与本页 {@link #finish()} 衔接）。
 */
public class BuyUsdtActivity extends WKBaseActivity<ActivityBuyUsdtBinding> {

    private static final int[] PRESET_AMOUNTS = {
            300, 400, 500,
            600, 700, 800,
            900, 1000, 2000
    };

    private final List<RechargeChannel> cnyChannels = new ArrayList<>();
    private final List<RechargeChannel> uCoinChannels = new ArrayList<>();
    private int selectedCnyIndex;
    private int selectedPresetAmount = -1;
    @Nullable
    private TextView selectedPresetView;
    /** 九宫格写入「可填金额」时避免 TextWatcher 误清选中态 */
    private boolean syncingGridAmountToInput;

    @Override
    protected ActivityBuyUsdtBinding getViewBinding() {
        return ActivityBuyUsdtBinding.inflate(getLayoutInflater());
    }

    /** 与 Coordinator + 侧滑返回容器叠加时部分机型易异常，本页关闭侧滑。 */
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
        applyToolbarChrome();
        wkVBinding.buyUsdtToolbar.inflateMenu(R.menu.menu_buy_usdt);
        wkVBinding.buyUsdtToolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.buy_usdt_menu_orders) {
                openOrdersFromToolbar();
                return true;
            }
            return false;
        });
        wkVBinding.buyUsdtToolbar.setNavigationOnClickListener(v -> finish());
        tintToolbarNavIcon();

        wireAmountGrid();
        wkVBinding.buyUsdtCustomAmountEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!syncingGridAmountToInput) {
                    clearPresetSelectionUi();
                }
                refreshAmountUi();
            }
        });

        wkVBinding.buyUsdtMethodRow.setOnClickListener(v -> {
            if (cnyChannels.size() > 1) {
                showCnyChannelPicker();
            }
        });

        wkVBinding.buyUsdtConfirmBtn.setOnClickListener(v -> submitRecharge());
        wkVBinding.buyUsdtContactCsBtn.setOnClickListener(v -> onContactCustomerService());

        applyBuyUsdtFaqBodyStyled();
    }

    /** 常见问题第 1、5 条整行红色（文案行首为「1、」「5、」） */
    private void applyBuyUsdtFaqBodyStyled() {
        String full = getString(R.string.buy_usdt_faq_body);
        SpannableString ss = new SpannableString(full);
        int red = ContextCompat.getColor(this, R.color.buy_usdt_error);
        int pos = 0;
        while (pos < full.length()) {
            int nextNl = full.indexOf('\n', pos);
            int lineEnd = nextNl == -1 ? full.length() : nextNl;
            String line = full.substring(pos, lineEnd);
            if (line.startsWith("1、") || line.startsWith("5、")) {
                ss.setSpan(new ForegroundColorSpan(red), pos, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (nextNl == -1) {
                break;
            }
            pos = nextNl + 1;
        }
        wkVBinding.buyUsdtFaqBodyTv.setText(ss);
    }

    @Override
    protected void initData() {
        loadChannels();
    }

    private void applyToolbarChrome() {
        int tint = ContextCompat.getColor(this, R.color.buy_usdt_text_primary);
        wkVBinding.buyUsdtToolbar.setTitleTextColor(tint);
    }

    private void tintToolbarNavIcon() {
        if (wkVBinding.buyUsdtToolbar.getNavigationIcon() == null) {
            return;
        }
        int tint = ContextCompat.getColor(this, R.color.buy_usdt_text_primary);
        DrawableCompat.setTint(Objects.requireNonNull(wkVBinding.buyUsdtToolbar.getNavigationIcon().mutate()), tint);
    }

    private void openOrdersFromToolbar() {
        startActivity(new Intent(this, BuyUsdtOrderListActivity.class));
        overridePendingTransition(R.anim.wallet_buy_usdt_open_enter, R.anim.wallet_buy_usdt_open_exit);
    }

    private void wireAmountGrid() {
        ViewGroup root = wkVBinding.buyUsdtAmountGrid;
        if (root.getChildCount() != 3) {
            return;
        }
        int idx = 0;
        for (int r = 0; r < 3; r++) {
            View row = root.getChildAt(r);
            if (!(row instanceof ViewGroup)) {
                return;
            }
            ViewGroup rowLayout = (ViewGroup) row;
            if (rowLayout.getChildCount() != 3) {
                return;
            }
            for (int c = 0; c < 3; c++) {
                TextView tv = (TextView) rowLayout.getChildAt(c);
                int amount = PRESET_AMOUNTS[idx++];
                tv.setText(String.format(Locale.getDefault(), "¥%d", amount));
                tv.setOnClickListener(v -> onPresetCellClicked(amount, tv));
            }
        }
    }

    private void onPresetCellClicked(int amount, TextView tv) {
        if (selectedPresetView != null) {
            selectedPresetView.setSelected(false);
        }
        selectedPresetView = tv;
        tv.setSelected(true);
        selectedPresetAmount = amount;

        syncingGridAmountToInput = true;
        try {
            String digits = String.format(Locale.US, "%d", amount);
            wkVBinding.buyUsdtCustomAmountEt.setText(digits);
            Editable ed = wkVBinding.buyUsdtCustomAmountEt.getText();
            if (ed != null) {
                wkVBinding.buyUsdtCustomAmountEt.setSelection(ed.length());
            }
        } finally {
            syncingGridAmountToInput = false;
        }
        refreshAmountUi();
    }

    private void clearPresetSelectionUi() {
        selectedPresetAmount = -1;
        if (selectedPresetView != null) {
            selectedPresetView.setSelected(false);
            selectedPresetView = null;
        }
    }

    private void loadChannels() {
        WalletModel.getInstance().getRechargeChannels(new IRequestResultListener<List<RechargeChannel>>() {
            @Override
            public void onSuccess(List<RechargeChannel> list) {
                partitionChannels(list);
                selectedCnyIndex = Math.min(selectedCnyIndex, Math.max(0, cnyChannels.size() - 1));
                applyChannelUi();
                refreshAmountUi();
            }

            @Override
            public void onFail(int c, String m) {
                cnyChannels.clear();
                uCoinChannels.clear();
                applyChannelUi();
                WKToastUtils.getInstance().showToastNormal(m != null ? m : getString(R.string.wallet_load_fail));
            }
        });
    }

    private void partitionChannels(@Nullable List<RechargeChannel> list) {
        WalletCnyPerUsdtRates.partitionEnabledChannels(list, cnyChannels, uCoinChannels);
    }

    private void applyChannelUi() {
        boolean hasCny = !cnyChannels.isEmpty();
        wkVBinding.buyUsdtMethodRow.setClickable(hasCny && cnyChannels.size() > 1);
        wkVBinding.buyUsdtMethodRow.setFocusable(hasCny && cnyChannels.size() > 1);
        wkVBinding.buyUsdtMethodChevron.setVisibility(hasCny && cnyChannels.size() > 1 ? View.VISIBLE : View.GONE);

        if (!hasCny) {
            wkVBinding.buyUsdtMethodNameTv.setText(R.string.buy_usdt_no_pay_channel);
            wkVBinding.buyUsdtMethodIcon.setImageDrawable(null);
            wkVBinding.buyUsdtConfirmBtn.setEnabled(false);
            wkVBinding.buyUsdtCurrentRateTv.setVisibility(View.GONE);
            return;
        }

        RechargeChannel ch = cnyChannels.get(Math.min(selectedCnyIndex, cnyChannels.size() - 1));
        wkVBinding.buyUsdtMethodNameTv.setText(formatChannelLine(ch));
        applyPayIcon(wkVBinding.buyUsdtMethodIcon, ch.getPayTypeInt());
    }

    /**
     * 后台充值渠道配置的「人民币/1USDT」：优先当前选中的微信/支付宝渠道，再 U 盾，再其它渠道的 exchange_rate。
     */
    private double resolveCnyPerUsdtRate() {
        return WalletCnyPerUsdtRates.resolveCnyPerUsdt(cnyChannels, uCoinChannels, selectedCnyIndex);
    }

    @Nullable
    private RechargeChannel currentCnyChannelOrNull() {
        if (cnyChannels.isEmpty()) {
            return null;
        }
        return cnyChannels.get(Math.min(selectedCnyIndex, cnyChannels.size() - 1));
    }

    private static void applyPayIcon(ImageView iv, int payType) {
        if (payType == 2) {
            iv.setImageResource(R.drawable.ic_recharge_pay_alipay);
        } else if (payType == 3) {
            iv.setImageResource(R.drawable.ic_recharge_pay_wechat);
        } else {
            iv.setImageDrawable(null);
        }
    }

    private static String formatChannelLine(RechargeChannel ch) {
        String display = ch.getDisplayName();
        String payType = ch.getPayTypeName();
        if (!payType.isEmpty()) {
            display += "（" + payType + "）";
        }
        return display;
    }

    private void showCnyChannelPicker() {
        if (cnyChannels.size() <= 1) {
            return;
        }
        View content = getLayoutInflater().inflate(R.layout.dialog_recharge_channel_picker, null, false);
        RecyclerView rv = content.findViewById(R.id.pickerRv);
        TextView cancelTv = content.findViewById(R.id.pickerCancelTv);
        rv.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration decoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        decoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(this, R.drawable.recharge_picker_divider)));
        rv.addItemDecoration(decoration);

        int sel = Math.min(selectedCnyIndex, cnyChannels.size() - 1);
        BaseQuickAdapter<RechargeChannel, BaseViewHolder> adapter =
                new BaseQuickAdapter<RechargeChannel, BaseViewHolder>(R.layout.item_recharge_channel_sheet) {
                    @Override
                    protected void convert(@NotNull BaseViewHolder h, RechargeChannel item) {
                        h.setText(R.id.channelNameTv, item.getDisplayName());
                        String payType = item.getPayTypeName();
                        h.setText(R.id.payTypeTv, payType);
                        h.setGone(R.id.payTypeTv, payType.isEmpty());
                        int pos = getItemPosition(item);
                        ImageView checkIv = h.getView(R.id.checkIv);
                        checkIv.setVisibility(pos == sel ? View.VISIBLE : View.GONE);
                    }
                };
        adapter.setList(new ArrayList<>(cnyChannels));
        rv.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(content).create();
        cancelTv.setOnClickListener(v -> dialog.dismiss());
        adapter.setOnItemClickListener((a, view, position) -> {
            selectedCnyIndex = position;
            applyChannelUi();
            refreshAmountUi();
            dialog.dismiss();
        });
        dialog.setOnShowListener(d -> stylePickerWindow(dialog));
        dialog.show();
    }

    private void stylePickerWindow(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setGravity(Gravity.BOTTOM);
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.dimAmount = 0.5f;
        window.setAttributes(lp);
    }

    private void refreshAmountUi() {
        RechargeChannel ch = currentCnyChannelOrNull();
        updateAmountRangeHint(ch);
        updateEstimateUsdt();
        updateCurrentRateLine();
        updateConfirmEnabled();
    }

    private void updateAmountRangeHint(@Nullable RechargeChannel ch) {
        if (ch == null) {
            wkVBinding.buyUsdtAmountRangeTv.setVisibility(View.GONE);
            return;
        }
        boolean hasMin = ch.min_amount > 0;
        boolean hasMax = ch.max_amount > 0;
        if (!hasMin && !hasMax) {
            wkVBinding.buyUsdtAmountRangeTv.setVisibility(View.GONE);
            return;
        }
        wkVBinding.buyUsdtAmountRangeTv.setVisibility(View.VISIBLE);
        String minStr = hasMin ? formatChannelAmountHint(ch.min_amount) : "";
        String maxStr = hasMax ? formatChannelAmountHint(ch.max_amount) : "";
        if (hasMin && hasMax) {
            wkVBinding.buyUsdtAmountRangeTv.setText(
                    getString(R.string.recharge_sheet_amount_range_min_max, minStr, maxStr));
        } else if (hasMin) {
            wkVBinding.buyUsdtAmountRangeTv.setText(getString(R.string.recharge_sheet_amount_range_min, minStr));
        } else {
            wkVBinding.buyUsdtAmountRangeTv.setText(getString(R.string.recharge_sheet_amount_range_max, maxStr));
        }
    }

    private static String formatChannelAmountHint(double v) {
        if (v == Math.floor(v)) {
            return String.format(Locale.US, "%d", (long) v);
        }
        return stripTrailingZeros(v);
    }

    private static String stripTrailingZeros(double v) {
        String s = String.format(Locale.US, "%.8f", v);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }

    private void updateEstimateUsdt() {
        double rate = resolveCnyPerUsdtRate();
        double cny = resolveCurrentAmountCny();
        if (Double.isNaN(rate) || rate <= 0 || Double.isNaN(cny) || cny <= 0) {
            wkVBinding.buyUsdtEstimateTv.setVisibility(View.GONE);
            return;
        }
        double usdt = cny / rate;
        if (Double.isNaN(usdt) || Double.isInfinite(usdt)) {
            wkVBinding.buyUsdtEstimateTv.setVisibility(View.GONE);
            return;
        }
        wkVBinding.buyUsdtEstimateTv.setText(getString(R.string.buy_usdt_estimated_usdt, formatUsdt(usdt)));
        wkVBinding.buyUsdtEstimateTv.setVisibility(View.VISIBLE);
    }

    /** 与 {@link #resolveCnyPerUsdtRate()} 一致：每 1 USDT 折合人民币（元），展示在「预计到账」下方 */
    private void updateCurrentRateLine() {
        double rate = resolveCnyPerUsdtRate();
        if (Double.isNaN(rate) || rate <= 0) {
            wkVBinding.buyUsdtCurrentRateTv.setVisibility(View.GONE);
            return;
        }
        wkVBinding.buyUsdtCurrentRateTv.setText(
                getString(R.string.buy_usdt_current_rate_fmt, stripTrailingZeros(rate)));
        wkVBinding.buyUsdtCurrentRateTv.setVisibility(View.VISIBLE);
    }

    private static String formatUsdt(double v) {
        return String.format(Locale.getDefault(), "%.4f", v);
    }

    /**
     * 当前 CNY 金额：以输入框为准（与九宫格同步写入）；输入为空时再读快捷档位（若未清空选中）。
     */
    private double resolveCurrentAmountCny() {
        String raw = wkVBinding.buyUsdtCustomAmountEt.getText() != null
                ? wkVBinding.buyUsdtCustomAmountEt.getText().toString().trim() : "";
        if (!TextUtils.isEmpty(raw)) {
            try {
                return Double.parseDouble(raw);
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
        if (selectedPresetAmount > 0) {
            return selectedPresetAmount;
        }
        return Double.NaN;
    }

    private void updateConfirmEnabled() {
        RechargeChannel ch = currentCnyChannelOrNull();
        if (ch == null) {
            wkVBinding.buyUsdtConfirmBtn.setEnabled(false);
            return;
        }
        double amt = resolveCurrentAmountCny();
        if (Double.isNaN(amt) || amt <= 0) {
            wkVBinding.buyUsdtConfirmBtn.setEnabled(false);
            return;
        }
        if (ch.min_amount > 0 && amt < ch.min_amount - 1e-9) {
            wkVBinding.buyUsdtConfirmBtn.setEnabled(false);
            return;
        }
        if (ch.max_amount > 0 && amt > ch.max_amount + 1e-9) {
            wkVBinding.buyUsdtConfirmBtn.setEnabled(false);
            return;
        }
        wkVBinding.buyUsdtConfirmBtn.setEnabled(true);
    }

    private void submitRecharge() {
        RechargeChannel ch = currentCnyChannelOrNull();
        if (ch == null) {
            WKToastUtils.getInstance().showToastNormal(getString(R.string.buy_usdt_no_pay_channel));
            return;
        }
        double amount = resolveCurrentAmountCny();
        if (Double.isNaN(amount) || amount <= 0) {
            WKToastUtils.getInstance().showToastNormal(getString(R.string.wallet_input_amount));
            return;
        }
        if (ch.min_amount > 0 && amount < ch.min_amount - 1e-9) {
            WKToastUtils.getInstance().showToastNormal(getString(R.string.recharge_sheet_amount_below_min));
            return;
        }
        if (ch.max_amount > 0 && amount > ch.max_amount + 1e-9) {
            WKToastUtils.getInstance().showToastNormal(getString(R.string.recharge_sheet_amount_above_max));
            return;
        }
        WalletModel.getInstance().rechargeApply(ch, amount, new IRequestResultListener<RechargeApplyResp>() {
            @Override
            public void onSuccess(RechargeApplyResp r) {
                double credited = r != null ? r.getResolvedCreditedAmount() : Double.NaN;
                if (!Double.isNaN(credited) && !Double.isInfinite(credited)) {
                    String amtStr = String.format(Locale.getDefault(), "%.2f", credited);
                    WKToastUtils.getInstance().showToastNormal(
                            getString(R.string.wallet_recharge_success_with_amount, amtStr));
                } else {
                    WKToastUtils.getInstance().showToastNormal(getString(R.string.wallet_recharge_success));
                }
                String appNo = r != null ? r.getResolvedApplicationNo() : null;
                if (!TextUtils.isEmpty(appNo)) {
                    RechargeApplyTrackingStore.trackNewApplication(appNo);
                }
                wkVBinding.buyUsdtCustomAmountEt.setText("");
                clearPresetSelectionUi();
                refreshAmountUi();
                startActivity(new Intent(BuyUsdtActivity.this, BuyUsdtOrderListActivity.class));
                overridePendingTransition(R.anim.wallet_buy_usdt_open_enter, R.anim.wallet_buy_usdt_open_exit);
            }

            @Override
            public void onFail(int c, String m) {
                WKToastUtils.getInstance().showToastNormal(
                        m != null ? m : getString(R.string.wallet_recharge_fail));
            }
        });
    }

    private void onContactCustomerService() {
        WalletChatRouter.openOfficialCustomerService(this);
    }
}
