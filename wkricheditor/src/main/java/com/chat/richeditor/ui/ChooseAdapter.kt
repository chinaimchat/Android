package com.chat.richeditor.ui

import android.text.TextUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.chat.base.ui.components.AvatarView
import com.chat.richeditor.R
import com.xinbida.wukongim.entity.WKChannelMember
import com.xinbida.wukongim.entity.WKChannelType

class ChooseAdapter :
    BaseQuickAdapter<WKChannelMember, BaseViewHolder>(R.layout.item_choose_layout) {
    override fun convert(holder: BaseViewHolder, item: WKChannelMember) {
        val avatarView: AvatarView = holder.getView(R.id.avatarView)
        avatarView.showAvatar(
            item.memberUID,
            WKChannelType.PERSONAL,
            item.memberAvatarCacheKey
        )
        holder.setText(
            R.id.nameTv,
            if (TextUtils.isEmpty(item.memberRemark)) item.memberName else item.memberRemark
        )
    }

}