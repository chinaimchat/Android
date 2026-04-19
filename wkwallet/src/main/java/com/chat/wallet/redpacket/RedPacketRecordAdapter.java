package com.chat.wallet.redpacket;

import android.widget.ImageView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.base.config.WKApiConfig;
import com.chat.base.glide.GlideUtils;
import com.chat.wallet.R;
import com.chat.wallet.entity.RedPacketRecord;
import com.chat.wallet.util.WalletDisplayNameHelper;
import com.xinbida.wukongim.entity.WKChannelType;

import org.jetbrains.annotations.NotNull;

public class RedPacketRecordAdapter extends BaseQuickAdapter<RedPacketRecord, BaseViewHolder> {

    private String chatChannelId;
    private int chatChannelType = WKChannelType.PERSONAL;
    /** 领取记录总数；仅 1 个红包时不展示「手气最佳」 */
    private int totalCount = 0;

    public RedPacketRecordAdapter() {
        super(R.layout.item_redpacket_record);
    }

    /** 与进入详情时的会话一致，用于群成员昵称解析 */
    public void setResolveContext(String channelId, int channelType) {
        this.chatChannelId = channelId;
        this.chatChannelType = channelType;
    }

    public void setRedPacketTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    @Override
    protected void convert(@NotNull BaseViewHolder h, RedPacketRecord item) {
        String name = WalletDisplayNameHelper.displayNameForUid(item.uid, chatChannelId, chatChannelType);
        h.setText(R.id.nameTv, name);
        h.setText(R.id.amountTv, String.format("¥%.2f", item.amount));
        h.setText(R.id.timeTv, item.created_at != null ? item.created_at : "");
        boolean showBestLuck = item.is_best && totalCount > 1;
        h.setGone(R.id.bestLuckTv, !showBestLuck);
        ImageView iv = h.getView(R.id.avatarIv);
        GlideUtils.getInstance().showAvatarImg(iv.getContext(), WKApiConfig.getAvatarUrl(item.uid), item.uid, iv);
    }
}
