package com.chat.security.ui.device.service;

import com.chat.base.base.WKBasePresenter;
import com.chat.base.base.WKBaseView;
import com.chat.security.entity.DeviceEntity;

import java.util.List;

/**
 * 2020-10-22 17:25
 */
public class DeviceContract {
    public interface DevicePresenter extends WKBasePresenter {
        void getDeviceList();

        void deleteDevice(String deviceID);
    }

    public interface DeviceView extends WKBaseView {
        void setDeviceList(List<DeviceEntity> list);

        void setDeleteDevice(String deviceID);
    }
}
