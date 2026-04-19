package com.chat.label.adapter

import android.widget.LinearLayout
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.chat.base.entity.PopupMenuItem
import com.chat.base.utils.WKDialogUtils
import com.chat.label.R
import com.chat.label.entity.Label

/**
 * 2020-11-03 11:47
 */

class LabelAdapter : BaseQuickAdapter<Label, BaseViewHolder>(R.layout.item_label_layout) {
    override fun convert(holder: BaseViewHolder, item: Label) {
        holder.setText(R.id.nameTv, String.format("%s(%d)", item.name, item.count))
        //长按事件

        val list = ArrayList<PopupMenuItem>()
        list.add(
            PopupMenuItem(
                context.getString(R.string.str_delete),
                0,
                object : PopupMenuItem.IClick {
                    override fun onClick() {
                        iLongClick!!.onClick(item, 1)
                    }
                })
        )
        list.add(
            PopupMenuItem(
                context.getString(R.string.str_edit),
                0,
                object : PopupMenuItem.IClick {
                    override fun onClick() {
                        iLongClick!!.onClick(item, 2)
                    }
                })
        )
        WKDialogUtils.getInstance()
            .setViewLongClickPopup(holder.getView<LinearLayout>(R.id.contentLayout), list)
    }

    private var iLongClick: ILongClick? = null
    fun setILongClick(click: ILongClick) {
        iLongClick = click
    }

    interface ILongClick {
        fun onClick(label: Label, type: Int)
    }

}