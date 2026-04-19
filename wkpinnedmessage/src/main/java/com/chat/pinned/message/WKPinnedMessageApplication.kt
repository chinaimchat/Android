package com.chat.pinned.message

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.chat.base.WKBaseApplication
import com.chat.base.config.WKApiConfig
import com.chat.base.config.WKConfig
import com.chat.base.config.WKSharedPreferencesUtil
import com.chat.base.emoji.MoonUtil
import com.chat.base.endpoint.EndpointCategory
import com.chat.base.endpoint.EndpointManager
import com.chat.base.endpoint.entity.ChatItemPopupMenu
import com.chat.base.endpoint.entity.DBMenu
import com.chat.base.endpoint.entity.MsgConfig
import com.chat.base.glide.GlideUtils
import com.chat.base.msg.IConversationContext
import com.chat.base.msgitem.WKChannelMemberRole
import com.chat.base.msgitem.WKContentType
import com.chat.base.net.HttpResponseCode
import com.chat.base.ui.Theme
import com.chat.base.ui.components.RadialProgressView
import com.chat.base.utils.AndroidUtilities
import com.chat.base.utils.LayoutHelper
import com.chat.base.utils.WKDialogUtils
import com.chat.base.utils.WKReader
import com.chat.base.utils.WKToastUtils
import com.chat.pinned.message.components.PinnedLineView
import com.chat.pinned.message.db.PinnedMessageDB
import com.chat.pinned.message.service.PinnedMsgModel
import com.chat.pinned.message.ui.PinnedMessageListActivity
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.xinbida.wukongim.WKIM
import com.xinbida.wukongim.entity.WKChannel
import com.xinbida.wukongim.entity.WKChannelType
import com.xinbida.wukongim.entity.WKMsg
import com.xinbida.wukongim.msgmodel.WKImageContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File


object WKPinnedMessageApplication {
    fun init() {
        EndpointManager.getInstance().setMethod(
            "pinned_message_sql",
            EndpointCategory.wkDBMenus
        ) { DBMenu("pinned_message_sql") }
        EndpointManager.getInstance().setMethod(
            "is_register_pin_msg_module"
        ) { true }
        EndpointManager.getInstance().setMethod(
            "pin_message_item", EndpointCategory.wkChatPopupItem, 50
        ) { `object`: Any ->
            val mMsg =
                `object` as WKMsg
            val msgConfigObject: Any? =
                EndpointManager.getInstance().invoke(EndpointCategory.msgConfig + mMsg.type, null)
            var allowMemberPinnedMessage = 0
            if (mMsg.channelType == WKChannelType.GROUP) {
                val channel =
                    WKIM.getInstance().channelManager.getChannel(mMsg.channelID, mMsg.channelType)
                if (channel?.remoteExtraMap != null && channel.remoteExtraMap.containsKey(Const.allowMemberPinnedMessage)) {
                    allowMemberPinnedMessage =
                        channel.remoteExtraMap[Const.allowMemberPinnedMessage] as Int
                }
                val member = WKIM.getInstance().channelMembersManager.getMember(
                    mMsg.channelID,
                    mMsg.channelType,
                    WKConfig.getInstance().uid
                )
                if (allowMemberPinnedMessage == 0) {
                    allowMemberPinnedMessage = if (member != null) {
                        if (member.role == WKChannelMemberRole.normal) {
                            0
                        } else {
                            1
                        }
                    } else {
                        0
                    }
                }
            } else if (mMsg.channelType == WKChannelType.PERSONAL || mMsg.channelType == WKChannelType.CUSTOMER_SERVICE) {
                allowMemberPinnedMessage = 1
            }

            if (msgConfigObject != null && msgConfigObject is MsgConfig && msgConfigObject.isCanShowPinMenu && allowMemberPinnedMessage == 1) {
                val resourceId: Int
                val text: String
                if (mMsg.remoteExtra.isPinned == 1) {
                    resourceId = R.mipmap.msg_unpin
                    text = WKBaseApplication.getInstance().context.getString(R.string.unpin_message)
                } else {
                    resourceId = R.mipmap.msg_pin
                    text = WKBaseApplication.getInstance().context.getString(R.string.pin_message)
                }
                return@setMethod ChatItemPopupMenu(resourceId,
                    text,
                    object : ChatItemPopupMenu.IPopupItemClick {
                        override fun onClick(
                            mMsg: WKMsg,
                            iConversationContext: IConversationContext
                        ) {
                            PinnedMsgModel.instance.pinMessage(
                                mMsg.messageID,
                                mMsg.messageSeq,
                                mMsg.channelID,
                                mMsg.channelType.toInt()
                            ) { code, msg ->
                                if (code != HttpResponseCode.success.toInt()) {
                                    WKToastUtils.getInstance().showToast(msg!!)
                                }
                            }

                        }
                    })
            }

            null
        }
        EndpointManager.getInstance()
            .setMethod(
                "get_pinned_message_view"
            ) { `object` -> getPinnedView(`object` as IConversationContext) }
    }

