package com.chat.wallet.redpacket;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chat.base.net.IRequestResultListener;
import com.chat.base.utils.WKToastUtils;
import com.chat.wallet.R;
import com.chat.wallet.api.WalletModel;
import com.chat.wallet.entity.RedPacketDetailResp;
import com.chat.wallet.entity.RedPacketOpenResp;
import com.chat.wallet.util.RedPacketLocalSync;

public class OpenRedPacketDialog extends Dialog {
    private final String packetNo;
    private final String senderName;
    private final String remark;
    @Nullable
    private final String channelId;
    private final byte channelType;
    private final int packetType;
    @Nullable
    private final String clientMsgNo;

    public OpenRedPacketDialog(@NonNull Context ctx, String packetNo, String senderName, String remark,
                               @Nullable String channelId, byte channelType,
                               int packetType, @Nullable String clientMsgNo) {
        super(ctx, android.R.style.Theme_Material_Dialog_NoActionBar);
        this.packetNo = packetNo;
        this.senderName = senderName;
        this.remark = remark;
        this.channelId = channelId;
        this.channelType = channelType;
        this.packetType = packetType;
        this.clientMsgNo = clientMsgNo;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_open_redpacket);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        ((TextView) findViewById(R.id.senderNameTv)).setText(senderName != null ? senderName + getContext().getString(R.string.wallet_redpacket_suffix) : getContext().getString(R.string.redpacket));
        ((TextView) findViewById(R.id.remarkTv)).setText(remark != null && !remark.isEmpty() ? remark : getContext().getString(R.string.redpacket_remark));
        View openBtn = findViewById(R.id.openBtn);
        openBtn.setOnTouchListener((v, e) -> {
            int a = e.getAction();
            if (a == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).start();
            } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
            }
            return false;
        });
        openBtn.setOnClickListener(v ->
                WalletModel.getInstance().openRedPacket(packetNo, new IRequestResultListener<RedPacketOpenResp>() {
                    @Override
                    public void onSuccess(RedPacketOpenResp r) {
                        int st = r != null ? r.getLocalMessageStatusAfterOpenSuccess() : 1;
                        RedPacketLocalSync.applyOpened(
                                packetNo,
                                channelId,
                                channelType,
                                clientMsgNo,
                                senderName,
                                remark,
                                packetType,
                                st);
                        dismiss();
                        final String toastText = buildClaimSuccessToast(r);
                        final Context appCtx = getContext().getApplicationContext();
                        Handler h = new Handler(Looper.getMainLooper());
                        // 系统 Toast + 延迟，避免与 dismiss/跳转抢主线程导致不显示
                        h.postDelayed(() -> Toast.makeText(appCtx, toastText, Toast.LENGTH_LONG).show(), 150);
                        h.postDelayed(() -> OpenRedPacketDialog.this.startRedPacketDetail(), 450);
                        refineLocalMessageFromDetail();
                    }

                    @Override
                    public void onFail(int c, String m) {
                        dismiss();
                        WKToastUtils.getInstance().showToastFail(m != null ? m : getContext().getString(R.string.wallet_open_fail));
                        startRedPacketDetail();
                    }
                })
        );
    }

    /** 详情与 open 字段可能不一致时再写回一次本地消息，保证气泡状态与后台一致。 */
    private void refineLocalMessageFromDetail() {
        if (TextUtils.isEmpty(packetNo)) {
            return;
        }
        WalletModel.getInstance().getRedPacketDetail(packetNo, new IRequestResultListener<RedPacketDetailResp>() {
            @Override
            public void onSuccess(RedPacketDetailResp d) {
                int st = RedPacketLocalSync.statusFromDetail(d);
                RedPacketLocalSync.applyOpened(
                        packetNo,
                        channelId,
                        channelType,
                        clientMsgNo,
                        senderName,
                        remark,
                        packetType,
                        st);
            }

            @Override
            public void onFail(int c, String m) {
            }
        });
    }

    private String buildClaimSuccessToast(RedPacketOpenResp r) {
        Context ctx = getContext();
        double amt = r != null ? r.getResolvedAmount() : Double.NaN;
        if (!Double.isNaN(amt) && amt > 0) {
            return ctx.getString(R.string.wallet_redpacket_you_claimed_with_amount, amt);
        }
        return ctx.getString(R.string.wallet_redpacket_you_claimed);
    }

    private void startRedPacketDetail() {
        Intent i = new Intent(getContext(), RedPacketDetailActivity.class)
                .putExtra("packet_no", packetNo)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (!TextUtils.isEmpty(channelId)) {
            i.putExtra("channel_id", channelId);
            i.putExtra("channel_type", (int) channelType);
        }
        getContext().startActivity(i);
    }

    @Override
    protected void onStart() {
        super.onStart();
        View root = findViewById(R.id.rpDialogRoot);
        if (root != null) {
            root.setAlpha(0f);
            root.setScaleX(0.88f);
            root.setScaleY(0.88f);
            root.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(260)
                    .setInterpolator(new DecelerateInterpolator(1.4f))
                    .start();
        }
    }
}
