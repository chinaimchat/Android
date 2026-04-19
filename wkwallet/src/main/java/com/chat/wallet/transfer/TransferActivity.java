package com.chat.wallet.transfer;

import android.content.Intent;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.config.WKApiConfig;
import com.chat.base.glide.GlideUtils;
import com.chat.base.net.IRequestResultListener;
import com.chat.groupmanage.ui.ChooseNormalMembersActivity;
import com.chat.wallet.R;
import com.chat.wallet.api.WalletModel;
import com.chat.wallet.databinding.ActivityTransferBinding;
import com.chat.wallet.util.WalletPayPasswordHelper;
import com.chat.wallet.entity.TransferSendResp;
import com.chat.wallet.widget.PayPasswordDialog;
import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.entity.WKChannel;
import com.xinbida.wukongim.entity.WKChannelMember;
import com.xinbida.wukongim.entity.WKChannelType;

import java.util.List;

public class TransferActivity extends WKBaseActivity<ActivityTransferBinding> {

    private static final int REQ_GROUP_MEMBER = 10082;

    private String toUid;
    private String channelId;
    private int channelType;
    private boolean isGroup = false;
    private List<WKChannelMember> groupMembers = null;
    private boolean isSending = false;
    /** 来自钱包「收款码」扫码，请求体带 {@code pay_scene=receive_qr} 供服务端直接入账。 */
    private boolean fromWalletReceiveQr;

    @Override
    protected ActivityTransferBinding getViewBinding() {
        return ActivityTransferBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView t) {
        t.setText(R.string.transfer_send);
    }

    @Override
    protected void initView() {
        channelId = getIntent().getStringExtra("channel_id");
        channelType = getIntent().getIntExtra("channel_type", WKChannelType.PERSONAL);
        toUid = getIntent().getStringExtra("to_uid");
        fromWalletReceiveQr = getIntent().getBooleanExtra("from_wallet_receive_qr", false);
        isGroup = channelType == WKChannelType.GROUP;

        if (isGroup) {
            wkVBinding.toNameTv.setText(R.string.redpacket_select_member);
            wkVBinding.selectHintTv.setVisibility(View.VISIBLE);
            groupMembers = WKIM.getInstance().getChannelMembersManager().getMembers(channelId, WKChannelType.GROUP);
        } else {
            showSelectedUser(toUid);
        }
    }

    @Override
    protected void initListener() {
        wkVBinding.selectMemberLayout.setOnClickListener(v -> {
            if (isGroup) showMemberPicker();
        });

        wkVBinding.sendBtn.setOnClickListener(v -> onSend());
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

    private void showSelectedUser(String uid) {
        if (uid == null) return;
        toUid = uid;
        String name = uid;
        WKChannel ch = WKIM.getInstance().getChannelManager().getChannel(uid, WKChannelType.PERSONAL);
        if (ch != null && !TextUtils.isEmpty(ch.channelName)) name = ch.channelName;
        wkVBinding.toNameTv.setText(getString(R.string.transfer_to) + " " + name);
        wkVBinding.selectHintTv.setVisibility(View.GONE);
        GlideUtils.getInstance().showAvatarImg(this, WKApiConfig.getAvatarUrl(uid), uid, wkVBinding.avatarIv);
    }

    private void showMemberPicker() {
        if (TextUtils.isEmpty(channelId)) {
            showToast(getString(R.string.wallet_load_fail));
            return;
        }
        Intent intent = new Intent(this, ChooseNormalMembersActivity.class);
        intent.putExtra("groupId", channelId);
        intent.putExtra("type", ChooseNormalMembersActivity.TYPE_GROUP_TRANSFER);
        startActivityForResult(intent, REQ_GROUP_MEMBER);
    }

    private boolean isValidGroupMember(String uid) {
        if (!isGroup || TextUtils.isEmpty(uid)) {
            return false;
        }
        groupMembers = WKIM.getInstance().getChannelMembersManager().getMembers(channelId, WKChannelType.GROUP);
        if (groupMembers == null || groupMembers.isEmpty()) {
            return false;
        }
        for (WKChannelMember member : groupMembers) {
            if (member != null && uid.equals(member.memberUID)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // 先处理「设置支付密码」返回：设置成功则刷新余额并自动续接转账流程
        if (WalletPayPasswordHelper.onSetPayPasswordResult(this, requestCode, resultCode, this::onSend)) {
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_GROUP_MEMBER || resultCode != RESULT_OK || data == null) {
            return;
        }
        String uid = data.getStringExtra("member_uid");
        String name = data.getStringExtra("member_name");
        if (TextUtils.isEmpty(uid)) {
            return;
        }
        showSelectedUser(uid);
        if (!TextUtils.isEmpty(name)) {
            wkVBinding.toNameTv.setText(getString(R.string.transfer_to) + " " + name);
        }
    }

    private void onSend() {
        if (TextUtils.isEmpty(toUid)) {
            showToast(R.string.transfer_please_select);
            return;
        }

        String s = wkVBinding.amountEt.getText().toString().trim();
        if (TextUtils.isEmpty(s)) {
            showToast(R.string.wallet_input_amount);
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            showToast(R.string.wallet_amount_count_positive);
            return;
        }
        if (amount <= 0) {
            showToast(R.string.wallet_amount_count_positive);
            return;
        }

        if (isGroup && !isValidGroupMember(toUid)) {
            toUid = null;
            wkVBinding.toNameTv.setText(R.string.redpacket_select_member);
            wkVBinding.selectHintTv.setVisibility(View.VISIBLE);
            showToast(R.string.transfer_please_select);
            return;
        }

        String remark = wkVBinding.remarkEt.getText().toString().trim();
        final String apiChannelId = isGroup ? channelId : (TextUtils.isEmpty(channelId) ? toUid : channelId);
        if (TextUtils.isEmpty(apiChannelId)) {
            showToast(getString(R.string.wallet_load_fail));
            return;
        }
        if (isSending) {
            return;
        }
        WalletPayPasswordHelper.runIfPayPasswordReady(this, () -> {
            PayPasswordDialog d = new PayPasswordDialog(TransferActivity.this);
            d.setRemark(String.format("转账 ¥%.2f", amount));
            d.setOnPasswordCompleteListener(pwd -> {
                d.dismiss();
                if (isSending) {
                    return;
                }
                isSending = true;
                wkVBinding.sendBtn.setEnabled(false);
                WalletModel.getInstance().sendTransfer(toUid, amount, remark, pwd,
                        apiChannelId, channelType,
                        fromWalletReceiveQr ? "receive_qr" : null,
                        new IRequestResultListener<TransferSendResp>() {
                    @Override
                    public void onSuccess(TransferSendResp r) {
                        isSending = false;
                        wkVBinding.sendBtn.setEnabled(true);
                        showToast(getString(R.string.wallet_transfer_sent));
                        finish();
                    }

                    @Override
                    public void onFail(int c, String m) {
                        isSending = false;
                        wkVBinding.sendBtn.setEnabled(true);
                        showToast(m != null ? m : getString(R.string.wallet_send_fail));
                    }
                });
            });
            d.show();
        });
    }
}
