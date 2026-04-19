package com.chat.wallet.ui;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.chat.base.config.WKApiConfig;
import com.chat.base.net.IRequestResultListener;
import com.chat.base.net.OkHttpUtils;
import com.chat.base.utils.ImageUtils;
import com.chat.base.utils.WKToastUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.chat.wallet.R;
import com.chat.wallet.api.WalletModel;
import com.chat.wallet.databinding.SheetRechargeDepositBinding;
import com.chat.wallet.entity.RechargeApplyResp;
import com.chat.wallet.entity.RechargeChannel;
import com.chat.wallet.util.RechargeApplyTrackingStore;
import com.chat.wallet.util.WalletChatRouter;
import com.chat.wallet.util.WalletQrEncodeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Material 风格充值收款 BottomSheet（参考 USDT 地址 + 二维码页），日夜间随系统/主题资源切换。
 */
public class RechargeDepositBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "RechargeDepositBottomSheet";

    private static final Executor QR_ENCODE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "wallet-recharge-qr");
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    private SheetRechargeDepositBinding binding;
    private final List<RechargeChannel> addressChannels = new ArrayList<>();
    private int selectedIndex = 0;
    private Bitmap lastQrBitmap;
    private String lastAddress = "";
    /** 防止快速切换渠道时旧二维码异步结果覆盖新地址 */
    private int qrEncodeGeneration;

    public static void show(@Nullable FragmentActivity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        RechargeDepositBottomSheet sheet = new RechargeDepositBottomSheet();
        sheet.show(activity.getSupportFragmentManager(), TAG);
    }

    @Override
    public int getTheme() {
        return R.style.Theme_Wallet_RechargeBottomSheet;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = SheetRechargeDepositBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.sheetDoneTv.setOnClickListener(v -> {
            hideSheetKeyboard();
            dismiss();
        });
        binding.sheetOrdersTv.setOnClickListener(v -> {
            hideSheetKeyboard();
            requireActivity().startActivity(new Intent(requireActivity(), BuyUsdtOrderListActivity.class));
        });
        binding.rechargeSheetContactCsBtn.setOnClickListener(v -> onContactCustomerService());
        binding.chainSelectorRow.setOnClickListener(v -> showChannelPicker());
        binding.saveQrLayout.setOnClickListener(v -> saveQr());
        binding.copyAddressLayout.setOnClickListener(v -> copyAddress());
        binding.openFullRechargeTv.setOnClickListener(v -> {
            hideSheetKeyboard();
            startActivity(new Intent(requireContext(), RechargeActivity.class));
            dismiss();
        });
        binding.sheetAmountCard.setVisibility(View.GONE);
        binding.sheetAmountEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateSheetEstimate();
            }
        });
        binding.sheetConfirmRechargeBtn.setOnClickListener(v -> onConfirmSheetRecharge());
        binding.chainNameTv.setText(R.string.wallet_recharge_loading_config);
        binding.depositAddressTv.setText(R.string.recharge_sheet_loading);
        binding.chainChevronIv.setVisibility(View.GONE);
        binding.chainSelectorRow.setClickable(false);
        binding.chainSelectorRow.setFocusable(false);
        setupSheetTapToHideKeyboard();
        // 与 BottomSheet 进入动画错开一帧，减少首帧布局 + 网络同时抢主线程造成的「卡顿感」
        binding.getRoot().post(this::loadChannels);
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        if (dialog == null) {
            return;
        }
        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setSkipCollapsed(true);
            // 等首帧测量完成后再设为展开，避免与系统滑入动画抢同一帧布局
            bottomSheet.post(() -> {
                if (!isAdded()) {
                    return;
                }
                BottomSheetBehavior<View> b = BottomSheetBehavior.from(bottomSheet);
                b.setState(BottomSheetBehavior.STATE_EXPANDED);
            });
        }
    }

    @Override
    public void onDestroyView() {
        qrEncodeGeneration++;
        if (binding != null) {
            Glide.with(binding.getRoot().getContext()).clear(binding.depositQrIv);
            if (lastQrBitmap != null && !lastQrBitmap.isRecycled()) {
                lastQrBitmap.recycle();
                lastQrBitmap = null;
            }
        }
        super.onDestroyView();
        binding = null;
    }

    private void loadChannels() {
        WalletModel.getInstance().getRechargeChannels(new IRequestResultListener<List<RechargeChannel>>() {
            @Override
            public void onSuccess(List<RechargeChannel> list) {
                if (!isAdded() || binding == null) {
                    return;
                }
                addressChannels.clear();
                if (list != null) {
                    List<RechargeChannel> uFirst = new ArrayList<>();
                    List<RechargeChannel> rest = new ArrayList<>();
                    for (RechargeChannel c : list) {
                        if (c == null || !c.isChannelEnabled()) {
                            continue;
                        }
                        if (c.getPayTypeInt() == 4) {
                            uFirst.add(c);
                        } else if (!TextUtils.isEmpty(c.getDepositAddressForDisplay())) {
                            rest.add(c);
                        }
                    }
                    addressChannels.addAll(uFirst);
                    addressChannels.addAll(rest);
                }
                if (addressChannels.isEmpty()) {
                    binding.chainNameTv.setText(R.string.recharge_sheet_no_enabled_method);
                    binding.chainChevronIv.setVisibility(View.GONE);
                    binding.chainSelectorRow.setClickable(false);
                    binding.chainSelectorRow.setFocusable(false);
                    binding.depositAddressTv.setText(R.string.recharge_sheet_no_address);
                    binding.depositQrIv.setImageDrawable(null);
                    binding.sheetAmountCard.setVisibility(View.GONE);
                    binding.openFullRechargeTv.setVisibility(View.VISIBLE);
                    lastQrBitmap = null;
                    lastAddress = "";
                    return;
                }
                selectedIndex = 0;
                binding.openFullRechargeTv.setVisibility(View.GONE);
                applySelectorRowChrome();
                applySelectedChannel();
            }

            @Override
            public void onFail(int c, String m) {
                if (!isAdded() || binding == null) {
                    return;
                }
                binding.chainNameTv.setText(R.string.wallet_load_fail);
                binding.chainChevronIv.setVisibility(View.GONE);
                binding.chainSelectorRow.setClickable(false);
                binding.chainSelectorRow.setFocusable(false);
                binding.depositAddressTv.setText(R.string.recharge_sheet_no_address);
                binding.sheetAmountCard.setVisibility(View.GONE);
                binding.openFullRechargeTv.setVisibility(View.VISIBLE);
                lastQrBitmap = null;
                lastAddress = "";
            }
        });
    }

    /**
     * 与 {@link RechargeActivity#loadChannels()} 一致：仅展示后台支付配置中已开启（{@link RechargeChannel#isChannelEnabled()}）的渠道。
     * U 盾（pay_type=4）始终进入本页可选列表；其它类型仅当有链上地址时纳入。
     */
    private void applySelectorRowChrome() {
        if (binding == null) {
            return;
        }
        int n = addressChannels.size();
        boolean multi = n > 1;
        binding.chainChevronIv.setVisibility(multi ? View.VISIBLE : View.GONE);
        binding.chainSelectorRow.setClickable(multi);
        binding.chainSelectorRow.setFocusable(multi);
    }

    private void applySelectedChannel() {
        if (binding == null || addressChannels.isEmpty()) {
            return;
        }
        RechargeChannel ch = addressChannels.get(Math.min(selectedIndex, addressChannels.size() - 1));
        String name = ch.getDisplayName();
        if (TextUtils.isEmpty(name)) {
            name = getString(R.string.recharge_sheet_default_chain);
        }
        binding.chainNameTv.setText(name);
        String addr = ch.getDepositAddressForDisplay();
        lastAddress = addr;
        boolean hasAddr = !TextUtils.isEmpty(addr);
        if (hasAddr) {
            binding.depositAddressTv.setText(addr);
        } else {
            binding.depositAddressTv.setText(R.string.recharge_sheet_no_address);
        }

        int sizePx = getResources().getDimensionPixelSize(R.dimen.recharge_sheet_qr_size);
        Glide.with(requireContext()).clear(binding.depositQrIv);
        binding.depositQrIv.setImageDrawable(null);
        Bitmap prev = lastQrBitmap;
        lastQrBitmap = null;
        if (prev != null && !prev.isRecycled()) {
            prev.recycle();
        }
        final int gen = ++qrEncodeGeneration;

        String qrImageUrl = ch.getDepositQrImageUrlOrEmpty();
        if (!TextUtils.isEmpty(qrImageUrl)) {
            // 用与 Retrofit 相同的 OkHttp 拉取字节再解码，避免 Glide 与拦截器/缓存/解码链导致预览图 403 或失败
            String showUrl = WKApiConfig.getRechargeChannelQrImageLoadUrl(qrImageUrl);
            if (TextUtils.isEmpty(showUrl)) {
                onServerQrImageUnavailable(gen);
            } else {
                FragmentActivity hostAct = getActivity();
                if (hostAct == null || hostAct.isFinishing()) {
                    onServerQrImageUnavailable(gen);
                } else {
                    QR_ENCODE_EXECUTOR.execute(() -> loadServerQrImageWithOkHttp(showUrl, sizePx, gen, hostAct));
                }
            }
        } else if (hasAddr) {
            scheduleGeneratedQrFromAddress(addr, sizePx, gen);
        }

        binding.sheetAmountEt.setText("");
        updateAmountSection(ch);
    }

    /**
     * 通过 {@link OkHttpUtils} 下载收款码原图（自动带 token 等与接口一致的 Header），主线程更新 ImageView。
     *
     * @param act 须在主线程传入 {@link #getActivity()}，避免在后台线程取 Activity 为 null 导致失败无提示
     */
    private void loadServerQrImageWithOkHttp(@NonNull String showUrl, int sizePx, int gen,
                                             @NonNull FragmentActivity act) {
        try {
            // 网关 302 → 预签名 OSS：主 Client 自动跟随会把 Bridge 头带进签名请求 → 403；改用手动重定向 + bare Client
            byte[] bytes = OkHttpUtils.getInstance().fetchGatewayThenBareRedirect(showUrl);
            if (bytes == null || bytes.length == 0) {
                notifyServerQrLoadFailedOnMain(act, gen);
                return;
            }
            Bitmap decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (decoded == null) {
                notifyServerQrLoadFailedOnMain(act, gen);
                return;
            }
            Bitmap toShow = scaleQrBitmapToMaxSide(decoded, sizePx);
            if (act == null || act.isFinishing()) {
                if (toShow != null && !toShow.isRecycled()) {
                    toShow.recycle();
                }
                return;
            }
            act.runOnUiThread(() -> {
                if (!isAdded() || binding == null || gen != qrEncodeGeneration) {
                    if (toShow != null && !toShow.isRecycled()) {
                        toShow.recycle();
                    }
                    return;
                }
                Bitmap old = lastQrBitmap;
                lastQrBitmap = toShow;
                binding.depositQrIv.setImageBitmap(toShow);
                if (old != null && !old.isRecycled() && old != toShow) {
                    old.recycle();
                }
            });
        } catch (Exception e) {
            notifyServerQrLoadFailedOnMain(act, gen);
        }
    }

    private void notifyServerQrLoadFailedOnMain(@Nullable FragmentActivity act, int gen) {
        if (act == null || act.isFinishing()) {
            return;
        }
        act.runOnUiThread(() -> {
            if (!isAdded() || binding == null || gen != qrEncodeGeneration) {
                return;
            }
            onServerQrImageUnavailable(gen);
        });
    }

    /** 缩小过大位图，避免 OOM；保持宽高比 */
    private static Bitmap scaleQrBitmapToMaxSide(@NonNull Bitmap src, int maxPx) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= 0 || h <= 0) {
            return src;
        }
        if (w <= maxPx && h <= maxPx) {
            return src;
        }
        float scale = Math.min((float) maxPx / w, (float) maxPx / h);
        int nw = Math.max(1, Math.round(w * scale));
        int nh = Math.max(1, Math.round(h * scale));
        Bitmap out = Bitmap.createScaledBitmap(src, nw, nh, true);
        if (out != src && !src.isRecycled()) {
            src.recycle();
        }
        return out;
    }

    /** 后台配置了图片 URL 但未成功展示：清空占位并提示，不生成「假」二维码 */
    private void onServerQrImageUnavailable(int gen) {
        if (!isAdded() || binding == null || gen != qrEncodeGeneration) {
            return;
        }
        lastQrBitmap = null;
        binding.depositQrIv.setImageDrawable(null);
        WKToastUtils.getInstance().showToastNormal(getString(R.string.recharge_sheet_server_qr_load_fail));
    }

    private void scheduleGeneratedQrFromAddress(@NonNull String addrForQr, int sizePx, int gen) {
        QR_ENCODE_EXECUTOR.execute(() -> {
            Bitmap bmp = WalletQrEncodeUtil.encodeQrBitmap(addrForQr, sizePx);
            FragmentActivity act = getActivity();
            if (act == null) {
                if (bmp != null && !bmp.isRecycled()) {
                    bmp.recycle();
                }
                return;
            }
            act.runOnUiThread(() -> {
                if (!isAdded() || binding == null || gen != qrEncodeGeneration) {
                    if (bmp != null && !bmp.isRecycled()) {
                        bmp.recycle();
                    }
                    return;
                }
                lastQrBitmap = bmp;
                if (lastQrBitmap != null) {
                    binding.depositQrIv.setImageBitmap(lastQrBitmap);
                } else {
                    binding.depositQrIv.setImageDrawable(null);
                }
            });
        });
    }

    private void updateAmountSection(RechargeChannel ch) {
        if (binding == null || ch == null) {
            return;
        }
        binding.sheetAmountCard.setVisibility(View.VISIBLE);
        boolean isU = ch.getPayTypeInt() == 4;
        binding.sheetAmountSymbolTv.setText(isU ? "$" : "¥");
        updateAmountRangeHint(ch);
        updateSheetEstimate();
    }

    private void updateAmountRangeHint(RechargeChannel ch) {
        if (binding == null || ch == null) {
            return;
        }
        boolean hasMin = ch.min_amount > 0;
        boolean hasMax = ch.max_amount > 0;
        if (!hasMin && !hasMax) {
            binding.sheetAmountRangeTv.setVisibility(View.GONE);
            return;
        }
        binding.sheetAmountRangeTv.setVisibility(View.VISIBLE);
        String minStr = hasMin ? formatChannelAmountHint(ch.min_amount) : "";
        String maxStr = hasMax ? formatChannelAmountHint(ch.max_amount) : "";
        if (hasMin && hasMax) {
            binding.sheetAmountRangeTv.setText(
                    getString(R.string.recharge_sheet_amount_range_min_max, minStr, maxStr));
        } else if (hasMin) {
            binding.sheetAmountRangeTv.setText(getString(R.string.recharge_sheet_amount_range_min, minStr));
        } else {
            binding.sheetAmountRangeTv.setText(getString(R.string.recharge_sheet_amount_range_max, maxStr));
        }
    }

    private static String formatChannelAmountHint(double v) {
        if (v == Math.floor(v)) {
            return String.format(Locale.US, "%d", (long) v);
        }
        return stripTrailingZeros(v);
    }

    /**
     * 与 {@link RechargeActivity#updateUCoinEstimate()} 一致：仅 U 盾（pay_type=4）且汇率有效时展示预计到账（CNY）。
     */
    private void updateSheetEstimate() {
        if (binding == null || addressChannels.isEmpty()) {
            return;
        }
        RechargeChannel ch = addressChannels.get(Math.min(selectedIndex, addressChannels.size() - 1));
        if (ch.getPayTypeInt() != 4) {
            binding.sheetEstimateTv.setVisibility(View.GONE);
            return;
        }
        double rate = ch.getUcoinRateMultiplier();
        if (Double.isNaN(rate) || rate <= 0) {
            binding.sheetEstimateTv.setVisibility(View.GONE);
            return;
        }
        String raw = binding.sheetAmountEt.getText().toString().trim();
        if (TextUtils.isEmpty(raw)) {
            binding.sheetEstimateTv.setVisibility(View.GONE);
            return;
        }
        double uAmt;
        try {
            uAmt = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            binding.sheetEstimateTv.setVisibility(View.GONE);
            return;
        }
        if (uAmt <= 0) {
            binding.sheetEstimateTv.setVisibility(View.GONE);
            return;
        }
        double est = uAmt * rate;
        String estStr = String.format(Locale.getDefault(), "%.2f", est);
        binding.sheetEstimateTv.setText(getString(R.string.wallet_ucoin_estimate_fmt, estStr));
        binding.sheetEstimateTv.setVisibility(View.VISIBLE);
    }

    /**
     * numberDecimal 键盘无「完成」：点空白收起；与 {@link WindowManager.LayoutParams#SOFT_INPUT_ADJUST_RESIZE} 配合可滚到「确认充值」。
     */
    private void setupSheetTapToHideKeyboard() {
        binding.rechargeSheetNestedScrollView.setOnTouchListener((v, event) -> {
            if (event.getAction() != MotionEvent.ACTION_DOWN) {
                return false;
            }
            if (binding.sheetAmountCard.getVisibility() != View.VISIBLE) {
                return false;
            }
            if (rawTouchInsideView(binding.sheetAmountEt, event)) {
                return false;
            }
            hideSheetKeyboard();
            return false;
        });
    }

    private static boolean rawTouchInsideView(@NonNull View target, @NonNull MotionEvent event) {
        if (target.getVisibility() != View.VISIBLE) {
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

    private void hideSheetKeyboard() {
        if (binding == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return;
        }
        View et = binding.sheetAmountEt;
        imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
        et.clearFocus();
        FragmentActivity act = getActivity();
        if (act != null) {
            View focus = act.getCurrentFocus();
            if (focus != null) {
                imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
            }
        }
    }

    private void onConfirmSheetRecharge() {
        if (binding == null || addressChannels.isEmpty()) {
            return;
        }
        RechargeChannel ch = addressChannels.get(Math.min(selectedIndex, addressChannels.size() - 1));
        String raw = binding.sheetAmountEt.getText().toString().trim();
        if (TextUtils.isEmpty(raw)) {
            WKToastUtils.getInstance().showToastNormal(getString(R.string.wallet_input_amount));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            WKToastUtils.getInstance().showToastNormal(getString(R.string.wallet_input_amount));
            return;
        }
        if (amount <= 0) {
            WKToastUtils.getInstance().showToastNormal(getString(R.string.wallet_amount_count_positive));
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
                if (!isAdded() || binding == null) {
                    return;
                }
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
                binding.sheetAmountEt.setText("");
                binding.sheetEstimateTv.setVisibility(View.GONE);
                FragmentActivity act = requireActivity();
                act.startActivity(new Intent(act, BuyUsdtOrderListActivity.class));
                act.overridePendingTransition(R.anim.wallet_buy_usdt_open_enter, R.anim.wallet_buy_usdt_open_exit);
                dismiss();
            }

            @Override
            public void onFail(int c, String m) {
                WKToastUtils.getInstance().showToastNormal(
                        m != null ? m : getString(R.string.wallet_recharge_fail));
            }
        });
    }

    private static String stripTrailingZeros(double v) {
        String s = String.format(Locale.US, "%.8f", v);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }

    private void showChannelPicker() {
        if (addressChannels.size() <= 1) {
            return;
        }
        BottomSheetDialog picker = new BottomSheetDialog(requireContext(), R.style.Theme_Wallet_RechargeBottomSheet);
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.sheet_recharge_pick_currency, null, false);
        RecyclerView rv = content.findViewById(R.id.pickCurrencyRv);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        RechargeCurrencyPickAdapter adapter = new RechargeCurrencyPickAdapter(
                this, addressChannels, selectedIndex, idx -> {
                    selectedIndex = idx;
                    applySelectedChannel();
                    picker.dismiss();
                });
        rv.setAdapter(adapter);
        picker.setContentView(content);
        picker.setOnShowListener(dlg -> {
            View sheet = picker.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                BottomSheetBehavior<View> b = BottomSheetBehavior.from(sheet);
                b.setFitToContents(true);
                b.setSkipCollapsed(true);
                b.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        picker.show();
    }

    private void copyAddress() {
        if (TextUtils.isEmpty(lastAddress)) {
            WKToastUtils.getInstance().showToastNormal(getString(R.string.recharge_sheet_no_address));
            return;
        }
        ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("address", lastAddress));
            WKToastUtils.getInstance().showToastNormal(getString(R.string.recharge_sheet_copied));
        }
    }

    private void saveQr() {
        if (lastQrBitmap == null || lastQrBitmap.isRecycled()) {
            WKToastUtils.getInstance().showToastNormal(getString(R.string.recharge_sheet_save_fail));
            return;
        }
        Context ctx = requireContext();
        ImageUtils.getInstance().saveBitmap(ctx, lastQrBitmap, true,
                path -> WKToastUtils.getInstance().showToastNormal(getString(R.string.recharge_sheet_save_ok)));
    }

    /** 与通讯录「客服」、{@link BuyUsdtActivity} 一致：走 {@code show_customer_service} / 热线会话接口。 */
    private void onContactCustomerService() {
        FragmentActivity act = getActivity();
        if (act instanceof AppCompatActivity && !act.isFinishing()) {
            WalletChatRouter.openOfficialCustomerService((AppCompatActivity) act);
        }
    }
}
