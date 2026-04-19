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
import com.chat.wallet.redpacket.OpenRedPacketDialog
import com.chat.wallet.redpacket.RedPacketDetailActivity

class WKRedPacketProvider : WKChatBaseProvider() {
    override val itemViewType get() = WKContentType.WK_REDPACKET

    override fun getChatViewItem(parentView: ViewGroup, from: WKChatIteMsgFromType): View? =
        View.inflate(parentView.context, R.layout.chat_item_redpacket, null)

    override fun setData(adapterPosition: Int, parentView: View, entity: WKUIChatMsgItemEntity, from: WKChatIteMsgFromType) {
        val layout = parentView.findViewById<LinearLayout>(R.id.redpacketLayout)
        val remarkTv = parentView.findViewById<TextView>(R.id.remarkTv)
        val typeTv = parentView.findViewById<TextView>(R.id.typeTv)
        val content = entity.wkMsg.baseContentMsgModel as? WKRedPacketContent ?: return

        remarkTv.text = if (content.remark.isNullOrEmpty()) parentView.context.getString(R.string.redpacket_remark) else content.remark

        when (content.status) {
            1 -> {
                layout.setBackgroundResource(R.drawable.bg_redpacket_bubble_opened)
                typeTv.text = parentView.context.getString(R.string.redpacket_finished)
                applyRedPacketOpenedSkin(parentView, opened = true)
            }
            2 -> {
                layout.setBackgroundResource(R.drawable.bg_redpacket_bubble_opened)
                typeTv.text = parentView.context.getString(R.string.redpacket_expired)
                applyRedPacketOpenedSkin(parentView, opened = true)
            }
            else -> {
                if (content.status != 0) {
                    layout.setBackgroundResource(R.drawable.bg_redpacket_bubble_opened)
                    typeTv.text = parentView.context.getString(R.string.redpacket_received)
                    applyRedPacketOpenedSkin(parentView, opened = true)
                } else {
                    layout.setBackgroundResource(R.drawable.bg_redpacket_bubble)
                    typeTv.text = when (content.packetType) {
                        WKRedPacketContent.TYPE_GROUP_RANDOM -> parentView.context.getString(R.string.redpacket_type_group_random)
                        WKRedPacketContent.TYPE_GROUP_NORMAL -> parentView.context.getString(R.string.redpacket_type_group_normal)
                        WKRedPacketContent.TYPE_EXCLUSIVE -> parentView.context.getString(R.string.redpacket_type_exclusive)
                        else -> parentView.context.getString(R.string.redpacket)
                    }
                    applyRedPacketOpenedSkin(parentView, opened = false)
                }
            }
        }
        layout.setOnClickListener { v ->
            if (content.status == 0) {
                OpenRedPacketDialog(
                    v.context,
                    content.packetNo,
                    content.senderName,
                    content.remark,
                    entity.wkMsg.channelID,
                    entity.wkMsg.channelType,
                    content.packetType,
                    entity.wkMsg.clientMsgNO
                ).show()
            } else {
                val intent = Intent(v.context, RedPacketDetailActivity::class.java).putExtra("packet_no", content.packetNo)
                entity.wkMsg.channelID?.let { id ->
                    intent.putExtra("channel_id", id)
                    intent.putExtra("channel_type", entity.wkMsg.channelType.toInt())
                }
                v.context.startActivity(intent)
            }
        }
    }

    /** 已抢/领完/过期：灰红底 + 浅色字；未拆：正红底 + 白字 */
    private fun applyRedPacketOpenedSkin(parentView: View, opened: Boolean) {
        val ctx = parentView.context
        val remarkTv = parentView.findViewById<TextView>(R.id.remarkTv)
        val typeTv = parentView.findViewById<TextView>(R.id.typeTv)
        val divider = parentView.findViewById<View>(R.id.redpacketDivider)
        if (opened) {
            remarkTv.setTextColor(ContextCompat.getColor(ctx, R.color.wallet_rp_qq_opened_text_main))
            typeTv.setTextColor(ContextCompat.getColor(ctx, R.color.wallet_rp_qq_opened_text_sub))
            typeTv.alpha = 0.88f
            divider?.setBackgroundColor(ContextCompat.getColor(ctx, R.color.wallet_rp_qq_opened_divider))
            divider?.alpha = 1f
        } else {
            remarkTv.setTextColor(Color.WHITE)
            typeTv.setTextColor(Color.WHITE)
            typeTv.alpha = 0.75f
            divider?.setBackgroundColor(Color.WHITE)
            divider?.alpha = 0.2f
        }
    }
}
