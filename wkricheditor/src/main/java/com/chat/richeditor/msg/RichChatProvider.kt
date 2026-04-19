package com.chat.richeditor.msg

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.chat.base.msg.ChatAdapter
import com.chat.base.msgitem.WKChatBaseProvider
import com.chat.base.msgitem.WKChatIteMsgFromType
import com.chat.base.msgitem.WKContentType
import com.chat.base.msgitem.WKUIChatMsgItemEntity
import com.chat.base.views.BubbleLayout
import com.chat.richeditor.R
import com.chat.richeditor.component.span.ClickableMovementMethod
import java.util.*

class RichChatProvider : WKChatBaseProvider() {
    override fun getChatViewItem(parentView: ViewGroup, from: WKChatIteMsgFromType): View? {
        return LayoutInflater.from(context).inflate(R.layout.chat_chat_rich, parentView, false)
    }

    override fun convert(
        helper: BaseViewHolder,
        item: WKUIChatMsgItemEntity,
        payloads: List<Any>
    ) {
        val msgItemEntity = payloads[0] as WKUIChatMsgItemEntity
        val textView = helper.getView<TextView>(R.id.contentTv)
        val richTextContent = msgItemEntity.wkMsg.baseContentMsgModel as RichTextContent
        textView.text = richTextContent.getShowSpan(
            context, true, msgItemEntity.wkMsg.clientMsgNO,
            (Objects.requireNonNull(getAdapter()) as ChatAdapter)
        )
    }

    override fun setData(
        adapterPosition: Int,
        parentView: View,
        uiChatMsgItemEntity: WKUIChatMsgItemEntity,
        from: WKChatIteMsgFromType
    ) {
        val receivedTextNameTv = parentView.findViewById<TextView>(R.id.receivedTextNameTv)
        val contentLayout = parentView.findViewById<LinearLayout>(R.id.contentLayout)
        val contentTvLayout = parentView.findViewById<BubbleLayout>(R.id.contentTvLayout)
        val textView = parentView.findViewById<TextView>(R.id.contentTv)
        val bgType = getMsgBgType(
            uiChatMsgItemEntity.previousMsg,
            uiChatMsgItemEntity.wkMsg,
            uiChatMsgItemEntity.nextMsg
        )
        contentTvLayout.setAll(bgType, from, WKContentType.WK_TEXT)
        if (from == WKChatIteMsgFromType.SEND) {
            receivedTextNameTv.visibility = View.GONE
            contentLayout.gravity = Gravity.END
            textView.setBackgroundResource(R.drawable.send_chat_text_bg)
            textView.setTextColor(ContextCompat.getColor(context, R.color.send_text_color))
        } else {
            setFromName(uiChatMsgItemEntity, from, receivedTextNameTv)
            contentLayout.gravity = Gravity.START
            textView.setBackgroundResource(R.drawable.received_chat_text_bg)
            textView.setTextColor(ContextCompat.getColor(context, R.color.receive_text_color))
        }
        if (uiChatMsgItemEntity.wkMsg.baseContentMsgModel != null && uiChatMsgItemEntity.wkMsg.baseContentMsgModel is RichTextContent) {
            val richTextContent = uiChatMsgItemEntity.wkMsg.baseContentMsgModel as RichTextContent
            textView.text = richTextContent.getShowSpan(
                context, false, uiChatMsgItemEntity.wkMsg.clientMsgNO,
                (getAdapter() as ChatAdapter)
            )
            addLongClick(contentTvLayout, uiChatMsgItemEntity)
            addLongClick(textView, uiChatMsgItemEntity)
            textView.movementMethod = ClickableMovementMethod.getInstance()
        }
    }

    override fun resetCellListener(
        position: Int,
        parentView: View,
        uiChatMsgItemEntity: WKUIChatMsgItemEntity,
        from: WKChatIteMsgFromType
    ) {
        val contentTvLayout = parentView.findViewById<BubbleLayout>(R.id.contentTvLayout)
        val textView = parentView.findViewById<TextView>(R.id.contentTv)
        super.resetCellListener(position, parentView, uiChatMsgItemEntity, from)
        addLongClick(contentTvLayout, uiChatMsgItemEntity)
        addLongClick(textView, uiChatMsgItemEntity)
    }

    override fun resetCellBackground(
        parentView: View,
        uiChatMsgItemEntity: WKUIChatMsgItemEntity,
        from: WKChatIteMsgFromType
    ) {
        super.resetCellBackground(parentView, uiChatMsgItemEntity, from)
        val contentTvLayout = parentView.findViewById<BubbleLayout>(R.id.contentTvLayout)
        val textContentLayout = parentView.findViewById<View>(R.id.textContentLayout)
        val msgTimeView = parentView.findViewById<View>(R.id.msgTimeView)
        // 这里要指定文本宽度 - padding的距离
        textContentLayout.layoutParams.width = getViewWidth(from, uiChatMsgItemEntity)
        val bgType = getMsgBgType(
            uiChatMsgItemEntity.previousMsg,
            uiChatMsgItemEntity.wkMsg,
            uiChatMsgItemEntity.nextMsg
        )
        contentTvLayout.setAll(bgType, from, WKContentType.WK_TEXT)
        if (textContentLayout.layoutParams.width < msgTimeView.layoutParams.width) {
            textContentLayout.layoutParams.width = msgTimeView.layoutParams.width
        }
    }


    override fun resetFromName(
        position: Int,
        parentView: View,
        uiChatMsgItemEntity: WKUIChatMsgItemEntity,
        from: WKChatIteMsgFromType
    ) {
        val receivedTextNameTv = parentView.findViewById<TextView>(R.id.receivedTextNameTv)
        setFromName(uiChatMsgItemEntity, from, receivedTextNameTv)
    }

    override val itemViewType: Int
        get() = WKContentType.richText
}