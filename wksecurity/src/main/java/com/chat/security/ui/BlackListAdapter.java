package com.chat.security.ui;

import android.text.TextUtils;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.base.ui.components.AvatarView;
import com.chat.security.R;
import com.chat.security.entity.UserInfo;
import com.xinbida.wukongim.entity.WKChannelType;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 2020-07-10 13:27
 * 黑名单
 */
public class BlackListAdapter extends BaseQuickAdapter<UserInfo, BaseViewHolder> {
    BlackListAdapter(List<UserInfo> list) {
        super(R.layout.item_black_list_layout, list);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, UserInfo userInfo) {
        baseViewHolder.setText(R.id.nameTv, TextUtils.isEmpty(userInfo.remark) ? userInfo.name : userInfo.remark);
        AvatarView avatarView= baseViewHolder.getView(R.id.avatarView);
        avatarView.showAvatar(userInfo.uid, WKChannelType.PERSONAL);
    }
}
