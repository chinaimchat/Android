package com.chat.wallet.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.bumptech.glide.Glide;
import com.chat.base.base.WKBaseActivity;
import com.chat.base.config.WKApiConfig;
import com.chat.base.net.IRequestResultListener;
import com.chat.wallet.entity.WithdrawApplyResp;
import com.chat.base.ui.Theme;
import com.chat.base.utils.systembar.WKStatusBarUtils;
import com.chat.scan.WKScanActivity;
import com.chat.wallet.R;
import com.chat.wallet.api.WalletModel;
import com.chat.wallet.databinding.ActivityWithdrawBinding;
import com.chat.wallet.entity.RechargeChannel;
import com.chat.wallet.entity.WalletBalanceResp;
import com.chat.wallet.entity.WithdrawalFeeConfig;
import com.chat.wallet.entity.WithdrawalFeePreview;
import com.chat.wallet.util.WalletBalanceSyncNotifier;
import com.chat.wallet.util.WalletChatRouter;
import com.chat.wallet.util.WalletPayPasswordHelper;
import com.chat.wallet.widget.PayPasswordDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 链上 USDT 提币：网络选择、地址（粘贴/扫码）、数量与手续费/到账预览，提交走 {@link WalletModel#withdraw}。
 */
public class WithdrawActivity extends WKBaseActivity<ActivityWithdrawBinding> {

    private static final double DEFAULT_MIN_WITHDRAW = WithdrawalFeeConfig.DEFAULT_MIN_WITHDRAW;
    private static final double DEFAULT_SERVICE_FEE = WithdrawalFeeConfig.DEFAULT_SERVICE_FEE;

    private final List<RechargeChannel> usdtChains = new ArrayList<>();
    private RechargeChannel selectedChain;
    private double availableUsdt = 0;
    /** {@code GET /v1/wallet/withdrawal/fee-config} 成功且与 {@link #feeConfigLoadedForChannelId} 一致时优先于渠道本地字段 */
    @Nullable
    private WithdrawalFeeConfig feeConfigFromApi;
    private long feeConfigLoadedForChannelId;
    private long feeConfigRequestSeq;

    private final Handler feePreviewHandler = new Handler(Looper.getMainLooper());
    private final Runnable feePreviewRunnable = this::runFeePreviewRequest;
    @Nullable
    private String pendingFeePreviewAmountRaw;
    private static final long FEE_PREVIEW_DEBOUNCE_MS = 350;

    private long feePreviewRequestSeq;
    @Nullable
    private WithdrawalFeePreview lastFeePreview;
    @Nullable
    private String lastFeePreviewAmountKey;
    private long lastFeePreviewChannelId;

    private final WalletBalanceSyncNotifier.Listener walletBalanceSyncListener = resp -> {
        if (resp == null) {
            return;
        }
        availableUsdt = resp.getAvailableUsdtOrFallback();
        wkVBinding.balanceTv.setText(getString(R.string.wallet_withdraw_available_fmt,
                String.format(Locale.US, "%.6f", availableUsdt)));
        refreshArrivalAndButton();
    };

    private final ActivityResultLauncher<Intent> scanLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                String s = result.getData().getStringExtra(WKScanActivity.EXTRA_SCAN_RESULT);
                if (!TextUtils.isEmpty(s)) {
                    wkVBinding.addressEt.setText(s.trim());
                }
            });

    @Override
    protected ActivityWithdrawBinding getViewBinding() {
        return ActivityWithdrawBinding.inflate(getLayoutInflater());
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
    protected void setTitle(TextView t) {
    }

    @Override
    protected void initView() {
        int tint = ContextCompat.getColor(this, R.color.buy_usdt_text_primary);
        wkVBinding.withdrawToolbar.setTitleTextColor(tint);
        wkVBinding.withdrawToolbar.setNavigationOnClickListener(v -> finish());
        Drawable nav = wkVBinding.withdrawToolbar.getNavigationIcon();
        if (nav != null) {
            DrawableCompat.setTint(Objects.requireNonNull(nav.mutate()), tint);
        }
        wkVBinding.withdrawToolbar.inflateMenu(R.menu.menu_withdraw);
        wkVBinding.withdrawToolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.withdraw_menu_orders) {
                startActivity(new Intent(this, WithdrawalOrderListActivity.class));
                overridePendingTransition(R.anim.wallet_buy_usdt_open_enter, R.anim.wallet_buy_usdt_open_exit);
                return true;
            }
            return false;
        });

        wkVBinding.withdrawChainRow.setOnClickListener(v -> showChainPicker());
        wkVBinding.pasteTv.setOnClickListener(v -> pasteFromClipboard());
        wkVBinding.scanAddressBtn.setOnClickListener(v -> openScanForAddress());
        wkVBinding.withdrawAllTv.setOnClickListener(v -> fillAllAmount());
        wkVBinding.feeHelpTv.setOnClickListener(v -> showFeeTip());
        wkVBinding.withdrawConfirmBtn.setOnClickListener(v -> onConfirmWithdraw());
        wkVBinding.withdrawContactCsBtn.setOnClickListener(v -> onContactCustomerService());

        TextWatcher refreshWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                refreshArrivalAndButton();
            }
        };
        wkVBinding.addressEt.addTextChangedListener(refreshWatcher);
        wkVBinding.amountEt.addTextChangedListener(refreshWatcher);
    }

    @Override
    protected void initListener() {
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            maybeHideKeyboardOnOutsideTouch(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void maybeHideKeyboardOnOutsideTouch(MotionEvent ev) {
        View focus = getCurrentFocus();
        if (!(focus instanceof EditText)) {
            return;
        }
        Rect rect = new Rect();
        focus.getGlobalVisibleRect(rect);
        if (rect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
        focus.clearFocus();
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
        loadUsdtChannels();
    }

    private void loadBalance() {
        WalletModel.getInstance().getBalance(new IRequestResultListener<WalletBalanceResp>() {
            @Override
            public void onSuccess(WalletBalanceResp r) {
                availableUsdt = r.getAvailableUsdtOrFallback();
                wkVBinding.balanceTv.setText(getString(R.string.wallet_withdraw_available_fmt,
                        String.format(Locale.US, "%.6f", availableUsdt)));
                refreshArrivalAndButton();
            }

            @Override
            public void onFail(int c, String m) {
            }
        });
    }

    private void loadUsdtChannels() {
        WalletModel.getInstance().getRechargeChannels(new IRequestResultListener<List<RechargeChannel>>() {
            @Override
            public void onSuccess(List<RechargeChannel> list) {
                usdtChains.clear();
                if (list != null) {
                    for (RechargeChannel c : list) {
                        if (c.getPayTypeInt() == 4 && c.isChannelEnabled()) {
                            usdtChains.add(c);
                        }
                    }
                }
                long prevId = selectedChain != null ? selectedChain.id : 0;
                selectedChain = null;
                if (prevId > 0) {
                    for (RechargeChannel c : usdtChains) {
                        if (c.id == prevId) {
                            selectedChain = c;
                            break;
                        }
                    }
                }
                if (selectedChain == null && !usdtChains.isEmpty()) {
                    selectedChain = pickDefaultTrc(usdtChains);
                }
                applyChainUi();
                refreshArrivalAndButton();
                requestFeeConfigForSelectedChain();
            }

            @Override
            public void onFail(int c, String m) {
                usdtChains.clear();
                selectedChain = null;
                feeConfigFromApi = null;
                feeConfigLoadedForChannelId = 0;
                feePreviewHandler.removeCallbacks(feePreviewRunnable);
                feePreviewRequestSeq++;
                lastFeePreview = null;
                lastFeePreviewAmountKey = null;
                lastFeePreviewChannelId = 0;
                applyChainUi();
                refreshArrivalAndButton();
            }
        });
    }

    private static RechargeChannel pickDefaultTrc(List<RechargeChannel> list) {
        for (RechargeChannel c : list) {
            String blob = (c.getDisplayName() + " " + nullToEmpty(c.type) + " " + nullToEmpty(c.name)).toUpperCase(Locale.US);
            if (blob.contains("TRC")) {
                return c;
            }
        }
        return list.get(0);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private void applyChainUi() {
        if (selectedChain != null) {
            String name = selectedChain.getDisplayName();
            wkVBinding.withdrawChainNameTv.setText(!TextUtils.isEmpty(name) ? name : getString(R.string.recharge_sheet_default_chain));
            bindWithdrawChainIcon(selectedChain);
        } else if (!usdtChains.isEmpty()) {
            selectedChain = pickDefaultTrc(usdtChains);
            applyChainUi();
            return;
        } else {
            wkVBinding.withdrawChainNameTv.setText(R.string.wallet_withdraw_no_chain);
            bindWithdrawChainIcon(null);
        }
        refreshArrivalAndButton();
    }

    private String getWithdrawAmountRaw() {
        return wkVBinding.amountEt.getText() != null ? wkVBinding.amountEt.getText().toString().trim() : "";
    }

    private void applyMinHintOnly() {
        double min = currentMinWithdraw();
        wkVBinding.amountEt.setHint(getString(R.string.wallet_withdraw_min_amount_fmt, formatCompact(min)));
    }

    private void requestFeeConfigForSelectedChain() {
        feePreviewHandler.removeCallbacks(feePreviewRunnable);
        feePreviewRequestSeq++;
        lastFeePreview = null;
        lastFeePreviewAmountKey = null;
        lastFeePreviewChannelId = 0;

        if (selectedChain == null || selectedChain.id <= 0) {
            feeConfigFromApi = null;
            feeConfigLoadedForChannelId = 0;
            refreshArrivalAndButton();
            return;
        }
        feeConfigFromApi = null;
        feeConfigLoadedForChannelId = 0;
        refreshArrivalAndButton();
        final long chainId = selectedChain.id;
        final long seq = ++feeConfigRequestSeq;
        WalletModel.getInstance().getWithdrawalFeeConfig(chainId, new IRequestResultListener<WithdrawalFeeConfig>() {
            @Override
            public void onSuccess(WithdrawalFeeConfig cfg) {
                if (seq != feeConfigRequestSeq || selectedChain == null || selectedChain.id != chainId) {
                    return;
                }
                if (cfg != null) {
                    feeConfigFromApi = cfg;
                    feeConfigLoadedForChannelId = chainId;
                } else {
                    feeConfigFromApi = null;
                    feeConfigLoadedForChannelId = 0;
                }
                refreshArrivalAndButton();
            }

            @Override
            public void onFail(int c, String m) {
                if (seq != feeConfigRequestSeq || selectedChain == null || selectedChain.id != chainId) {
                    return;
                }
                feeConfigFromApi = null;
                feeConfigLoadedForChannelId = 0;
                refreshArrivalAndButton();
            }
        });
    }

    private boolean useApiFeeConfig() {
        return feeConfigFromApi != null && selectedChain != null
                && selectedChain.id == feeConfigLoadedForChannelId;
    }

    /**
     * 优先展示渠道 {@link RechargeChannel#icon}；无图时用矢量 USDT 占位（绿底 + T），避免纯绿点无识别度。
     */
    private void bindWithdrawChainIcon(@Nullable RechargeChannel ch) {
        ImageView iv = wkVBinding.withdrawChainIcon;
        if (ch == null || TextUtils.isEmpty(ch.icon)) {
            Glide.with(getApplicationContext()).clear(iv);
            iv.setImageResource(R.drawable.ic_withdraw_usdt_token);
            return;
        }
        String url = WKApiConfig.getShowUrl(ch.icon.trim());
        Glide.with(this)
                .load(url)
                .centerCrop()
                .placeholder(R.drawable.ic_withdraw_usdt_token)
                .error(R.drawable.ic_withdraw_usdt_token)
                .into(iv);
    }

    @Override
    protected void onDestroy() {
        ImageView iv = wkVBinding != null ? wkVBinding.withdrawChainIcon : null;
        if (iv != null) {
            // Activity 已进入销毁流程时 Glide.with(Activity|View) 会抛 IllegalArgumentException
            Glide.with(getApplicationContext()).clear(iv);
        }
        feePreviewHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private double currentMinWithdraw() {
        if (useApiFeeConfig()) {
            return feeConfigFromApi.minWithdrawUsdt;
        }
        if (selectedChain != null) {
            return selectedChain.getMinWithdrawUsdtOrDefault();
        }
        return DEFAULT_MIN_WITHDRAW;
    }

    private double currentServiceFee() {
        if (useApiFeeConfig()) {
            return feeConfigFromApi.serviceFeeUsdt;
        }
        if (selectedChain != null) {
            return selectedChain.getWithdrawFeeUsdtOrDefault();
        }
        return DEFAULT_SERVICE_FEE;
    }

    private void showChainPicker() {
        if (usdtChains.size() <= 1) {
            return;
        }
        String[] labels = new String[usdtChains.size()];
        for (int i = 0; i < usdtChains.size(); i++) {
            String n = usdtChains.get(i).getDisplayName();
            labels[i] = !TextUtils.isEmpty(n) ? n : getString(R.string.recharge_sheet_default_chain);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.recharge_sheet_pick_chain)
                .setItems(labels, (d, which) -> {
                    selectedChain = usdtChains.get(which);
                    applyChainUi();
                    refreshArrivalAndButton();
                    requestFeeConfigForSelectedChain();
                })
                .show();
    }

    private void pasteFromClipboard() {
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm == null) {
            showToast(R.string.wallet_withdraw_clipboard_empty);
            return;
        }
        ClipData clip = cm.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            showToast(R.string.wallet_withdraw_clipboard_empty);
            return;
        }
        CharSequence t = clip.getItemAt(0).getText();
        if (TextUtils.isEmpty(t)) {
            showToast(R.string.wallet_withdraw_clipboard_empty);
            return;
        }
        wkVBinding.addressEt.setText(t.toString().trim());
    }

    private void openScanForAddress() {
        Intent intent = new Intent(this, WKScanActivity.class);
        intent.putExtra(WKScanActivity.EXTRA_RETURN_RAW_ONLY, true);
        scanLauncher.launch(intent);
    }

    private void fillAllAmount() {
        if (availableUsdt <= 0) {
            return;
        }
        wkVBinding.amountEt.setText(String.format(Locale.US, "%.6f", availableUsdt));
    }

    private void showFeeTip() {
        String desc = null;
        if (useApiFeeConfig() && feeConfigFromApi != null && !TextUtils.isEmpty(feeConfigFromApi.feeDescription)) {
            desc = feeConfigFromApi.feeDescription;
        }
        if (TextUtils.isEmpty(desc)) {
            desc = getString(R.string.wallet_withdraw_service_fee_tip);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallet_withdraw_service_fee)
                .setMessage(desc)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void scheduleFeePreview(String amountRaw, double amount) {
        feePreviewHandler.removeCallbacks(feePreviewRunnable);
        pendingFeePreviewAmountRaw = amountRaw;
        if (TextUtils.isEmpty(amountRaw) || Double.isNaN(amount) || amount <= 0
                || selectedChain == null || selectedChain.id <= 0) {
            return;
        }
        feePreviewHandler.postDelayed(feePreviewRunnable, FEE_PREVIEW_DEBOUNCE_MS);
    }

    private void runFeePreviewRequest() {
        String raw = pendingFeePreviewAmountRaw;
        if (TextUtils.isEmpty(raw) || selectedChain == null || selectedChain.id <= 0) {
            return;
        }
        double amt = parseAmountOrNaN(raw);
        if (Double.isNaN(amt) || amt <= 0) {
            return;
        }
        final long seq = ++feePreviewRequestSeq;
        final long chId = selectedChain.id;
        WalletModel.getInstance().getWithdrawalFeePreview(raw, chId, new IRequestResultListener<WithdrawalFeePreview>() {
            @Override
            public void onSuccess(WithdrawalFeePreview p) {
                if (seq != feePreviewRequestSeq) {
                    return;
                }
                String cur = getWithdrawAmountRaw();
                if (!cur.equals(raw)) {
                    return;
                }
                if (selectedChain == null || selectedChain.id != chId) {
                    return;
                }
                lastFeePreview = p != null ? p : WithdrawalFeePreview.failed(null);
                lastFeePreviewAmountKey = raw;
                lastFeePreviewChannelId = chId;
                refreshArrivalAndButton();
            }

            @Override
            public void onFail(int c, String m) {
                if (seq != feePreviewRequestSeq) {
                    return;
                }
                lastFeePreview = WithdrawalFeePreview.failed(m);
                lastFeePreviewAmountKey = raw;
                lastFeePreviewChannelId = chId;
                refreshArrivalAndButton();
            }
        });
    }

    private void applyArrivalAndFeeDisplay(String amountRaw, double amount) {
        boolean previewKeyMatch = lastFeePreview != null
                && lastFeePreviewAmountKey != null
                && lastFeePreviewAmountKey.equals(amountRaw)
                && selectedChain != null
                && selectedChain.id == lastFeePreviewChannelId;

        if (previewKeyMatch && lastFeePreview != null && lastFeePreview.ok) {
            String feePart = !TextUtils.isEmpty(lastFeePreview.feeText)
                    ? stripUsdtUnit(lastFeePreview.feeText)
                    : formatPreviewDecimalPlain(lastFeePreview.fee);
            wkVBinding.serviceFeeTv.setText(getString(R.string.wallet_withdraw_service_fee_fmt, feePart));

            String arrPart = !TextUtils.isEmpty(lastFeePreview.arrivalText)
                    ? stripUsdtUnit(lastFeePreview.arrivalText)
                    : formatPreviewDecimalPlain(lastFeePreview.arrival);
            wkVBinding.arrivalTv.setText(getString(R.string.wallet_withdraw_arrival_fmt, arrPart));
            return;
        }

        double refFee = currentServiceFee();
        wkVBinding.serviceFeeTv.setText(getString(R.string.wallet_withdraw_service_fee_fmt, formatCompact(refFee)));
        if (TextUtils.isEmpty(amountRaw) || Double.isNaN(amount) || amount <= 0) {
            wkVBinding.arrivalTv.setText(getString(R.string.wallet_withdraw_arrival_fmt, "0.00"));
        } else if (previewKeyMatch && lastFeePreview != null && !lastFeePreview.ok) {
            wkVBinding.arrivalTv.setText(getString(R.string.wallet_withdraw_arrival_fmt, "\u2014"));
        } else {
            wkVBinding.arrivalTv.setText(getString(R.string.wallet_withdraw_arrival_fmt, "\u2026"));
        }
    }

    private static String stripUsdtUnit(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().replaceAll("(?i)\\s*USDT\\s*$", "").trim();
    }

    private static String formatPreviewDecimalPlain(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return "0";
        }
        String s = String.format(Locale.US, "%.8f", v);
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 0 && s.charAt(end - 1) == '.') {
            end--;
        }
        return end > 0 ? s.substring(0, end) : "0";
    }

    private void refreshArrivalAndButton() {
        String amountRaw = getWithdrawAmountRaw();
        double amount = parseAmountOrNaN(amountRaw);
        if (TextUtils.isEmpty(amountRaw) || Double.isNaN(amount) || amount <= 0) {
            feePreviewHandler.removeCallbacks(feePreviewRunnable);
            feePreviewRequestSeq++;
            lastFeePreview = null;
            lastFeePreviewAmountKey = null;
            lastFeePreviewChannelId = 0;
        } else {
            scheduleFeePreview(amountRaw, amount);
        }
        applyMinHintOnly();
        applyArrivalAndFeeDisplay(amountRaw, amount);
        wkVBinding.withdrawConfirmBtn.setEnabled(validateFormQuiet(amount));
    }

    private boolean feePreviewMatchesInput(String amountRaw) {
        return lastFeePreview != null
                && amountRaw.equals(lastFeePreviewAmountKey)
                && selectedChain != null
                && selectedChain.id == lastFeePreviewChannelId;
    }

    private boolean previewAllowsSubmit(double amount) {
        if (lastFeePreview == null || !lastFeePreview.ok) {
            return false;
        }
        if (!Double.isNaN(lastFeePreview.arrival) && lastFeePreview.arrival > 0) {
            return true;
        }
        return !Double.isNaN(lastFeePreview.fee) && amount > lastFeePreview.fee + 1e-12;
    }

    private boolean validateFormQuiet(double amount) {
        if (selectedChain == null || usdtChains.isEmpty()) {
            return false;
        }
        String addr = wkVBinding.addressEt.getText().toString().trim();
        if (!isLikelyTrc20Address(addr)) {
            return false;
        }
        if (Double.isNaN(amount) || amount <= 0) {
            return false;
        }
        double min = currentMinWithdraw();
        if (amount + 1e-9 < min) {
            return false;
        }
        if (amount > availableUsdt + 1e-9) {
            return false;
        }
        String amountRaw = getWithdrawAmountRaw();
        if (!feePreviewMatchesInput(amountRaw)) {
            return false;
        }
        return previewAllowsSubmit(amount);
    }

    private static double parseAmountOrNaN(String s) {
        if (TextUtils.isEmpty(s)) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(s.replace(",", "."));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /** TRC20：以 T 开头、34 位 Base58 常见形态（宽松校验）。 */
    private static boolean isLikelyTrc20Address(String s) {
        return s != null && s.length() == 34 && s.startsWith("T");
    }

    private void onConfirmWithdraw() {
        if (selectedChain == null || usdtChains.isEmpty()) {
            showToast(R.string.wallet_withdraw_no_chain);
            return;
        }
        String addr = wkVBinding.addressEt.getText().toString().trim();
        if (!isLikelyTrc20Address(addr)) {
            showToast(R.string.wallet_withdraw_address_invalid);
            return;
        }
        String amountStr = wkVBinding.amountEt.getText().toString().trim();
        double amount = parseAmountOrNaN(amountStr);
        if (Double.isNaN(amount) || amount <= 0) {
            showToast(R.string.wallet_input_amount);
            return;
        }
        double min = currentMinWithdraw();
        if (amount + 1e-9 < min) {
            showToast(R.string.wallet_withdraw_below_min);
            return;
        }
        if (amount > availableUsdt + 1e-9) {
            showToast(R.string.wallet_balance_insufficient);
            return;
        }
        String amountRawConfirm = getWithdrawAmountRaw();
        if (!feePreviewMatchesInput(amountRawConfirm)) {
            showToast(R.string.wallet_withdraw_fee_preview_wait);
            return;
        }
        if (lastFeePreview == null || !lastFeePreview.ok) {
            showToast(!TextUtils.isEmpty(lastFeePreview != null ? lastFeePreview.msg : null)
                    ? lastFeePreview.msg
                    : getString(R.string.wallet_withdraw_fee_preview_invalid));
            return;
        }
        if (!previewAllowsSubmit(amount)) {
            showToast(!TextUtils.isEmpty(lastFeePreview.msg)
                    ? lastFeePreview.msg
                    : getString(R.string.wallet_withdraw_fee_preview_invalid));
            return;
        }

        final double submitAmount = amount;
        final String submitAddr = addr;
        final long channelId = selectedChain.id;

        WalletPayPasswordHelper.runIfPayPasswordReady(this, () -> {
            PayPasswordDialog dialog = new PayPasswordDialog(WithdrawActivity.this);
            dialog.setRemark(getString(R.string.wallet_withdraw_remark_fmt, formatCompact(submitAmount)));
            dialog.setOnPasswordCompleteListener(password -> {
                dialog.dismiss();
                submitWithdraw(submitAmount, password, submitAddr, channelId);
            });
            dialog.show();
        });
    }

    private void submitWithdraw(double amount, String password, String address, long channelId) {
        WalletModel.getInstance().withdraw(amount, password, address, channelId, new IRequestResultListener<WithdrawApplyResp>() {
            @Override
            public void onSuccess(WithdrawApplyResp result) {
                loadBalance();
                showToast(getString(R.string.wallet_withdraw_submitted_frozen));
                startActivity(new Intent(WithdrawActivity.this, WithdrawalOrderListActivity.class));
                overridePendingTransition(R.anim.wallet_buy_usdt_open_enter, R.anim.wallet_buy_usdt_open_exit);
                finish();
            }

            @Override
            public void onFail(int code, String msg) {
                showToast(msg != null ? msg : getString(R.string.wallet_withdraw_fail));
            }
        });
    }

    /** 与通讯录「客服」一致：热线会话接口 + 打开会话。 */
    private void onContactCustomerService() {
        WalletChatRouter.openOfficialCustomerService(this);
    }

    private static String formatCompact(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return "0";
        }
        long l = (long) v;
        if (Math.abs(v - l) < 1e-9) {
            return String.valueOf(l);
        }
        String s = String.format(Locale.US, "%.6f", v);
        s = s.replaceAll("0+$", "");
        if (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
