package com.chat.wallet.transfer;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.config.WKConfig;
import com.chat.base.net.IRequestResultListener;
import com.chat.wallet.R;
import com.chat.wallet.api.WalletModel;
import com.chat.wallet.databinding.ActivityTransferDetailBinding;
import com.chat.wallet.entity.TransferDetailResp;
import com.chat.wallet.entity.TransferSendResp;
import com.chat.wallet.msg.WKTransferContent;
import com.chat.wallet.util.TransferLocalSync;

public class TransferDetailActivity extends WKBaseActivity<ActivityTransferDetailBinding> {
    private String transferNo;
    @Nullable
    private String channelId;
    private byte channelType;
    @Nullable
    private String clientMsgNo;

    @Override protected ActivityTransferDetailBinding getViewBinding(){return ActivityTransferDetailBinding.inflate(getLayoutInflater());}
    @Override protected void setTitle(TextView t){t.setText(R.string.transfer_detail);}
    @Override protected void initView(){
        transferNo = getIntent().getStringExtra("transfer_no");
        channelId = getIntent().getStringExtra("channel_id");
        channelType = (byte) getIntent().getIntExtra("channel_type", 0);
        clientMsgNo = getIntent().getStringExtra("client_msg_no");
    }
    @Override protected void initListener(){wkVBinding.acceptBtn.setOnClickListener(v->accept());}
    @Override protected void initData(){if(transferNo!=null)load();}
    private void load(){
        WalletModel.getInstance().getTransferDetail(transferNo,new IRequestResultListener<TransferDetailResp>(){
            @Override public void onSuccess(TransferDetailResp r){
                wkVBinding.amountTv.setText(String.format("¥%.2f",r.amount));
                wkVBinding.remarkTv.setText(r.remark!=null?r.remark:"");
                String myUid=WKConfig.getInstance().getUid();
                switch(r.status_code){
                    case 0:wkVBinding.statusTv.setText(R.string.transfer_pending);if(r.to_uid!=null&&r.to_uid.equals(myUid))wkVBinding.acceptBtn.setVisibility(View.VISIBLE);break;
                    case 1:wkVBinding.statusTv.setText(R.string.transfer_accepted);wkVBinding.acceptBtn.setVisibility(View.GONE);break;
                    case 2:wkVBinding.statusTv.setText(R.string.transfer_refunded);wkVBinding.acceptBtn.setVisibility(View.GONE);break;
                }
                syncLocalTransferBubbleFromDetail(r);
            }
            @Override public void onFail(int c,String m){showToast(m!=null?m:getString(R.string.wallet_load_fail));}
        });
    }

    /** 与红包一致：详情态与本地气泡对齐，避免已收款后回聊天气泡仍显示待接收。 */
    private void syncLocalTransferBubbleFromDetail(TransferDetailResp r) {
        if (TextUtils.isEmpty(transferNo)) {
            return;
        }
        String effChannelId = !TextUtils.isEmpty(channelId) ? channelId : r.channel_id;
        byte effChannelType = channelType != 0
                ? channelType
                : (r.channel_type != null ? r.channel_type.byteValue() : 0);
        if (r.status_code == 1) {
            TransferLocalSync.applyStatus(
                    transferNo, effChannelId, effChannelType, clientMsgNo, WKTransferContent.STATUS_ACCEPTED);
        } else if (r.status_code == 2) {
            TransferLocalSync.applyStatus(
                    transferNo, effChannelId, effChannelType, clientMsgNo, WKTransferContent.STATUS_REFUNDED);
        }
    }

    private void accept(){
        WalletModel.getInstance().acceptTransfer(transferNo, new IRequestResultListener<TransferSendResp>() {
            @Override public void onSuccess(TransferSendResp r) {
                if (!TextUtils.isEmpty(transferNo)) {
                    int local = WKTransferContent.statusFromApiStatusCode(
                            r != null ? r.getResolvedStatusCode() : 0);
                    if (local == WKTransferContent.STATUS_PENDING) {
                        local = WKTransferContent.STATUS_ACCEPTED;
                    }
                    TransferLocalSync.applyStatus(
                            transferNo,
                            channelId,
                            channelType,
                            clientMsgNo,
                            local);
                }
                showToast(getString(R.string.wallet_accept_success));
                load();
            }

            @Override public void onFail(int c, String m) {
                showToast(m != null ? m : getString(R.string.wallet_accept_fail));
            }
        });
    }
}
