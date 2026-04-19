package com.chat.security.ui;

import android.content.Intent;
import android.widget.TextView;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.config.WKConfig;
import com.chat.base.entity.BottomSheetItem;
import com.chat.base.entity.UserInfoEntity;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.utils.WKDialogUtils;
import com.chat.security.R;
import com.chat.security.databinding.ActLockScreenPwdLayoutBinding;
import com.chat.security.service.SecurityModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 2021/8/9 16:22
 * 锁屏密码
 */
public class LockScreenPwdActivity extends WKBaseActivity<ActLockScreenPwdLayoutBinding> {
    @Override
    protected ActLockScreenPwdLayoutBinding getViewBinding() {
        return ActLockScreenPwdLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.lock_screen_password);
    }

    @Override
    protected void initView() {
        int time = WKConfig.getInstance().getUserInfo().lock_after_minute;
        if (time == 0) wkVBinding.lockTimeTv.setText(R.string.now);
        else if (time == 1) wkVBinding.lockTimeTv.setText(R.string.leaving_1);
        else if (time == 5) wkVBinding.lockTimeTv.setText(R.string.leaving_5);
        else if (time == 30) wkVBinding.lockTimeTv.setText(R.string.leaving_30);
        else if (time == 60) wkVBinding.lockTimeTv.setText(R.string.leaving_60);
    }

    @Override
    protected void initListener() {
        wkVBinding.chooseTimeLayout.setOnClickListener(v -> {
            List<BottomSheetItem> list = new ArrayList<>();
            list.add(new BottomSheetItem(getString(R.string.now), 0, () -> close(0, getString(R.string.now))));
            list.add(new BottomSheetItem(getString(R.string.leaving_1), 0, () -> close(1, getString(R.string.leaving_1))));
            list.add(new BottomSheetItem(getString(R.string.leaving_5), 0, () -> close(5, getString(R.string.leaving_5))));
            list.add(new BottomSheetItem(getString(R.string.leaving_30), 0, () -> close(30, getString(R.string.leaving_30))));
            list.add(new BottomSheetItem(getString(R.string.leaving_60), 0, () -> close(60, getString(R.string.leaving_60))));
            WKDialogUtils.getInstance().showBottomSheet(this, getString(R.string.auto_lock), false, list);
        });
        wkVBinding.updateLayout.setOnClickListener(v -> {
            Intent intent = new Intent(this, InputLockScreenPwdActivity.class);
            intent.putExtra("isSetNewPwd", true);
            startActivity(intent);
        });
        wkVBinding.closeTv.setOnClickListener(v -> WKDialogUtils.getInstance().showDialog(this, getString(R.string.close_lock_screen_pwd), getString(R.string.close_lock_screen_pwd_tips), true, "", "", 0, 0, index -> {
            if (index == 1) {
                new SecurityModel().deleteLockScreenPwd((code, msg) -> {
                    if (code == HttpResponseCode.success) {
                        UserInfoEntity userInfoEntity = WKConfig.getInstance().getUserInfo();
                        userInfoEntity.lock_screen_pwd = "";
                        WKConfig.getInstance().saveUserInfo(userInfoEntity);
                        finish();
                    } else showToast(msg);
                });

            }
        }));
    }

    private void close(int time, String content) {
        new SecurityModel().updateLockTime(time, (code, msg) -> {
            if (code == HttpResponseCode.success) {
                wkVBinding.lockTimeTv.setText(content);
                UserInfoEntity userInfoEntity = WKConfig.getInstance().getUserInfo();
                userInfoEntity.lock_after_minute = time;
                WKConfig.getInstance().saveUserInfo(userInfoEntity);
            } else showToast(msg);
        });
    }
}
