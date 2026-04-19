package com.chat.favorite.service

import android.text.TextUtils
import com.alibaba.fastjson.JSONObject
import com.chat.favorite.entity.FavoriteEntity
import com.chat.base.base.WKBaseModel
import com.chat.base.net.HttpResponseCode
import com.chat.base.net.ICommonListener
import com.chat.base.net.IRequestResultListener
import com.chat.base.net.entity.CommonResponse
import com.xinbida.wukongim.WKIM
import com.xinbida.wukongim.entity.WKChannelType

class FavoriteModel : WKBaseModel() {

    fun list(page: Int, iList: IList) {
        request(
            createService(
                FavoriteService::class.java
            ).list(page, 20), object : IRequestResultListener<List<FavoriteEntity>> {
                override fun onSuccess(result: List<FavoriteEntity>) {
                    var i = 0
                    val size = result.size
                    while (i < size) {
                        val channel = WKIM.getInstance().channelManager.getChannel(
                            result[i].author_uid,
                            WKChannelType.PERSONAL
                        )
                        if (channel != null) {
                            result[i].author_avatar = channel.avatarCacheKey
                            result[i].author_name =
                                if (TextUtils.isEmpty(channel.channelRemark)) channel.channelName else channel.channelRemark
                        }
                        i++
                    }
                    iList.onResult(HttpResponseCode.success.toInt(), "", result)
                }

                override fun onFail(code: Int, msg: String) {
                    iList.onResult(code, msg, null)
                }
            })
    }

    interface IList {
        fun onResult(code: Int, msg: String?, list: List<FavoriteEntity>?)
    }

    fun delete(id: String, listener: ICommonListener) {
        request(
            createService(FavoriteService::class.java).delete(
                id
            ), object : IRequestResultListener<CommonResponse> {
                override fun onSuccess(result: CommonResponse) {
                    listener.onResult(result.status, result.msg)
                }

                override fun onFail(code: Int, msg: String) {
                    listener.onResult(code, msg)
                }
            })
    }

    fun add(
        type: Int,
        unique_key: String?,
        author_uid: String?,
        author_name: String?,
        payload: JSONObject?,
        listener: ICommonListener
    ) {
        val jsonObject = JSONObject()
        jsonObject["type"] = type
        jsonObject["unique_key"] = unique_key
        jsonObject["author_name"] = author_name
        jsonObject["author_uid"] = author_uid
        jsonObject["payload"] = payload
        request(
            createService(FavoriteService::class.java).add(
                jsonObject
            ), object : IRequestResultListener<CommonResponse> {
                override fun onSuccess(result: CommonResponse) {
                    listener.onResult(result.status, result.msg)
                }

                override fun onFail(code: Int, msg: String) {
                    listener.onResult(code, msg)
                }
            })
    }
}