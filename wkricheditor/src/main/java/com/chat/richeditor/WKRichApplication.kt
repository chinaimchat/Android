package com.chat.richeditor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.chat.base.WKBaseApplication
import com.chat.base.endpoint.EndpointCategory
import com.chat.base.endpoint.EndpointManager
import com.chat.base.endpoint.entity.ChatItemPopupMenu
import com.chat.base.endpoint.entity.ChatItemPopupMenu.IPopupItemClick
import com.chat.base.endpoint.entity.MsgConfig
import com.chat.base.msg.IConversationContext
import com.chat.base.msgitem.WKContentType
import com.chat.base.msgitem.WKMsgItemViewManager
import com.chat.base.utils.WKToastUtils
import com.chat.richeditor.msg.RichChatProvider
import com.chat.richeditor.msg.RichTextContent
import com.chat.richeditor.ui.EditActivity
import com.xinbida.wukongim.WKIM
import com.xinbida.wukongim.entity.WKMsg
import com.xinbida.wukongim.msgmodel.WKMessageContent

class WKRichApplication private constructor() {
    private object SingletonInstance {
        val INSTANCE = WKRichApplication()
    }

    companion object {
        val instance: WKRichApplication
            get() = SingletonInstance.INSTANCE
    }

    lateinit var iConversationContext: IConversationContext
    fun init() {
        val appModule = WKBaseApplication.getInstance().getAppModuleWithSid("rich")
        if (!WKBaseApplication.getInstance().appModuleIsInjection(appModule)) return

        WKIM.getInstance().msgManager.registerContentMsg(RichTextContent::class.java)
        WKMsgItemViewManager.getInstance()
            .addChatItemViewProvider(WKContentType.richText, RichChatProvider())
        EndpointManager.getInstance()
            .setMethod(EndpointCategory.msgConfig + WKContentType.richText) { MsgConfig(true) }

        EndpointManager.getInstance().setMethod(
            "show_rich_edit"
        ) { `object` ->
            iConversationContext = `object` as IConversationContext
            val intent = Intent(iConversationContext.chatActivity, EditActivity::class.java)
            iConversationContext.chatActivity.startActivity(intent)
            null
        }
        EndpointManager.getInstance()
            .setMethod("", EndpointCategory.chatShowBubble) { `object`: Any ->
                val type = `object` as Int
                type == WKContentType.richText
            }
        EndpointManager.getInstance().setMethod(
            "rich_text", EndpointCategory.wkChatPopupItem, 30
        ) { `object`: Any ->
            val msg =
                `object` as WKMsg
            if (msg.type == WKContentType.richText) {
                return@setMethod ChatItemPopupMenu(R.mipmap.msg_copy,
                    WKBaseApplication.getInstance().context.getString(R.string.copy),
                    object : IPopupItemClick {
                        override fun onClick(
                            mMsg: WKMsg,
                            iConversationContext: IConversationContext
                        ) {
                            val richTextContent = mMsg.baseContentMsgModel as RichTextContent
                            val content = richTextContent.displayContent
                            val cm =
                                iConversationContext.chatActivity
                                    .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val mClipData = ClipData.newPlainText("Label", content)
                            cm.setPrimaryClip(mClipData)
                            WKToastUtils.getInstance()
                                .showToastNormal(
                                    iConversationContext.chatActivity
                                        .getString(R.string.copyed)
                                )

                        }
                    })
            }
            null
        }
    }

    fun sendMsg(contentModel: WKMessageContent) {
        iConversationContext.sendMessage(contentModel)
    }
}