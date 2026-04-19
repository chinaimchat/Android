package com.chat.security.ui.device.service;

import com.chat.base.base.WKBaseModel;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.net.ICommonListener;
import com.chat.base.net.IRequestResultListener;
import com.chat.base.net.entity.CommonResponse;
import com.chat.security.entity.DeviceEntity;

import java.util.List;

/**
 * 2020-10-22 17:24
 * 设备管理
 */
public class DeviceModel extends WKBaseModel {
    private DeviceModel() {
    }

    private static class DeviceModelBinder {
        final static DeviceModel deviceModel = new DeviceModel();
    }

    public static DeviceModel getInstance() {
        return DeviceModelBinder.deviceModel;
    }

    void getDeviceList(final IDeviceList iDeviceList) {
        request(createService(DeviceService.class).getDeviceList(), new IRequestResultListener<List<DeviceEntity>>() {
            @Override
            public void onSuccess(List<DeviceEntity> result) {
                iDeviceList.onResult(HttpResponseCode.success, "", result);
            }

            @Override
            public void onFail(int code, String msg) {
                iDeviceList.onResult(code, msg, null);
            }
        });
    }

    interface IDeviceList {
        void onResult(int code, String msg, List<DeviceEntity> list);
    }

    void deleteDevice(String deviceID, final ICommonListener iCommonLisenter) {
        request(createService(DeviceService.class).deleteDevice(deviceID), new IRequestResultListener<CommonResponse>() {
            @Override
            public void onSuccess(CommonResponse result) {
                iCommonLisenter.onResult(result.status, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                iCommonLisenter.onResult(code, msg);
            }
        });
    }
}
