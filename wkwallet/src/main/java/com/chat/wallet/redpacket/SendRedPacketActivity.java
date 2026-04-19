package com.chat.wallet.redpacket;

import android.content.Intent;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.chat.base.base.WKBaseActivity;
import com.chat.groupmanage.ui.ChooseNormalMembersActivity;
import com.chat.base.net.IRequestResultListener;
import com.chat.wallet.R;
import com.chat.wallet.api.WalletModel;
import com.chat.wallet.databinding.ActivitySendRedpacketBinding;
import com.chat.wallet.entity.RedPacketSendResp;
import com.chat.wallet.msg.WKRedPacketContent;
import com.chat.wallet.util.WalletPayPasswordHelper;
import com.chat.wallet.widget.PayPasswordDialog;
import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.entity.WKChannelMember;
import com.xinbida.wukongim.entity.WKChannelType;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class SendRedPacketActivity extends WKBaseActivity<ActivitySendRedpacketBinding> {

    private static final int REQ_EXCLUSIVE_MEMBER = 10081;

    private String channelId;
    private int channelType;
    private String exclusiveToUid = null;
    private List<WKChannelMember> groupMembers = null;
    private boolean isSending = false;

    /** 群聊 Tab 与 {@link WKRedPacketContent} 类型一一对应。 */
    private final int[] groupTypeVals = {
            WKRedPacketContent.TYPE_GROUP_RANDOM,
            WKRedPacketContent.TYPE_GROUP_NORMAL,
            WKRedPacketContent.TYPE_EXCLUSIVE
    };

    @Override
    protected ActivitySendRedpacketBinding getViewBinding() {
        return ActivitySendRedpacketBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView t) {
        t.setText(R.string.redpacket_send);
    }

    @Override
    protected void initView() {
        channelId = getIntent().getStringExtra("channel_id");
        channelType = getIntent().getIntExtra("channel_type", WKChannelType.PERSONAL);

        if (channelType == WKChannelType.PERSONAL) {
            setupPersonalTabs();
        } else {
            setupGroupTabs();
        }
    }

    private void setupPersonalTabs() {
        wkVBinding.redpacketTypeTabLayout.setVisibility(View.GONE);
        wkVBinding.countLayout.setVisibility(View.GONE);
        wkVBinding.countLimitHintTv.setVisibility(View.GONE);
        wkVBinding.countEt.setText("1");
        wkVBinding.amountTypeBadgeTv.setVisibility(View.GONE);
        wkVBinding.amountLabelTv.setText(R.string.redpacket_amount);
        updateTotal();
    }

    private void setupGroupTabs() {
        wkVBinding.redpacketTypeTabLayout.setVisibility(View.VISIBLE);
        TabLayout tabs = wkVBinding.redpacketTypeTabLayout;
        tabs.removeAllTabs();
        tabs.addTab(tabs.newTab().setText(R.string.redpacket_tab_lucky));
        tabs.addTab(tabs.newTab().setText(R.string.redpacket_tab_normal));
        tabs.addTab(tabs.newTab().setText(R.string.redpacket_tab_exclusive));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                applyGroupPacketTypeUi();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        groupMembers = WKIM.getInstance().getChannelMembersManager().getMembers(channelId, WKChannelType.GROUP);
        TabLayout.Tab first = tabs.getTabAt(0);
        if (first != null) {
            tabs.selectTab(first);
        }
        applyGroupPacketTypeUi();
        clampGroupRedPacketCountInput();
    }

    private void applyGroupPacketTypeUi() {
        int selectedType = getSelectedGroupPacketType();
        boolean isExclusive = selectedType == WKRedPacketContent.TYPE_EXCLUSIVE;

        if (isExclusive) {
            wkVBinding.countLayout.setVisibility(View.GONE);
            wkVBinding.countLimitHintTv.setVisibility(View.GONE);
            wkVBinding.countEt.setText("1");
            wkVBinding.exclusiveMemberLayout.setVisibility(View.VISIBLE);
            wkVBinding.exclusiveDivider.setVisibility(View.VISIBLE);
        } else {
            wkVBinding.countLayout.setVisibility(View.VISIBLE);
            wkVBinding.exclusiveMemberLayout.setVisibility(View.GONE);
            wkVBinding.exclusiveDivider.setVisibility(View.GONE);
            exclusiveToUid = null;
            wkVBinding.exclusiveMemberTv.setText("");
            refreshGroupMemberCountAndHint();
            clampGroupRedPacketCountInput();
        }
        updateAmountTypeBadge();
        updateAmountLabel();
        updateTotal();
    }

    private void updateAmountTypeBadge() {
        if (channelType == WKChannelType.PERSONAL) {
            wkVBinding.amountTypeBadgeTv.setVisibility(View.GONE);
            return;
        }
        int t = getSelectedGroupPacketType();
        TextView b = wkVBinding.amountTypeBadgeTv;
        if (t == WKRedPacketContent.TYPE_GROUP_RANDOM) {
            b.setVisibility(View.VISIBLE);
            b.setText(R.string.redpacket_history_badge_pin);
        } else if (t == WKRedPacketContent.TYPE_GROUP_NORMAL) {
            b.setVisibility(View.VISIBLE);
            b.setText(R.string.redpacket_history_badge_normal);
        } else if (t == WKRedPacketContent.TYPE_EXCLUSIVE) {
            b.setVisibility(View.VISIBLE);
            b.setText(R.string.redpacket_history_badge_exclusive);
        } else {
            b.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (channelType == WKChannelType.GROUP) {
            refreshGroupMemberCountAndHint();
            clampGroupRedPacketCountInput();
            updateTotal();
        }
    }

    /** 刷新群成员并更新「本群共 N 人」提示（拼手气 / 普通红包） */
    private void refreshGroupMemberCountAndHint() {
        if (channelType != WKChannelType.GROUP) {
            return;
        }
        if (getSelectedGroupPacketType() == WKRedPacketContent.TYPE_EXCLUSIVE) {
            wkVBinding.countLimitHintTv.setVisibility(View.GONE);
            return;
        }
        groupMembers = WKIM.getInstance().getChannelMembersManager().getMembers(channelId, WKChannelType.GROUP);
        int n = groupMembers != null ? groupMembers.size() : 0;
        if (n <= 0) {
            wkVBinding.countLimitHintTv.setText(R.string.redpacket_group_members_empty);
            wkVBinding.countLimitHintTv.setVisibility(View.VISIBLE);
            return;
        }
        wkVBinding.countLimitHintTv.setText(getString(R.string.redpacket_group_member_count_line, n));
        wkVBinding.countLimitHintTv.setVisibility(View.VISIBLE);
    }

    private int getGroupRedPacketCountMax() {
        if (channelType != WKChannelType.GROUP) {
            return Integer.MAX_VALUE;
        }
        if (getSelectedGroupPacketType() == WKRedPacketContent.TYPE_EXCLUSIVE) {
            return 1;
        }
        if (groupMembers == null || groupMembers.isEmpty()) {
            groupMembers = WKIM.getInstance().getChannelMembersManager().getMembers(channelId, WKChannelType.GROUP);
        }
        return groupMembers != null ? groupMembers.size() : 0;
    }

    private void clampGroupRedPacketCountInput() {
        if (channelType != WKChannelType.GROUP) {
            return;
        }
        if (getSelectedGroupPacketType() == WKRedPacketContent.TYPE_EXCLUSIVE) {
            return;
        }
        int max = getGroupRedPacketCountMax();
        if (max <= 0) {
            return;
        }
        String cs = wkVBinding.countEt.getText().toString().trim();
        if (cs.isEmpty()) {
            return;
        }
        try {
            int c = Integer.parseInt(cs);
            if (c <= max) {
                return;
            }
            wkVBinding.countEt.removeTextChangedListener(countTextWatcher);
            String repl = String.valueOf(max);
            wkVBinding.countEt.setText(repl);
            wkVBinding.countEt.setSelection(repl.length());
            wkVBinding.countEt.addTextChangedListener(countTextWatcher);
        } catch (NumberFormatException ignored) {
        }
    }

    private final TextWatcher countTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (channelType == WKChannelType.GROUP
                    && getSelectedGroupPacketType() != WKRedPacketContent.TYPE_EXCLUSIVE) {
                int max = getGroupRedPacketCountMax();
                if (max > 0 && s.length() > 0) {
                    try {
                        int v = Integer.parseInt(s.toString().trim());
                        if (v > max) {
                            wkVBinding.countEt.removeTextChangedListener(this);
                            String repl = String.valueOf(max);
                            s.replace(0, s.length(), repl);
                            wkVBinding.countEt.setSelection(repl.length());
                            wkVBinding.countEt.addTextChangedListener(this);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            updateTotal();
        }
    };

    private void updateAmountLabel() {
        if (channelType == WKChannelType.PERSONAL) {
            return;
        }
        int t = getSelectedGroupPacketType();
        if (t == WKRedPacketContent.TYPE_GROUP_RANDOM) {
            wkVBinding.amountLabelTv.setText(R.string.redpacket_amount_total);
        } else if (t == WKRedPacketContent.TYPE_GROUP_NORMAL) {
            wkVBinding.amountLabelTv.setText(R.string.redpacket_amount_single);
        } else {
            wkVBinding.amountLabelTv.setText(R.string.redpacket_amount);
        }
    }

    private int getSelectedGroupPacketType() {
        if (channelType == WKChannelType.PERSONAL) {
            return WKRedPacketContent.TYPE_INDIVIDUAL;
        }
        int pos = wkVBinding.redpacketTypeTabLayout.getSelectedTabPosition();
        if (pos < 0 || pos >= groupTypeVals.length) {
            return WKRedPacketContent.TYPE_GROUP_RANDOM;
        }
        return groupTypeVals[pos];
    }

    private double computePayTotalAmount(double inputAmount, int count, int packetType) {
        if (channelType == WKChannelType.PERSONAL) {
            return inputAmount;
        }
        if (packetType == WKRedPacketContent.TYPE_GROUP_RANDOM) {
            return inputAmount;
        }
        if (packetType == WKRedPacketContent.TYPE_GROUP_NORMAL) {
            return inputAmount * count;
        }
        return inputAmount;
    }

    private int computeApiCount(int count, int packetType) {
        if (channelType == WKChannelType.PERSONAL) {
            return 1;
        }
        if (packetType == WKRedPacketContent.TYPE_EXCLUSIVE) {
            return 1;
        }
        return count;
    }

    @Override
    protected void initListener() {
        TextWatcher w = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void afterTextChanged(Editable s) {
                updateTotal();
            }
        };
        wkVBinding.amountEt.addTextChangedListener(w);
        wkVBinding.countEt.addTextChangedListener(countTextWatcher);
        wkVBinding.sendBtn.setOnClickListener(v -> onSend());
        wkVBinding.exclusiveMemberLayout.setOnClickListener(v -> showMemberPicker());
        wkVBinding.remarkEditHintIv.setOnClickListener(v -> {
            wkVBinding.remarkEt.requestFocus();
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(wkVBinding.remarkEt, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
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

    private void showMemberPicker() {
        if (TextUtils.isEmpty(channelId)) {
            showToast(getString(R.string.wallet_load_fail));
            return;
        }
        Intent intent = new Intent(this, ChooseNormalMembersActivity.class);
        intent.putExtra("groupId", channelId);
        intent.putExtra("type", ChooseNormalMembersActivity.TYPE_EXCLUSIVE_REDPACKET);
        startActivityForResult(intent, REQ_EXCLUSIVE_MEMBER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // 先处理「设置支付密码」返回：设置成功则刷新余额并自动续接发红包流程
        if (WalletPayPasswordHelper.onSetPayPasswordResult(this, requestCode, resultCode, this::onSend)) {
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_EXCLUSIVE_MEMBER || resultCode != RESULT_OK || data == null) {
            return;
        }
        String uid = data.getStringExtra("member_uid");
        String name = data.getStringExtra("member_name");
        if (TextUtils.isEmpty(uid)) {
            return;
        }
        exclusiveToUid = uid;
        if (!TextUtils.isEmpty(name)) {
            wkVBinding.exclusiveMemberTv.setText(name);
        } else {
            wkVBinding.exclusiveMemberTv.setText(uid);
        }
    }

    private void updateTotal() {
        try {
            double a = Double.parseDouble(wkVBinding.amountEt.getText().toString().trim());
            int c = Integer.parseInt(wkVBinding.countEt.getText().toString().trim());
            int pt = getSelectedGroupPacketType();
            double pay = computePayTotalAmount(a, c, pt);
            wkVBinding.totalAmountTv.setText(String.format("¥ %.2f", pay));
        } catch (NumberFormatException e) {
            wkVBinding.totalAmountTv.setText("¥ 0.00");
        }
    }

    private boolean isValidGroupMember(String uid) {
        if (TextUtils.isEmpty(uid) || channelType != WKChannelType.GROUP) {
            return false;
        }
        List<WKChannelMember> members = WKIM.getInstance().getChannelMembersManager().getMembers(channelId, WKChannelType.GROUP);
        if (members == null || members.isEmpty()) {
            return false;
        }
        for (WKChannelMember member : members) {
            if (member != null && uid.equals(member.memberUID)) {
                return true;
            }
        }
        return false;
    }

    private void onSend() {
        String as = wkVBinding.amountEt.getText().toString().trim();
        String cs = wkVBinding.countEt.getText().toString().trim();
        if (TextUtils.isEmpty(as)) {
            showToast(R.string.wallet_input_amount);
            return;
        }
        if (TextUtils.isEmpty(cs)) {
            showToast(R.string.wallet_input_count);
            return;
        }

        double amount;
        int count;
        try {
            amount = Double.parseDouble(as);
            count = Integer.parseInt(cs);
        } catch (NumberFormatException e) {
            showToast(R.string.wallet_amount_count_positive);
            return;
        }
        if (amount <= 0 || count <= 0) {
            showToast(R.string.wallet_amount_count_positive);
            return;
        }

        if (channelType == WKChannelType.GROUP) {
            refreshGroupMemberCountAndHint();
            int selType = getSelectedGroupPacketType();
            if (selType != WKRedPacketContent.TYPE_EXCLUSIVE) {
                int max = getGroupRedPacketCountMax();
                if (max <= 0) {
                    showToast(getString(R.string.redpacket_group_members_empty));
                    return;
                }
                if (count > max) {
                    showToast(getString(R.string.redpacket_count_exceeds_max, max));
                    return;
                }
            } else {
                if (!isValidGroupMember(exclusiveToUid)) {
                    exclusiveToUid = null;
                    wkVBinding.exclusiveMemberTv.setText("");
                    showToast(R.string.redpacket_please_select);
                    return;
                }
            }
        }

        String remark = wkVBinding.remarkEt.getText().toString().trim();
        if (remark.isEmpty()) {
            remark = getString(R.string.redpacket_remark);
        }

        int packetType = getSelectedGroupPacketType();
        String toUid = null;
        if (channelType == WKChannelType.GROUP && packetType == WKRedPacketContent.TYPE_EXCLUSIVE) {
            if (TextUtils.isEmpty(exclusiveToUid)) {
                showToast(R.string.redpacket_please_select);
                return;
            }
            toUid = exclusiveToUid;
        }

        int apiCount = computeApiCount(count, packetType);
        double apiTotalAmount = computePayTotalAmount(amount, count, packetType);
        if (apiTotalAmount <= 0) {
            showToast(R.string.wallet_amount_count_positive);
            return;
        }
        String finalRemark = remark;
        String finalToUid = toUid;
        int finalPacketType = packetType;
        if (isSending) {
            return;
        }
        WalletPayPasswordHelper.runIfPayPasswordReady(this, () -> {
            PayPasswordDialog d = new PayPasswordDialog(SendRedPacketActivity.this);
            d.setRemark(String.format("红包 ¥%.2f", apiTotalAmount));
            d.setOnPasswordCompleteListener(pwd -> {
                d.dismiss();
                if (isSending) {
                    return;
                }
                isSending = true;
                wkVBinding.sendBtn.setEnabled(false);
                WalletModel.getInstance().sendRedPacket(finalPacketType, channelId, channelType,
                        apiTotalAmount, apiCount, finalToUid, finalRemark, pwd, new IRequestResultListener<RedPacketSendResp>() {
                            @Override
                            public void onSuccess(RedPacketSendResp r) {
                                isSending = false;
                                wkVBinding.sendBtn.setEnabled(true);
                                showToast(getString(R.string.wallet_redpacket_sent));
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
