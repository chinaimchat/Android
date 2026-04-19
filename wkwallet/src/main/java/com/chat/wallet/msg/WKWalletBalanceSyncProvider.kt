package com.chat.wallet.msg

import android.view.View
import android.view.ViewGroup
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.chat.base.msgitem.WKChatBaseProvider
import com.chat.base.msgitem.WKChatIteMsgFromType
import com.chat.base.msgitem.WKContentType
import com.chat.base.msgitem.WKUIChatMsgItemEntity
import com.chat.wallet.R

/**
 * type=1020 不在会话内展示为普通气泡。
 */
class WKWalletBalanceSyncProvider : WKChatBaseProvider() {

    override fun getChatViewItem(parentView: ViewGroup, from: WKChatIteMsgFromType): View? {
        return null
    }

    override fun setData(
        adapterPosition: Int,
        parentView: View,
        uiChatMsgItemEntity: WKUIChatMsgItemEntity,
        from: WKChatIteMsgFromType
    ) {
    }

    override val itemViewType: Int
        get() = WKContentType.WK_WALLET_BALANCE_SYNC

    override val layoutId: Int
        get() = R.layout.chat_item_wallet_balance_sync

    override fun convert(helper: BaseViewHolder, item: WKUIChatMsgItemEntity) {
        super.convert(helper, item)
        helper.itemView.layoutParams?.height = 0
        helper.itemView.visibility = View.GONE
    }
}
