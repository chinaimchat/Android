package com.chat.security.ui.device.service;

import com.chat.base.net.entity.CommonResponse;
import com.chat.security.entity.DeviceEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * 2020-10-22 17:25
 */
public interface DeviceService {
    @GET("user/devices")
    Observable<List<DeviceEntity>> getDeviceList();

    @DELETE("user/devices/{device_id}")
    Observable<CommonResponse> deleteDevice(@Path("device_id") String device_id);
}
