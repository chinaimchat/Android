package com.chat.rtc.msg;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.RTCMenu;
import com.chat.base.msg.ChatAdapter;
import com.chat.base.msgitem.WKChatBaseProvider;
import com.chat.base.msgitem.WKChatIteMsgFromType;
import com.chat.base.msgitem.WKMsgBgType;
import com.chat.base.msgitem.WKUIChatMsgItemEntity;
import com.chat.base.ui.Theme;
import com.chat.base.views.BubbleLayout;
import com.chat.rtc.R;
import rtc.WKRTCCallType;

import java.util.Objects;


/**
 * 5/17/21 11:30 AM
 * 两人音视频
 */
public class P2PRTCProviderItem extends WKChatBaseProvider {
    @Override
    protected View getChatViewItem(@NonNull ViewGroup parentView, @NonNull WKChatIteMsgFromType from) {
        return LayoutInflater.from(getContext()).inflate(R.layout.chat_item_p2p_video_call, parentView, false);
    }

    @Override
    protected void setData(int adapterPosition, View parentView, WKUIChatMsgItemEntity uiChatMsgItemEntity, @NonNull WKChatIteMsgFromType from) {
        LinearLayout contentLayout = parentView.findViewById(R.id.contentLayout);
        BubbleLayout callView = parentView.findViewById(R.id.callView);
        AppCompatImageView typeIv = parentView.findViewById(R.id.typeIv);
        TextView contentTv = parentView.findViewById(R.id.contentTv);
        RTCMsgContent RTCMsgContent = (RTCMsgContent) uiChatMsgItemEntity.wkMsg.baseContentMsgModel;
        if (from == WKChatIteMsgFromType.RECEIVED) {
            contentLayout.setGravity(Gravity.START);
            Theme.setColorFilter(getContext(), typeIv, R.color.receive_text_color);
            contentTv.setTextColor(ContextCompat.getColor(getContext(), R.color.receive_text_color));
            typeIv.setImageResource(RTCMsgContent.callType == WKRTCCallType.audio ? R.mipmap.chat_calls_voice : R.mipmap.chat_calls_video);
        } else {
            contentLayout.setGravity(Gravity.END);
            contentTv.setTextColor(ContextCompat.getColor(getContext(), R.color.send_text_color));
            Theme.setColorFilter(getContext(), typeIv, R.color.send_text_color);
            typeIv.setImageResource(RTCMsgContent.callType == WKRTCCallType.audio ? R.mipmap.chat_calls_voice : R.mipmap.chat_calls_video);
        }
        WKMsgBgType bgType = getMsgBgType(uiChatMsgItemEntity.previousMsg, uiChatMsgItemEntity.wkMsg, uiChatMsgItemEntity.nextMsg);
        callView.setAll(bgType, from, uiChatMsgItemEntity.wkMsg.type);
        String content = "";
        if (RTCMsgContent.resultType == WKCallResultType.hangup) {
            // 正常通话结束
            content = String.format(getContext().getString(R.string.call_time), getTotalTime(RTCMsgContent.second * 1000L));
        } else if (RTCMsgContent.resultType == WKCallResultType.cancel) {
            // 取消
            if (from == WKChatIteMsgFromType.RECEIVED) {
                content = getContext().getString(R.string.caller_cancel);
            } else content = getContext().getString(R.string.my_cancel);
        } else if (RTCMsgContent.resultType == WKCallResultType.missed) {
            if (from == WKChatIteMsgFromType.RECEIVED) {
                content = getContext().getString(R.string.caller_missed);
            } else content = getContext().getString(R.string.my_missed);
        } else if (RTCMsgContent.resultType == WKCallResultType.refuse) {
            // 拒绝
            if (from == WKChatIteMsgFromType.RECEIVED) {
                content = getContext().getString(R.string.call_declined);
            } else {
                content = getContext().getString(R.string.caller_declined);
            }
        }
        contentTv.setText(content);
        addLongClick(callView, uiChatMsgItemEntity);
        callView.setOnClickListener(v -> EndpointManager.getInstance().invoke("wk_p2p_call", new RTCMenu(((ChatAdapter) Objects.requireNonNull(getAdapter())).getConversationContext(), RTCMsgContent.callType)));
    }

    @Override
    public int getItemViewType() {
        return WKRTCType.WK_P2P_CALL;
    }

    public String getTotalTime(long totalDuration) {
        long minute = totalDuration / 1000 / 60;
        long second = (totalDuration / 1000) % 60;
        String showM = String.valueOf(minute);
        if (minute < 10) {
            showM = "0" + minute;
        }
        String showS = String.valueOf(second);
        if (second < 10) {
            showS = "0" + second;
        }
        String finalShowM = showM;
        String finalShowS = showS;
        return String.format("%s:%s", finalShowM, finalShowS);
    }

    @Override
    public void resetCellBackground(@NonNull View parentView, @NonNull WKUIChatMsgItemEntity uiChatMsgItemEntity, @NonNull WKChatIteMsgFromType from) {
        super.resetCellBackground(parentView, uiChatMsgItemEntity, from);
        BubbleLayout callView = parentView.findViewById(R.id.callView);
        WKMsgBgType bgType = getMsgBgType(uiChatMsgItemEntity.previousMsg, uiChatMsgItemEntity.wkMsg, uiChatMsgItemEntity.nextMsg);
        callView.setAll(bgType, from, uiChatMsgItemEntity.wkMsg.type);
    }
}
