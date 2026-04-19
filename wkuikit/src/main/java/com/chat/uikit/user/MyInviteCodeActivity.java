package com.chat.uikit.user;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;
import android.widget.TextView;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.config.WKConfig;
import com.chat.base.entity.UserInfoEntity;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActMyInviteCodeLayoutBinding;
import com.chat.uikit.enity.InviteCodeEntity;
import com.chat.uikit.user.service.InviteCodeModel;
import com.xinbida.wukongim.entity.WKChannelType;

public class MyInviteCodeActivity extends WKBaseActivity<ActMyInviteCodeLayoutBinding> {
    private boolean isDisabled = false;
    private String inviteCode = "";

    @Override
    protected ActMyInviteCodeLayoutBinding getViewBinding() {
        return ActMyInviteCodeLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.my_invite_code);
    }

    @Override
    protected void initView() {
        wkVBinding.avatarView.showAvatar(WKConfig.getInstance().getUid(), WKChannelType.PERSONAL);
        UserInfoEntity userInfo = WKConfig.getInstance().getUserInfo();
        wkVBinding.nicknameTv.setText(userInfo != null ? userInfo.name : "");
        wkVBinding.inviteCodeTv.setText("-------");
        refreshStatusButton();
    }

    @Override
    protected void initListener() {
        SingleClickUtil.onSingleClick(wkVBinding.copyTv, view -> copyInviteCode());
        SingleClickUtil.onSingleClick(wkVBinding.statusTv, view -> switchInviteStatus());
    }

    @Override
    protected void initData() {
        loadInviteCode();
    }

    private void loadInviteCode() {
        loadingPopup.show();
        InviteCodeModel.getInstance().getInviteCode((code, msg, entity) -> {
            loadingPopup.dismiss();
            if (code == HttpResponseCode.success && entity != null) {
                updateInviteInfo(entity);
            } else {
                showToast(msg);
            }
        });
    }

    private void switchInviteStatus() {
        loadingPopup.show();
        InviteCodeModel.getInstance().switchInviteStatus((code, msg) -> {
            loadingPopup.dismiss();
            if (code == HttpResponseCode.success) {
                loadInviteCode();
            } else {
                showToast(msg);
            }
        });
    }

    private void updateInviteInfo(InviteCodeEntity entity) {
        inviteCode = entity.invite_code == null ? "" : entity.invite_code;
        wkVBinding.inviteCodeTv.setText(TextUtils.isEmpty(inviteCode) ? "-------" : inviteCode);
        isDisabled = entity.status == 0;
        refreshStatusButton();
    }

    private void refreshStatusButton() {
        wkVBinding.statusTv.setText(isDisabled ? R.string.invite_enable : R.string.invite_disable);
    }

    private void copyInviteCode() {
        if (TextUtils.isEmpty(inviteCode)) {
            showToast(R.string.network_error_tips);
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("invite_code", inviteCode));
            showToast(R.string.copyed);
        }
    }
}
