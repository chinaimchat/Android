package com.chat.base.msgitem

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.chat.base.R
import com.chat.base.endpoint.EndpointManager
import com.chat.base.endpoint.entity.WalletTipTappableRoute
import com.chat.base.msg.ChatAdapter
import com.chat.base.ui.components.NormalClickableContent
import com.chat.base.ui.components.NormalClickableSpan
import com.chat.base.ui.components.SystemMsgBackgroundColorSpan
import com.chat.base.utils.AndroidUtilities
import com.chat.base.utils.GroupTipNicknamePolicy
import com.chat.base.utils.StringUtils
import com.chat.base.utils.WKToastUtils
import com.xinbida.wukongim.entity.WKChannelType
import org.json.JSONObject

class WKSystemProvider(val type: Int) : WKChatBaseProvider() {
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
        get() = type

    override val layoutId: Int
        get() = R.layout.chat_system_layout

    override fun convert(
        helper: BaseViewHolder,
        item: WKUIChatMsgItemEntity
    ) {
        super.convert(helper, item)
        helper.getView<View>(R.id.systemRootView).setOnClickListener {
            (getAdapter() as? ChatAdapter)?.conversationContext?.hideSoftKeyboard()
        }
        val textView = helper.getView<TextView>(R.id.contentTv)
        if (type == WKContentType.msgPromptTime) {
            val content = item.wkMsg.content
            textView.setShadowLayer(AndroidUtilities.dp(5f).toFloat(), 0f, 0f, 0)
            val str = SpannableString(content)
            str.setSpan(
                SystemMsgBackgroundColorSpan(
                    ContextCompat.getColor(context, R.color.colorSystemBg),
                    AndroidUtilities.dp(5f),
                    AndroidUtilities.dp((2 * 5).toFloat())
                ),
                0,
                content.length,
                0
            )
            textView.text = str
            return
        }
        val rawJson = item.wkMsg.content
        if (type == WKContentType.redpacketOpen || StringUtils.isRedPacketClaimTipContentJson(rawJson)) {
            val plain = getShowContent(rawJson) ?: ""
            applyRedPacketClaimTipStyle(textView, plain, rawJson, item)
            return
        }
        if (type == WKContentType.tradeSystemNotify && StringUtils.isTemplateNotifyJson(rawJson)) {
            val tc = StringUtils.buildTemplateNotifyClickable(context, rawJson)
            if (tc != null) {
                applyTradeSystemTemplateNotify(textView, tc, item)
                return
            }
        }
        val content: String? = getShowContent(rawJson)
        textView.setShadowLayer(AndroidUtilities.dp(5f).toFloat(), 0f, 0f, 0)
        val str = SpannableString(content)
        str.setSpan(
            SystemMsgBackgroundColorSpan(
                ContextCompat.getColor(
                    context,
                    R.color.colorSystemBg
                ), AndroidUtilities.dp(5f), AndroidUtilities.dp((2 * 5).toFloat())
            ), 0, content!!.length, 0
        )
        textView.text = str
    }

