package com.chat.login.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
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
import com.chat.base.common.WKCommonModel;
import com.chat.base.config.WKApiConfig;
import com.chat.base.config.WKConfig;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.LoginMenu;
import com.chat.base.entity.UserInfoEntity;
import com.chat.base.entity.WKAPPConfig;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.SoftKeyboardUtils;
import com.chat.base.utils.WKReader;
import com.chat.login.R;
import com.chat.login.databinding.ActRegisterLayoutBinding;
import com.chat.login.entity.CountryCodeEntity;
import com.chat.login.service.LoginContract;
import com.chat.login.service.LoginPresenter;
import com.chat.login.utils.LoginErrorMessageMapper;

import java.util.List;
import java.util.Objects;

/**
 * 2020-06-19 15:42
 * 注册
 */
public class WKRegisterActivity extends WKBaseActivity<ActRegisterLayoutBinding> implements LoginContract.LoginView {
    private String code = "0086";
    // 短信验证码栏隐藏后，仍需要提交给后端的固定验证码
    private static final String AUTO_SMS_CODE = "123456";
    private LoginPresenter presenter;
    private WKAPPConfig appConfig;

    @Override
    protected ActRegisterLayoutBinding getViewBinding() {
        return ActRegisterLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initPresenter() {
        presenter = new LoginPresenter(this);
    }

    @Override
    protected void initView() {
        wkVBinding.getVCodeBtn.getBackground().setTint(Theme.colorAccount);
        wkVBinding.registerBtn.getBackground().setTint(Theme.colorAccount);
        wkVBinding.privacyPolicyTv.setTextColor(Theme.colorAccount);
        wkVBinding.userAgreementTv.setTextColor(Theme.colorAccount);
        wkVBinding.loginTv.setTextColor(Theme.colorAccount);
        wkVBinding.authCheckBox.setResId(getContext(), R.mipmap.round_check2);
        wkVBinding.authCheckBox.setDrawBackground(true);
        wkVBinding.authCheckBox.setHasBorder(true);
        wkVBinding.authCheckBox.setStrokeWidth(AndroidUtilities.dp(1));
        wkVBinding.authCheckBox.setBorderColor(ContextCompat.getColor(getContext(), R.color.color999));
        wkVBinding.authCheckBox.setSize(18);
        wkVBinding.authCheckBox.setColor(Theme.colorAccount, ContextCompat.getColor(getContext(), R.color.white));
        wkVBinding.authCheckBox.setVisibility(View.VISIBLE);
        wkVBinding.authCheckBox.setEnabled(true);
        wkVBinding.authCheckBox.setChecked(false, true);

        wkVBinding.privacyPolicyTv.setOnClickListener(v -> showWebView(WKApiConfig.baseWebUrl + "privacy_policy.html"));
        wkVBinding.userAgreementTv.setOnClickListener(v -> showWebView(WKApiConfig.baseWebUrl + "user_agreement.html"));
        wkVBinding.registerAppTv.setText(String.format(getString(R.string.register_app), getString(R.string.app_name)));
        // 区号固定 0086，文案固定显示“账号”，不允许点击切换。
        wkVBinding.codeTv.setText(R.string.account_input_label);
        wkVBinding.chooseCodeTv.setEnabled(false);
        wkVBinding.chooseCodeTv.setClickable(false);
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
                    wkVBinding.getVCodeBtn.setAlpha(1f);
                    wkVBinding.getVCodeBtn.setEnabled(true);
                } else {
                    wkVBinding.getVCodeBtn.setEnabled(false);
                    wkVBinding.getVCodeBtn.setAlpha(0.2f);
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

        // 隐藏“获取验证码/验证码输入”区：不展示短信验证码输入栏，但提交时仍固定使用 123456
        wkVBinding.verfiEt.setText(AUTO_SMS_CODE);
        wkVBinding.verfiEt.setEnabled(false);
        wkVBinding.verfiEt.setVisibility(View.GONE);
        wkVBinding.getVCodeBtn.setEnabled(false);
        wkVBinding.getVCodeBtn.setVisibility(View.GONE);
        if (wkVBinding.vcodeLineBottom != null) {
            wkVBinding.vcodeLineBottom.setVisibility(View.GONE);
        }
        checkStatus();

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
        wkVBinding.pwdConfirmEt.addTextChangedListener(new TextWatcher() {
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
        wkVBinding.loginTv.setOnClickListener(v -> startActivity(new Intent(this, WKLoginActivity.class)));
        wkVBinding.chooseCodeTv.setOnClickListener(null);
        wkVBinding.registerBtn.setOnClickListener(v -> {
            if (!wkVBinding.authCheckBox.isChecked()) {
                showToast(R.string.agree_auth_tips);
                return;
            }

            String phone = Objects.requireNonNull(wkVBinding.nameEt.getText()).toString();
            String smsCode = Objects.requireNonNull(wkVBinding.verfiEt.getText()).toString();
            String pwd = Objects.requireNonNull(wkVBinding.pwdEt.getText()).toString();
            String confirmPwd = Objects.requireNonNull(wkVBinding.pwdConfirmEt.getText()).toString();
            String inviteCode = Objects.requireNonNull(wkVBinding.inviteCodeTv.getText())
                    .toString()
                    .replaceAll("\\s+", "")
                    .trim();
            if (!TextUtils.isEmpty(phone) && !TextUtils.isEmpty(smsCode) && !TextUtils.isEmpty(pwd) && !TextUtils.isEmpty(confirmPwd)) {
                if (pwd.length() < 6 || pwd.length() > 16) {
                    showSingleBtnDialog(getString(R.string.pwd_length_error));
                } else if (!TextUtils.equals(pwd, confirmPwd)) {
                    showSingleBtnDialog(getString(R.string.pwd_confirm_not_match));
                } else {
                    if (appConfig != null && appConfig.invite_code_system_on == 1 && TextUtils.isEmpty(inviteCode)) {
                        showSingleBtnDialog(getString(R.string.invite_code_not_null));
                        return;
                    }
                    loadingPopup.show();
                    presenter.registerApp(smsCode, code, "", phone, pwd, inviteCode);
                }
            }
        });
        wkVBinding.getVCodeBtn.setOnClickListener(v -> {
            String phone = Objects.requireNonNull(wkVBinding.nameEt.getText()).toString();
            if (!TextUtils.isEmpty(phone)) {
                presenter.registerCode(code, phone);
            }
        });

        wkVBinding.myTv.setOnClickListener(view1 -> wkVBinding.authCheckBox.setChecked(!wkVBinding.authCheckBox.isChecked(), true));
        wkVBinding.authCheckBox.setOnClickListener(view1 -> wkVBinding.authCheckBox.setChecked(!wkVBinding.authCheckBox.isChecked(), true));
        wkVBinding.pwdConfirmEt.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String pwd = Objects.requireNonNull(wkVBinding.pwdEt.getText()).toString();
                String confirmPwd = Objects.requireNonNull(wkVBinding.pwdConfirmEt.getText()).toString();
                if (!TextUtils.isEmpty(pwd) && !TextUtils.isEmpty(confirmPwd) && !TextUtils.equals(pwd, confirmPwd)) {
                    showSingleBtnDialog(getString(R.string.pwd_confirm_not_match));
                }
            }
        });
    }

