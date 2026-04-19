package com.chat.sticker.adapter

import android.widget.LinearLayout
import android.widget.TextView
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.chat.base.emoji.EmojiManager
import com.chat.base.ui.components.FilterImageView
import com.chat.base.utils.AndroidUtilities
import com.chat.sticker.R
import com.chat.sticker.entity.EmojiEntity

/**
 * 12/31/20 11:26 AM
 * emoji表情
 */
class EmojiAdapter(var width: Int) : BaseMultiItemQuickAdapter<EmojiEntity, BaseViewHolder>() {
    init {
        addItemType(0, R.layout.item_emoji_layout)
        addItemType(1, R.layout.item_emoji_text_layout)
        addItemType(2, R.layout.item_empty_layout)
    }

    override fun convert(holder: BaseViewHolder, item: EmojiEntity) {
        if (item.itemType == 0) {
            val imageView: FilterImageView = holder.getView(R.id.emojiIv)
            imageView.setAllCorners(0)
            imageView.strokeWidth = 0f
            val layoutParams = LinearLayout.LayoutParams(
                (width - AndroidUtilities.dp(80f)) / 8,
                (width - AndroidUtilities.dp(80f)) / 8
            )
            layoutParams.setMargins(
                AndroidUtilities.dp(5f),
                AndroidUtilities.dp(5f),
                AndroidUtilities.dp(5f),
                AndroidUtilities.dp(5f)
            )
            imageView.layoutParams = layoutParams
            imageView.setImageDrawable(EmojiManager.getInstance().getDrawable(context, item.text))
        } else if (item.itemType == 1) {
            val titleTv = holder.getView<TextView>(R.id.titleCenterTv)
            titleTv.text = item.text
            titleTv.typeface = AndroidUtilities.getTypeface("fonts/rmedium.ttf")
//            titleTv.setTextColor(0x7d746c)
        }
    }

}