    /**
     * 参考 QQ：红包图标 + 灰字主体 + 领取者昵称可点击进资料卡（需 extra 带 uid）+「红包」标红。
     */
    private fun applyRedPacketClaimTipStyle(
        textView: TextView,
        plain: String,
        rawJson: String,
        item: WKUIChatMsgItemEntity
    ) {
        val emoji = "\uD83E\uDDE7" // 🧧
        val full = emoji + plain
        val ss = SpannableString(full)
        val gray = ContextCompat.getColor(context, R.color.color999)
        val nameBlue = ContextCompat.getColor(context, R.color.blue)
        val emojiLen = emoji.length
        if (full.length > emojiLen) {
            ss.setSpan(
                ForegroundColorSpan(gray),
                emojiLen,
                full.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        val takeIdx = full.indexOf("领取了")
        val claimerUid = StringUtils.getRedPacketClaimTipClaimerUid(item.wkMsg.content)
        val link = item.iLinkClick
        val forbidden = GroupTipNicknamePolicy.parseForbiddenAddFriendFlag(rawJson)
        val blockNick = GroupTipNicknamePolicy.shouldBlockNicknameProfileJump(
            item.wkMsg.channelID,
            item.wkMsg.channelType,
            forbidden
        )
        var needLinks = false
        if (takeIdx > emojiLen && !claimerUid.isNullOrEmpty() && link != null && !blockNick) {
            needLinks = true
            val groupNo =
                if (item.wkMsg.channelType == WKChannelType.GROUP) item.wkMsg.channelID ?: "" else ""
            val uidFinal = claimerUid
            ss.setSpan(
                NormalClickableSpan(
                    false,
                    nameBlue,
                    NormalClickableContent(
                        NormalClickableContent.NormalClickableTypes.Remind,
                        if (groupNo.isNotEmpty()) "$uidFinal|$groupNo" else uidFinal
                    ),
                    object : NormalClickableSpan.IClick {
                        override fun onClick(view: View) {
                            link.onShowUserDetail(uidFinal, groupNo)
                        }
                    }
                ),
                emojiLen,
                takeIdx,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else if (takeIdx > emojiLen && !claimerUid.isNullOrEmpty() && link != null && blockNick) {
            needLinks = true
            ss.setSpan(
                NormalClickableSpan(
                    false,
                    nameBlue,
                    NormalClickableContent(
                        NormalClickableContent.NormalClickableTypes.Other,
                        ""
                    ),
                    object : NormalClickableSpan.IClick {
                        override fun onClick(view: View) {
                            WKToastUtils.getInstance().showToastNormal(
                                context.getString(R.string.group_tip_forbidden_nickname_profile)
                            )
                        }
                    }
                ),
                emojiLen,
                takeIdx,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else if (takeIdx > emojiLen) {
            ss.setSpan(
                ForegroundColorSpan(nameBlue),
                emojiLen,
                takeIdx,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        val jo = try {
            JSONObject(rawJson)
        } catch (_: Exception) {
            null
        }
        val suffixInfo = if (jo != null) {
            StringUtils.parseNotifyTappableSuffix(jo, plain, "红包")
        } else {
            null
        }
        if (applyWalletTipSuffixSpan(
                ss,
                plain,
                suffixInfo,
                emojiLen,
                context,
                item.wkMsg.channelID,
                item.wkMsg.channelType
            )
        ) {
            needLinks = true
        }
        textView.movementMethod = if (needLinks) LinkMovementMethod.getInstance() else null
        textView.setShadowLayer(0f, 0f, 0f, 0)
        textView.text = ss
    }

    /**
     * type=1012 且为 JSON 模板：与旧版纯文案并存；带 extra[].uid 时昵称可点进资料卡；
     * 句末 tappable_suffix（如「转账」）按 color_hint 上色并可点进详情。
     */
    private fun applyTradeSystemTemplateNotify(
        textView: TextView,
        tc: StringUtils.TemplateNotifyClickable,
        item: WKUIChatMsgItemEntity
    ) {
        val content = tc.text
        var needLinks = false
        val forbidden = GroupTipNicknamePolicy.parseForbiddenAddFriendFlag(item.wkMsg.content)
        val blockNick = GroupTipNicknamePolicy.shouldBlockNicknameProfileJump(
            item.wkMsg.channelID,
            item.wkMsg.channelType,
            forbidden
        )
        textView.setShadowLayer(AndroidUtilities.dp(5f).toFloat(), 0f, 0f, 0)
        val str = SpannableString(content)
        str.setSpan(
            SystemMsgBackgroundColorSpan(
                ContextCompat.getColor(context, R.color.colorSystemBg),
                AndroidUtilities.dp(5f),
                AndroidUtilities.dp((2 * 5).toFloat())
            ),
            0,
            content.length,
            0
        )
        val link = item.iLinkClick
        val groupNo =
            if (item.wkMsg.channelType == WKChannelType.GROUP) item.wkMsg.channelID ?: "" else ""
        if (link != null) {
            for (span in tc.spans) {
                if (span.start >= 0 && span.end <= content.length && span.start < span.end
                    && !span.uid.isNullOrEmpty()
                ) {
                    val uidFinal = span.uid!!
                    needLinks = true
                    if (blockNick) {
                        str.setSpan(
                            NormalClickableSpan(
                                false,
                                ContextCompat.getColor(context, R.color.blue),
                                NormalClickableContent(
                                    NormalClickableContent.NormalClickableTypes.Other,
                                    ""
                                ),
                                object : NormalClickableSpan.IClick {
                                    override fun onClick(view: View) {
                                        WKToastUtils.getInstance().showToastNormal(
                                            context.getString(R.string.group_tip_forbidden_nickname_profile)
                                        )
                                    }
                                }
                            ),
                            span.start,
                            span.end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    } else {
                        str.setSpan(
                            NormalClickableSpan(
                                false,
                                ContextCompat.getColor(context, R.color.blue),
                                NormalClickableContent(
                                    NormalClickableContent.NormalClickableTypes.Remind,
                                    if (groupNo.isNotEmpty()) "$uidFinal|$groupNo" else uidFinal
                                ),
                                object : NormalClickableSpan.IClick {
                                    override fun onClick(view: View) {
                                        link.onShowUserDetail(uidFinal, groupNo)
                                    }
                                }
                            ),
                            span.start,
                            span.end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            }
        }
        if (applyWalletTipSuffixSpan(
                str,
                content,
                tc.tappableSuffix,
                0,
                context,
                item.wkMsg.channelID,
                item.wkMsg.channelType
            )
        ) {
            needLinks = true
        }
        textView.movementMethod = if (needLinks) LinkMovementMethod.getInstance() else null
        textView.text = str
    }

    /**
     * 句末「红包」「转账」等：可点时走 {@link WalletTipTappableRoute}；仅展示色时无点击。
     * @return 是否添加了可点击后缀
     */
    private fun applyWalletTipSuffixSpan(
        str: SpannableString,
        plain: String,
        info: StringUtils.TappableSuffixInfo?,
        emojiPrefixLen: Int,
        ctx: Context,
        channelId: String?,
        channelType: Byte
    ): Boolean {
        if (info == null || info.start < 0 || info.end > plain.length || info.start >= info.end) {
            return false
        }
        val st = info.start + emojiPrefixLen
        val en = info.end + emojiPrefixLen
        if (en > plain.length + emojiPrefixLen) {
            return false
        }
        val color = suffixColorForTappable(ctx, info)
        when {
            info.opensRedPacketDetail() -> {
                str.setSpan(
                    NormalClickableSpan(
                        false,
                        color,
                        NormalClickableContent(
                            NormalClickableContent.NormalClickableTypes.Other,
                            ""
                        ),
                        object : NormalClickableSpan.IClick {
                            override fun onClick(view: View) {
                                val route = WalletTipTappableRoute.redPacket(
                                    ctx,
                                    info.packetNo,
                                    channelId ?: "",
                                    channelType
                                )
                                EndpointManager.getInstance().invoke(WalletTipTappableRoute.ENDPOINT_SID, route)
                            }
                        }
                    ),
                    st,
                    en,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                return true
            }
            info.opensTransferDetail() -> {
                str.setSpan(
                    NormalClickableSpan(
                        false,
                        color,
                        NormalClickableContent(
                            NormalClickableContent.NormalClickableTypes.Other,
                            ""
                        ),
                        object : NormalClickableSpan.IClick {
                            override fun onClick(view: View) {
                                val route = WalletTipTappableRoute.transfer(ctx, info.transferNo)
                                EndpointManager.getInstance().invoke(WalletTipTappableRoute.ENDPOINT_SID, route)
                            }
                        }
                    ),
                    st,
                    en,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                return true
            }
            else -> {
                str.setSpan(
                    ForegroundColorSpan(color),
                    st,
                    en,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                return false
            }
        }
    }

    private fun suffixColorForTappable(ctx: Context, info: StringUtils.TappableSuffixInfo): Int {
        val h = info.colorHint.trim().lowercase()
        if (h == "blue") {
            return ContextCompat.getColor(ctx, R.color.blue)
        }
        if (h == "red") {
            return ContextCompat.getColor(ctx, R.color.red)
        }
        return if (info.opensTransferDetail() || info.transferNo.isNotEmpty()) {
            ContextCompat.getColor(ctx, R.color.blue)
        } else {
            ContextCompat.getColor(ctx, R.color.red)
        }
    }
}