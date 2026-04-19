package com.chat.security.service

import com.alibaba.fastjson.JSONObject
import com.chat.base.base.WKBaseModel
import com.chat.base.config.WKConfig
import com.chat.base.net.HttpResponseCode
import com.chat.base.net.ICommonListener
import com.chat.base.net.IRequestResultListener
import com.chat.base.net.entity.CommonResponse
import com.chat.base.utils.WKCommonUtils
import com.chat.security.entity.UserInfo
import com.xinbida.wukongim.entity.WKChannelType

class SecurityModel : WKBaseModel() {

    fun updateMySetting(key: String, value: Int, iCommonListener: ICommonListener) {
        val jsonObject = JSONObject()
        jsonObject[key] = value
        request(
            createService(SecurityService::class.java).setting(jsonObject),
            object : IRequestResultListener<CommonResponse> {
                override fun onSuccess(result: CommonResponse) {
                    iCommonListener.onResult(result.status, result.msg)
                }

                override fun onFail(code: Int, msg: String) {
                    iCommonListener.onResult(code, msg)
                }
            })
    }


    /**
     * 获取黑名单列表
     *
     * @param iBlacklist 返回
     */
    fun getBlacklists(iBlacklist: IBlacklist) {
        request(
            createService(
                SecurityService::class.java
            ).getBlacklists(),
            object : IRequestResultListener<List<UserInfo>> {
                override fun onSuccess(result: List<UserInfo>) {
                    iBlacklist.onResult(HttpResponseCode.success.toInt(), "", result)
                }

                override fun onFail(code: Int, msg: String) {
                    iBlacklist.onResult(code, msg, emptyList())
                }
            })
    }

    interface IBlacklist {
        fun onResult(code: Int, msg: String?, list: List<UserInfo>)
    }

    /**
     * 设置聊天密码
     *
     * @param loginPwd        登录密码
     * @param chatPwd         聊天密码
     * @param iCommonListener 返回
     */
    fun chatPwd(loginPwd: String?, chatPwd: String?, iCommonListener: ICommonListener) {
        val jsonObject = JSONObject()
        jsonObject["login_pwd"] = loginPwd
        jsonObject["chat_pwd"] =
            WKCommonUtils.digest(String.format("%s%s", chatPwd, WKConfig.getInstance().uid))
        request(
            createService(SecurityService::class.java).chatPwd(jsonObject),
            object : IRequestResultListener<CommonResponse> {
                override fun onSuccess(result: CommonResponse) {
                    iCommonListener.onResult(result.status, result.msg)
                }

                override fun onFail(code: Int, msg: String) {
                    iCommonListener.onResult(code, msg)
                }
            })
    }

    fun lockScreenPwd(lockScreenPwd: String, iCommonListener: ICommonListener) {
        val jsonObject = JSONObject()
        jsonObject["lock_screen_pwd"] =
            WKCommonUtils.digest(String.format("%s%s", lockScreenPwd, WKConfig.getInstance().uid))
        request(
            createService(SecurityService::class.java).lockScreenPwd(jsonObject),
            object : IRequestResultListener<CommonResponse> {
                override fun onSuccess(result: CommonResponse) {
                    iCommonListener.onResult(result.status, result.msg)
                }

                override fun onFail(code: Int, msg: String) {
                    iCommonListener.onResult(code, msg)
                }
            })
    }

    fun destroyAccount(code: String, iCommonListener: ICommonListener) {
        request(
            createService(SecurityService::class.java).destroyAccount(code),
            object : IRequestResultListener<CommonResponse> {
                override fun onSuccess(result: CommonResponse) {
                    iCommonListener.onResult(result.status, result.msg)
                }

                override fun onFail(code: Int, msg: String) {
                    iCommonListener.onResult(code, msg)
                }
            })
    }

    fun sendDestroyCode(iCommonListener: ICommonListener) {
        request(
            createService(SecurityService::class.java).sendDestroyCode(),
            object : IRequestResultListener<CommonResponse> {
                override fun onSuccess(result: CommonResponse) {
                    iCommonListener.onResult(result.status, result.msg)
                }

                override fun onFail(code: Int, msg: String) {
                    iCommonListener.onResult(code, msg)
                }
            })
    }


    fun deleteLockScreenPwd(iCommonListener: ICommonListener) {
        request(
            createService(SecurityService::class.java).deleteLockScreenPwd(),
            object : IRequestResultListener<CommonResponse> {
                override fun onSuccess(result: CommonResponse) {
                    iCommonListener.onResult(result.status, result.msg)
                }

                override fun onFail(code: Int, msg: String) {
                    iCommonListener.onResult(code, msg)
                }
            })
    }

    fun updateLockTime(time: Int, iCommonListener: ICommonListener) {
        val jsonObject = JSONObject()
        jsonObject["lock_after_minute"] = time
        request(
            createService(SecurityService::class.java).updateLockAfterTime(jsonObject),
            object : IRequestResultListener<CommonResponse> {
                override fun onSuccess(result: CommonResponse) {
                    iCommonListener.onResult(result.status, result.msg)
                }

                override fun onFail(code: Int, msg: String) {
                    iCommonListener.onResult(code, msg)
                }
            })
    }

    fun updateChannelChatPwd(
        channelId: String,
        channelType: Byte,
        value: Int,
        iCommonListener: ICommonListener?
    ) {
        if (channelType == WKChannelType.GROUP) {
            updateGroupSetting(channelId, "chat_pwd_on", value, iCommonListener)
        } else {
            updateUserSetting(channelId, "chat_pwd_on", value, iCommonListener)
        }
    }

    private fun updateUserSetting(
        uid: String,
        key: String,
        value: Int,
        iCommonListener: ICommonListener?
    ) {
        val jsonObject = JSONObject()
        jsonObject[key] = value
        request<CommonResponse>(
            createService(SecurityService::class.java).updateUserSetting(
                uid,
                jsonObject
            ), object : IRequestResultListener<CommonResponse> {
                override fun onSuccess(result: CommonResponse) {
                    iCommonListener?.onResult(result.status, result.msg)
                }

                override fun onFail(code: Int, msg: String) {
                    iCommonListener?.onResult(code, msg)
                }
            })
    }

    private fun updateGroupSetting(
        groupNo: String,
        key: String,
        value: Int,
        iCommonListener: ICommonListener?
    ) {
        val jsonObject = JSONObject()
        jsonObject[key] = value
        request<CommonResponse>(
            createService(SecurityService::class.java).updateGroupSetting(
                groupNo,
                jsonObject
            ), object : IRequestResultListener<CommonResponse> {
                override fun onSuccess(result: CommonResponse) {
                    iCommonListener?.onResult(result.status, result.msg)
                }

                override fun onFail(code: Int, msg: String) {
                    iCommonListener?.onResult(code, msg)
                }
            })
    }
}