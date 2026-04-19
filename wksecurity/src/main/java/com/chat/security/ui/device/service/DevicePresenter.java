package com.chat.security.ui.device.service;

import com.chat.base.net.HttpResponseCode;

import java.lang.ref.WeakReference;

/**
 * 2020-10-22 17:27
 */
public class DevicePresenter implements DeviceContract.DevicePresenter {
    private final WeakReference<DeviceContract.DeviceView> view;

    public DevicePresenter(DeviceContract.DeviceView deviceView) {
        view = new WeakReference<>(deviceView);
    }

    @Override
    public void getDeviceList() {
        DeviceModel.getInstance().getDeviceList((code, msg, list) -> {
            if (code == HttpResponseCode.success) {
                if (view.get() != null)
                    view.get().setDeviceList(list);
            } else {
                view.get().hideLoading();
                view.get().showError(msg);
            }
        });
    }

    @Override
    public void deleteDevice(String deviceID) {
        DeviceModel.getInstance().deleteDevice(deviceID, (code, msg) -> {
            if (code == HttpResponseCode.success) {
                if (view.get() != null) view.get().setDeleteDevice(deviceID);
            } else {
                view.get().hideLoading();
                view.get().showError(msg);
            }
        });
    }

    @Override
    public void showLoading() {

    }
}
