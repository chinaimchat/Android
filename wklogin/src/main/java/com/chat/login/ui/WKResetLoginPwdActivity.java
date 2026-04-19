package com.chat.login.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.config.WKConfig;
import com.chat.base.entity.UserInfoEntity;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.login.R;
import com.chat.login.databinding.ActResetLoginPwdLayoutBinding;
import com.chat.login.entity.CountryCodeEntity;
import com.chat.login.service.LoginContract;
import com.chat.login.service.LoginPresenter;
import com.chat.login.utils.LoginErrorMessageMapper;

import java.util.List;
import java.util.Objects;

/**
 * 2020-11-25 11:21
 * 重置登录密码
 */
public class WKResetLoginPwdActivity extends WKBaseActivity<ActResetLoginPwdLayoutBinding> implements LoginContract.LoginView {

    private String code = "0086";
    private static final String AUTO_SMS_CODE = "123456";
    private LoginPresenter presenter;

    @Override
    protected ActResetLoginPwdLayoutBinding getViewBinding() {
        return ActResetLoginPwdLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initPresenter() {
        presenter = new LoginPresenter(this);
    }

    @Override
    protected void initView() {
        wkVBinding.sureBtn.getBackground().setTint(Theme.colorAccount);
        wkVBinding.getVerCodeBtn.getBackground().setTint(Theme.colorAccount);
        Theme.setPressedBackground(wkVBinding.backIv);
        wkVBinding.backIv.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.colorDark), PorterDuff.Mode.MULTIPLY));
        boolean canEditPhone = getIntent().getBooleanExtra("canEditPhone", false);
        wkVBinding.nameEt.setEnabled(canEditPhone);
        wkVBinding.nameEt.setText(WKConfig.getInstance().getUserInfo().phone);
        String zone = WKConfig.getInstance().getUserInfo().zone;
        if (!TextUtils.isEmpty(zone)) {
            code = zone;
        }
        // Fixed: 不显示区号+86，且禁用国家区号/小箭头点击
        wkVBinding.codeTv.setText(R.string.account_input_label);
        wkVBinding.chooseCodeTv.setEnabled(false);
        wkVBinding.chooseCodeTv.setClickable(false);
        wkVBinding.chooseCodeTv.setOnClickListener(null);
        if (!canEditPhone || !TextUtils.isEmpty(Objects.requireNonNull(wkVBinding.nameEt.getText()).toString())) {
            wkVBinding.getVerCodeBtn.setEnabled(true);
            wkVBinding.getVerCodeBtn.setAlpha(1);
        }

        // 与注册页一致：隐藏短信验证码输入与发送按钮，但提交时固定使用 123456。
        wkVBinding.verfiEt.setText(AUTO_SMS_CODE);
        wkVBinding.verfiEt.setEnabled(false);
        wkVBinding.codeContainer.setVisibility(View.GONE);
        wkVBinding.vcodeLineBottom.setVisibility(View.GONE);
        wkVBinding.getVerCodeBtn.setEnabled(false);
        wkVBinding.getVerCodeBtn.setVisibility(View.GONE);

        wkVBinding.resetLoginPwdTv.setVisibility(View.GONE);
        checkStatus();
    }

    @Override
    protected void initListener() {
        wkVBinding.nameEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() > 0) {
                    wkVBinding.getVerCodeBtn.setEnabled(true);
                    wkVBinding.getVerCodeBtn.setAlpha(1f);
                } else {
                    wkVBinding.getVerCodeBtn.setEnabled(false);
                    wkVBinding.getVerCodeBtn.setAlpha(0.2f);
                }
                checkStatus();
            }
        });
        wkVBinding.verfiEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                checkStatus();
            }
        });
        wkVBinding.pwdEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                checkStatus();
            }
        });
        wkVBinding.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                wkVBinding.pwdEt.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                wkVBinding.pwdEt.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            wkVBinding.pwdEt.setSelection(Objects.requireNonNull(wkVBinding.pwdEt.getText()).length());
        });
        // 禁用国家区号点击切换
        wkVBinding.chooseCodeTv.setOnClickListener(null);
        wkVBinding.chooseCodeTv.setEnabled(false);
        wkVBinding.chooseCodeTv.setClickable(false);
        wkVBinding.sureBtn.setOnClickListener(v -> {

            String phone = Objects.requireNonNull(wkVBinding.nameEt.getText()).toString();
            String verCode = wkVBinding.verfiEt.getText().toString();
            String pwd = wkVBinding.pwdEt.getText().toString();
            if (!TextUtils.isEmpty(phone) && !TextUtils.isEmpty(verCode) && !TextUtils.isEmpty(pwd)) {
                if (pwd.length() < 6 || pwd.length() > 16) {
                    showToast(R.string.pwd_length_error);
                } else {
                    loadingPopup.show();
                    presenter.resetPwd(code, phone, verCode, pwd);
                }
            }

        });
        wkVBinding.getVerCodeBtn.setOnClickListener(v -> {
            String phone = wkVBinding.nameEt.getText().toString();
            if (!TextUtils.isEmpty(phone)) {
                presenter.forgetPwd(code, phone);
            }
        });
        wkVBinding.backIv.setOnClickListener(v -> finish());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            maybeHideKeyboardOnOutsideTouch(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void maybeHideKeyboardOnOutsideTouch(MotionEvent ev) {
        View focus = getCurrentFocus();
        if (!(focus instanceof EditText)) {
            return;
        }
        Rect rect = new Rect();
        focus.getGlobalVisibleRect(rect);
        if (rect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
        focus.clearFocus();
    }


    private void checkStatus() {
        String phone = wkVBinding.nameEt.getText().toString();
        String verCode = wkVBinding.verfiEt.getText().toString();
        String pwd = wkVBinding.pwdEt.getText().toString();
        if (!TextUtils.isEmpty(phone) && !TextUtils.isEmpty(verCode) && !TextUtils.isEmpty(pwd)) {
            wkVBinding.sureBtn.setAlpha(1f);
            wkVBinding.sureBtn.setEnabled(true);
        } else {
            wkVBinding.sureBtn.setAlpha(0.2f);
            wkVBinding.sureBtn.setEnabled(false);
        }
    }

    @Override
    public void loginResult(UserInfoEntity userInfoEntity) {

    }

    @Override
    public void setCountryCode(List<CountryCodeEntity> list) {

    }

    @Override
    public void setRegisterCodeSuccess(int code, String msg, int exist) {

    }

    @Override
    public void setLoginFail(int code, String uid, String phone) {

    }

    @Override
    public void setSendCodeResult(int code, String msg) {
        if (code == HttpResponseCode.success) {
            presenter.startTimer();
        } else {
            showToast(LoginErrorMessageMapper.map(this, msg));
        }
    }

    @Override
    public void setResetPwdResult(int code, String msg) {
        if (code == HttpResponseCode.success) {
            finish();
        }
    }

    @Override
    public Button getVerificationCodeBtn() {
        return wkVBinding.getVerCodeBtn;

    }

    @Override
    public EditText getNameEt() {
        return wkVBinding.nameEt;
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void showError(String msg) {
        showToast(LoginErrorMessageMapper.map(this, msg));
    }

    @Override
    public void hideLoading() {
        loadingPopup.dismiss();
    }


    ActivityResultLauncher<Intent> intentActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        //此处是跳转的result回调方法
        if (result.getData() != null && result.getResultCode() == Activity.RESULT_OK) {
            CountryCodeEntity entity = result.getData().getParcelableExtra("entity");
            assert entity != null;
            code = entity.code;
            String codeName = code.substring(2);
            wkVBinding.codeTv.setText(String.format("+%s", codeName));
        }
    });
}
