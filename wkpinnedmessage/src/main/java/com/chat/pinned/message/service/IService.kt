package com.chat.pinned.message.service

import com.alibaba.fastjson.JSONObject
import com.chat.base.net.entity.CommonResponse
import com.chat.pinned.message.entity.SyncPinnedMsg
import io.reactivex.rxjava3.core.Observable
import retrofit2.http.Body
import retrofit2.http.POST

interface IService {

    @POST("message/pinned")
    fun pinMessage(@Body jsonObject: JSONObject): Observable<CommonResponse>

    @POST("message/pinned/sync")
    fun syncPinMessage(@Body jsonObject: JSONObject): Observable<SyncPinnedMsg>

    @POST("message/pinned/clear")
    fun clear(@Body jsonObject: JSONObject): Observable<CommonResponse>
}