    private fun getPinnedView(iConversationContext: IConversationContext): View {
        var msgList = ArrayList<WKMsg>()
        var selectedMessageId = ""
        val linearLayout = LinearLayout(iConversationContext.chatActivity)
        linearLayout.orientation = LinearLayout.HORIZONTAL
        linearLayout.background =
            ContextCompat.getDrawable(iConversationContext.chatActivity, R.drawable.layout_bg)

        linearLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val pinnedLineView = PinnedLineView(iConversationContext.chatActivity)
        linearLayout.addView(
            pinnedLineView,
            LayoutHelper.createFrame(
                2,
                50f,
                Gravity.START or Gravity.CENTER_VERTICAL,
                8f,
                0f,
                0f,
                0f
            )
        )
        pinnedLineView.updateColors()
        val imageView = ShapeableImageView(iConversationContext.chatActivity)
        linearLayout.addView(
            imageView, LayoutHelper.createLinear(
                35,
                35,
                Gravity.CENTER_VERTICAL,
                10,
                0,
                0,
                0
            )
        )
        imageView.shapeAppearanceModel = imageView.shapeAppearanceModel
            .toBuilder()
            .setTopLeftCorner(
                CornerFamily.ROUNDED,
                AndroidUtilities.dp(3f).toFloat()
            )
            .setTopRightCorner(
                CornerFamily.ROUNDED,
                AndroidUtilities.dp(3f).toFloat()
            )
            .setBottomRightCorner(
                CornerFamily.ROUNDED,
                AndroidUtilities.dp(3f).toFloat()
            )
            .setBottomLeftCorner(
                CornerFamily.ROUNDED,
                AndroidUtilities.dp(3f).toFloat()
            )
            .build()
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.setImageResource(R.mipmap.shadow_left)
        imageView.visibility = View.GONE
        //CommonAnim.getInstance().showOrHide(imageView, false, false)
        val contentLayout = LinearLayout(iConversationContext.chatActivity)
        contentLayout.orientation = LinearLayout.VERTICAL
        contentLayout.gravity = Gravity.CENTER
        linearLayout.addView(
            contentLayout,
            LayoutHelper.createLinear(
                LayoutHelper.WRAP_CONTENT,
                50, 1.0f,
                Gravity.CENTER_VERTICAL,
                10,
                0,
                0,
                0
            )
        )
        val topTextView = TextView(iConversationContext.chatActivity)
        topTextView.typeface = AndroidUtilities.getTypeface("fonts/rmedium.ttf")
        topTextView.setTextColor(
            ContextCompat.getColor(
                iConversationContext.chatActivity,
                R.color.colorAccent
            )
        )
        topTextView.setText(R.string.pin_message_count)
        topTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)

