package com.chat.wallet.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.base.base.WKBaseActivity;
import com.chat.base.net.IRequestResultListener;
import com.chat.wallet.R;
import com.chat.wallet.api.WalletModel;
import com.chat.wallet.databinding.ActivityRechargeBinding;
import com.chat.wallet.entity.RechargeApplicationRecord;
import com.chat.wallet.entity.RechargeApplyResp;
import com.chat.wallet.entity.RechargeBlockCustomer;
import com.chat.wallet.entity.RechargeChannel;
import com.chat.wallet.util.RechargeApplyTrackingStore;
import com.chat.wallet.util.WalletChatRouter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class RechargeActivity extends WKBaseActivity<ActivityRechargeBinding> {

    private boolean lastChannelsLoadFailed;
    private String lastChannelsFailMsg;

    private int selectedWxIndex = 0;
    private int selectedUCoinIndex = 0;
    private final List<RechargeChannel> wxAliChannels = new ArrayList<>();
    private final List<RechargeChannel> uCoinChannels = new ArrayList<>();

    @FunctionalInterface
    private interface OnChannelIndexPicked {
        void onPicked(int index);
    }

    @Override
    protected ActivityRechargeBinding getViewBinding() {
        return ActivityRechargeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView t) {
        t.setText(R.string.wallet_recharge);
    }

    @Override
    protected void initData() {
        refreshTitleBadgeUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshTitleBadgeUi();
        refreshTrackedStatusFromServer();
    }

    @Override
    protected void rightLayoutClick() {
        if (!RechargeApplyTrackingStore.hasTrackedApplication()) {
            super.rightLayoutClick();
            return;
        }
        Intent i = new Intent(this, RechargeApplyDetailActivity.class);
        i.putExtra("application_no", RechargeApplyTrackingStore.getTrackedApplicationNo());
        startActivity(i);
    }

    private void refreshTitleBadgeUi() {
        TextView rightTv = findViewById(com.chat.base.R.id.titleRightTv);
        ImageView rightIv = findViewById(com.chat.base.R.id.titleRightIv);
        if (rightTv == null) {
            return;
        }
        if (!RechargeApplyTrackingStore.hasTrackedApplication()) {
            hideTitleRightView();
            return;
        }
        rightTv.setText(RechargeApplyTrackingStore.getTitleLabel(this));
        rightTv.setVisibility(View.VISIBLE);
        if (rightIv != null) {
            rightIv.setVisibility(View.GONE);
        }
        showTitleRightView();
    }

    private void refreshTrackedStatusFromServer() {
        if (!RechargeApplyTrackingStore.hasTrackedApplication()) {
            return;
        }
        String no = RechargeApplyTrackingStore.getTrackedApplicationNo();
        WalletModel.getInstance().fetchRechargeApplicationByApplicationNo(no, 5,
                new IRequestResultListener<RechargeApplicationRecord>() {
                    @Override
                    public void onSuccess(RechargeApplicationRecord r) {
                        RechargeApplyTrackingStore.updateCachedAuditStatus(r.resolveAuditStatus());
                        refreshTitleBadgeUi();
                    }

                    @Override
                    public void onFail(int c, String m) {
                        // 列表尚未包含该单或分页不一致时不提示，避免打扰
                    }
                });
    }

    @Override
    protected void initView() {
        showLoadingEmptyState();
        loadChannels();
    }

    @Override
    protected void initListener() {
        wkVBinding.rechargePageScroll.setOnTouchListener((v, event) -> {
            if (event.getAction() != MotionEvent.ACTION_DOWN) {
                return false;
            }
            if (!rawTouchInsideView(wkVBinding.amountEt, event) && !rawTouchInsideView(wkVBinding.uCoinAmountEt, event)) {
                hideRechargeKeyboard();
            }
            return false;
        });
        wkVBinding.rechargeRetryTv.setOnClickListener(v -> loadChannels());
        wkVBinding.wxMethodBar.setOnClickListener(v -> {
            if (wxAliChannels.size() > 1) {
                showRechargeChannelPicker(wxAliChannels, selectedWxIndex, index -> {
                    selectedWxIndex = index;
                    updateWxAliChannelSummary();
                });
            }
        });
        wkVBinding.uCoinMethodBar.setOnClickListener(v -> {
            if (uCoinChannels.size() > 1) {
                showRechargeChannelPicker(uCoinChannels, selectedUCoinIndex, index -> {
                    selectedUCoinIndex = index;
                    updateUCoinAddressDisplay();
                    updateUCoinMethodValue();
                });
            }
        });
        wkVBinding.rechargeBtn.setOnClickListener(v -> onConfirmRechargeClick());
        wkVBinding.uCoinAmountEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateUCoinEstimate();
            }
        });
    }

    private static boolean rawTouchInsideView(View target, MotionEvent event) {
        if (target == null || target.getVisibility() != View.VISIBLE) {
            return false;
        }
        Rect r = new Rect();
        if (!target.getGlobalVisibleRect(r)) {
            return false;
        }
        int x = (int) event.getRawX();
        int y = (int) event.getRawY();
        return r.contains(x, y);
    }

    private void hideRechargeKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return;
        }
        View focus = getCurrentFocus();
        if (focus != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
            focus.clearFocus();
        }
        wkVBinding.amountEt.clearFocus();
        wkVBinding.uCoinAmountEt.clearFocus();
    }

    /**
     * 底部统一「确认充值」：仅微信/支付宝、仅 U 币、或两者同时开启时，根据已填金额选择对应方式提交。
     */
    private void onConfirmRechargeClick() {
        boolean hasWx = !wxAliChannels.isEmpty();
        boolean hasU = !uCoinChannels.isEmpty();
        if (!hasWx && !hasU) {
            showToast(R.string.wallet_input_amount);
            return;
        }
        String wxAmountStr = wkVBinding.amountEt.getText().toString().trim();
        String uAmountStr = wkVBinding.uCoinAmountEt.getText().toString().trim();
        boolean wxFilled = !TextUtils.isEmpty(wxAmountStr);
        boolean uFilled = !TextUtils.isEmpty(uAmountStr);

        if (hasWx && !hasU) {
            if (!wxFilled) {
                showToast(R.string.wallet_input_amount);
                return;
            }
            submitIfValidAmount(wxAmountStr, currentWxAliChannelOrNull());
            return;
        }
        if (!hasWx && hasU) {
            if (!uFilled) {
                showToast(R.string.wallet_input_amount);
                return;
            }
            submitIfValidAmount(uAmountStr, currentUCoinChannelOrNull());
            return;
        }
        if (wxFilled && uFilled) {
            showToast(R.string.wallet_recharge_fill_one_amount);
            return;
        }
        if (wxFilled) {
            submitIfValidAmount(wxAmountStr, currentWxAliChannelOrNull());
        } else if (uFilled) {
            submitIfValidAmount(uAmountStr, currentUCoinChannelOrNull());
        } else {
            showToast(R.string.wallet_input_amount);
        }
    }

    private RechargeChannel currentWxAliChannelOrNull() {
        if (wxAliChannels.isEmpty()) {
            return null;
        }
        return wxAliChannels.size() == 1
                ? wxAliChannels.get(0)
                : wxAliChannels.get(selectedWxIndex);
    }

    private RechargeChannel currentUCoinChannelOrNull() {
        if (uCoinChannels.isEmpty()) {
            return null;
        }
        return uCoinChannels.size() == 1
                ? uCoinChannels.get(0)
                : uCoinChannels.get(selectedUCoinIndex);
    }

    private void submitIfValidAmount(String amountStr, RechargeChannel channel) {
        if (channel == null) {
            showToast(R.string.wallet_recharge_no_channel);
            return;
        }
        double a;
        try {
            a = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            showToast(R.string.wallet_input_amount);
            return;
        }
        if (a <= 0) {
            showToast(R.string.wallet_amount_count_positive);
            return;
        }
        submitWalletRechargeApply(a, channel);
    }

    private void submitWalletRechargeApply(double amount, RechargeChannel channel) {
        WalletModel.getInstance().rechargeApply(channel, amount,
                new IRequestResultListener<RechargeApplyResp>() {
                    @Override
                    public void onSuccess(RechargeApplyResp r) {
                        double credited = r != null ? r.getResolvedCreditedAmount() : Double.NaN;
                        if (!Double.isNaN(credited) && !Double.isInfinite(credited)) {
                            String amtStr = String.format(Locale.getDefault(), "%.2f", credited);
                            showToast(getString(R.string.wallet_recharge_success_with_amount, amtStr));
                        } else {
                            showToast(getString(R.string.wallet_recharge_success));
                        }
                        String appNo = r != null ? r.getResolvedApplicationNo() : null;
                        if (!TextUtils.isEmpty(appNo)) {
                            RechargeApplyTrackingStore.trackNewApplication(appNo);
                            refreshTitleBadgeUi();
                        }
                        wkVBinding.amountEt.setText("");
                        wkVBinding.uCoinAmountEt.setText("");
                        startActivity(new Intent(RechargeActivity.this, BuyUsdtOrderListActivity.class));
                        overridePendingTransition(R.anim.wallet_buy_usdt_open_enter, R.anim.wallet_buy_usdt_open_exit);
                    }

                    @Override
                    public void onFail(int c, String m) {
                        showToast(m != null ? m : getString(R.string.wallet_recharge_fail));
                    }
                });
    }

    private void showRechargeChannelPicker(List<RechargeChannel> channels, int selectedIndex, OnChannelIndexPicked onPicked) {
        if (channels == null || channels.size() <= 1) {
            return;
        }
        View content = getLayoutInflater().inflate(R.layout.dialog_recharge_channel_picker, null, false);
        RecyclerView rv = content.findViewById(R.id.pickerRv);
        TextView cancelTv = content.findViewById(R.id.pickerCancelTv);
        rv.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration decoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        decoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(this, R.drawable.recharge_picker_divider)));
        rv.addItemDecoration(decoration);

        BaseQuickAdapter<RechargeChannel, BaseViewHolder> adapter = new BaseQuickAdapter<RechargeChannel, BaseViewHolder>(R.layout.item_recharge_channel_sheet) {
            @Override
            protected void convert(@NotNull BaseViewHolder h, RechargeChannel item) {
                h.setText(R.id.channelNameTv, item.getDisplayName());
                String payType = item.getPayTypeName();
                h.setText(R.id.payTypeTv, payType);
                h.setGone(R.id.payTypeTv, payType.isEmpty());
                int pos = getItemPosition(item);
                ImageView checkIv = h.getView(R.id.checkIv);
                checkIv.setVisibility(pos == selectedIndex ? View.VISIBLE : View.GONE);
            }
        };
        adapter.setList(new ArrayList<>(channels));
        rv.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(content).create();
        cancelTv.setOnClickListener(v -> dialog.dismiss());
        adapter.setOnItemClickListener((a, view, position) -> {
            onPicked.onPicked(position);
            dialog.dismiss();
        });
        dialog.setOnShowListener(d -> styleRechargePickerWindow(dialog));
        dialog.show();
    }

    private void styleRechargePickerWindow(AlertDialog dialog) {
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

    private void applyMethodBarRipple(LinearLayout bar, boolean selectable) {
        if (selectable) {
            TypedValue tv = new TypedValue();
            if (getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true)) {
                bar.setBackgroundResource(tv.resourceId);
            }
        } else {
            bar.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void showLoadingEmptyState() {
        wkVBinding.rechargeEmptyState.setVisibility(View.VISIBLE);
        wkVBinding.rechargeEmptyMessageTv.setText(R.string.wallet_recharge_loading_config);
        wkVBinding.rechargeRetryTv.setVisibility(View.GONE);
        wkVBinding.blockWxAli.setVisibility(View.GONE);
        wkVBinding.blockUCoin.setVisibility(View.GONE);
        wkVBinding.rechargeBtn.setVisibility(View.GONE);
    }

    private void loadChannels() {
        showLoadingEmptyState();
        lastChannelsLoadFailed = false;
        lastChannelsFailMsg = null;
        WalletModel.getInstance().getRechargeChannels(new IRequestResultListener<List<RechargeChannel>>() {
            @Override
            public void onSuccess(List<RechargeChannel> channels) {
                wxAliChannels.clear();
                uCoinChannels.clear();
                if (channels != null) {
                    for (RechargeChannel c : channels) {
                        if (!c.isChannelEnabled()) {
                            continue;
                        }
                        if (c.getPayTypeInt() == 4) {
                            uCoinChannels.add(c);
                        } else {
                            wxAliChannels.add(c);
                        }
                    }
                }
                applyChannelDataToUi();
            }

            @Override
            public void onFail(int c, String m) {
                lastChannelsLoadFailed = true;
                lastChannelsFailMsg = m;
                showToast(m != null ? m : getString(R.string.wallet_load_fail));
                wxAliChannels.clear();
                uCoinChannels.clear();
                applyChannelDataToUi();
            }
        });
    }

    private void applyChannelDataToUi() {
        boolean hasWx = !wxAliChannels.isEmpty();
        boolean hasU = !uCoinChannels.isEmpty();

        wkVBinding.blockWxAli.setVisibility(hasWx ? View.VISIBLE : View.GONE);
        wkVBinding.blockUCoin.setVisibility(hasU ? View.VISIBLE : View.GONE);
        wkVBinding.rechargeBtn.setVisibility((hasWx || hasU) ? View.VISIBLE : View.GONE);

        if (hasWx) {
            selectedWxIndex = Math.min(selectedWxIndex, wxAliChannels.size() - 1);
            selectedWxIndex = Math.max(0, selectedWxIndex);
            wkVBinding.wxMethodCard.setVisibility(View.VISIBLE);
            boolean wxMulti = wxAliChannels.size() > 1;
            wkVBinding.wxMethodChevron.setVisibility(wxMulti ? View.VISIBLE : View.GONE);
            wkVBinding.wxMethodBar.setClickable(wxMulti);
            wkVBinding.wxMethodBar.setFocusable(wxMulti);
            applyMethodBarRipple(wkVBinding.wxMethodBar, wxMulti);
            updateWxAliChannelSummary();
        }

        if (hasU) {
            selectedUCoinIndex = Math.min(selectedUCoinIndex, uCoinChannels.size() - 1);
            selectedUCoinIndex = Math.max(0, selectedUCoinIndex);
            boolean uMulti = uCoinChannels.size() > 1;
            wkVBinding.uCoinMethodCard.setVisibility(uMulti ? View.VISIBLE : View.GONE);
            wkVBinding.uCoinMethodChevron.setVisibility(uMulti ? View.VISIBLE : View.GONE);
            wkVBinding.uCoinMethodBar.setClickable(uMulti);
            wkVBinding.uCoinMethodBar.setFocusable(uMulti);
            applyMethodBarRipple(wkVBinding.uCoinMethodBar, uMulti);
            updateUCoinAddressDisplay();
            updateUCoinMethodValue();
            updateUCoinEstimate();
        } else {
            wkVBinding.uCoinEstimateTv.setVisibility(View.GONE);
        }

        bindCustomerRow(firstCustomerInList(wxAliChannels), wkVBinding.customerRowA,
                wkVBinding.customerNameATv, wkVBinding.customerDescATv);
        bindCustomerRow(firstCustomerInList(uCoinChannels), wkVBinding.customerRowB,
                wkVBinding.customerNameBTv, wkVBinding.customerDescBTv);

        boolean hasAny = hasWx || hasU;
        if (hasAny) {
            wkVBinding.rechargeEmptyState.setVisibility(View.GONE);
        } else {
            wkVBinding.rechargeEmptyState.setVisibility(View.VISIBLE);
            wkVBinding.rechargeRetryTv.setVisibility(View.VISIBLE);
            if (lastChannelsLoadFailed) {
                String detail = !TextUtils.isEmpty(lastChannelsFailMsg)
                        ? lastChannelsFailMsg
                        : getString(R.string.wallet_load_fail);
                wkVBinding.rechargeEmptyMessageTv.setText(
                        detail + "\n\n" + getString(R.string.wallet_recharge_parse_or_network_fail));
            } else {
                wkVBinding.rechargeEmptyMessageTv.setText(R.string.wallet_recharge_no_channel);
            }
        }
    }

    private void updateWxAliChannelSummary() {
        if (wxAliChannels.isEmpty()) {
            return;
        }
        RechargeChannel ch = wxAliChannels.size() == 1
                ? wxAliChannels.get(0)
                : wxAliChannels.get(selectedWxIndex);
        wkVBinding.wxMethodValueTv.setText(formatChannelLine(ch));
    }

    private void updateUCoinMethodValue() {
        if (uCoinChannels.isEmpty()) {
            return;
        }
        RechargeChannel ch = uCoinChannels.size() == 1
                ? uCoinChannels.get(0)
                : uCoinChannels.get(selectedUCoinIndex);
        wkVBinding.uCoinMethodValueTv.setText(formatChannelLine(ch));
    }

    private static String formatChannelLine(RechargeChannel ch) {
        String display = ch.getDisplayName();
        String payType = ch.getPayTypeName();
        if (!payType.isEmpty()) {
            display += "（" + payType + "）";
        }
        return display;
    }

    private void updateUCoinAddressDisplay() {
        if (uCoinChannels.isEmpty()) {
            return;
        }
        RechargeChannel ch = uCoinChannels.size() == 1
                ? uCoinChannels.get(0)
                : uCoinChannels.get(selectedUCoinIndex);
        bindUcoinAddressLabel(ch);
        String addr = ch.getDepositAddressForDisplay();
        if (TextUtils.isEmpty(addr)) {
            addr = ch.getDisplayName();
        }
        wkVBinding.uCoinAddressTv.setText(addr);
        updateUCoinEstimate();
    }

    private void bindUcoinAddressLabel(RechargeChannel ch) {
        String rate = ch != null ? ch.getUcoinRateDisplayForLabel() : null;
        if (TextUtils.isEmpty(rate)) {
            rate = getString(R.string.wallet_ucoin_rate_unknown);
        }
        wkVBinding.uCoinAddressLabelTv.setText(getString(R.string.wallet_ucoin_address_label_fmt, rate));
    }

    /**
     * U 币金额 × 渠道汇率（pay_type=4 时 install_key）的预估到账展示。
     */
    private void updateUCoinEstimate() {
        if (wkVBinding.blockUCoin.getVisibility() != View.VISIBLE || uCoinChannels.isEmpty()) {
            wkVBinding.uCoinEstimateTv.setVisibility(View.GONE);
            return;
        }
        RechargeChannel ch = currentUCoinChannelOrNull();
        if (ch == null) {
            wkVBinding.uCoinEstimateTv.setVisibility(View.GONE);
            return;
        }
        double rate = ch.getUcoinRateMultiplier();
        if (Double.isNaN(rate) || rate <= 0) {
            wkVBinding.uCoinEstimateTv.setVisibility(View.GONE);
            return;
        }
        String raw = wkVBinding.uCoinAmountEt.getText().toString().trim();
        if (TextUtils.isEmpty(raw)) {
            wkVBinding.uCoinEstimateTv.setVisibility(View.GONE);
            return;
        }
        double uAmt;
        try {
            uAmt = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            wkVBinding.uCoinEstimateTv.setVisibility(View.GONE);
            return;
        }
        if (uAmt <= 0) {
            wkVBinding.uCoinEstimateTv.setVisibility(View.GONE);
            return;
        }
        double est = uAmt * rate;
        String estStr = String.format(Locale.getDefault(), "%.2f", est);
        wkVBinding.uCoinEstimateTv.setText(getString(R.string.wallet_ucoin_estimate_fmt, estStr));
        wkVBinding.uCoinEstimateTv.setVisibility(View.VISIBLE);
    }

    private static RechargeBlockCustomer firstCustomerInList(List<RechargeChannel> list) {
        if (list == null) {
            return null;
        }
        for (RechargeChannel c : list) {
            RechargeBlockCustomer b = c.toBlockCustomerOrNull();
            if (b != null) {
                return b;
            }
        }
        return null;
    }

    private void bindCustomerRow(RechargeBlockCustomer c, View row, TextView nameTv, TextView descTv) {
        if (c == null || !c.hasUid()) {
            row.setVisibility(View.GONE);
            row.setOnClickListener(null);
            return;
        }
        row.setVisibility(View.VISIBLE);
        String name = !TextUtils.isEmpty(c.name) ? c.name : getString(R.string.wallet_customer_service);
        nameTv.setText(name);
        if (!TextUtils.isEmpty(c.description)) {
            descTv.setText(c.description);
            descTv.setVisibility(View.VISIBLE);
        } else {
            descTv.setVisibility(View.GONE);
        }
        row.setOnClickListener(v -> WalletChatRouter.openP2PChat(RechargeActivity.this, c.uid));
    }
}
