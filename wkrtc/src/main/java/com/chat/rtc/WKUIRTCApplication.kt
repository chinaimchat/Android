package com.chat.rtc

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import com.chat.base.WKBaseApplication
import com.chat.base.config.WKApiConfig
import com.chat.base.config.WKConfig
import com.chat.base.endpoint.EndpointCategory
import com.chat.base.endpoint.EndpointHandler
import com.chat.base.endpoint.EndpointManager
import com.chat.base.endpoint.EndpointSID
import com.chat.base.endpoint.entity.CreateVideoCallMenu
import com.chat.base.endpoint.entity.MsgConfig
import com.chat.base.endpoint.entity.RTCMenu
import com.chat.base.glide.GlideUtils
import com.chat.base.msgitem.WKContentType
import com.chat.base.msgitem.WKMsgItemViewManager
import com.chat.base.net.HttpResponseCode
import com.chat.base.utils.AndroidUtilities
import com.chat.base.utils.WKReader
import com.chat.base.utils.WKToastUtils
import com.chat.rtc.entity.RtcOfflineMsg
import com.chat.rtc.msg.MultiMsgContent
import com.chat.rtc.msg.MultiRTCProviderItem
import com.chat.rtc.msg.P2PRTCProviderItem
import com.chat.rtc.msg.RTCDataContent
import com.chat.rtc.msg.RTCMsgContent
import com.chat.rtc.msg.SwitchVideoRespondContent
import com.chat.rtc.msg.WKRTCType
import com.chat.rtc.multi.ChooseRTCMemberActivity
import com.chat.rtc.service.RTCModel
import rtc.WKRTCApplication
import rtc.WKRTCCallType
import rtc.inters.IChooseMembersBack
import rtc.inters.ISendMsgBack
import rtc.inters.ISendMsgListener
import rtc.utils.WKRTCManager
import com.xinbida.wukongim.WKIM
import com.xinbida.wukongim.entity.WKCMD
import com.xinbida.wukongim.entity.WKCMDKeys
import com.xinbida.wukongim.entity.WKChannel
import com.xinbida.wukongim.entity.WKChannelType
import com.xinbida.wukongim.entity.WKMsg
import com.xinbida.wukongim.message.type.WKSendMsgResult
import org.jetbrains.annotations.NotNull
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.PeerConnection.IceServer


object WKUIRTCApplication {

    // p2p call
    private var invokeCallType: Int = 0
    private var invokeUID: String = ""

    // 被呼叫通话的消息
    //    private ConcurrentHashMap<String, WKMsg> tempP2PCallMsg = new ConcurrentHashMap<>();
    private var clientSeq: Long = 1
    var iChooseMembersBack: IChooseMembersBack? = null
    var roomID: String? = null
    fun init(list: ArrayList<IceServer>) {
        val appModule = WKBaseApplication.getInstance().getAppModuleWithSid("rtc")
        if (!WKBaseApplication.getInstance().appModuleIsInjection(appModule)) return

        val tempList = if (WKReader.isEmpty(list)) {
            getList()
        } else {
            list
        }
        WKRTCApplication.getInstance()
            .initModule(
                WKBaseApplication.getInstance().context, tempList
            )
        initListener()
    }

    private fun getList(): ArrayList<IceServer> {
        val iceServer = IceServer.builder(
            "turn:175.27.245.108:3478?transport=udp"
        ).setUsername("tsdd").setPassword(
            "tsddpwd"
        ).createIceServer()
        val iceServers: ArrayList<IceServer> = ArrayList()
        iceServers.add(iceServer)
        return iceServers
    }

