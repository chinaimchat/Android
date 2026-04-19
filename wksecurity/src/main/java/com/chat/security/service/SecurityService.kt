package com.chat.security.service

import com.alibaba.fastjson.JSONObject
import com.chat.base.net.entity.CommonResponse
import com.chat.security.entity.UserInfo
import io.reactivex.rxjava3.core.Observable
import retrofit2.http.*

interface SecurityService {

    @GET("user/blacklists")
    fun getBlacklists(): Observable<List<UserInfo>>

    @POST("user/chatpwd")
    fun chatPwd(@Body jsonObject: JSONObject): Observable<CommonResponse>

    @POST("user/lockscreenpwd")
    fun lockScreenPwd(@Body jsonObject: JSONObject): Observable<CommonResponse>

    @DELETE("user/destroy/{code}")
    fun destroyAccount(@Path("code") code: String): Observable<CommonResponse>

    @POST("user/sms/destroy")
    fun sendDestroyCode(): Observable<CommonResponse>

    @PUT("user/my/setting")
    fun setting(@Body jsonObject: JSONObject): Observable<CommonResponse>

    @DELETE("user/lockscreenpwd")
    fun deleteLockScreenPwd(): Observable<CommonResponse>

    @PUT("user/lock_after_minute")
    fun updateLockAfterTime(@Body jsonObject: JSONObject): Observable<CommonResponse>

    @PUT("groups/{groupNo}/setting")
    fun updateGroupSetting(
        @Path("groupNo") groupNo: String?,
        @Body jsonObject: JSONObject?
    ): Observable<CommonResponse>


    @PUT("users/{uid}/setting")
    fun updateUserSetting(
        @Path("uid") uid: String,
        @Body jsonObject: JSONObject
    ): Observable<CommonResponse>


}