package com.chat.wallet.redpacket;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.net.IRequestResultListener;
import com.chat.base.utils.GroupTipNicknamePolicy;
import com.chat.base.utils.WKToastUtils;
import com.chat.wallet.R;
import com.chat.wallet.api.WalletModel;
import com.chat.wallet.databinding.ActivityRedpacketDetailBinding;
import com.chat.wallet.entity.RedPacketDetailResp;
import com.chat.wallet.util.WalletDisplayNameHelper;
import com.chat.wallet.entity.RedPacketRecord;
import com.chat.uikit.user.UserDetailActivity;
import com.xinbida.wukongim.entity.WKChannelType;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class RedPacketDetailActivity extends WKBaseActivity<ActivityRedpacketDetailBinding> {

    /** 从「红包记录」列表再进详情时不显示右上角「红包记录」，避免 A→记录→B 详情仍出现入口的循环感 */
    public static final String EXTRA_HIDE_REDPACKET_RECORD_ENTRY = "hide_redpacket_record_entry";

    private RedPacketRecordAdapter recordAdapter;
    /** 进入详情时的会话，用于群昵称解析与打开资料卡时传 groupID */
    private String detailChannelId;
    private int detailChannelType = WKChannelType.PERSONAL;
    private boolean hideRedpacketRecordEntry;

    @Override
    protected ActivityRedpacketDetailBinding getViewBinding() {
        return ActivityRedpacketDetailBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView t) {
        t.setText(R.string.redpacket_detail);
    }

    @Override
    protected String getRightTvText(TextView textView) {
        if (hideRedpacketRecordEntry) {
            return "";
        }
        return getString(R.string.redpacket_record_entry);
    }

    @Override
    protected void rightLayoutClick() {
        if (hideRedpacketRecordEntry) {
            return;
        }
        startActivity(new Intent(this, RedPacketRecordHistoryActivity.class));
    }

    @Override
    protected void initView() {
        hideRedpacketRecordEntry = getIntent().getBooleanExtra(EXTRA_HIDE_REDPACKET_RECORD_ENTRY, false);
        detailChannelId = getIntent().getStringExtra("channel_id");
        detailChannelType = getIntent().getIntExtra("channel_type", WKChannelType.PERSONAL);
        recordAdapter = new RedPacketRecordAdapter();
        recordAdapter.setResolveContext(detailChannelId, detailChannelType);
        initAdapter(wkVBinding.recordsRv, recordAdapter);
        recordAdapter.setOnItemClickListener((adapter, view, position) -> {
            RedPacketRecord rec = recordAdapter.getItem(position);
            if (rec == null || TextUtils.isEmpty(rec.uid)) {
                return;
            }
            byte ct = (byte) detailChannelType;
            int ff = GroupTipNicknamePolicy.forbiddenAddFriendFlagFromLocalGroupChannel(detailChannelId, ct);
            if (GroupTipNicknamePolicy.shouldBlockNicknameProfileJump(detailChannelId, ct, ff)) {
                WKToastUtils.getInstance().showToastNormal(
                        getString(com.chat.base.R.string.group_tip_forbidden_nickname_profile));
                return;
            }
            Intent intent = new Intent(RedPacketDetailActivity.this, UserDetailActivity.class);
            intent.putExtra("uid", rec.uid);
            if (detailChannelType == WKChannelType.GROUP && !TextUtils.isEmpty(detailChannelId)) {
                intent.putExtra("groupID", detailChannelId);
            }
            startActivity(intent);
        });
    }

    @Override
    protected void initData() {
        applyHideRedpacketRecordEntryInTitleBar();
        String no = getIntent().getStringExtra("packet_no");
        if (no == null) {
            return;
        }
        WalletModel.getInstance().getRedPacketDetail(no, new IRequestResultListener<RedPacketDetailResp>() {
            @Override
            public void onSuccess(RedPacketDetailResp r) {
                if (TextUtils.isEmpty(detailChannelId) && !TextUtils.isEmpty(r.channel_id) && r.channel_type != null) {
                    detailChannelId = r.channel_id;
                    detailChannelType = r.channel_type;
                    recordAdapter.setResolveContext(detailChannelId, detailChannelType);
                }
                String senderName = WalletDisplayNameHelper.displayNameForUid(
                        r.sender_uid, detailChannelId, detailChannelType);
                if (TextUtils.isEmpty(senderName) && r.sender_uid != null) {
                    senderName = r.sender_uid;
                }
                wkVBinding.senderNameTv.setText(!TextUtils.isEmpty(senderName)
                        ? senderName + getString(R.string.wallet_redpacket_suffix)
                        : getString(R.string.redpacket));
                wkVBinding.remarkTv.setText(r.remark != null && !r.remark.isEmpty() ? r.remark : getString(R.string.redpacket_remark));
                if (r.my_amount > 0) {
                    wkVBinding.amountTv.setText(String.format("¥%.2f", r.my_amount));
                } else {
                    wkVBinding.amountTv.setText("");
                }
                String st;
                if (r.redpacket_status == 2) {
                    st = getString(R.string.redpacket_expired);
                } else if (r.remaining_count == 0) {
                    st = getString(R.string.redpacket_finished);
                } else {
                    st = String.format(getString(R.string.redpacket_received_count), r.total_count - r.remaining_count, r.total_count);
                }
                st += " | " + String.format(getString(R.string.redpacket_total_amount), String.valueOf(r.total_count), String.format("%.2f", r.total_amount));
                wkVBinding.statusTv.setText(st);
                recordAdapter.setRedPacketTotalCount(r.total_count);
                ArrayList<RedPacketRecord> records = r.records != null
                        ? new ArrayList<>(r.records)
                        : new ArrayList<>();
                sortRecordsByClaimTimeDesc(records);
                recordAdapter.setList(records);
            }

            @Override
            public void onFail(int c, String m) {
                showToast(m != null ? m : getString(R.string.wallet_load_fail));
            }
        });
    }

    /**
     * {@link WKBaseActivity} 在 {@code initTitleBar} 后仍会留出右侧区域时，整栏收起。
     */
    private void applyHideRedpacketRecordEntryInTitleBar() {
        if (!hideRedpacketRecordEntry) {
            return;
        }
        View layout = findViewById(com.chat.base.R.id.titleRightLayout);
        if (layout != null) {
            layout.setVisibility(View.GONE);
        }
        View iv = findViewById(com.chat.base.R.id.titleRightIv);
        if (iv != null) {
            iv.setVisibility(View.GONE);
        }
        TextView tv = findViewById(com.chat.base.R.id.titleRightTv);
        if (tv != null) {
            tv.setVisibility(View.GONE);
        }
    }

    /**
     * 领取记录按领取时间倒序：最新在上、最早在下。
     */
    private static void sortRecordsByClaimTimeDesc(List<RedPacketRecord> list) {
        if (list == null || list.size() <= 1) {
            return;
        }
        Collections.sort(list, (a, b) -> Long.compare(
                parseClaimTimeMillis(b != null ? b.created_at : null),
                parseClaimTimeMillis(a != null ? a.created_at : null)));
    }

    private static long parseClaimTimeMillis(String createdAt) {
        if (TextUtils.isEmpty(createdAt)) {
            return 0L;
        }
        String s = createdAt.trim();
        if (s.endsWith("Z")) {
            s = s.substring(0, s.length() - 1);
        }
        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss.SSS",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss",
        };
        for (String p : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(p, Locale.US);
                sdf.setLenient(false);
                return sdf.parse(s).getTime();
            } catch (ParseException ignored) {
            }
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sdf.setLenient(false);
            return sdf.parse(s).getTime();
        } catch (ParseException e) {
            return 0L;
        }
    }
}
