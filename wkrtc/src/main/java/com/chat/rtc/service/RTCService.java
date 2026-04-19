package com.chat.rtc.service;

import com.alibaba.fastjson.JSONObject;
import com.chat.base.net.entity.CommonResponse;
import com.chat.rtc.entity.RTCToken;
import com.chat.rtc.entity.VideoRoom;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * 5/7/21 6:21 PM
 */
interface RTCService {
    @POST("rtc/rooms")
    Observable<VideoRoom> getRoomID(@Body JSONObject jsonObject);

    @GET("rtc/rooms/{id}/token")
    Observable<RTCToken> getRTCToken(@Path("id") String roomID);

    @POST("rtc/rooms/{id}/invoke")
    Observable<CommonResponse> invokeJoinRoom(@Path("id") String roomID, @Body JSONObject jsonObject);

    @POST("rtc/p2p/invoke")
    Observable<CommonResponse> inviteP2PCall(@Body JSONObject jsonObject);

    @POST("rtc/p2p/accept")
    Observable<CommonResponse> acceptP2PCall(@Body JSONObject jsonObject);

    @POST("rtc/p2p/refuse")
    Observable<CommonResponse> refuseP2PCall(@Body JSONObject jsonObject);

    @POST("rtc/p2p/cancel")
    Observable<CommonResponse> cancelP2PCall(@Body JSONObject jsonObject);

    @POST("rtc/p2p/hangup")
    Observable<CommonResponse> hangupP2PCall(@Body JSONObject jsonObject);

    @POST("rtc/rooms/{id}/refuse")
    Observable<CommonResponse> multiRefuse(@Path("id") String roomID);

    @POST("rtc/rooms/{id}/hangup")
    Observable<CommonResponse> multiHangup(@Path("id") String roomID);

    @POST("rtc/rooms/{id}/joined")
    Observable<CommonResponse> multiJoined(@Path("id") String roomID);
}
