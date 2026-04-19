package com.chat.wallet.ui;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.net.IRequestResultListener;
import com.chat.wallet.R;
import com.chat.wallet.api.WalletModel;
import com.chat.wallet.databinding.ActivityRechargeApplyDetailBinding;
import com.chat.wallet.entity.RechargeApplicationRecord;
import com.chat.wallet.util.RechargeApplyTrackingStore;

import java.util.Locale;

public class RechargeApplyDetailActivity extends WKBaseActivity<ActivityRechargeApplyDetailBinding> {

    @Override
    protected ActivityRechargeApplyDetailBinding getViewBinding() {
        return ActivityRechargeApplyDetailBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView t) {
        t.setText(R.string.wallet_recharge_apply_detail);
    }

    @Override
    protected void initData() {
        String no = getIntent().getStringExtra("application_no");
        if (TextUtils.isEmpty(no)) {
            showToast(R.string.wallet_load_fail);
            finish();
            return;
        }
        wkVBinding.applicationNoTv.setText(no);
        loadingPopup.show();
        WalletModel.getInstance().fetchRechargeApplicationByApplicationNo(no, 10,
                new IRequestResultListener<RechargeApplicationRecord>() {
                    @Override
                    public void onSuccess(RechargeApplicationRecord r) {
                        loadingPopup.dismiss();
                        bindRecord(r);
                        RechargeApplyTrackingStore.updateCachedAuditStatus(r.resolveAuditStatus());
                    }

                    @Override
                    public void onFail(int c, String m) {
                        loadingPopup.dismiss();
                        showToast(!TextUtils.isEmpty(m) ? m : getString(R.string.wallet_recharge_record_not_found));
                        finish();
                    }
                });
    }

    private void bindRecord(RechargeApplicationRecord r) {
        if (r.amountU != null && r.amountU > 0) {
            wkVBinding.amountTv.setText(String.format(Locale.getDefault(), "$%.2f", r.amountU));
        } else if (r.amount != null && !r.amount.isNaN()) {
            wkVBinding.amountTv.setText(String.format(Locale.getDefault(), "¥%.2f", r.amount));
        } else {
            wkVBinding.amountTv.setText("—");
        }

        switch (r.resolveAuditStatus()) {
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
                wkVBinding.statusTv.setText(String.valueOf(r.resolveAuditStatus()));
                wkVBinding.statusTv.setTextColor(0xFF999999);
                break;
        }

        wkVBinding.timeTv.setText(!TextUtils.isEmpty(r.createdAt) ? r.createdAt : "");

        if (!TextUtils.isEmpty(r.adminRemark)) {
            wkVBinding.remarkTv.setVisibility(View.VISIBLE);
            wkVBinding.remarkTv.setText(r.adminRemark);
        } else {
            wkVBinding.remarkTv.setVisibility(View.GONE);
        }
    }
}