    @Override
    protected void initListener() {
        // 两个眼睛相互独立：避免“瞟一眼确认栏也把密码栏暴露给肩膀后的人”
        wkVBinding.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            wkVBinding.pwdEt.setTransformationMethod(isChecked
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            wkVBinding.pwdEt.setSelection(Objects.requireNonNull(wkVBinding.pwdEt.getText()).length());
        });
        wkVBinding.checkBoxConfirm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            wkVBinding.pwdConfirmEt.setTransformationMethod(isChecked
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            wkVBinding.pwdConfirmEt.setSelection(Objects.requireNonNull(wkVBinding.pwdConfirmEt.getText()).length());
        });
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

    @Override
    protected void initData() {
        showConfigLoading(true);
        // 先按本地缓存渲染一次，避免首次打开时邀请码栏状态抖动。
        appConfig = WKConfig.getInstance().getAppConfig();
        applyInviteCodeConfig(appConfig);
        WKCommonModel.getInstance().getAppConfig((code, msg, wkappConfig) -> {
            if (code == HttpResponseCode.success) {
                appConfig = wkappConfig;
                applyInviteCodeConfig(appConfig);
            } else {
                showToast(LoginErrorMessageMapper.map(this, msg));
            }
            showConfigLoading(false);
        });
    }

    private void applyInviteCodeConfig(WKAPPConfig config) {
        if (config != null && config.invite_code_system_on == 1) {
            wkVBinding.inviteCodeTv.setHint(R.string.input_invite_code_must);
            wkVBinding.inviteLayout.setVisibility(View.VISIBLE);
            wkVBinding.inviteLineView.setVisibility(View.VISIBLE);
        } else {
            wkVBinding.inviteCodeTv.setHint(R.string.input_invite_code_not_must);
            wkVBinding.inviteLayout.setVisibility(View.GONE);
            wkVBinding.inviteLineView.setVisibility(View.GONE);
        }
    }

    private void showConfigLoading(boolean show) {
        if (show) {
            wkVBinding.configLoadingLayout.setVisibility(View.VISIBLE);
            wkVBinding.registerContentLayout.setVisibility(View.INVISIBLE);
            wkVBinding.registerAppTv.setVisibility(View.INVISIBLE);
        } else {
            wkVBinding.configLoadingLayout.setVisibility(View.GONE);
            wkVBinding.registerContentLayout.setVisibility(View.VISIBLE);
            wkVBinding.registerAppTv.setVisibility(View.VISIBLE);
        }
    }

    private void checkStatus() {
        String phone = Objects.requireNonNull(wkVBinding.nameEt.getText()).toString();
        String smsCode = Objects.requireNonNull(wkVBinding.verfiEt.getText()).toString();
        String pwd = Objects.requireNonNull(wkVBinding.pwdEt.getText()).toString();
        String confirmPwd = Objects.requireNonNull(wkVBinding.pwdConfirmEt.getText()).toString();
        if (!TextUtils.isEmpty(phone) && !TextUtils.isEmpty(smsCode) && !TextUtils.isEmpty(pwd) && !TextUtils.isEmpty(confirmPwd)) {
            wkVBinding.registerBtn.setAlpha(1f);
            wkVBinding.registerBtn.setEnabled(true);
        } else {
            wkVBinding.registerBtn.setAlpha(0.2f);
            wkVBinding.registerBtn.setEnabled(false);
        }
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

    @Override
    public void loginResult(UserInfoEntity userInfoEntity) {
        loadingPopup.dismiss();
        SoftKeyboardUtils.getInstance().hideInput(this, wkVBinding.pwdEt);
        hideLoading();

        if (TextUtils.isEmpty(userInfoEntity.name)) {
            Intent intent = new Intent(this, PerfectUserInfoActivity.class);
            startActivity(intent);
            finish();
        } else {
            new Handler(Objects.requireNonNull(Looper.myLooper())).postDelayed(() -> {
                List<LoginMenu> list = EndpointManager.getInstance().invokes(EndpointCategory.loginMenus, null);
                if (WKReader.isNotEmpty(list)) {
                    for (LoginMenu menu : list) {
                        if (menu.iMenuClick != null) menu.iMenuClick.onClick();
                    }
                }
                finish();
            }, 500);
        }
    }

    @Override
    public void setCountryCode(List<CountryCodeEntity> list) {

    }

    @Override
    public void setRegisterCodeSuccess(int code, String msg, int exist) {
        if (code == HttpResponseCode.success) {
            if (exist == 1) {
                showSingleBtnDialog(getString(R.string.account_exist));
            } else {
                wkVBinding.nameEt.setEnabled(false);
                presenter.startTimer();
            }
        } else {
            showToast(LoginErrorMessageMapper.map(this, msg));
        }
    }

    @Override
    public void setLoginFail(int code, String uid, String phone) {

    }

    @Override
    public void setSendCodeResult(int code, String msg) {

    }

    @Override
    public void setResetPwdResult(int code, String msg) {
    }

    @Override
    public Button getVerificationCodeBtn() {
        return wkVBinding.getVCodeBtn;
    }

    @Override
    public EditText getNameEt() {
        return wkVBinding.nameEt;
    }

    @Override
    public void showError(String msg) {
        showSingleBtnDialog(LoginErrorMessageMapper.map(this, msg));
    }

    @Override
    public void hideLoading() {
        loadingPopup.dismiss();
    }


    @Override
    public Context getContext() {
        return this;
    }

}
