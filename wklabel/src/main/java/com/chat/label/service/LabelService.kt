package com.chat.label.service

import com.alibaba.fastjson.JSONObject
import com.chat.base.net.entity.CommonResponse
import com.chat.label.entity.Label
import io.reactivex.rxjava3.core.Observable
import retrofit2.http.*

/**
 *
 * 2020-11-03 10:22
 *
 */

interface LabelService {
    @GET("label")
    fun getLabels(): Observable<List<Label>>

    @POST("label")
    fun addLabel(@Body json: JSONObject): Observable<CommonResponse>

    @PUT("label/{id}")
    fun updateLabel(@Path("id") id: String, @Body json: JSONObject): Observable<CommonResponse>

    @DELETE("label/{id}")
    fun deleteLabel(@Path("id") id: String): Observable<CommonResponse>

    @GET("label/{id}")
    fun detailLabel(@Path("id") id: String): Observable<Label>
}