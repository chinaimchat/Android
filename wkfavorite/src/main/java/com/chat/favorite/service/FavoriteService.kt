package com.chat.favorite.service

import com.alibaba.fastjson.JSONObject
import com.chat.favorite.entity.FavoriteEntity
import com.chat.base.net.entity.CommonResponse
import io.reactivex.rxjava3.core.Observable
import retrofit2.http.*

interface FavoriteService {
    @GET("favorite/my")
    fun list(
        @Query("page_index") page: Int,
        @Query("page_size") page_size: Int
    ): Observable<List<FavoriteEntity>>

    @DELETE("favorites/{id}")
    fun delete(@Path("id") id: String): Observable<CommonResponse>

    @POST("favorites")
    fun add(@Body jsonObject: JSONObject): Observable<CommonResponse>
}

