package com.chat.rtc.msg;

import android.text.SpannableString;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.base.msgitem.WKChatBaseProvider;
import com.chat.base.msgitem.WKChatIteMsgFromType;
import com.chat.base.msgitem.WKContentType;
import com.chat.base.msgitem.WKUIChatMsgItemEntity;
import com.chat.base.ui.components.SystemMsgBackgroundColorSpan;
import com.chat.base.utils.AndroidUtilities;
import com.chat.rtc.R;

import org.jetbrains.annotations.NotNull;

/**
 * 5/17/21 12:25 PM
 * 多人音视频
 */
public class MultiRTCProviderItem extends WKChatBaseProvider {

    @Override
    public int getLayoutId() {
        return R.layout.chat_system_layout;
    }

    @Override
    protected View getChatViewItem(@NonNull ViewGroup parentView, @NonNull WKChatIteMsgFromType from) {
        return null;
    }

    @Override
    protected void setData(int adapterPosition, @NonNull View parentView, @NonNull WKUIChatMsgItemEntity liMMessageContent, @NonNull WKChatIteMsgFromType from) {

    }

    @Override
    public void convert(@NotNull BaseViewHolder baseViewHolder, @NonNull WKUIChatMsgItemEntity liMMessageContent) {
        super.convert(baseViewHolder, liMMessageContent);
        MultiMsgContent multiMsgContent = (MultiMsgContent) liMMessageContent.wkMsg.baseContentMsgModel;
//        baseViewHolder.setText(R.id.contentTv, liMMultiMsgContent.content);
        TextView textView = baseViewHolder.getView(R.id.contentTv);
        textView.setShadowLayer(AndroidUtilities.dp(5f), 0f, 0f, 0);
        SpannableString str = new SpannableString(multiMsgContent.content);
        str.setSpan(new SystemMsgBackgroundColorSpan(ContextCompat.getColor(context, R.color.colorSystemBg), AndroidUtilities.dp(5), AndroidUtilities.dp(2*5)), 0, multiMsgContent.content.length(), 0);
        textView.setText(str);
    }

    @Override
    public int getItemViewType() {
        return WKContentType.videoCallGroup;
    }
}
