package com.chat.wallet.redpacket;

import android.content.Intent;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.net.IRequestResultListener;
import com.chat.wallet.R;
import com.chat.wallet.api.WalletModel;
import com.chat.wallet.databinding.ActivityRedpacketRecordHistoryBinding;
import com.chat.wallet.entity.RedPacketDetailResp;
import com.chat.wallet.entity.TransactionRecord;
import com.chat.wallet.util.TransactionRecordDetailEnricher;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 红包记录：我收到的 / 我发出的，按年筛选；数据来自 {@code GET /v1/wallet/transactions} 中的红包流水。
 */
public class RedPacketRecordHistoryActivity extends WKBaseActivity<ActivityRedpacketRecordHistoryBinding> {

    private static final int PAGE_SIZE = 20;

    private RedPacketHistoryListAdapter adapter;
    private final List<TransactionRecord> yearBuffer = new ArrayList<>();
    private boolean tabReceived = true;
    private int selectedYear;
    private String yearStartStr;
    private String yearEndStr;
    private boolean loadingYear;
    private int loadPage;
    /** 切换年份 / Tab / 重新拉取时递增，丢弃过期的补全回调 */
    private int displayGeneration;

    @Override
    protected ActivityRedpacketRecordHistoryBinding getViewBinding() {
        return ActivityRedpacketRecordHistoryBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView t) {
        t.setText(R.string.redpacket_history_title);
    }

    @Override
    protected void initView() {
        selectedYear = Calendar.getInstance().get(Calendar.YEAR);
        refreshYearRangeStrings();
        refreshYearLabel();

        adapter = new RedPacketHistoryListAdapter();
        initAdapter(wkVBinding.recyclerView, adapter);
        adapter.setOnItemClickListener((a, v, position) -> {
            TransactionRecord r = adapter.getItem(position);
            openRedPacketDetailFromRecord(r);
        });

        applyTabStyle();
        wkVBinding.tabReceived.setOnClickListener(v -> {
            if (!tabReceived) {
                tabReceived = true;
                applyTabStyle();
                applyTabFromBuffer();
            }
        });
        wkVBinding.tabSent.setOnClickListener(v -> {
            if (tabReceived) {
                tabReceived = false;
                applyTabStyle();
                applyTabFromBuffer();
            }
        });
        wkVBinding.yearTv.setOnClickListener(v -> showYearPicker());
    }

    @Override
    protected void initListener() {
        wkVBinding.swipeRefresh.setOnRefreshListener(() -> {
            if (!loadingYear) {
                startLoadYear();
            }
        });
    }

    @Override
    protected void initData() {
        startLoadYear();
    }

    @Override
    protected void onDestroy() {
        setPageLoadingUi(false);
        super.onDestroy();
    }

    private void refreshYearRangeStrings() {
        yearStartStr = String.format(Locale.US, "%d-01-01 00:00:00", selectedYear);
        yearEndStr = String.format(Locale.US, "%d-12-31 23:59:59", selectedYear);
    }

    private void refreshYearLabel() {
        wkVBinding.yearTv.setText(getString(R.string.redpacket_history_year_fmt, selectedYear) + " ▼");
    }

    /**
     * 打开红包详情时尽量带上 channel_id/type，以便详情内领取列表点击资料卡遵守「群内禁止互加」。
     */
    private void openRedPacketDetailFromRecord(@Nullable TransactionRecord r) {
        if (r == null || TextUtils.isEmpty(r.related_id)) {
            return;
        }
        String cid = r.resolveChannelId();
        Integer ct = r.resolveChannelType();
        if (!TextUtils.isEmpty(cid) && ct != null) {
            openRedPacketDetail(r.related_id, cid, ct);
            return;
        }
        WalletModel.getInstance().getRedPacketDetail(r.related_id, new IRequestResultListener<RedPacketDetailResp>() {
            @Override
            public void onSuccess(RedPacketDetailResp d) {
                if (isFinishing()) {
                    return;
                }
                String ch = d != null ? d.channel_id : null;
                Integer tp = d != null ? d.channel_type : null;
                openRedPacketDetail(r.related_id, ch, tp);
            }

            @Override
            public void onFail(int c, String m) {
                if (isFinishing()) {
                    return;
                }
                openRedPacketDetail(r.related_id, null, null);
            }
        });
    }

