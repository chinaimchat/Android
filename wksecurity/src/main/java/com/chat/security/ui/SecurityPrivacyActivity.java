package com.chat.security.ui;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.config.WKConfig;
import com.chat.base.config.WKSharedPreferencesUtil;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.entity.WKAPPConfig;
import com.chat.base.entity.UserInfoEntity;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.security.R;
import com.chat.security.databinding.ActSecurityLayoutBinding;
import com.chat.security.service.SecurityModel;
import com.chat.security.ui.device.DeviceManageActivity;

/**
 * 2020-06-30 13:49
 * 安全与隐私
 */
public class SecurityPrivacyActivity extends WKBaseActivity<ActSecurityLayoutBinding> {
    private UserInfoEntity userInfoEntity;

    @Override
    protected ActSecurityLayoutBinding getViewBinding() {
        return ActSecurityLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.security_and_privacy);
    }

    @Override
    protected void initView() {
        wkVBinding.searchIdTv.setText(String.format(getString(R.string.search_by_id), getString(R.string.app_name)));
        wkVBinding.commonPrivacyLayout.removeAllViews();
        View view = (View) EndpointManager.getInstance().invoke("common_setting_msg_privacy", this);
        if (view != null) {
            wkVBinding.commonPrivacyLayout.addView(view);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        userInfoEntity = WKConfig.getInstance().getUserInfo();
        wkVBinding.refreshLayout.setEnableOverScrollDrag(true);
        wkVBinding.refreshLayout.setEnableLoadMore(false);
        wkVBinding.refreshLayout.setEnableRefresh(false);
        wkVBinding.disconnectedSwitch.setChecked(userInfoEntity.setting.offline_protection == 1);
        wkVBinding.idSwitch.setChecked(userInfoEntity.setting.search_by_short == 1);
        wkVBinding.phoneSwitch.setChecked(userInfoEntity.setting.search_by_phone == 1);
        if (userInfoEntity.setting.device_lock == 1) {
            wkVBinding.deviceStatusTv.setText(R.string.device_status_open);
        } else {
            wkVBinding.deviceStatusTv.setText(R.string.device_status_close);
        }
        String uid = WKConfig.getInstance().getUid();
        boolean disable_screenshot = WKSharedPreferencesUtil.getInstance().getBoolean(uid + "_disable_screenshot",false);
        wkVBinding.screenshotSwitchView.setChecked(disable_screenshot);
        WKAPPConfig appConfig = WKConfig.getInstance().getAppConfig();
        wkVBinding.searchWithPhoneLayout.setVisibility(appConfig.phone_search_off == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void initListener() {
        wkVBinding.screenshotSwitchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                String uid = WKConfig.getInstance().getUid();
                WKSharedPreferencesUtil.getInstance().putBoolean(uid + "_disable_screenshot", isChecked);
                if (isChecked)
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
                else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
                }
            }
        });
        wkVBinding.lockScreenPasswordLayout.setOnClickListener(v -> {
            Intent intent = new Intent(this, InputLockScreenPwdActivity.class);
            intent.putExtra("isSetNewPwd", TextUtils.isEmpty(WKConfig.getInstance().getUserInfo().lock_screen_pwd));
            startActivity(intent);
        });
        wkVBinding.disconnectedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                userInfoEntity.setting.offline_protection = isChecked ? 1 : 0;
                new SecurityModel().updateMySetting("offline_protection", userInfoEntity.setting.offline_protection, (code, msg) -> {
                    if (code == HttpResponseCode.success) {
                        WKConfig.getInstance().saveUserInfo(userInfoEntity);
                    } else showToast(msg);
                });
            }
        });
        wkVBinding.idSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                userInfoEntity.setting.search_by_short = b ? 1 : 0;
                new SecurityModel().updateMySetting("search_by_short", userInfoEntity.setting.search_by_short, (code, msg) -> {
                    if (code == HttpResponseCode.success) {
                        WKConfig.getInstance().saveUserInfo(userInfoEntity);
                    } else showToast(msg);
                });
            }
        });
        wkVBinding.phoneSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                userInfoEntity.setting.search_by_phone = b ? 1 : 0;
                new SecurityModel().updateMySetting("search_by_phone", userInfoEntity.setting.search_by_phone, (code, msg) -> {
                    if (code == HttpResponseCode.success) {
                        WKConfig.getInstance().saveUserInfo(userInfoEntity);
                    } else showToast(msg);
                });
            }
        });
        SingleClickUtil.onSingleClick(wkVBinding.blackListLayout, v -> startActivity(new Intent(this, BlackListActivity.class)));
        SingleClickUtil.onSingleClick(wkVBinding.loginPwdLayout, v -> EndpointManager.getInstance().invoke("chow_reset_login_pwd_view", null));
        SingleClickUtil.onSingleClick(wkVBinding.deviceLayout, v -> startActivity(new Intent(this, DeviceManageActivity.class)));
        SingleClickUtil.onSingleClick(wkVBinding.chatPwdLayout, v -> startActivity(new Intent(this, ChatPwdActivity.class)));
        SingleClickUtil.onSingleClick(wkVBinding.destroyAccountLayout, v -> startActivity(new Intent(this, DestroyAccountActivity.class)));
    }
}
