package com.chat.wallet.msg

import android.graphics.Color
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chat.base.msgitem.WKChatBaseProvider
import com.chat.base.msgitem.WKChatIteMsgFromType
import com.chat.base.msgitem.WKContentType
import com.chat.base.msgitem.WKUIChatMsgItemEntity
import com.chat.wallet.R
import com.chat.wallet.transfer.TransferDetailActivity

class WKTransferProvider : WKChatBaseProvider() {
    override val itemViewType get() = WKContentType.WK_TRANSFER

    override fun getChatViewItem(parentView: ViewGroup, from: WKChatIteMsgFromType): View? =
        View.inflate(parentView.context, R.layout.chat_item_transfer, null)

    override fun setData(adapterPosition: Int, parentView: View, entity: WKUIChatMsgItemEntity, from: WKChatIteMsgFromType) {
        val layout = parentView.findViewById<LinearLayout>(R.id.transferLayout)
        val amountTv = parentView.findViewById<TextView>(R.id.amountTv)
        val remarkTv = parentView.findViewById<TextView>(R.id.remarkTv)
        val statusTv = parentView.findViewById<TextView>(R.id.statusTv)
        val content = entity.wkMsg.baseContentMsgModel as? WKTransferContent ?: return

        amountTv.text = String.format("¥%.2f", content.amount)
        remarkTv.text = if (content.remark.isNullOrEmpty()) parentView.context.getString(R.string.transfer) else content.remark

        when (content.status) {
            WKTransferContent.STATUS_ACCEPTED -> {
                layout.setBackgroundResource(R.drawable.bg_transfer_bubble_accepted)
                statusTv.text = parentView.context.getString(R.string.transfer_accepted)
                applyTransferDoneSkin(parentView, done = true)
            }
            WKTransferContent.STATUS_REFUNDED -> {
                layout.setBackgroundResource(R.drawable.bg_transfer_bubble_accepted)
                statusTv.text = parentView.context.getString(R.string.transfer_refunded)
                applyTransferDoneSkin(parentView, done = true)
            }
            else -> {
                layout.setBackgroundResource(R.drawable.bg_transfer_bubble)
                statusTv.text = parentView.context.getString(R.string.transfer_pending)
                applyTransferDoneSkin(parentView, done = false)
            }
        }
        layout.setOnClickListener { v ->
            val intent = Intent(v.context, TransferDetailActivity::class.java)
                .putExtra("transfer_no", content.transferNo)
            entity.wkMsg.channelID?.let { id ->
                intent.putExtra("channel_id", id)
                intent.putExtra("channel_type", entity.wkMsg.channelType.toInt())
            }
            entity.wkMsg.clientMsgNO?.let { intent.putExtra("client_msg_no", it) }
            v.context.startActivity(intent)
        }
    }

    /** 已接收/已退回：灰橙底 + 浅色字；待接收：亮橙底 + 白字 */
    private fun applyTransferDoneSkin(parentView: View, done: Boolean) {
        val ctx = parentView.context
        val amountTv = parentView.findViewById<TextView>(R.id.amountTv)
        val remarkTv = parentView.findViewById<TextView>(R.id.remarkTv)
        val statusTv = parentView.findViewById<TextView>(R.id.statusTv)
        val tagTv = parentView.findViewById<TextView>(R.id.transferTagTv)
        val divider = parentView.findViewById<View>(R.id.transferDivider)
        if (done) {
            amountTv.setTextColor(ContextCompat.getColor(ctx, R.color.wallet_transfer_accepted_text_main))
            remarkTv.setTextColor(ContextCompat.getColor(ctx, R.color.wallet_transfer_accepted_text_sub))
            remarkTv.alpha = 0.9f
            statusTv.setTextColor(ContextCompat.getColor(ctx, R.color.wallet_transfer_accepted_text_sub))
            statusTv.alpha = 0.88f
            tagTv.setTextColor(ContextCompat.getColor(ctx, R.color.wallet_transfer_accepted_text_sub))
            tagTv.alpha = 0.88f
            divider?.setBackgroundColor(ContextCompat.getColor(ctx, R.color.wallet_transfer_accepted_divider))
            divider?.alpha = 1f
        } else {
            amountTv.setTextColor(Color.WHITE)
            remarkTv.setTextColor(Color.WHITE)
            remarkTv.alpha = 0.8f
            statusTv.setTextColor(Color.WHITE)
            statusTv.alpha = 0.75f
            tagTv.setTextColor(Color.WHITE)
            tagTv.alpha = 0.75f
            divider?.setBackgroundColor(Color.WHITE)
            divider?.alpha = 0.2f
        }
    }
}
