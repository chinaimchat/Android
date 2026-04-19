package com.chat.pinned.message.ui

import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.chat.base.base.WKBaseActivity
import com.chat.base.config.WKConfig
import com.chat.base.config.WKSharedPreferencesUtil
import com.chat.base.endpoint.EndpointManager
import com.chat.base.endpoint.entity.SetChatBgMenu
import com.chat.base.endpoint.entity.WKMsg2UiMsgMenu
import com.chat.base.msg.ChatAdapter
import com.chat.base.msg.IConversationContext
import com.chat.base.msgitem.WKChannelMemberRole
import com.chat.base.msgitem.WKContentType
import com.chat.base.msgitem.WKUIChatMsgItemEntity
import com.chat.base.net.HttpResponseCode
import com.chat.base.utils.AndroidUtilities
import com.chat.base.utils.WKDialogUtils
import com.chat.base.utils.WKReader
import com.chat.base.utils.WKTimeUtils
import com.chat.pinned.message.Const
import com.chat.pinned.message.R
import com.chat.pinned.message.databinding.ActPinnedMessageListLayoutBinding
import com.chat.pinned.message.db.PinnedMessageDB
import com.chat.pinned.message.service.PinnedMsgModel
import com.xinbida.wukongim.WKIM
import com.xinbida.wukongim.entity.WKChannel
import com.xinbida.wukongim.entity.WKChannelType
import com.xinbida.wukongim.entity.WKMsg
import com.xinbida.wukongim.msgmodel.WKMessageContent

