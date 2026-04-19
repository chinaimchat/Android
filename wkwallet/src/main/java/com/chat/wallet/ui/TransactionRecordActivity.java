package com.chat.wallet.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.net.IRequestResultListener;
import com.chat.wallet.R;
import com.chat.wallet.api.WalletModel;
import com.chat.wallet.databinding.ActivityTransactionRecordBinding;
import com.chat.wallet.entity.TransactionRecord;
import com.chat.wallet.util.TransactionRecordDetailEnricher;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionRecordActivity extends WKBaseActivity<ActivityTransactionRecordBinding> {
    private TransactionRecordAdapter adapter;
    private int page = 1;
    private boolean loading = false;
    private boolean more = true;
    @Nullable
    private Long filterStartSec;
    @Nullable
    private Long filterEndSec;

    @Override
    protected ActivityTransactionRecordBinding getViewBinding() {
        return ActivityTransactionRecordBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView t) {
        t.setText(R.string.wallet_transaction_record);
    }

    @Override
    protected String getRightTvText(TextView textView) {
        return getString(R.string.wallet_transaction_filter);
    }

    @Override
    protected void rightLayoutClick() {
        showTimeFilterDialog();
    }

    @Override
    protected void initView() {
        adapter = new TransactionRecordAdapter();
        initAdapter(wkVBinding.recyclerView, adapter);
    }

    @Override
    protected void initListener() {
        wkVBinding.swipeRefresh.setOnRefreshListener(() -> {
            page = 1;
            more = true;
            load(true);
        });
        wkVBinding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm != null && !loading && more
                        && lm.findLastVisibleItemPosition() >= lm.getItemCount() - 3) {
                    page++;
                    load(false);
                }
            }
        });
    }

    @Override
    protected void initData() {
        load(true);
    }

    private void load(boolean refresh) {
        loading = true;
        int reqPage = refresh ? 1 : page;
        String startDate = null;
        String endDate = null;
        if (filterStartSec != null && filterEndSec != null) {
            SimpleDateFormat apiFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            startDate = apiFmt.format(new Date(filterStartSec * 1000L));
            endDate = apiFmt.format(new Date(filterEndSec * 1000L));
        }
        WalletModel.getInstance().getTransactions(reqPage, 20, startDate, endDate,
                new IRequestResultListener<List<TransactionRecord>>() {
                    @Override
                    public void onSuccess(List<TransactionRecord> list) {
                        loading = false;
                        wkVBinding.swipeRefresh.setRefreshing(false);
                        if (list == null) {
                            list = new ArrayList<>();
                        }
                        if (refresh) {
                            adapter.setList(list);
                        } else {
                            adapter.addData(list);
                        }
                        more = list.size() >= 20;
                        TransactionRecordDetailEnricher.scheduleSequentialEnrich(
                                TransactionRecordActivity.this, list, adapter);
                    }

                    @Override
                    public void onFail(int c, String m) {
                        loading = false;
                        wkVBinding.swipeRefresh.setRefreshing(false);
                        showToast(!TextUtils.isEmpty(m) ? m : getString(R.string.wallet_load_fail));
                    }
                });
    }

    private interface OnCalendarPicked {
        void onPicked(Calendar cal);
    }

    private void showTimeFilterDialog() {
        View root = LayoutInflater.from(this).inflate(R.layout.dialog_transaction_time_filter, null, false);
        TextView tvStart = root.findViewById(R.id.tvFilterStart);
        TextView tvEnd = root.findViewById(R.id.tvFilterEnd);

        final Calendar[] tempStart = {filterStartSec != null ? fromUnixSec(filterStartSec) : null};
        final Calendar[] tempEnd = {filterEndSec != null ? fromUnixSec(filterEndSec) : null};

        refreshFilterDialogLabels(tvStart, tvEnd, tempStart[0], tempEnd[0]);

        tvStart.setOnClickListener(v -> pickDateTime(tempStart[0], cal -> {
            tempStart[0] = cal;
            refreshFilterDialogLabels(tvStart, tvEnd, tempStart[0], tempEnd[0]);
        }));
        tvEnd.setOnClickListener(v -> pickDateTime(tempEnd[0], cal -> {
            tempEnd[0] = cal;
            refreshFilterDialogLabels(tvStart, tvEnd, tempStart[0], tempEnd[0]);
        }));

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(R.string.wallet_transaction_filter)
                .setView(root)
                .setPositiveButton(R.string.wallet_transaction_query, null)
                .setNeutralButton(R.string.wallet_transaction_clear_filter, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dlg.setOnShowListener(dialog -> {
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (tempStart[0] == null || tempEnd[0] == null) {
                    showToast(getString(R.string.wallet_transaction_select_both));
                    return;
                }
                long start = tempStart[0].getTimeInMillis() / 1000L;
                long end = tempEnd[0].getTimeInMillis() / 1000L;
                if (start > end) {
                    showToast(getString(R.string.wallet_transaction_time_invalid));
                    return;
                }
                filterStartSec = start;
                filterEndSec = end;
                updateFilterSubtitle();
                page = 1;
                more = true;
                load(true);
                dlg.dismiss();
            });
            dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                filterStartSec = null;
                filterEndSec = null;
                updateFilterSubtitle();
                page = 1;
                more = true;
                load(true);
                dlg.dismiss();
            });
        });
        dlg.show();
    }

    private static void refreshFilterDialogLabels(TextView tvStart, TextView tvEnd,
                                                  @Nullable Calendar start, @Nullable Calendar end) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        tvStart.setText(start != null ? fmt.format(start.getTime())
                : tvStart.getContext().getString(R.string.wallet_transaction_pick_time));
        tvEnd.setText(end != null ? fmt.format(end.getTime())
                : tvEnd.getContext().getString(R.string.wallet_transaction_pick_time));
    }

    private void pickDateTime(@Nullable Calendar initial, OnCalendarPicked onDone) {
        Calendar cal = initial != null ? (Calendar) initial.clone() : Calendar.getInstance();
        new DatePickerDialog(this, (v, year, month, dayOfMonth) ->
                new TimePickerDialog(this, (v2, hourOfDay, minute) -> {
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    cal.set(Calendar.MINUTE, minute);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    onDone.onPicked(cal);
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show(),
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private static Calendar fromUnixSec(long sec) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(sec * 1000L);
        return c;
    }

    private void updateFilterSubtitle() {
        TextView st = findViewById(com.chat.base.R.id.subtitleTv);
        if (st == null) {
            return;
        }
        if (filterStartSec != null && filterEndSec != null) {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String a = fmt.format(fromUnixSec(filterStartSec).getTime());
            String b = fmt.format(fromUnixSec(filterEndSec).getTime());
            st.setVisibility(View.VISIBLE);
            st.setText(getString(R.string.wallet_transaction_filter_subtitle, a, b));
        } else {
            st.setVisibility(View.GONE);
        }
    }
}