        contentLayout.addView(
            topTextView,
            LayoutHelper.createLinear(
                LayoutHelper.WRAP_CONTENT,
                LayoutHelper.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL,
                0,
                5,
                0,
                0
            )
        )
        val contentTextView = TextView(iConversationContext.chatActivity)
        contentTextView.setTextColor(
            ContextCompat.getColor(
                iConversationContext.chatActivity,
                R.color.popupTextColor
            )
        )
        contentTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        contentTextView.setSingleLine()
        contentTextView.ellipsize = TextUtils.TruncateAt.END
        contentTextView.maxLines = 1
        contentTextView.text = ""
        contentLayout.addView(
            contentTextView,
            LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL,
                0,
                0,
                0,
                5
            )
        )
        val progressView = RadialProgressView(iConversationContext.chatActivity)
        progressView.setSize(AndroidUtilities.dp(15f))
        progressView.setProgressColor(
            ContextCompat.getColor(
                iConversationContext.chatActivity,
                R.color.colorAccent
            )
        )
        linearLayout.addView(
            progressView,
            LayoutHelper.createLinear(
                20, 20, Gravity.CENTER_VERTICAL,
                10,
                0,
                15,
                0
            )
        )
        progressView.visibility = View.GONE
        val rightIV = AppCompatImageView(iConversationContext.chatActivity)
        linearLayout.addView(
            rightIV,
            LayoutHelper.createLinear(
                LayoutHelper.WRAP_CONTENT,
                LayoutHelper.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL,
                10,
                0,
                15,
                0
            )
        )
        rightIV.setImageResource(R.mipmap.msg_pinnedlist)
        Theme.setPressedBackground(rightIV)
