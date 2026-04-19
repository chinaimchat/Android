package com.chat.pinned.message.service

import com.alibaba.fastjson.JSONObject
import com.chat.base.base.WKBaseModel
import com.chat.base.config.WKSharedPreferencesUtil
import com.chat.base.endpoint.EndpointManager
import com.chat.base.net.HttpResponseCode
import com.chat.base.net.ICommonListener
import com.chat.base.net.IRequestResultListener
import com.chat.base.net.entity.CommonResponse
import com.chat.base.utils.WKLogUtils
import com.chat.base.utils.WKReader
import com.chat.pinned.message.Const
import com.chat.pinned.message.db.PinnedMessageDB
import com.chat.pinned.message.entity.SyncPinnedMsg
import com.xinbida.wukongim.WKIM
import com.xinbida.wukongim.entity.WKChannelType

class PinnedMsgModel private constructor() : WKBaseModel() {

    companion object {
        val instance = SingletonHolder.holder
    }

    private object SingletonHolder {
        val holder = PinnedMsgModel()
    }

    fun pinMessage(
        messageId: String,
        messageSeq: Int,
        channelId: String,
        channelType: Int,
        iCommonListener: ICommonListener
    ) {
        val json = JSONObject()
        json["message_id"] = messageId
        json["message_seq"] = messageSeq
        json["channel_id"] = channelId
        json["channel_type"] = channelType
        request(createService(IService::class.java).pinMessage(json),
            object : IRequestResultListener<CommonResponse> {
                override fun onSuccess(result: CommonResponse) {
                    iCommonListener.onResult(HttpResponseCode.success.toInt(), "")
                }

                override fun onFail(code: Int, msg: String?) {
                    iCommonListener.onResult(code, msg)
                }
            })
    }

    interface ISyncPinnedMsg {
        fun onResult(code: Int, msg: String?, selectedMsgId: String)
    }

    fun syncPinnedMessage(channelId: String, channelType: Byte, iSyncPinnedMsg: ISyncPinnedMsg) {
        val version = PinnedMessageDB.instance.getMaxVersion(channelId, channelType.toInt())
        val json = JSONObject()
        json["version"] = version
        json["channel_id"] = channelId
        json["channel_type"] = channelType
        request(createService(IService::class.java).syncPinMessage(json),
            object : IRequestResultListener<SyncPinnedMsg> {
                override fun onSuccess(result: SyncPinnedMsg) {
                    var isSendRefresh = false
                    var msg = ""
                    var selectMessageId = ""
                    if (WKReader.isNotEmpty(result.pinned_messages)) {
                        if (channelType == WKChannelType.GROUP) {
                            WKSharedPreferencesUtil.getInstance()
                                .putIntWithUID(
                                    Const.getHideChannelPinnedMsgKey(channelId, channelType),
                                    0
                                )
                        }
                        for (item in result.pinned_messages.withIndex()) {
                            if (item.value.is_deleted == 0) {
                                selectMessageId = item.value.message_id
                                break
                            }
                        }
                        isSendRefresh = true
                        PinnedMessageDB.instance.insertPinnedMessage(result.pinned_messages)
                        msg = "data"
                    }
                    if (WKReader.isNotEmpty(result.messages)) {
                        WKIM.getInstance().msgManager.saveSyncChannelMSGs(result.messages)
                    }
                    if (isSendRefresh) {
                        EndpointManager.getInstance().invoke("reset_channel_all_pinned_msg", null)
                    }
                    iSyncPinnedMsg.onResult(HttpResponseCode.success.toInt(), msg, selectMessageId)
                }

                override fun onFail(code: Int, msg: String?) {
                    WKLogUtils.e("syncPinnedMessage fail, code: $code, msg: $msg")
                    iSyncPinnedMsg.onResult(code, msg, "")
                }
            })
    }

    fun clear(channelId: String, channelType: Byte, iCommonListener: ICommonListener) {
        val json = JSONObject()
        json["channel_id"] = channelId
        json["channel_type"] = channelType
        request(createService(IService::class.java).clear(json),
            object : IRequestResultListener<CommonResponse> {
                override fun onSuccess(result: CommonResponse) {
                    iCommonListener.onResult(HttpResponseCode.success.toInt(), "")
                }

                override fun onFail(code: Int, msg: String?) {
                    WKLogUtils.e("clear all pinned message fail, code: $code, msg: $msg")
                    iCommonListener.onResult(code, msg)
                }
            })
    }
}