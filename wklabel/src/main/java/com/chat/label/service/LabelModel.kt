package com.chat.label.service

import android.text.TextUtils
import com.alibaba.fastjson.JSONObject
import com.chat.base.base.WKBaseModel
import com.chat.base.net.HttpResponseCode
import com.chat.base.net.IRequestResultListener
import com.chat.base.net.entity.CommonResponse
import com.chat.label.entity.Label
import com.xinbida.wukongim.WKIM
import com.xinbida.wukongim.entity.WKChannelType

/**
 * 2020-11-03 10:05
 * 标签
 */
class LabelModel : WKBaseModel() {
    fun getLabels(iGetLabels: IGetLabels) {
        request(createService(LabelService::class.java).getLabels(), object : IRequestResultListener<List<Label>> {
            override fun onSuccess(result: List<Label>?) {
                iGetLabels.onResult(HttpResponseCode.success.toInt(), "", result!!.toList())
            }

            override fun onFail(code: Int, msg: String?) {
                iGetLabels.onResult(code, msg.toString(), emptyList())
            }
        })
    }

    interface IGetLabels {
        fun onResult(code: Int, msg: String, list: List<Label>)
    }

    fun add(name: String, members: List<String>, iLabel: ILabelCommon) {
        val jsonObject = JSONObject()
        jsonObject["name"] = name
        jsonObject["member_uids"] = members
        request(createService(LabelService::class.java).addLabel(jsonObject), object : IRequestResultListener<CommonResponse> {
            override fun onSuccess(result: CommonResponse?) {
                iLabel.onResult(HttpResponseCode.success.toInt(), "")
            }

            override fun onFail(code: Int, msg: String?) {
                iLabel.onResult(code, msg.toString())
            }
        })
    }

    fun delete(id: String, iLabel: ILabelCommon) {
        request(createService(LabelService::class.java).deleteLabel(id), object : IRequestResultListener<CommonResponse> {
            override fun onSuccess(result: CommonResponse?) {
                iLabel.onResult(HttpResponseCode.success.toInt(), "")
            }

            override fun onFail(code: Int, msg: String?) {
                iLabel.onResult(code, msg.toString())
            }
        })
    }

    fun update(id: String, name: String, members: List<String>, iLabel: ILabelCommon) {
        val jsonObject = JSONObject()
        jsonObject["name"] = name
        jsonObject["member_uids"] = members
        request(createService(LabelService::class.java).updateLabel(id, jsonObject), object : IRequestResultListener<CommonResponse> {
            override fun onSuccess(result: CommonResponse?) {
                iLabel.onResult(HttpResponseCode.success.toInt(), "")
            }

            override fun onFail(code: Int, msg: String?) {
                iLabel.onResult(code, msg.toString())
            }
        })
    }

    // 标签详情
    fun detail(id: String, iLabelDetail: ILabelDetail) {
        request(createService(LabelService::class.java).detailLabel(id), object : IRequestResultListener<Label> {
            override fun onSuccess(result: Label) {
                for (j in result.members!!.indices) {
                    val channel = WKIM.getInstance().channelManager.getChannel(result.members!![j].uid, WKChannelType.PERSONAL)
                    if (channel != null) {
                        if (!TextUtils.isEmpty(channel.avatar))
                            result.members!![j].avatarCacheKey = channel.avatarCacheKey
                        if (!TextUtils.isEmpty(channel.channelName))
                            result.members!![j].name = channel.channelName
                        if (!TextUtils.isEmpty(channel.channelRemark))
                            result.members!![j].remark = channel.channelRemark
                    }
                }
                iLabelDetail.onResult(HttpResponseCode.success.toInt(), "", result)
            }

            override fun onFail(code: Int, msg: String?) {
                iLabelDetail.onResult(code, msg.toString(), Label())
            }
        })

    }

    interface ILabelDetail {
        fun onResult(code: Int, msg: String, label: Label)
    }

    interface ILabelCommon {
        fun onResult(code: Int, msg: String)
    }
}