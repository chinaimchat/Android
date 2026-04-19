package com.chat.wallet.util;

import android.app.Activity;
import android.content.Intent;

import com.chat.base.net.IRequestResultListener;
import com.chat.base.utils.WKToastUtils;
import com.chat.wallet.R;
import com.chat.wallet.api.WalletModel;
import com.chat.wallet.entity.WalletBalanceResp;
import com.chat.wallet.ui.SetPayPasswordActivity;

/**
 * 依赖 {@link WalletBalanceResp#has_password} 判断用户是否已设置支付密码，
 * 用于发红包、提现、转账等需校验密码的入口（在弹出 {@link com.chat.wallet.widget.PayPasswordDialog} 之前调用）。
 */
public final class WalletPayPasswordHelper {

    private static final int REQ_SET_PAY_PASSWORD = 41201;

    private WalletPayPasswordHelper() {
    }

    /**
     * 已设置支付密码时执行 {@code action}；未设置则提示并跳转 {@link SetPayPasswordActivity}（mode=set）。
     */
    public static void runIfPayPasswordReady(Activity activity, Runnable action) {
        WalletModel.getInstance().getBalance(new IRequestResultListener<WalletBalanceResp>() {
            @Override
            public void onSuccess(WalletBalanceResp r) {
                boolean hasPwd = r != null && r.has_password;
                if (hasPwd) {
                    action.run();
                } else {
                    WKToastUtils.getInstance().showToastNormal(
                            activity.getString(R.string.wallet_please_set_pay_password_first));
                    Intent intent = new Intent(activity, SetPayPasswordActivity.class);
                    intent.putExtra("mode", "set");
                    // 用 startActivityForResult，便于设置成功后立刻重试
                    activity.startActivityForResult(intent, REQ_SET_PAY_PASSWORD);
                }
            }

            @Override
            public void onFail(int c, String m) {
                WKToastUtils.getInstance().showToastNormal(
                        m != null && !m.isEmpty() ? m : activity.getString(R.string.wallet_load_fail));
            }
        });
    }

    /**
     * 在调用 {@link #runIfPayPasswordReady(Activity, Runnable)} 的 Activity 的 onActivityResult 里调用。
     * 若完成设置，则重新拉一次余额并在已具备支付密码时执行 action（用于自动续接流程）。
     */
    public static boolean onSetPayPasswordResult(Activity activity, int requestCode, int resultCode, Runnable action) {
        if (requestCode != REQ_SET_PAY_PASSWORD) {
            return false;
        }
        if (resultCode != Activity.RESULT_OK) {
            return true; // 已消费 requestCode，但用户取消
        }
        WalletModel.getInstance().getBalance(new IRequestResultListener<WalletBalanceResp>() {
            @Override
            public void onSuccess(WalletBalanceResp r) {
                if (r != null && r.has_password) {
                    action.run();
                } else {
                    WKToastUtils.getInstance().showToastNormal(
                            activity.getString(R.string.wallet_please_set_pay_password_first));
                }
            }

            @Override
            public void onFail(int c, String m) {
                WKToastUtils.getInstance().showToastNormal(
                        m != null && !m.isEmpty() ? m : activity.getString(R.string.wallet_load_fail));
            }
        });
        return true;
    }
}

