package com.chat.security.ui;

import android.content.Intent;
import android.text.Html;
import android.widget.TextView;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.config.WKApiConfig;
import com.chat.base.config.WKConfig;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.base.utils.StringUtils;
import com.chat.security.R;
import com.chat.security.databinding.ActDestoryAccountLayoutBinding;
import com.chat.security.service.SecurityModel;

public class DestroyAccountActivity extends WKBaseActivity<ActDestoryAccountLayoutBinding> {
    @Override
    protected ActDestoryAccountLayoutBinding getViewBinding() {
        return ActDestoryAccountLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText("");
    }

    @Override
    protected void initView() {
        wkVBinding.cancelBtn.getBackground().setTint(Theme.colorAccount);
        String phone = WKConfig.getInstance().getUserInfo().phone;
        wkVBinding.tvNum.setText(StringUtils.phoneHide(phone, phone.length()));
        String str3 = getResources().getString(R.string.btn_agree_privacy) + "<font color = \"#3DA3FF\">" + getResources().getString(R.string.kit_user_agreement) + "</font>";
        wkVBinding.closeAccountUserPolicy.setText(Html.fromHtml(str3));
        wkVBinding.noticeTv.setText(String.format(getString(R.string.notice_4), getString(R.string.app_name)));
    }

    @Override
    protected void initListener() {
        wkVBinding.closeAccountUserPolicy.setOnClickListener(v -> showWebView(WKApiConfig.baseWebUrl + "user_agreement.html"));
        wkVBinding.cancelBtn.setOnClickListener(v -> finish());
        wkVBinding.logOffBtn.setOnClickListener(v -> new SecurityModel().sendDestroyCode((code, msg) -> {
            if (code == HttpResponseCode.success) {
                startActivity(new Intent(DestroyAccountActivity.this, InputDestroyAccountCodeActivity.class));
                finish();
            } else showToast(msg);
        }));
    }
}
