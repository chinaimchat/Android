package com.chat.rtc.multi

import android.text.TextUtils
import android.view.View
import androidx.core.content.ContextCompat
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.chat.base.ui.Theme
import com.chat.base.ui.components.AvatarView
import com.chat.base.ui.components.CheckBox
import com.chat.base.utils.AndroidUtilities
import com.chat.rtc.R
import com.xinbida.wukongim.entity.WKChannelType

class ChooseMemberAdapter :
    BaseQuickAdapter<GroupMemberEntity, BaseViewHolder>(R.layout.item_call_member) {


    override fun convert(holder: BaseViewHolder, item: GroupMemberEntity, payloads: List<Any>) {
        super.convert(holder, item, payloads)
        val groupMemberEntity = payloads[0] as GroupMemberEntity?
        if (groupMemberEntity != null) {
            val checkBox = holder.getView<CheckBox>(R.id.checkBox)
            val isChecked = groupMemberEntity.checked == 1 || groupMemberEntity.isCanCheck == 0
            checkBox.setChecked(isChecked, true)
            checkBox.setDrawBackground(isChecked)
        }
    }

    override fun convert(holder: BaseViewHolder, item: GroupMemberEntity) {
        val avatarView: AvatarView = holder.getView(R.id.avatarView)
        avatarView.showAvatar(item.member.memberUID, WKChannelType.PERSONAL)
        holder.setText(
            R.id.nameTv,
            if (TextUtils.isEmpty(item.member.memberRemark)) item.member.memberName else item.member.memberRemark
        )

        val isChecked = item.checked == 1 || item.isCanCheck == 0
        val checkBox: CheckBox = holder.getView(R.id.checkBox)
        checkBox.setResId(context, R.mipmap.round_check2)
        checkBox.setDrawBackground(isChecked)
        checkBox.setHasBorder(true)
        checkBox.setStrokeWidth(AndroidUtilities.dp(2f))
        checkBox.setBorderColor(ContextCompat.getColor(context, R.color.layoutColor))
        checkBox.setSize(24)
//        checkBox.setCheckOffset(AndroidUtilities.dp(2));
        //        checkBox.setCheckOffset(AndroidUtilities.dp(2));
        checkBox.setColor(
            if (item.isCanCheck == 1) Theme.colorAccount else Theme.colorAccountDisable,
            ContextCompat.getColor(context, R.color.white)
        )
        checkBox.visibility = View.VISIBLE
        checkBox.isEnabled = item.isCanCheck == 1
        checkBox.setChecked(isChecked, true)
    }
}