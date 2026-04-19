package com.chat.security.ui;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.TextView;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.config.WKConfig;
import com.chat.base.entity.UserInfoEntity;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.base.utils.WKCommonUtils;
import com.chat.security.R;
import com.chat.security.databinding.ActChatPwdLayoutBinding;
import com.chat.security.service.SecurityModel;

import java.util.Objects;

/**
 * 2020-11-02 10:19
 * 聊天密码
 */
public class ChatPwdActivity extends WKBaseActivity<ActChatPwdLayoutBinding> {
    @Override
    protected ActChatPwdLayoutBinding getViewBinding() {
        return ActChatPwdLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.chat_pwd);
    }

    @Override
    protected void initView() {
        wkVBinding.submitBtn.getBackground().setTint(Theme.colorAccount);
    }

    @Override
    protected void initListener() {
        wkVBinding.loginPwdEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                checkInput();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        wkVBinding.chatPwdEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                checkInput();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        wkVBinding.chatPwdEtAgain.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                checkInput();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        wkVBinding.submitBtn.setOnClickListener(v -> {

            String loginPwd = Objects.requireNonNull(wkVBinding.loginPwdEt.getText()).toString();
            String chatPwd = Objects.requireNonNull(wkVBinding.chatPwdEt.getText()).toString();
            String chatPwdAgain = Objects.requireNonNull(wkVBinding.chatPwdEtAgain.getText()).toString();
            if (!chatPwd.equals(chatPwdAgain)) {
                showToast(getString(R.string.chat_pwd_two_passwords_do_not_match));
            } else {
                loadingPopup.setTitle(getString(R.string.setting_chat_pwding));
                loadingPopup.show();
                new SecurityModel().chatPwd(loginPwd, chatPwd, (code, msg) -> {
                    loadingPopup.dismiss();
                    if (code == HttpResponseCode.success) {
                        UserInfoEntity userInfoEntity = WKConfig.getInstance().getUserInfo();
                        userInfoEntity.chat_pwd = WKCommonUtils.digest(String.format("%s%s", chatPwd, WKConfig.getInstance().getUid()));
                        WKConfig.getInstance().saveUserInfo(userInfoEntity);
                        showToast(R.string.str_success);
                        finish();
                    } else {
                        showToast(msg);
                    }
                });
            }

        });
    }

    private void checkInput() {
        String loginPwd = Objects.requireNonNull(wkVBinding.loginPwdEt.getText()).toString();
        String chatPwd = Objects.requireNonNull(wkVBinding.chatPwdEt.getText()).toString();
        String chatPwdAgain = Objects.requireNonNull(wkVBinding.chatPwdEtAgain.getText()).toString();

        if (!TextUtils.isEmpty(loginPwd)
                && !TextUtils.isEmpty(chatPwd)
                && !TextUtils.isEmpty(chatPwdAgain)
                && loginPwd.length() >= 6
                && chatPwd.length() == 6
                && chatPwdAgain.length() == 6) {
            wkVBinding.submitBtn.setAlpha(1f);
            wkVBinding.submitBtn.setEnabled(true);
        } else {
            wkVBinding.submitBtn.setEnabled(false);
            wkVBinding.submitBtn.setAlpha(0.2f);
        }
    }

}
