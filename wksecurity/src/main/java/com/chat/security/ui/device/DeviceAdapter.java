package com.chat.security.ui.device;

import android.text.TextUtils;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.security.R;
import com.chat.security.entity.DeviceEntity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 2020-10-22 17:00
 * 设备管理
 */
public class DeviceAdapter extends BaseQuickAdapter<DeviceEntity, BaseViewHolder> {
    DeviceAdapter(@Nullable List<DeviceEntity> data) {
        super(R.layout.item_device_layout, data);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, DeviceEntity deviceEntity) {
        baseViewHolder.setText(R.id.nameTv, deviceEntity.device_name);
        baseViewHolder.setText(R.id.descTv, TextUtils.isEmpty(deviceEntity.device_model) ? "" : deviceEntity.device_model);
        baseViewHolder.setGone(R.id.descTv, TextUtils.isEmpty(deviceEntity.device_model));
    }
}
