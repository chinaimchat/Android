package com.chat.wallet.ui;

import android.view.View;
import android.widget.TextView;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.net.IRequestResultListener;
import com.chat.wallet.R;
import com.chat.wallet.api.WalletModel;
import com.chat.wallet.databinding.ActivityWithdrawalDetailBinding;
import com.chat.wallet.entity.WithdrawalDetail;

import java.util.Locale;

public class WithdrawalDetailActivity extends WKBaseActivity<ActivityWithdrawalDetailBinding> {

    @Override
    protected ActivityWithdrawalDetailBinding getViewBinding() {
        return ActivityWithdrawalDetailBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView t) {
        t.setText(R.string.wallet_withdrawal_detail);
    }

    @Override
    protected void initData() {
        String no = getIntent().getStringExtra("withdrawal_no");
        if (no == null) return;

        WalletModel.getInstance().getWithdrawalDetail(no, new IRequestResultListener<WithdrawalDetail>() {
            @Override
            public void onSuccess(WithdrawalDetail r) {
                wkVBinding.amountTv.setText(String.format(Locale.getDefault(), "%s USDT",
                        formatUsdtAmount(r.amount)));
                wkVBinding.timeTv.setText(r.created_at != null ? r.created_at : "");

                if (r.fee != null && !r.fee.isNaN() && r.fee >= 0) {
                    wkVBinding.feeLineTv.setVisibility(View.VISIBLE);
                    wkVBinding.feeLineTv.setText(getString(R.string.wallet_withdraw_detail_fee_fmt,
                            formatUsdtAmount(r.fee)));
                } else {
                    wkVBinding.feeLineTv.setVisibility(View.GONE);
                }
                if (r.actual_amount != null && !r.actual_amount.isNaN() && r.actual_amount >= 0) {
                    wkVBinding.actualAmountLineTv.setVisibility(View.VISIBLE);
                    wkVBinding.actualAmountLineTv.setText(getString(R.string.wallet_withdraw_detail_actual_fmt,
                            formatUsdtAmount(r.actual_amount)));
                } else {
                    wkVBinding.actualAmountLineTv.setVisibility(View.GONE);
                }

                switch (r.resolveWithdrawalStatus()) {
                    case 0:
                        wkVBinding.statusTv.setText(R.string.wallet_withdrawal_pending);
                        wkVBinding.statusTv.setTextColor(0xFFF5A623);
                        break;
                    case 1:
                        wkVBinding.statusTv.setText(R.string.wallet_withdrawal_approved);
                        wkVBinding.statusTv.setTextColor(0xFF4CAF50);
                        break;
                    case 2:
                        wkVBinding.statusTv.setText(R.string.wallet_withdrawal_rejected);
                        wkVBinding.statusTv.setTextColor(0xFFF44336);
                        break;
                    default:
                        if (r.status_text != null && !r.status_text.isEmpty()) {
                            wkVBinding.statusTv.setText(r.status_text);
                        } else {
                            wkVBinding.statusTv.setText(String.valueOf(r.resolveWithdrawalStatus()));
                        }
                        break;
                }

                String remarkShow = r.admin_remark;
                if (remarkShow == null || remarkShow.isEmpty()) {
                    remarkShow = r.remark;
                }
                if (remarkShow != null && !remarkShow.isEmpty()) {
                    wkVBinding.remarkTv.setVisibility(View.VISIBLE);
                    wkVBinding.remarkTv.setText(remarkShow);
                }
            }

            @Override
            public void onFail(int c, String m) {
                showToast(m != null ? m : getString(R.string.wallet_load_fail));
            }
        });
    }

    private static String formatUsdtAmount(Double v) {
        if (v == null || v.isNaN() || v < 0) {
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
}
