package com.chat.label.adapter

import android.text.TextUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.chat.base.ui.components.AvatarView
import com.chat.label.R
import com.chat.label.entity.LabelMember
import com.xinbida.wukongim.entity.WKChannelType

/**
 * 2020-11-17 18:34
 * 标签成员
 */
class LabelMemberAdapter : BaseQuickAdapter<LabelMember, BaseViewHolder>(R.layout.item_label_member_layout) {
    override fun convert(holder: BaseViewHolder, item: LabelMember) {
        val avatarView = holder.getView<AvatarView>(R.id.avatarView)
        when (item.uid) {
            "-1" -> {
                holder.setGone(R.id.deleteIv, true)
                holder.setVisible(R.id.nameTv, false)
                avatarView.imageView.setImageResource(R.mipmap.icon_label_add)
            }
            "-2" -> {
                holder.setVisible(R.id.nameTv, false)
                holder.setGone(R.id.deleteIv, true)
                avatarView.imageView.setImageResource(R.mipmap.icon_label_delete)
            }
            else -> {
                if (TextUtils.isEmpty(item.remark)) {
                    holder.setText(R.id.nameTv, item.name)
                } else {
                    holder.setText(R.id.nameTv, item.remark)
                }
                holder.setVisible(R.id.nameTv, true)
                holder.setGone(R.id.deleteIv, !item.isShowDelete)
                avatarView.showAvatar(item.uid,WKChannelType.PERSONAL,item.avatarCacheKey)
            }
        }
    }

}