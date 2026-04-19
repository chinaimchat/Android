package com.chat.customerservice.service

import com.alibaba.fastjson.JSONObject
import com.chat.customerservice.entity.ChatInfo
import com.chat.customerservice.entity.RegisterResult
import io.reactivex.rxjava3.core.Observable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface CustomerService {
    @POST("hotline/widget/{appID}/visitor")
    fun initVisitor(
        @Path("appID") appID: String,
        @Body json: JSONObject
    ): Observable<RegisterResult>

    @POST("hotline/visitor/topic/channel")
    fun getChatInfo(@Body json:JSONObject):Observable<ChatInfo>
}