//        rightIV.setImageResource(R.mipmap.ic_close_white)
        rightIV.setColorFilter(
            ContextCompat.getColor(
                iConversationContext.chatActivity,
                R.color.popupTextColor
            )
        )
        rightIV.setOnClickListener {
            if (WKReader.isNotEmpty(msgList) && msgList.size > 1) {
                val intent =
                    Intent(iConversationContext.chatActivity, PinnedMessageListActivity::class.java)
                intent.putExtra("channel_id", iConversationContext.chatChannelInfo.channelID)
                intent.putExtra("channel_type", iConversationContext.chatChannelInfo.channelType)
                iConversationContext.chatActivity.startActivity(intent)
            } else {
                var showDialog = false
                if (iConversationContext.chatChannelInfo.channelType == WKChannelType.PERSONAL) {
                    showDialog = true
                } else {
                    val member = WKIM.getInstance().channelMembersManager.getMember(
                        iConversationContext.chatChannelInfo.channelID,
                        iConversationContext.chatChannelInfo.channelType,
                        WKConfig.getInstance().uid
                    )
                    if (member != null && member.role != WKChannelMemberRole.normal) {
                        showDialog = true
                    }
                }
                if (showDialog) {
                    WKDialogUtils.getInstance().showDialog(
                        iConversationContext.chatActivity,
                        iConversationContext.chatActivity.getString(R.string.clear_all_pinned_messages),
                        iConversationContext.chatActivity.getString(R.string.clear_all_pinned_messages_alert_content),
                        true,
                        iConversationContext.chatActivity.getString(R.string.cancel),
                        iConversationContext.chatActivity.getString(R.string.sure),
                        0,
                        0
                    ) { index ->
                        if (index == 1) {
                            PinnedMsgModel.instance.clear(
                                iConversationContext.chatChannelInfo.channelID,
                                iConversationContext.chatChannelInfo.channelType
                            ) { code, msg ->
                                if (code == HttpResponseCode.success.toInt()) {
                                    EndpointManager.getInstance().invoke("hide_pinned_view", null)
                                } else {
                                    WKToastUtils.getInstance().showToast(msg)
                                }
                            }
                        }
                    }
                } else {
                    // 修改本地
                    WKSharedPreferencesUtil.getInstance()
                        .putIntWithUID(
                            Const.getHideChannelPinnedMsgKey(
                                iConversationContext.chatChannelInfo.channelID,
                                iConversationContext.chatChannelInfo.channelType
                            ),
                            1
                        )
                    EndpointManager.getInstance()
                        .invoke("hide_pinned_view", null)
                    EndpointManager.getInstance().invoke("reset_channel_all_pinned_msg", null)
                }


            }
        }

        linearLayout.setOnClickListener {
            if (WKReader.isNotEmpty(msgList)) {
                var nextIndex = 0
                if (msgList.size > 1) {
                    for (item in msgList.withIndex()) {
                        if (selectedMessageId == item.value.messageID) {
                            nextIndex = item.index + 1
                            break
                        }
                    }
                    if (nextIndex >= msgList.size) {
                        nextIndex = 0
                    }

                    selectedMessageId = showContent(
                        imageView,
                        topTextView,
                        contentTextView,
                        pinnedLineView,
                        msgList,
                        msgList[nextIndex].messageID,
                        iConversationContext.chatActivity
                    )
                }
                EndpointManager.getInstance()
                    .invoke("tip_msg_in_chat", msgList[nextIndex].clientMsgNO)
            }
        }
        WKIM.getInstance().cmdManager.addCmdListener(
            "pinned_message"
        ) { cmd ->
            if (cmd.cmdKey == Const.cmdMessageDeleted || cmd.cmdKey == Const.cmdSyncPinnedMessage) {
                PinnedMsgModel.instance.syncPinnedMessage(
                    iConversationContext.chatChannelInfo.channelID,
                    iConversationContext.chatChannelInfo.channelType,
                    object : PinnedMsgModel.ISyncPinnedMsg {
                        override fun onResult(code: Int, msg: String?, selectedMsgId: String) {
                            if (code == HttpResponseCode.success.toInt()) {
                                getPinnedMessageList(
                                    iConversationContext.chatChannelInfo.channelID,
                                    iConversationContext.chatChannelInfo.channelType.toInt(),
                                    object : IGetPinnedMessageResult {
                                        override fun onResult(list: ArrayList<WKMsg>) {
                                            msgList = list
                                            if (WKReader.isNotEmpty(msgList)) {
                                                if (msgList.size == 1) {
                                                    rightIV.setImageResource(R.mipmap.ic_close_white)
                                                } else {
                                                    rightIV.setImageResource(R.mipmap.msg_pinnedlist)
                                                }
                                                selectedMessageId = showContent(
                                                    imageView,
                                                    topTextView,
                                                    contentTextView,
                                                    pinnedLineView,
                                                    msgList,
                                                    selectedMsgId,
                                                    iConversationContext.chatActivity
                                                )
                                                if (!TextUtils.isEmpty(selectedMsgId)) {
                                                    for (item in msgList.withIndex()) {
                                                        if (item.value.messageID == selectedMsgId) {
                                                            pinnedLineView.set(
                                                                item.index,
                                                                msgList.size,
                                                                true
                                                            )
                                                            break
                                                        }
                                                    }
                                                }
                                                EndpointManager.getInstance()
                                                    .invoke("show_pinned_view", null)
                                            } else {
                                                EndpointManager.getInstance()
                                                    .invoke("hide_pinned_view", null)
                                            }
                                        }
                                    })
                            }
                        }
                    })
            }
        }


        getPinnedMessageList(
            iConversationContext.chatChannelInfo.channelID,
            iConversationContext.chatChannelInfo.channelType.toInt(),
            object : IGetPinnedMessageResult {
                override fun onResult(list: ArrayList<WKMsg>) {
                    msgList = list
                    if (msgList.size == 1) {
                        rightIV.setImageResource(R.mipmap.ic_close_white)
                    } else {
                        rightIV.setImageResource(R.mipmap.msg_pinnedlist)
                    }
                    if (WKReader.isNotEmpty(msgList)) {
                        selectedMessageId = showContent(
                            imageView,
                            topTextView,
                            contentTextView,
                            pinnedLineView,
                            msgList,
                            "",
                            iConversationContext.chatActivity
                        )
                        EndpointManager.getInstance().invoke("show_pinned_view", null)
                    }
                }
            })
        PinnedMsgModel.instance.syncPinnedMessage(
            iConversationContext.chatChannelInfo.channelID,
            iConversationContext.chatChannelInfo.channelType,
            object : PinnedMsgModel.ISyncPinnedMsg {
                override fun onResult(code: Int, msg: String?, selectedMsgId: String) {
                    if (code == HttpResponseCode.success.toInt() && msg == "data") {
                        getPinnedMessageList(
                            iConversationContext.chatChannelInfo.channelID,
                            iConversationContext.chatChannelInfo.channelType.toInt(),
                            object : IGetPinnedMessageResult {
                                override fun onResult(list: ArrayList<WKMsg>) {
                                    msgList = list
                                    if (WKReader.isNotEmpty(msgList)) {
                                        if (msgList.size == 1) {
                                            rightIV.setImageResource(R.mipmap.ic_close_white)
                                        } else {
                                            rightIV.setImageResource(R.mipmap.msg_pinnedlist)
                                        }
                                        selectedMessageId = showContent(
                                            imageView,
                                            topTextView,
                                            contentTextView,
                                            pinnedLineView,
                                            msgList,
                                            selectedMsgId,
                                            iConversationContext.chatActivity
                                        )
                                        EndpointManager.getInstance()
                                            .invoke("show_pinned_view", null)
                                    } else {
                                        EndpointManager.getInstance()
                                            .invoke("hide_pinned_view", null)
                                    }
                                }
                            })
                    }
                }
            })
        EndpointManager.getInstance().setMethod(
            "tip_pinned_message"
        ) { `object` ->
            if (`object` is String && `object` != selectedMessageId) {
                if (WKReader.isNotEmpty(msgList)) {
                    var selectMessageId = ""
                    for (item in msgList.withIndex()) {
                        if (item.value.messageID == `object`) {
                            selectMessageId = item.value.messageID
                            break
                        }
                    }
                    if (!TextUtils.isEmpty(selectMessageId)) {
                        selectedMessageId = showContent(
                            imageView,
                            topTextView,
                            contentTextView,
                            pinnedLineView,
                            msgList,
                            selectMessageId,
                            iConversationContext.chatActivity
                        )
                    }
                }
            }
            null
        }
        WKIM.getInstance().msgManager.addOnDeleteMsgListener("pinned_message_show_content") { mMsg ->
            if (mMsg != null && WKReader.isNotEmpty(msgList)) {
                for (item in msgList.withIndex()) {
                    if (mMsg.messageID == item.value.messageID) {
                        msgList.removeAt(item.index)
                        if (msgList.size == 0) {
                            EndpointManager.getInstance().invoke("hide_pinned_view", null)
                        } else {
                            if (item.value.messageID == selectedMessageId) {
                                selectedMessageId = showContent(
                                    imageView,
                                    topTextView,
                                    contentTextView,
                                    pinnedLineView,
                                    msgList,
                                    "",
                                    iConversationContext.chatActivity
                                )
                            }
                        }
                        break
                    }
                }
            }
        }
        WKIM.getInstance().msgManager.addOnRefreshMsgListener(
            "pinned_message_show_content"
        ) { mMsg, _ ->
            if (mMsg != null && WKReader.isNotEmpty(msgList)) {
                for (item in msgList.withIndex()) {
                    if (mMsg.messageID == item.value.messageID) {
                        if (item.value.isDeleted == 1 || item.value.remoteExtra?.isMutualDeleted == 1 || item.value.remoteExtra?.revoke == 1) {
                            msgList.removeAt(item.index)
                            if (msgList.size == 0) {
                                EndpointManager.getInstance().invoke("hide_pinned_view", null)
                            } else {
                                if (item.value.messageID == selectedMessageId) {
                                    selectedMessageId = showContent(
                                        imageView,
                                        topTextView,
                                        contentTextView,
                                        pinnedLineView,
                                        msgList,
                                        "",
                                        iConversationContext.chatActivity
                                    )
                                }
                            }
                        } else {
                            if (item.value.remoteExtra != null && item.value.remoteExtra.contentEditMsgModel != null) {
                                item.value.remoteExtra.contentEditMsgModel =
                                    mMsg.remoteExtra.contentEditMsgModel
                                if (selectedMessageId == mMsg.messageID) {
                                    selectedMessageId = showContent(
                                        imageView,
                                        topTextView,
                                        contentTextView,
                                        pinnedLineView,
                                        msgList,
                                        selectedMessageId,
                                        iConversationContext.chatActivity
                                    )
                                }
                            }
                        }
                        break
                    }
                }
            }
        }
        EndpointManager.getInstance().setMethod(
            "chat_page_reset"
        ) { `object` ->
            if (WKReader.isNotEmpty(msgList) && `object` is WKChannel && `object`.channelID == iConversationContext.chatChannelInfo.channelID) {
                EndpointManager.getInstance().invoke("show_pinned_view", null)
            }
            null
        }
        EndpointManager.getInstance().setMethod("is_syncing_message") { `object` ->
            if (`object` is Int) {
                if (`object` == 1) {
                    progressView.visibility = View.VISIBLE
                    rightIV.visibility = View.GONE
                } else {
                    progressView.visibility = View.GONE
                    rightIV.visibility = View.VISIBLE
                }
            }
        }
        return linearLayout
    }

    private fun getPinnedMessageList(
        channelId: String, channelType: Int,
        iGetPinnedMessageResult: IGetPinnedMessageResult
    ) {
        runBlocking {
            launch(Dispatchers.Default) {
                val msgList = ArrayList<WKMsg>()
                if (channelType == WKChannelType.GROUP.toInt()) {
                    val hideAll = WKSharedPreferencesUtil.getInstance()
                        .getIntWithUID(
                            Const.getHideChannelPinnedMsgKey(
                                channelId,
                                channelType.toByte()
                            )
                        )
                    if (hideAll == 1) {
                        AndroidUtilities.runOnUIThread {
                            iGetPinnedMessageResult.onResult(msgList)
                        }
                        return@launch
                    }
                }
                // 查询消息
                val pinnedMsgList =
                    PinnedMessageDB.instance.queryPinnedMessage(channelId, channelType)
                if (WKReader.isNotEmpty(pinnedMsgList)) {
                    val msgIds: ArrayList<String> = ArrayList()
                    for (pinnedMsg in pinnedMsgList) {
                        msgIds.add(pinnedMsg.message_id)
                    }
                    var tempMsgList = WKIM.getInstance().msgManager.getWithMessageIDs(msgIds)
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
                        msgList.sortedBy { it.messageSeq }
                    }
                }
                AndroidUtilities.runOnUIThread {
                    iGetPinnedMessageResult.onResult(msgList)
                }
            }
        }
    }

    interface IGetPinnedMessageResult {
        fun onResult(list: ArrayList<WKMsg>)
    }

    private fun showContent(
        imageView: ImageView,
        topTextView: TextView,
        contentTextView: TextView,
        pinnedLineView: PinnedLineView,
        msgList: ArrayList<WKMsg>,
        selectMessageId: String,
        context: Context
    ): String {
        var index = 0
        if (!TextUtils.isEmpty(selectMessageId)) {
            for (item in msgList.withIndex()) {
                if (selectMessageId == item.value.messageID) {
                    index = item.index
                    break
                }
            }
        }
        val content = if (msgList.size > 1) String.format(
            "%s #%s",
            context.getString(R.string.pin_message_count), index + 1
        ) else {
            context.getString(R.string.pin_message_count)
        }

        val selectedMessageId = msgList[index].messageID
        topTextView.text = content
        if (msgList[index].baseContentMsgModel != null) {
            var displayContent = msgList[index].baseContentMsgModel.getDisplayContent()
            if (msgList[index].remoteExtra != null && msgList[index].remoteExtra.contentEditMsgModel != null) {
                displayContent = msgList[index].remoteExtra.contentEditMsgModel.getDisplayContent()
            }
            MoonUtil.identifyFaceExpression(
                context,
                contentTextView,
                displayContent,
                MoonUtil.SMALL_SCALE
            )
//            contentTextView.movementMethod = LinkMovementMethod.getInstance()
        }
        pinnedLineView.set(index, msgList.size, true)


        val anim = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f
        )
        anim.duration = 200
        anim.fillAfter = true
        contentTextView.startAnimation(anim)

        if (msgList[index].type == WKContentType.WK_IMAGE) {
            val url = getShowURL(msgList[index])
            GlideUtils.getInstance().showImg(context, url, imageView)
            zoomInOut(imageView, true)
        } else {
            if (imageView.visibility == View.VISIBLE) {
                zoomInOut(imageView, false)
            }
        }

        return selectedMessageId
    }


    private fun getShowURL(wkMsg: WKMsg): String {
        val imgMsgModel = wkMsg.baseContentMsgModel as WKImageContent
        if (!TextUtils.isEmpty(imgMsgModel.localPath)) {
            val file = File(imgMsgModel.localPath)
            if (file.exists() && file.length() > 0L) {
                return file.absolutePath
            }
        }
        if (!TextUtils.isEmpty(imgMsgModel.url)) {
            return WKApiConfig.getShowUrl(imgMsgModel.url)
        }
        return ""
    }

    private fun zoomInOut(view: View?, zoomIn: Boolean) {
        if (view == null) return
        val startScale = 0.10f
        val endScale = 1f
        view.pivotX = (view.width / 2).toFloat()
        view.pivotY = (view.height / 2).toFloat()
        view.scaleX = startScale
        view.scaleY = startScale

        val animatorSet = AnimatorSet()
        var scaleAnimator: Animator

        if (zoomIn) {
            scaleAnimator = ObjectAnimator.ofFloat(view, "scaleX", startScale, endScale)
            scaleAnimator.setDuration(200)
            scaleAnimator.setInterpolator(OvershootInterpolator())
            animatorSet.play(scaleAnimator)
            scaleAnimator = ObjectAnimator.ofFloat(view, "scaleY", startScale, endScale)
            scaleAnimator.setDuration(200)
            scaleAnimator.setInterpolator(OvershootInterpolator())
            animatorSet.play(scaleAnimator)
        } else {
            scaleAnimator = ObjectAnimator.ofFloat(view, "scaleX", endScale, startScale)
            scaleAnimator.setDuration(200)
            scaleAnimator.setInterpolator(AnticipateInterpolator())
            animatorSet.play(scaleAnimator)

            scaleAnimator = ObjectAnimator.ofFloat(view, "scaleY", endScale, startScale)
            scaleAnimator.setDuration(200)
            scaleAnimator.setInterpolator(AnticipateInterpolator())
            animatorSet.play(scaleAnimator)
        }

        animatorSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {
                if (zoomIn) {
                    view.visibility = View.VISIBLE
                }
            }

            override fun onAnimationEnd(p0: Animator) {
                if (zoomIn) {
                    view.visibility = View.VISIBLE
                } else {
                    view.visibility = View.GONE
                }
            }

            override fun onAnimationCancel(p0: Animator) {
            }

            override fun onAnimationRepeat(p0: Animator) {
            }
        })
        animatorSet.start()
    }
}