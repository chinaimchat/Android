package com.chat.security.ui;

import android.view.KeyEvent;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.config.WKConfig;
import com.chat.base.config.WKSharedPreferencesUtil;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.utils.WKCommonUtils;
import com.chat.security.R;
import com.chat.security.databinding.ActCheckLockScreenPwdLayoutBinding;
import com.chat.security.service.SecurityModel;
import com.xinbida.wukongim.entity.WKChannelType;

/**
 * 2021/8/9 17:55
 */
public class CheckLockScreenPwdActivity extends WKBaseActivity<ActCheckLockScreenPwdLayoutBinding> {
    @Override
    protected ActCheckLockScreenPwdLayoutBinding getViewBinding() {
        return ActCheckLockScreenPwdLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        wkVBinding.leftCountTv.setFactory(() -> {
            TextView textView = new TextView(CheckLockScreenPwdActivity.this);
            textView.setTextSize(18);
            textView.setTextColor(ContextCompat.getColor(CheckLockScreenPwdActivity.this, R.color.reminderColor));
            return textView;
        });
        wkVBinding.avatarView.setSize(60);
        wkVBinding.avatarView.showAvatar(WKConfig.getInstance().getUid(), WKChannelType.PERSONAL);
        wkVBinding.pwdView.hideCloseIV();
        wkVBinding.pwdView.setPwdViewBg();
        wkVBinding.pwdView.setBottomTv(getString(R.string.forge_lock_screen_pwd), ContextCompat.getColor(this,R.color.reminderColor), () -> showDialog(getString(R.string.clear_lock_pwd), index -> {
            if (index == 1) {
                new SecurityModel().deleteLockScreenPwd((code, msg) -> {
                    if (code == HttpResponseCode.success) {
                        EndpointManager.getInstance().invoke("exit_login", null);
                    } else showToast(msg);
                });
            }
        }));
    }

    @Override
    protected void initListener() {
        wkVBinding.pwdView.setOnFinishInput(() -> {
            if (WKCommonUtils.digest(wkVBinding.pwdView.getNumPwd() + WKConfig.getInstance().getUid()).equals(WKConfig.getInstance().getUserInfo().lock_screen_pwd)) {
                WKSharedPreferencesUtil.getInstance().putInt("lock_screen_pwd_count", 5);
                finish();
            } else {
                int count = WKSharedPreferencesUtil.getInstance().getInt("lock_screen_pwd_count");
                count = count - 1;
                if (count <= 0) {
                    showToast(R.string.lock_screen_pwd_err);
                    EndpointManager.getInstance().invoke("exit_login", null);
                } else {
                    WKSharedPreferencesUtil.getInstance().putInt("lock_screen_pwd_count", count);
                    String msg = String.format(getString(R.string.lock_screen_pwd_err_and_exit), count);
                    wkVBinding.leftCountTv.setText(msg);
                    wkVBinding.pwdView.clearAllPwd();
                }
            }
        });
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected boolean supportSlideBack() {
        return false;
    }

}