    private void openRedPacketDetail(String packetNo, @Nullable String channelId, @Nullable Integer channelType) {
        Intent i = new Intent(this, RedPacketDetailActivity.class)
                .putExtra("packet_no", packetNo)
                .putExtra(RedPacketDetailActivity.EXTRA_HIDE_REDPACKET_RECORD_ENTRY, true);
        if (!TextUtils.isEmpty(channelId) && channelType != null) {
            i.putExtra("channel_id", channelId);
            i.putExtra("channel_type", channelType);
        }
        startActivity(i);
    }

    private void applyTabStyle() {
        int active = ContextCompat.getColor(this, R.color.wallet_market_positive);
        int idle = ContextCompat.getColor(this, R.color.color999);
        wkVBinding.tabReceivedTv.setTextColor(tabReceived ? active : idle);
        wkVBinding.tabSentTv.setTextColor(tabReceived ? idle : active);
        wkVBinding.tabReceivedTv.setTypeface(null, tabReceived ? Typeface.BOLD : Typeface.NORMAL);
        wkVBinding.tabSentTv.setTypeface(null, tabReceived ? Typeface.NORMAL : Typeface.BOLD);
        wkVBinding.tabReceivedIndicator.setVisibility(tabReceived ? View.VISIBLE : View.INVISIBLE);
        wkVBinding.tabSentIndicator.setVisibility(tabReceived ? View.INVISIBLE : View.VISIBLE);
    }

    private void startLoadYear() {
        if (loadingYear) {
            return;
        }
        displayGeneration++;
        loadingYear = true;
        yearBuffer.clear();
        loadPage = 1;
        wkVBinding.swipeRefresh.setRefreshing(false);
        setPageLoadingUi(true);
        fetchNextPage();
    }

    private void fetchNextPage() {
        WalletModel.getInstance().getTransactions(loadPage, PAGE_SIZE, yearStartStr, yearEndStr,
                new IRequestResultListener<List<TransactionRecord>>() {
                    @Override
                    public void onSuccess(List<TransactionRecord> list) {
                        if (isFinishing()) {
                            return;
                        }
                        if (list == null) {
                            list = Collections.emptyList();
                        }
                        for (TransactionRecord r : list) {
                            if (isRedPacketRow(r)) {
                                yearBuffer.add(r);
                            }
                        }
                        if (list.size() >= PAGE_SIZE) {
                            loadPage++;
                            fetchNextPage();
                        } else {
                            loadingYear = false;
                            sortByTimeDesc(yearBuffer);
                            applyTabFromBuffer();
                        }
                    }

                    @Override
                    public void onFail(int c, String m) {
                        if (isFinishing()) {
                            return;
                        }
                        displayGeneration++;
                        loadingYear = false;
                        yearBuffer.clear();
                        adapter.setList(Collections.emptyList());
                        updateSummary(Collections.emptyList());
                        wkVBinding.swipeRefresh.setRefreshing(false);
                        setPageLoadingUi(false);
                        showToast(!TextUtils.isEmpty(m) ? m : getString(R.string.wallet_load_fail));
                    }
                });
    }

    private static boolean isRedPacketRow(TransactionRecord r) {
        if (r == null) {
            return false;
        }
        String t = r.type;
        return "redpacket_receive".equals(t) || "redpacket_send".equals(t);
    }

    private void applyTabFromBuffer() {
        displayGeneration++;
        final int myGen = displayGeneration;

        List<TransactionRecord> show = new ArrayList<>();
        for (TransactionRecord r : yearBuffer) {
            if (tabReceived && "redpacket_receive".equals(r.type)) {
                show.add(r);
            } else if (!tabReceived && "redpacket_send".equals(r.type)) {
                show.add(r);
            }
        }
        sortByTimeDesc(show);

        setPageLoadingUi(true);
        adapter.setList(Collections.emptyList());
        updateSummary(Collections.emptyList());

        List<TransactionRecord> batch = new ArrayList<>(show);
        TransactionRecordDetailEnricher.scheduleParallelEnrichOnce(this, batch, null, () -> {
            if (isFinishing() || myGen != displayGeneration) {
                return;
            }
            adapter.setList(show);
            updateSummary(show);
            setPageLoadingUi(false);
        });
    }

