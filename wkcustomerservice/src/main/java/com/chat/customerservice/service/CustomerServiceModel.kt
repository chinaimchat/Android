package com.chat.customerservice.service

import com.alibaba.fastjson.JSONObject
import com.chat.customerservice.entity.ChatInfo
import com.chat.customerservice.entity.RegisterResult
import com.chat.base.WKBaseApplication
import com.chat.base.base.WKBaseModel
import com.chat.base.config.WKConfig
import com.chat.base.net.HttpResponseCode
import com.chat.base.net.IRequestResultListener
import com.chat.base.utils.WKDeviceUtils
import com.chat.base.utils.WKLogUtils
import com.chat.customerservice.WKCustomerServiceApplication
import java.util.*

class CustomerServiceModel : WKBaseModel() {
    fun initVisitor() {
        val json = JSONObject()
        json["vid"] = WKConfig.getInstance().uid
        json["site_title"] = WKBaseApplication.getInstance().appID
        json["not_register_token"] = 1
        json["local"] = 1
        json["timezone"] = TimeZone.getDefault().displayName
        val deviceJson = JSONObject()
        deviceJson["device"] = WKDeviceUtils.getInstance().deviceName
        deviceJson["model"] = WKDeviceUtils.getInstance().systemModel
        deviceJson["os"] = "Android"
        deviceJson["version"] = WKDeviceUtils.getInstance().systemVersion
        json["device"] = deviceJson
        request(createService(CustomerService::class.java).initVisitor(
            WKBaseApplication.getInstance().appID,
            json
        ),
            object : IRequestResultListener<RegisterResult> {
                override fun onSuccess(result: RegisterResult?) {
                }

                override fun onFail(code: Int, msg: String?) {
                    WKLogUtils.e("注册访客信息失败")
                }
            })
    }

    fun getChatInfo(iChatInfo: IChatInfo) {
        val json = JSONObject()
        json["topic_id"] = 0
        json["appid"] = WKCustomerServiceApplication.instance.customerServiceAppId
        request(createService(CustomerService::class.java).getChatInfo(json),
            object : IRequestResultListener<ChatInfo> {
                override fun onSuccess(result: ChatInfo?) {
                    iChatInfo.onResult(HttpResponseCode.success.toInt(), "", result)
                }

                override fun onFail(code: Int, msg: String?) {
                    iChatInfo.onResult(code, msg!!, null)
                }
            })
    }

    interface IChatInfo {
        fun onResult(code: Int, msg: String, chatInfo: ChatInfo?)
    }
}