package com.chat.security.ui;

import android.content.Intent;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.config.WKConfig;
import com.chat.base.entity.UserInfoEntity;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.utils.WKCommonUtils;
import com.chat.security.R;
import com.chat.security.databinding.ActInputLockScreenPwdLayoutBinding;
import com.chat.security.service.SecurityModel;

/**
 * 2021/8/9 16:38
 */
public class InputLockScreenPwdActivity extends WKBaseActivity<ActInputLockScreenPwdLayoutBinding> {
    String oldPwd;
    private boolean isSetNewPwd;

    @Override
    protected ActInputLockScreenPwdLayoutBinding getViewBinding() {
        return ActInputLockScreenPwdLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.lock_screen_password);
    }

    @Override
    protected void initPresenter() {
        if (getIntent().hasExtra("oldPwd")) oldPwd = getIntent().getStringExtra("oldPwd");
        if (getIntent().hasExtra("isSetNewPwd"))
            isSetNewPwd = getIntent().getBooleanExtra("isSetNewPwd", false);
        if (!TextUtils.isEmpty(oldPwd)) {
            wkVBinding.lockScreenPwdTv.setText(R.string.input_again_screen_pwd);
        }
    }

    @Override
    protected void initView() {
        wkVBinding.pwdView.setPwdViewBg();
        wkVBinding.pwdView.hideCloseIV();
        wkVBinding.pwdView.setOnFinishInput(() -> {
            if (isSetNewPwd) {
                if (TextUtils.isEmpty(oldPwd)) {
                    Intent intent = new Intent(InputLockScreenPwdActivity.this, InputLockScreenPwdActivity.class);
                    intent.putExtra("oldPwd", wkVBinding.pwdView.getNumPwd());
                    intent.putExtra("isSetNewPwd", isSetNewPwd);
                    chooseResultLac.launch(intent);
                } else {
                    if (!oldPwd.equals(wkVBinding.pwdView.getNumPwd())) {
                        showToast(R.string.the_two_passwords_are_inconsistent);
                        return;
                    }
                     new SecurityModel().lockScreenPwd(oldPwd, (code, msg) -> {
                        if (code == HttpResponseCode.success) {
                            UserInfoEntity userInfoEntity = WKConfig.getInstance().getUserInfo();
                            userInfoEntity.lock_screen_pwd = WKCommonUtils.digest(String.format("%s%s", oldPwd, WKConfig.getInstance().getUid()));
                            WKConfig.getInstance().saveUserInfo(userInfoEntity);
                            setResult(RESULT_OK);
                            finish();
                        } else showToast(msg);
                    });
                }
            } else {
                if (!WKCommonUtils.digest(wkVBinding.pwdView.getNumPwd() + WKConfig.getInstance().getUid()).equals(WKConfig.getInstance().getUserInfo().lock_screen_pwd)) {
                    showToast(R.string.lock_screen_pwd_err);
                    wkVBinding.pwdView.clearAllPwd();
                } else {
                    startActivity(new Intent(InputLockScreenPwdActivity.this, LockScreenPwdActivity.class));
                    finish();
                }
            }

        });
    }

    ActivityResultLauncher<Intent> chooseResultLac = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            finish();
        }
    });
}
