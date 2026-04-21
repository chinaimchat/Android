package com.chat.wallet.ui;

import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.net.IRequestResultListener;
import com.chat.base.net.entity.CommonResponse;
import com.chat.wallet.R;
import com.chat.wallet.api.WalletModel;
import com.chat.wallet.databinding.ActivitySetPayPasswordBinding;

public class SetPayPasswordActivity extends WKBaseActivity<ActivitySetPayPasswordBinding> {

    private final View[] dots = new View[6];
    private final StringBuilder pwd = new StringBuilder();
    private String first = null, oldPwd = null;
    private boolean isChange = false;
    private int step = 0;

    @Override
    protected ActivitySetPayPasswordBinding getViewBinding() {
        return ActivitySetPayPasswordBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView t) {
        t.setText(isChange ? R.string.wallet_change_pay_password : R.string.wallet_set_pay_password);
    }

    @Override
    protected void initView() {
        isChange = "change".equals(getIntent().getStringExtra("mode"));

        dots[0] = wkVBinding.dot1;
        dots[1] = wkVBinding.dot2;
        dots[2] = wkVBinding.dot3;
        dots[3] = wkVBinding.dot4;
        dots[4] = wkVBinding.dot5;
        dots[5] = wkVBinding.dot6;

        step = isChange ? 0 : 1;
        wkVBinding.hintTv.setText(isChange ? R.string.wallet_old_password : R.string.wallet_input_new_password);

        setupKeyboard();
    }

    private void setupKeyboard() {
        GridLayout g = wkVBinding.keyboardGrid;
        g.removeAllViews();
        int h = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics());

        for (String k : new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "\u2190"}) {
            TextView tv = new TextView(this);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 0;
            p.height = h;
            p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            tv.setLayoutParams(p);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            tv.setTextColor(0xFF333333);
            tv.setText(k);

            if (k.isEmpty()) {
                tv.setEnabled(false);
                tv.setBackgroundColor(0xFFF5F5F5);
            } else if ("\u2190".equals(k)) {
                tv.setOnClickListener(v -> {
                    if (pwd.length() > 0) {
                        pwd.deleteCharAt(pwd.length() - 1);
                        upDots();
                    }
                });
            } else {
                tv.setOnClickListener(v -> {
                    if (pwd.length() < 6) {
                        pwd.append(k);
                        upDots();
                        if (pwd.length() == 6) onComplete();
                    }
                });
            }
            g.addView(tv);
        }
    }

    private void upDots() {
        for (int i = 0; i < 6; i++) {
            dots[i].setBackgroundResource(i < pwd.length()
                    ? R.drawable.bg_pay_password_dot
                    : R.drawable.bg_pay_password_dot_empty);
        }
    }

    private void reset() {
        pwd.setLength(0);
        upDots();
    }

    private void onComplete() {
        String p = pwd.toString();
        if (isChange) {
            switch (step) {
                case 0:
                    oldPwd = p;
                    step = 1;
                    reset();
                    wkVBinding.hintTv.setText(R.string.wallet_input_new_password);
                    break;
                case 1:
                    first = p;
                    step = 2;
                    reset();
                    wkVBinding.hintTv.setText(R.string.wallet_confirm_password);
                    break;
                case 2:
                    if (first.equals(p)) {
                        WalletModel.getInstance().changePayPassword(oldPwd, p, new IRequestResultListener<CommonResponse>() {
                            @Override
                            public void onSuccess(CommonResponse r) {
                                showToast(getString(R.string.wallet_password_change_success));
                                finish();
                            }

                            @Override
                            public void onFail(int c, String m) {
                                showToast(m != null ? m : getString(R.string.wallet_password_error));
                                resetAll();
                            }
                        });
                    } else {
                        showToast(R.string.wallet_password_not_match);
                        step = 1;
                        first = null;
                        reset();
                        wkVBinding.hintTv.setText(R.string.wallet_input_new_password);
                    }
                    break;
            }
        } else {
            if (step == 1) {
                first = p;
                step = 2;
                reset();
                wkVBinding.hintTv.setText(R.string.wallet_confirm_password);
            } else if (step == 2) {
                if (first.equals(p)) {
                    WalletModel.getInstance().setPayPassword(p, new IRequestResultListener<CommonResponse>() {
                        @Override
                        public void onSuccess(CommonResponse r) {
                            showToast(getString(R.string.wallet_password_set_success));
                            setResult(RESULT_OK);
                            finish();
                        }

                        @Override
                        public void onFail(int c, String m) {
                            showToast(m != null ? m : getString(R.string.wallet_password_set_fail));
                            resetAll();
                        }
                    });
                } else {
                    showToast(R.string.wallet_password_not_match);
                    step = 1;
                    first = null;
                    reset();
                    wkVBinding.hintTv.setText(R.string.wallet_input_new_password);
                }
            }
        }
    }

    private void resetAll() {
        first = null;
        oldPwd = null;
        step = isChange ? 0 : 1;
        reset();
        wkVBinding.hintTv.setText(isChange ? R.string.wallet_old_password : R.string.wallet_input_new_password);
    }
}