    private void setPageLoadingUi(boolean show) {
        wkVBinding.pageLoadingBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void updateSummary(List<TransactionRecord> show) {
        double sum = 0;
        for (TransactionRecord r : show) {
            if ("redpacket_receive".equals(r.type)) {
                sum += r.amount;
            } else {
                sum += Math.abs(r.amount);
            }
        }
        int n = show.size();
        if (tabReceived) {
            wkVBinding.summaryCountTv.setText(getString(R.string.redpacket_summary_received, n));
        } else {
            wkVBinding.summaryCountTv.setText(getString(R.string.redpacket_summary_sent, n));
        }
        wkVBinding.summaryAmountTv.setText(getString(R.string.redpacket_history_total_amount, sum));
    }

    private void showYearPicker() {
        int yNow = Calendar.getInstance().get(Calendar.YEAR);
        final int span = 11;
        final int[] yearValues = new int[span];
        final String[] labels = new String[span];
        for (int i = 0; i < span; i++) {
            yearValues[i] = yNow - i;
            labels[i] = getString(R.string.redpacket_history_year_fmt, yearValues[i]);
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.Theme_Wallet_RechargeBottomSheet);
        View root = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_redpacket_year_picker, null, false);
        dialog.setContentView(root);

        TextView cancelTv = root.findViewById(R.id.yearSheetCancelTv);
        TextView confirmTv = root.findViewById(R.id.yearSheetConfirmTv);
        NumberPicker picker = root.findViewById(R.id.yearPicker);

        picker.setMinValue(0);
        picker.setMaxValue(span - 1);
        picker.setDisplayedValues(labels);
        picker.setWrapSelectorWheel(false);

        int idx = 0;
        for (int i = 0; i < span; i++) {
            if (yearValues[i] == selectedYear) {
                idx = i;
                break;
            }
        }
        picker.setValue(idx);

        dialog.setOnShowListener(d -> {
            BottomSheetDialog bsd = (BottomSheetDialog) d;
            View sheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
                behavior.setSkipCollapsed(true);
                sheet.post(() -> behavior.setState(BottomSheetBehavior.STATE_EXPANDED));
            }
        });

        cancelTv.setOnClickListener(v -> dialog.dismiss());
        confirmTv.setOnClickListener(v -> {
            selectedYear = yearValues[picker.getValue()];
            refreshYearRangeStrings();
            refreshYearLabel();
            startLoadYear();
            dialog.dismiss();
        });

        dialog.show();
    }

    private static void sortByTimeDesc(List<TransactionRecord> list) {
        if (list == null || list.size() <= 1) {
            return;
        }
        Collections.sort(list, (a, b) -> Long.compare(parseCreatedMillis(b), parseCreatedMillis(a)));
    }

    private static long parseCreatedMillis(TransactionRecord r) {
        if (r == null) {
            return 0L;
        }
        return parseTimeMillis(r.created_at);
    }

    private static long parseTimeMillis(String createdAt) {
        if (TextUtils.isEmpty(createdAt)) {
            return 0L;
        }
        String s = createdAt.trim();
        if (s.endsWith("Z")) {
            s = s.substring(0, s.length() - 1);
        }
        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss.SSS",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss",
                "MM-dd HH:mm",
        };
        for (String p : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(p, Locale.US);
                sdf.setLenient(false);
                return sdf.parse(s).getTime();
            } catch (ParseException ignored) {
            }
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sdf.setLenient(false);
            return sdf.parse(s).getTime();
        } catch (ParseException e) {
            return 0L;
        }
    }

    /**
     * 列表行时间展示为 {@code MM-dd HH:mm}。
     */
    public static String formatRowTime(String createdAt) {
        if (TextUtils.isEmpty(createdAt)) {
            return "";
        }
        long ms = parseTimeMillis(createdAt);
        if (ms <= 0L) {
            return createdAt.trim();
        }
        return new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date(ms));
    }
}
