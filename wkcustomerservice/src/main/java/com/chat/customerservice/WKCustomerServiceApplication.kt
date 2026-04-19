package com.chat.customerservice

import android.content.Context
import android.text.TextUtils
import androidx.activity.ComponentActivity
import com.chat.base.config.WKConfig
import com.chat.base.endpoint.EndpointCategory
import com.chat.base.endpoint.EndpointManager
import com.chat.base.endpoint.EndpointSID
import com.chat.base.endpoint.entity.ChatViewMenu
import com.chat.base.endpoint.entity.ContactsMenu
import com.chat.base.utils.WKToastUtils
import com.chat.customerservice.entity.ChatInfo
import com.chat.customerservice.service.CustomerServiceModel

class WKCustomerServiceApplication private constructor() {
    var customerServiceAppId = "wukongchat"

    private object SingletonInstance {
        val INSTANCE = WKCustomerServiceApplication()
    }

    companion object {
        val instance: WKCustomerServiceApplication
            get() = SingletonInstance.INSTANCE
    }

    fun init(appID: String) {
        customerServiceAppId = appID
        addListener()
    }

    fun init() {
        init(customerServiceAppId)
    }

    private fun addListener() {
        if (!TextUtils.isEmpty(WKConfig.getInstance().uid))
            CustomerServiceModel().initVisitor()
        EndpointManager.getInstance().setMethod("", EndpointCategory.loginMenus) {
            CustomerServiceModel().initVisitor()
            null
        }
        EndpointManager.getInstance().setMethod(
            "show_customer_service"
        ) { `object` ->
            val context = `object` as Context
            start(context)
            null
        }
        EndpointManager.getInstance().setMethod(
            EndpointCategory.mailList + "_customer_service",
            EndpointCategory.mailList,
            70
        ) { `object` ->
            val context = `object` as Context
            ContactsMenu(
                "customer_service",
                R.mipmap.ic_customer_service,
                context.getString(R.string.customer_service)
            ) {
                start(context)
            }
        }

    }

    private fun start(context: Context) {
        CustomerServiceModel().getChatInfo(object :
            CustomerServiceModel.IChatInfo {
            override fun onResult(code: Int, msg: String, chatInfo: ChatInfo?) {
                if (chatInfo != null) {
                    EndpointManager.getInstance().invoke(
                        EndpointSID.chatView,
                        ChatViewMenu(
                            context as ComponentActivity?,
                            chatInfo.channel_id,
                            chatInfo.channel_type,
                            0,
                            true
                        )
                    )
                } else {
                    WKToastUtils.getInstance().showToast(msg)
                }
            }
        })
    }
}