class PinnedMessageListActivity : WKBaseActivity<ActPinnedMessageListLayoutBinding>(),
    IConversationContext {
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var channelId: String
    private var channelType: Byte = 0
    private lateinit var titleTv: TextView
    private var msgList = ArrayList<WKMsg>()
    override fun getViewBinding(): ActPinnedMessageListLayoutBinding {
        return ActPinnedMessageListLayoutBinding.inflate(layoutInflater)
    }

    override fun setTitle(titleTv: TextView?) {
        this.titleTv = titleTv!!
    }

    override fun initView() {
        channelId = intent.getStringExtra("channel_id")!!
        channelType = intent.getByteExtra("channel_type", 0)
        wkVBinding.clearAllTV.typeface = AndroidUtilities.getTypeface("fonts/rmedium.ttf")
        chatAdapter =
            ChatAdapter(this@PinnedMessageListActivity, ChatAdapter.AdapterType.pinnedMessage)
        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        wkVBinding.recyclerView.layoutManager = linearLayoutManager
        wkVBinding.recyclerView.adapter = chatAdapter
        chatAdapter.isAnimationFirstOnly = true
        chatAdapter.animationEnable = false
        EndpointManager.getInstance().invoke(
            "set_chat_bg",
            SetChatBgMenu(
                channelId,
                channelType,
                wkVBinding.imageView,
                wkVBinding.rootLayout,
                wkVBinding.blurView
            )
        )

    }

    override fun initListener() {
        wkVBinding.clearAllTV.setOnClickListener {
            var showDialog = false
            if (channelType == WKChannelType.PERSONAL) {
                showDialog = true
            } else {
                val member = WKIM.getInstance().channelMembersManager.getMember(
                    channelId,
                    channelType,
                    WKConfig.getInstance().uid
                )
                if (member != null && member.role != WKChannelMemberRole.normal) {
                    showDialog = true
                }
            }
            if (showDialog) {
                WKDialogUtils.getInstance().showDialog(
                    this@PinnedMessageListActivity,
                    getString(R.string.clear_all_pinned_messages),
                    getString(R.string.clear_all_pinned_messages_alert_content),
                    true,
                    getString(R.string.cancel),
                    getString(R.string.sure),
                    0,
                    0
                ) { index ->
                    if (index == 1) {
                        PinnedMsgModel.instance.clear(
                            channelId, channelType
                        ) { code, msg ->
                            if (code == HttpResponseCode.success.toInt()) {
                                EndpointManager.getInstance()
                                    .invoke("hide_pinned_view", null)
                                finish()
                            } else {
                                showToast(msg)
                            }
                        }
                    }
                }
            } else {
                WKSharedPreferencesUtil.getInstance().putIntWithUID(Const.getHideChannelPinnedMsgKey(channelId,channelType), 1)
                EndpointManager.getInstance().invoke("reset_channel_all_pinned_msg", null)
                EndpointManager.getInstance()
                    .invoke("hide_pinned_view", null)
                finish()
            }
        }
    }

    override fun initData() {
        val pinnedMsgList =
            PinnedMessageDB.instance.queryPinnedMessage(channelId, channelType.toInt())
        if (WKReader.isNotEmpty(pinnedMsgList)) {
            val msgIds: ArrayList<String> = ArrayList()
            for (pinnedMsg in pinnedMsgList) {
                msgIds.add(pinnedMsg.message_id)
            }
            var tempMsgList =
                WKIM.getInstance().msgManager.getWithMessageIDs(msgIds)
            if (WKReader.isNotEmpty(tempMsgList)) {
                tempMsgList = tempMsgList.sortedBy { it.messageSeq }
                for (msg in tempMsgList) {
                    if (msg.isDeleted == 1 || (msg.remoteExtra != null && msg.remoteExtra.isMutualDeleted == 1)) {
                        continue
                    }
                    msgList.add(msg)
                }
            }
            if (WKReader.isNotEmpty(msgList)) {
                AndroidUtilities.runOnUIThread {
                    titleTv.text =
                        String.format(getString(R.string.pinned_messages_count), msgList.size)
                    var preMsgTime = chatAdapter.lastTimeMsg
                    val list: ArrayList<WKUIChatMsgItemEntity> = ArrayList()
                    var i = 0
                    val size: Int = msgList.size
                    while (i < size) {
                        if (!WKTimeUtils.getInstance().isSameDay(
                                msgList[i].timestamp,
                                preMsgTime
                            ) && msgList[i].type != WKContentType.emptyView && msgList[i].type != WKContentType.spanEmptyView
                        ) {
                            //显示聊天时间
                            val uiChatMsgEntity = WKUIChatMsgItemEntity(
                                this@PinnedMessageListActivity,
                                WKMsg(),
                                null
                            )
                            uiChatMsgEntity.wkMsg.type = WKContentType.msgPromptTime
                            uiChatMsgEntity.wkMsg.content = WKTimeUtils.getInstance()
                                .getShowDate(msgList[i].timestamp * 1000)
                            uiChatMsgEntity.wkMsg.timestamp = msgList[i].timestamp
                            list.add(uiChatMsgEntity)
                        }
                        preMsgTime = msgList[i].timestamp
                        val uiMsg: WKUIChatMsgItemEntity? =
                            EndpointManager.getInstance().invoke(
                                "get_chat_uid_msg",
                                WKMsg2UiMsgMenu(
                                    this@PinnedMessageListActivity,
                                    msgList[i],
                                    0,
                                    false,
                                    chatAdapter.isShowChooseItem
                                )
                            ) as WKUIChatMsgItemEntity?
                        uiMsg?.isShowPinnedMessage = true
                        if (uiMsg != null) {
                            list.add(uiMsg)
                        }
                        i++
                    }
                    chatAdapter.setList(list)
                    val linearLayoutManager = wkVBinding.recyclerView.layoutManager
                    linearLayoutManager!!.scrollToPosition(chatAdapter.itemCount - 1)
                }
            } else {
                EndpointManager.getInstance()
                    .invoke("hide_pinned_view", null)
            }
        }

    }

    override fun sendMessage(wkMessageContent: WKMessageContent?) {

    }

    override fun getChatChannelInfo(): WKChannel {
        var channel = WKIM.getInstance().channelManager.getChannel(
            channelId,
            channelType
        )
        if (channel == null) {
            channel = WKChannel(channelId, channelType)
        }
        return channel
    }

    override fun showMultipleChoice() {
    }

    override fun setTitleRightText(text: String?) {
    }

    override fun showReply(wkMsg: WKMsg?) {
    }

    override fun showEdit(wkMsg: WKMsg?) {
    }

    override fun tipsMsg(clientMsgNo: String?) {
    }

    override fun setEditContent(content: String?) {
    }

    override fun getChatActivity(): AppCompatActivity {
        return this
    }

    override fun getReplyMsg(): WKMsg? {
        return null
    }

    override fun hideSoftKeyboard() {
    }

    override fun getChatAdapter(): ChatAdapter {
        return chatAdapter
    }

    override fun sendCardMsg() {
    }

    override fun chatRecyclerViewScrollToEnd() {
    }

    override fun deleteOperationMsg() {
    }

    override fun onChatAvatarClick(uid: String?, isLongClick: Boolean) {
    }

    override fun onViewPicture(isViewing: Boolean) {

    }

    override fun onMsgViewed(wkMsg: WKMsg?, position: Int) {
    }

    override fun getRecyclerViewLayout(): View {
        return wkVBinding.recyclerView
    }

    override fun isShowChatActivity(): Boolean {
        return true
    }

    override fun closeActivity() {
        finish()
    }
}