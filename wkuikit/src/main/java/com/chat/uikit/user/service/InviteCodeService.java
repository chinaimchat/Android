package com.chat.uikit.user.service;

import com.chat.base.net.entity.CommonResponse;
import com.chat.uikit.enity.InviteCodeEntity;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.GET;
import retrofit2.http.PUT;

public interface InviteCodeService {
    @GET("invite")
    Observable<InviteCodeEntity> getInviteCode();

    @PUT("invite/status")
    Observable<CommonResponse> switchInviteStatus();
}