    private fun initListener() {
        EndpointManager.getInstance()
            .setMethod(
                EndpointSID.appIsRunningBackground
            ) { `object` ->
                if (`object` is Boolean) {
                    if (!`object`) {
                        EndpointManager.getInstance().invoke("cancel_rtc_notification", null)
                        if (!TextUtils.isEmpty(invokeUID)) {
                            val channel: WKChannel? = WKIM.getInstance().channelManager
                                .getChannel(invokeUID, WKChannelType.PERSONAL)
                            var fromName = ""
                            if (channel != null) {
                                fromName =
                                    if (TextUtils.isEmpty(channel.channelRemark)) channel.channelName else channel.channelRemark
                            }
                            WKRTCManager.getInstance().joinP2PCall(
                                invokeUID,
                                fromName,
                                WKConfig.getInstance().uid,
                                invokeCallType
                            )
                        }
                    }
                }
                invokeUID = ""
                invokeCallType = 0
                null
            }
        // 注册消息
        WKIM.getInstance().msgManager
            .registerContentMsg(RTCDataContent::class.java)
        WKIM.getInstance().msgManager
            .registerContentMsg(RTCMsgContent::class.java)
        WKIM.getInstance().msgManager.registerContentMsg(MultiMsgContent::class.java)
        WKIM.getInstance().msgManager
            .registerContentMsg(SwitchVideoRespondContent::class.java)
        // 注册消息item
        WKMsgItemViewManager.getInstance()
            .addChatItemViewProvider(
                WKRTCType.WK_P2P_CALL,
                P2PRTCProviderItem()
            )
        WKMsgItemViewManager.getInstance()
            .addChatItemViewProvider(
                WKContentType.videoCallGroup,
                MultiRTCProviderItem()
            )
        EndpointManager.getInstance()
            .setMethod("", EndpointCategory.chatShowBubble,
                EndpointHandler { `object` ->
                    val msgType = `object` as Int
                    if (msgType == WKRTCType.WK_P2P_CALL) {
                        return@EndpointHandler true
                    }
                    false
                })
        EndpointManager.getInstance().setMethod(
            "is_register_rtc"
        ) { true }
        EndpointManager.getInstance()
            .setMethod(EndpointCategory.msgConfig + WKRTCType.WK_P2P_CALL) {
                MsgConfig(
                    false
                )
            }
        // 监听音视频事件
        WKRTCManager.getInstance().addOnAvatarLoader { context, uid, imageView, width, isP2PCall ->
            var key = ""
            val channel: WKChannel? =
                WKIM.getInstance().channelManager
                    .getChannel(uid, WKChannelType.PERSONAL)
            if (channel != null && !TextUtils.isEmpty(channel.avatarCacheKey)) {
                key = channel.avatarCacheKey
            }
            val url = WKApiConfig.getShowAvatar(uid, WKChannelType.PERSONAL)
            if (isP2PCall && width > 0) {
                GlideUtils.getInstance()
                    .showRoundedAvatar(
                        context,
                        url,
                        key,
                        imageView,
                        AndroidUtilities.dp(width * 0.4f)
                    )
            } else {
                GlideUtils.getInstance()
                    .showAvatarImg(context, uid, WKChannelType.PERSONAL, key, imageView)
            }
        }
        //选择通话成员
        WKRTCManager.getInstance()
            .addChooseMembers { context: Context, roomID: String, channelID: String?, channelType: Byte, selectedUIDs: List<String?>?, iChooseMembersBack: IChooseMembersBack ->
                this.iChooseMembersBack = iChooseMembersBack
                this.roomID = roomID
                val intent = Intent(context, ChooseRTCMemberActivity::class.java)
                intent.putExtra("channelID", channelID)
                intent.putExtra("channelType", channelType)
                intent.putStringArrayListExtra(
                    "selectedList",
                    selectedUIDs as ArrayList<String>?
                )
                context.startActivity(intent)
            }
        WKRTCManager.getInstance().addOnSendMsgListener(object : ISendMsgListener {
            override fun sendRTCMsg(uid: String, message: String, iSendMsgBack: ISendMsgBack?) {
                // 发送rtc消息
                if (!TextUtils.isEmpty(uid)) {
                    val mMsg = WKMsg()
                    mMsg.clientSeq = ++clientSeq
                    mMsg.header.noPersist = true
                    mMsg.header.redDot = false
                    mMsg.channelID = uid
                    mMsg.type = WKRTCType.wk_video_call_data
                    mMsg.channelType = WKChannelType.PERSONAL
                    mMsg.baseContentMsgModel =
                        RTCDataContent(message)
                    iSendMsgBack!!.onBack(mMsg.clientSeq)
                    WKIM.getInstance().msgManager.sendMessage(mMsg)
                }
            }

            override fun sendHangUpMsg(uid: String, second: Int, callType: Int, isCaller: Int) {
                // 发送挂断消息
                RTCModel.getInstance().hangupP2PCall(uid, second, callType, isCaller, null)
            }

            override fun sendRefuse(channelID: String, channelType: Byte, callType: Int) {
                // 发送拒绝消息
                RTCModel.getInstance().refuseP2PCall(callType, channelID, null)
            }

            override fun sendMultiRefuse(roomID: String) {
                RTCModel.getInstance().multiRefuse(roomID, null)
            }

            override fun sendMultiHangup(s: String) {
                RTCModel.getInstance().multiHangup(s, null)
            }

            override fun sendMultiJoined(s: String) {
                RTCModel.getInstance().multiJoined(s, null)
            }

            override fun sendCancel(uid: String, callType: Int) {
                RTCModel.getInstance().cancelP2PCall(callType, uid, null)
                //                sendP2PCancel(uid, callType);
            }

            override fun sendSwitchAudio(fromUID: String, toUID: String) {
                if (!TextUtils.isEmpty(fromUID)) {
                    val msg = WKMsg()
                    msg.clientSeq = ++clientSeq
                    msg.header.noPersist = true
                    msg.header.redDot = false
                    msg.channelID = toUID
                    msg.channelType = WKChannelType.PERSONAL
                    msg.type = WKRTCType.wk_video_switch_audio
                    WKIM.getInstance().msgManager.sendMessage(msg)
                }
            }

            override fun sendSwitchVideo(fromUID: String, toUID: String) {
                if (!TextUtils.isEmpty(fromUID)) {
                    val msg = WKMsg()
                    msg.clientSeq = ++clientSeq
                    msg.header.noPersist = true
                    msg.header.redDot = false
                    msg.channelID = toUID
                    msg.channelType = WKChannelType.PERSONAL
                    msg.type = WKRTCType.wk_video_switch_video
                    WKIM.getInstance().msgManager.sendMessage(msg)
                }
            }

            override fun sendSwitchVideoRespond(fromUID: String, toUID: String, status: Int) {
                if (!TextUtils.isEmpty(fromUID)) {
                    val msg = WKMsg()
                    msg.clientSeq = ++clientSeq
                    msg.header.noPersist = true
                    msg.header.redDot = false
                    msg.channelID = toUID
                    msg.channelType = WKChannelType.PERSONAL
                    val content = SwitchVideoRespondContent()
                    content.status = status
                    msg.baseContentMsgModel = content
                    msg.type = WKRTCType.wk_video_switch_video_respond
                    WKIM.getInstance().msgManager.sendMessage(msg)
                }
            }

            override fun sendAccept(fromUID: String, toUID: String, callType: Int) {
                RTCModel.getInstance().acceptP2PCall(callType, toUID, null)
            }
        })
        // 创建多人音视频通话
        EndpointManager.getInstance().setMethod("create_video_call") { `object` ->
            val createVideoCallMenu: CreateVideoCallMenu? = `object` as CreateVideoCallMenu?
            if (createVideoCallMenu != null) {
                val uidList: MutableList<String> =
                    ArrayList()
                for (channel in createVideoCallMenu.WKChannels) {
                    if (TextUtils.isEmpty(channel.channelID)) continue
                    uidList.add(channel.channelID)
                }
                RTCModel.getInstance().createRoomID(
                    "",
                    createVideoCallMenu.channelID,
                    createVideoCallMenu.channelType,
                    uidList
                ) { roomID, code, msg ->
                    if (code == HttpResponseCode.success.toInt()) {
                        val list =
                            ArrayList(uidList)
                        RTCModel.getInstance()
                            .getVideoCallToken(roomID) { token, _, _ ->
                                WKRTCManager.getInstance().createMultiCall(
                                    createVideoCallMenu.channelID,
                                    createVideoCallMenu.channelType,
                                    WKConfig.getInstance().uid,
                                    list,
                                    token,
                                    roomID
                                )
                                val content = java.lang.String.format(
                                    WKBaseApplication.getInstance().context.getString(R.string.user_create_multi_call),
                                    WKConfig.getInstance().userName
                                )
                                saveMultiMsg(
                                    content,
                                    createVideoCallMenu.channelID,
                                    createVideoCallMenu.channelType
                                )
                            }
                    } else WKToastUtils.getInstance().showToastNormal(msg)
                }
            }
            null
        }
        // 创建p2p音视频通话
        EndpointManager.getInstance().setMethod("wk_p2p_call") { `object` ->
            if (!WKRTCManager.getInstance().isCalling) {
                val menu: RTCMenu? = `object` as RTCMenu?
                if (menu != null) {
                    val toUID: String = menu.iConversationContext.chatChannelInfo.channelID
                    RTCModel.getInstance().inviteP2PCall(menu.callType, toUID) { code, msg ->
                        var showName: String =
                            menu.iConversationContext.chatChannelInfo.channelRemark
                        if (TextUtils.isEmpty(showName)) showName =
                            menu.iConversationContext.chatChannelInfo.channelName
                        if (code != HttpResponseCode.success.toInt()) {
                            WKToastUtils.getInstance().showToast(msg)
                        } else {
                            clientSeq = 1
                            WKRTCManager.getInstance()
                                .createP2PCall(
                                    WKConfig.getInstance().uid,
                                    toUID,
                                    showName,
                                    menu.callType
                                )
                        }
                    }
                }
            }
            null
        }
        // 判断是否正在通话中
        EndpointManager.getInstance()
            .setMethod("rtc_is_calling") { WKRTCManager.getInstance().isCalling }
        WKIM.getInstance().msgManager
            .addOnNewMsgListener("wk_rtc_application") { list ->
                for (msg in list) {
                    if (msg.type == WKRTCType.wk_video_call_received) {
                        // 请求通话
                        val channel: WKChannel? = WKIM.getInstance().channelManager
                            .getChannel(msg.fromUID, WKChannelType.PERSONAL)
                        var fromName = ""
                        if (channel != null) {
                            fromName =
                                if (TextUtils.isEmpty(channel.channelRemark)) channel.channelName else channel.channelRemark
                        }
                        var callType = WKRTCCallType.audio
                        if (!TextUtils.isEmpty(msg.content)) {
                            try {
                                val jsonObject =
                                    JSONObject(msg.content)
                                if (jsonObject.has("call_type")) callType =
                                    jsonObject.optInt("call_type")
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                        clientSeq = 1
                        WKRTCManager.getInstance().joinP2PCall(
                            msg.fromUID,
                            fromName,
                            WKConfig.getInstance().uid,
                            callType
                        )
                    } else if (msg.type == WKRTCType.wk_video_call_data) {
                        // rtc data
                        val videoCallContent: RTCDataContent? =
                            msg.baseContentMsgModel as RTCDataContent?
                        if (videoCallContent != null) {
                            WKRTCManager.getInstance()
                                .receivedRTCMsg(msg.channelID, videoCallContent.content)
                        }
                    } else if (msg.type == WKRTCType.wk_video_switch_audio) {
                        // 对方切换到音频
                        WKRTCManager.getInstance().onSwitchAudio(msg.fromUID)
                    } else if (msg.type == WKRTCType.wk_video_switch_video) {
                        // 对方请求切换到视频
                        WKRTCManager.getInstance().onSwitchVideoRequest(msg.fromUID)
                    } else if (msg.type == WKRTCType.wk_video_switch_video_respond) {
                        val respondContent: SwitchVideoRespondContent? =
                            msg.baseContentMsgModel as SwitchVideoRespondContent?
                        if (respondContent != null) WKRTCManager.getInstance()
                            .onSwitchVideoRespond(msg.fromUID, respondContent.status)
                    }
                }
            }
        EndpointManager.getInstance().setMethod(
            "rtc_offline_data"
        ) { `object` ->
            val offlineList = ArrayList<RtcOfflineMsg>()
            val list: List<WKCMD> = `object` as List<WKCMD>
            for (cmd in list) {
                if (cmd.cmdKey.equals("rtc.p2p.invoke")) {
                    var channelID = ""
                    var invokeUID = ""
                    var channelType: Byte = 0
                    var callType = WKRTCCallType.audio
                    if (cmd.paramJsonObject.has("from_uid")) {
                        invokeUID = cmd.paramJsonObject.optString("from_uid")
                        callType = cmd.paramJsonObject.optInt("call_type")
                    }
                    if (cmd.paramJsonObject.has("channel_id")) {
                        channelID = cmd.paramJsonObject.optString("channel_id")
                    }
                    if (cmd.paramJsonObject.has("channel_type")) {
                        channelType = cmd.paramJsonObject.optInt("channel_type").toByte()
                    }
                    if (!TextUtils.isEmpty(channelID) && !TextUtils.isEmpty(invokeUID)) {
                        offlineList.add(RtcOfflineMsg(channelID, channelType, invokeUID, callType))
                    }
                } else if (cmd.cmdKey.equals("rtc.p2p.cancel")) {
                    if (cmd.paramJsonObject.has("uid")) {
                        // val uid: String = cmd.paramJsonObject.optString("uid")
                        var channelID = ""
                        var channelType: Byte = 0
                        if (cmd.paramJsonObject.has("channel_id")) {
                            channelID = cmd.paramJsonObject.optString("channel_id")
                        }
                        if (cmd.paramJsonObject.has("channel_type")) {
                            channelType = cmd.paramJsonObject.optInt("channel_type").toByte()
                        }
                        if (WKReader.isNotEmpty(offlineList)) {
                            for (index in offlineList.indices) {
                                if (offlineList[index].channelId == channelID && offlineList[index].channelType == channelType) {
                                    offlineList.removeAt(index)
                                    break
                                }
                            }
                        }
                    }
                } else if (cmd.cmdKey.equals("rtc.p2p.hangup")) {
                    if (cmd.paramJsonObject.has("uid")) {
                        //  val uid: String = cmd.paramJsonObject.optString("uid")
                        var channelID = ""
                        var channelType: Byte = 0
                        if (cmd.paramJsonObject.has("channel_id")) {
                            channelID = cmd.paramJsonObject.optString("channel_id")
                        }
                        if (cmd.paramJsonObject.has("channel_type")) {
                            channelType = cmd.paramJsonObject.optInt("channel_type").toByte()
                        }
                        if (WKReader.isNotEmpty(offlineList)) {
                            for (index in offlineList.indices) {
                                if (offlineList[index].channelId == channelID && offlineList[index].channelType == channelType) {
                                    offlineList.removeAt(index)
                                    break
                                }
                            }
                        }
                    }
                } else if (cmd.cmdKey.equals("rtc.p2p.refuse")) {
                    if (cmd.paramJsonObject.has("uid")) {
                        //  val uid: String = cmd.paramJsonObject.optString("uid")
                        var channelID = ""
                        var channelType: Byte = 0
                        if (cmd.paramJsonObject.has("channel_id")) {
                            channelID = cmd.paramJsonObject.optString("channel_id")
                        }
                        if (cmd.paramJsonObject.has("channel_type")) {
                            channelType = cmd.paramJsonObject.optInt("channel_type").toByte()
                        }
                        if (WKReader.isNotEmpty(offlineList)) {
                            for (index in offlineList.indices) {
                                if (offlineList[index].channelId == channelID && offlineList[index].channelType == channelType) {
                                    offlineList.removeAt(index)
                                    break
                                }
                            }
                        }
                    }
                }
            }
            var callType = WKRTCCallType.audio
            var invokeUID = ""
            if (WKReader.isNotEmpty(offlineList)) {
                invokeUID = offlineList[0].invokeUID
                callType = offlineList[0].callType
            }
            if (!TextUtils.isEmpty(invokeUID) && invokeUID != WKConfig.getInstance().uid) {
                val channel: WKChannel? = WKIM.getInstance().channelManager
                    .getChannel(invokeUID, WKChannelType.PERSONAL)
                var fromName = ""
                if (channel != null) {
                    fromName =
                        if (TextUtils.isEmpty(channel.channelRemark)) channel.channelName else channel.channelRemark
                }
                clientSeq = 1
                WKRTCManager.getInstance().joinP2PCall(
                    invokeUID,
                    fromName,
                    WKConfig.getInstance().uid,
                    callType
                )
            }
            null
        }
        WKIM.getInstance().cmdManager.addCmdListener("wk_rtc_application") { cmd ->
            if (cmd == null || TextUtils.isEmpty(cmd.cmdKey) || cmd.paramJsonObject == null) return@addCmdListener
            when (cmd.cmdKey) {
                WKCMDKeys.wk_room_invoke -> {
                    //邀请音视频
                    val inviter: String = cmd.paramJsonObject.optString("inviter")
                    val channelID: String = cmd.paramJsonObject.optString("channel_id")
                    val channelType =
                        cmd.paramJsonObject.optInt("channel_type")
                    val roomID: String = cmd.paramJsonObject.optString("room_id")
                    val participants: JSONArray? =
                        cmd.paramJsonObject.optJSONArray("participants")
                    val uidList =
                        ArrayList<String>()
                    if (participants != null && participants.length() > 0) {
                        var i = 0
                        val size = participants.length()
                        while (i < size) {
                            uidList.add(participants.optString(i))
                            i++
                        }
                    }
                    if (!TextUtils.isEmpty(roomID)) {
                        RTCModel.getInstance()
                            .getVideoCallToken(roomID) { token, _, msg ->
                                if (!TextUtils.isEmpty(token)) {
                                    var fromName = ""
                                    val channel: WKChannel? =
                                        WKIM.getInstance().channelManager
                                            .getChannel(inviter, WKChannelType.PERSONAL)
                                    if (channel != null) {
                                        fromName =
                                            if (TextUtils.isEmpty(channel.channelRemark)) channel.channelName else channel.channelRemark
                                    }
                                    val content = String.format(
                                        WKBaseApplication.getInstance().context.getString(R.string.user_create_multi_call),
                                        fromName
                                    )
                                    // 保存消息记录
                                    saveMultiMsg(content, channelID, channelType.toByte())
                                    clientSeq = 1
                                    WKRTCManager.getInstance().joinMultiCall(
                                        channelID,
                                        channelType.toByte(),
                                        inviter,
                                        fromName,
                                        WKConfig.getInstance().uid,
                                        uidList,
                                        token,
                                        roomID
                                    )
                                } else WKToastUtils.getInstance().showToastNormal(msg)
                            }
                    }
                }

                "rtc.p2p.invoke" -> if (cmd.paramJsonObject.has("from_uid")) {
                    val fromUID: String = cmd.paramJsonObject.optString("from_uid")
                    if (!TextUtils.isEmpty(fromUID) && fromUID == WKConfig.getInstance().uid) {
                        return@addCmdListener
                    }
                    val callType: Int = cmd.paramJsonObject.optInt("call_type")
                    val channel: WKChannel? = WKIM.getInstance().channelManager
                        .getChannel(fromUID, WKChannelType.PERSONAL)
                    var fromName = ""
                    if (channel != null) {
                        fromName =
                            if (TextUtils.isEmpty(channel.channelRemark)) channel.channelName else channel.channelRemark
                    }
                    clientSeq = 1
//                    WKRTCManager.getInstance()
//                        .joinP2PCall(fromUID, fromName, WKConfig.getInstance().uid, callType)

                    if (AndroidUtilities.isBackground(WKBaseApplication.getInstance().context)) {
                        if (checkOverlayPermission(WKBaseApplication.getInstance().context)) {
                            WKRTCManager.getInstance()
                                .joinP2PCall(
                                    fromUID,
                                    fromName,
                                    WKConfig.getInstance().uid,
                                    callType
                                )
                        } else {
                            // 显示通知
                            if (!WKRTCManager.getInstance().isCalling) {
                                invokeCallType = callType
                                invokeUID = fromUID
//                                Notify.with(WKBaseApplication.getInstance().context).content {
//                                    title = "fromName"
//                                    text = "邀请你音视频通话"
//                                }.show()
                                EndpointManager.getInstance()
                                    .invoke("show_rtc_notification", fromUID)
                            }
                        }
                    } else {
                        Log.e("显示rtc邀请页面", "-->")
                        WKRTCManager.getInstance()
                            .joinP2PCall(fromUID, fromName, WKConfig.getInstance().uid, callType)

                    }
                }

                "rtc.p2p.accept" -> if (cmd.paramJsonObject.has("from_uid")) {
                    val fromUID: String = cmd.paramJsonObject.optString("from_uid")
                    if (!TextUtils.isEmpty(fromUID)) {
                        val callType: Int = cmd.paramJsonObject.optInt("call_type")
                        WKRTCManager.getInstance().onAccept(fromUID, callType)
                    }
                }

                "rtc.p2p.refuse" -> if (cmd.paramJsonObject.has("uid")) {
                    val uid: String = cmd.paramJsonObject.optString("uid")
                    WKRTCManager.getInstance().onRefuse(uid, WKChannelType.PERSONAL, uid)
                }

                "rtc.p2p.cancel" -> if (cmd.paramJsonObject.has("uid")) {
                    val uid: String = cmd.paramJsonObject.optString("uid")
                    WKRTCManager.getInstance().onCancel(uid)
                    if (!TextUtils.isEmpty(invokeUID) && uid == invokeUID) {
                        invokeUID = ""
                        invokeCallType = 0
                        EndpointManager.getInstance().invoke("cancel_rtc_notification", null)
                    }
                }

                "rtc.p2p.hangup" -> if (cmd.paramJsonObject.has("uid")) {
                    val uid: String = cmd.paramJsonObject.optString("uid")
                    val second: Int = cmd.paramJsonObject.optInt("second")
                    WKRTCManager.getInstance().onHangUp(uid, WKChannelType.PERSONAL, second)
                }

                "room.refuse" -> {
                    if (cmd.paramJsonObject.has("room_id")) {
                        val roomID: String = cmd.paramJsonObject.optString("room_id")
                        val uid: String = cmd.paramJsonObject.optString("participant")
                        if (!TextUtils.isEmpty(roomID) && !TextUtils.isEmpty(uid)) {
                            WKRTCManager.getInstance().onMultiRefuse(roomID, uid)
                        }
                    }
                }
            }
        }

        // 发送消息ack
        WKIM.getInstance().msgManager
            .addOnSendMsgAckListener("wk_rtc_application") { msg ->
                if (msg != null) {
                    WKRTCManager.getInstance()
                        .sendMsgAck(
                            msg.clientSeq,
                            msg.status == WKSendMsgResult.send_success
                        )
                } else {
                    WKRTCManager.getInstance().sendMsgAck(0, false)
                }
            }
    }

    private fun saveMultiMsg(content: String, channelID: String, channelType: Byte) {
        val msgContent = MultiMsgContent()
        msgContent.content = content
        val msg = WKMsg()
        msg.type = WKContentType.videoCallGroup
        msg.baseContentMsgModel = msgContent
        msg.content = msgContent.encodeMsg().toString()
        msg.channelID = channelID
        msg.channelType = channelType
        msg.status = WKSendMsgResult.send_success
        val tempOrderSeq: Long = WKIM.getInstance().msgManager
            .getMessageOrderSeq(0, msg.channelID, msg.channelType)
        msg.orderSeq = tempOrderSeq + 1
        WKIM.getInstance().msgManager.saveAndUpdateConversationMsg(msg, false)
    }

    private fun checkOverlayPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                return false
            }
        }
        return true
    }
}