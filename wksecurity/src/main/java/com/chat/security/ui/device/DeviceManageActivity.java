package com.chat.security.ui.device;

import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.config.WKConfig;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.entity.UserInfoEntity;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.utils.WKDialogUtils;
import com.chat.security.R;
import com.chat.security.databinding.ActDeviceManageLayoutBinding;
import com.chat.security.entity.DeviceEntity;
import com.chat.security.service.SecurityModel;
import com.chat.security.ui.device.service.DeviceContract;
import com.chat.security.ui.device.service.DevicePresenter;

import java.util.ArrayList;
import java.util.List;

/**
 * 2020-10-22 16:47
 * 设备管理
 */
public class DeviceManageActivity extends WKBaseActivity<ActDeviceManageLayoutBinding> implements DeviceContract.DeviceView {
    private DeviceAdapter adapter;
    private DevicePresenter presenter;

    @Override
    protected ActDeviceManageLayoutBinding getViewBinding() {
        return ActDeviceManageLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.device_manage);
    }

    @Override
    protected void initPresenter() {
        presenter = new DevicePresenter(this);
    }

    @Override
    protected void initView() {
        wkVBinding.deviceLockDescTv.setText(String.format(getString(R.string.device_lock_desc), getString(R.string.app_name)));
        adapter = new DeviceAdapter(new ArrayList<>());
        initAdapter(wkVBinding.recyclerView, adapter);
        presenter.getDeviceList();
        int device_lock = WKConfig.getInstance().getUserInfo().setting.device_lock;
        wkVBinding.deviceSwitchView.setChecked(device_lock == 1);
        wkVBinding.deviceLayout.setVisibility(device_lock == 1 ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void initListener() {
        wkVBinding.deviceSwitchView.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                new SecurityModel().updateMySetting("device_lock", b ? 1 : 0, (code, msg) -> {
                    if (code != HttpResponseCode.success) {
                        wkVBinding.deviceSwitchView.setChecked(!b);
                        showToast(msg);
                    } else {
                        UserInfoEntity userInfoEntity = WKConfig.getInstance().getUserInfo();
                        userInfoEntity.setting.device_lock = b ? 1 : 0;
                        WKConfig.getInstance().saveUserInfo(userInfoEntity);
                        wkVBinding.deviceLayout.setVisibility(b ? View.VISIBLE : View.GONE);
                    }
                });
            }
        });
        adapter.setOnItemClickListener((adapter1, view1, position) -> {
            DeviceEntity deviceEntity = (DeviceEntity) adapter1.getItem(position);
            if (deviceEntity != null) {
                WKDialogUtils.getInstance().showDialog(this, getString(R.string.search_by_id), String.format(getString(R.string.last_login_time), deviceEntity.last_login), true, "", getString(R.string.delete_device), 0, ContextCompat.getColor(this, R.color.red), index -> {
                    if (index == 1){
                        presenter.deleteDevice(deviceEntity.device_id);

                    }
                });
            }
        });
    }

    @Override
    public void setDeviceList(List<DeviceEntity> list) {
        adapter.setList(list);
    }

    @Override
    public void setDeleteDevice(String deviceID) {
        boolean isExitLogin = false;
        int index = 0;
        for (int i = 0, size = adapter.getData().size(); i < size; i++) {
            if (adapter.getData().get(i).device_id.equals(deviceID)) {
                isExitLogin = adapter.getData().get(i).self == 1;
                index = i;
                break;
            }
        }
        if (isExitLogin) {
            EndpointManager.getInstance().invoke("exit_login", null);
        } else {
            adapter.removeAt(index);
        }
    }

    @Override
    public void showError(String msg) {
        showToast(msg);
    }

    @Override
    public void hideLoading() {
        loadingPopup.dismiss();
    }
}
