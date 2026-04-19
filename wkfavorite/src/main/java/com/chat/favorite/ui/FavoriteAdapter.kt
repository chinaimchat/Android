package com.chat.favorite.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.chat.base.act.WKWebViewActivity
import com.chat.base.config.WKApiConfig
import com.chat.base.endpoint.EndpointManager
import com.chat.base.endpoint.EndpointSID
import com.chat.base.endpoint.entity.ChatChooseContacts
import com.chat.base.endpoint.entity.ChooseChatMenu
import com.chat.base.entity.PopupMenuItem
import com.chat.base.glide.GlideUtils
import com.chat.base.net.HttpResponseCode
import com.chat.base.ui.components.AvatarView
import com.chat.base.ui.components.NormalClickableContent
import com.chat.base.ui.components.NormalClickableSpan
import com.chat.base.utils.StringUtils
import com.chat.base.utils.WKDialogUtils
import com.chat.base.utils.WKToastUtils
import com.chat.favorite.R
import com.chat.favorite.entity.FavoriteEntity
import com.chat.favorite.service.FavoriteModel
import com.xinbida.wukongim.WKIM
import com.xinbida.wukongim.entity.WKChannelType
import com.xinbida.wukongim.entity.WKSendOptions
import com.xinbida.wukongim.msgmodel.WKImageContent
import com.xinbida.wukongim.msgmodel.WKTextContent

class FavoriteAdapter(list: List<FavoriteEntity>) :
    BaseMultiItemQuickAdapter<FavoriteEntity, BaseViewHolder>() {
    init {
        addItemType(0, R.layout.item_no_data_layout)
        addItemType(1, R.layout.item_text_layout)
        addItemType(2, R.layout.item_img_layout)
        addItemType(3, R.layout.item_img_layout)
        setList(list)
    }

    override fun convert(holder: BaseViewHolder, item: FavoriteEntity) {
        if (item.type == 1 || item.type == 2 ||item.type ==3) {
            holder.setText(R.id.authTv, item.author_name)
            holder.setText(R.id.timeTv, item.created_at)
            val avatarView: AvatarView = holder.getView(R.id.avatarView)
            avatarView.setSize(20f)
            avatarView.showAvatar(
                item.author_uid,
                WKChannelType.PERSONAL,
                item.author_avatar
            )
            val content = item.payload?.get("content") as String
            when (item.type) {
                1 -> {
                    val contentTv: TextView = holder.getView(R.id.contentTv)
                    val spannableString = SpannableString(content)
                    //                    MoonUtil.identifyFaceExpression(getContext(), contentTv, content, MoonUtil.DEF_SCALE);
                    contentTv.movementMethod = LinkMovementMethod.getInstance()
                    val list = StringUtils.getStrUrls(content)
                    //                    List<Link> linkList = new ArrayList<>();
                    if (list.size > 0 && !TextUtils.isEmpty(content)) {
                        for (url in list) {
                            var fromIndex = 0
                            while (fromIndex >= 0) {
                                fromIndex = content.indexOf(url, fromIndex)
                                if (fromIndex >= 0) {
                                    spannableString.setSpan(
                                        StyleSpan(Typeface.BOLD), fromIndex,
                                        fromIndex + url.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                    spannableString.setSpan(
                                        NormalClickableSpan(true,
                                            ContextCompat.getColor(context, R.color.blue),
                                            NormalClickableContent(
                                                NormalClickableContent.NormalClickableTypes.Other,
                                                ""
                                            ),
                                            object : NormalClickableSpan.IClick {
                                                override fun onClick(view: View) {
                                                    val intent = Intent(
                                                        context,
                                                        WKWebViewActivity::class.java
                                                    )
                                                    intent.putExtra("url", url)
                                                    context.startActivity(intent)
                                                }
                                            }),
                                        fromIndex,
                                        fromIndex + url.length,
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                    fromIndex += url.length
                                }
                            }
                        }
                    }
                    contentTv.text = spannableString
                    showDialog(item.type,
                        spannableString.toString(),
                        holder.getView(R.id.contentTv),
                        item.unique_key!!,
                        holder.bindingAdapterPosition,
                        0,
                        0
                    )
                    showDialog(
                        item.type,
                        spannableString.toString(),
                        holder.getView(R.id.contentLayout),
                        item.unique_key!!,
                        holder.bindingAdapterPosition,
                        0,
                        0
                    )
                }
                2 -> {

                    val imgHeight = item.payload?.get("height") as Int
                    val imgWidth = item.payload?.get("width") as Int
                    showDialog(
                        item.type,
                        WKApiConfig.getShowUrl(content),
                        holder.getView(R.id.contentLayout),
                        item.unique_key!!,
                        holder.bindingAdapterPosition,
                        imgWidth,
                        imgHeight
                    )
                    GlideUtils.getInstance().showImg(
                        context,
                        WKApiConfig.getShowUrl(content),
                        holder.getView(R.id.imageView)
                    )

                }
            }
        }
    }


    @SuppressLint("ResourceType")
    private fun showDialog(type: Int,content:String,view: View, key: String, position: Int,width: Int ,height: Int) {
        val list: MutableList<PopupMenuItem> = ArrayList()

        list.add(
            PopupMenuItem(view.context.getString(com.chat.base.R.string.forward),
                com.chat.base.R.mipmap.msg_forward,
                PopupMenuItem.IClick {
                    if(type == 1){
                        val  textContent  = WKTextContent(content)
                        EndpointManager.getInstance().invoke(
                            EndpointSID.showChooseChatView,
                            ChooseChatMenu(ChatChooseContacts { list ->
                                for (channel in list) {
                                    val options = WKSendOptions()
                                    options.setting.receipt = channel.receipt
                                    WKIM.getInstance().msgManager
                                        .sendWithOptions(textContent, channel, options)
                                }
                            }, textContent)
                        )
                    }else if(type == 2){
                        val textContent  = WKImageContent()
                        textContent.url = content;
                        textContent.width = width;
                        textContent.height = height;
                        EndpointManager.getInstance().invoke(
                            EndpointSID.showChooseChatView,
                            ChooseChatMenu(ChatChooseContacts { list ->
                                for (channel in list) {
                                    val options = WKSendOptions()
                                    options.setting.receipt = channel.receipt
                                    WKIM.getInstance().msgManager
                                        .sendWithOptions(textContent, channel, options)
                                }
                            }, textContent)
                        )
                    }

                })
        )


        list.add(
            PopupMenuItem(context.getString(R.string.base_delete), R.mipmap.msg_delete,
                object : PopupMenuItem.IClick {
                    override fun onClick() {
                        FavoriteModel().delete(
                            key
                        ) { code: Int, msg: String? ->
                            if (code == HttpResponseCode.success.toInt()) {
                                removeAt(position)
                            } else WKToastUtils.getInstance().showToastNormal(msg)
                        }
                    }

                })
        )
        WKDialogUtils.getInstance().setViewLongClickPopup( view, list)
    }
}