package com.chat.richeditor.msg

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Parcel
import android.os.Parcelable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.ImageView
import com.chat.base.act.WKWebViewActivity
import com.chat.base.config.WKApiConfig
import com.chat.base.config.WKConstants
import com.chat.base.endpoint.EndpointManager
import com.chat.base.endpoint.EndpointSID
import com.chat.base.endpoint.entity.UserDetailMenu
import com.chat.base.msg.ChatAdapter
import com.chat.base.msgitem.WKContentType
import com.chat.base.net.ud.WKDownloader
import com.chat.base.net.ud.WKProgressManager
import com.chat.base.ui.Theme
import com.chat.base.ui.components.AlignImageSpan
import com.chat.base.ui.components.NormalClickableContent
import com.chat.base.ui.components.NormalClickableSpan
import com.chat.base.utils.ImageUtils
import com.chat.base.utils.WKDialogUtils
import com.chat.richeditor.R
import com.chat.richeditor.component.RichStyles
import com.chat.richeditor.component.span.WMUnderlineSpan
import com.chat.richeditor.component.toolitem.WMToolImage
import com.xinbida.wukongim.WKIM
import com.xinbida.wukongim.entity.WKChannelType
import com.xinbida.wukongim.msgmodel.WKMessageContent
import com.xinbida.wukongim.msgmodel.WKMsgEntity
import org.json.JSONObject
import java.io.File


class RichTextContent : WKMessageContent {
    private var spanBuilder = SpannableStringBuilder()

    constructor()

    constructor(parcel: Parcel) : super(parcel)

    init {
        type = WKContentType.richText
    }

    override fun decodeMsg(jsonObject: JSONObject): WKMessageContent {
        content = jsonObject.optString("content")
        return this
    }

    override fun encodeMsg(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("content", content)
        return jsonObject
    }

    override fun getDisplayContent(): String {
        return content
    }

    override fun getSearchableWord(): String {
        return content
    }

    fun getShowSpan(
        context: Context,
        isRefresh: Boolean,
        clientMsgNO: String,
        chatAdapter: ChatAdapter
    ): CharSequence {
        if (spanBuilder.isNotEmpty() && !isRefresh) {
            return spanBuilder
        }
        if (isRefresh) {
            spanBuilder = SpannableStringBuilder()
        }
        spanBuilder.append(getDisplayContent())
        if (entities != null && entities.size > 0 && !TextUtils.isEmpty(getDisplayContent())) {
            for (entity in entities) {
                if (entity.type.equals(RichStyles.bold)) {
                    addBold(entity)
                } else if (entity.type.equals(RichStyles.italic)) {
                    addItalic(entity)
                } else if (entity.type.equals(RichStyles.color)) {
                    addColor(entity)
                } else if (entity.type.equals(RichStyles.underline)) {
                    addUnderline(entity)
                } else if (entity.type.equals(RichStyles.strikethrough)) {
                    addStrikethrough(entity)
                } else if (entity.type.equals(RichStyles.font)) {
                    addFont(entity)
                } else if (entity.type.equals(RichStyles.link)) {
                    addLink(entity, context)
                } else if (entity.type.equals(RichStyles.img)) {
                    addImg(entity, clientMsgNO, context, chatAdapter)
                }
            }
//            for (entity in entities) {
//                if (entity.type.equals(RichStyles.mention)) {
//                    addMention(entity, context, chatAdapter)
//                }
//            }
            addMentions(chatAdapter, context)
        }
        return spanBuilder
    }

    private fun addBold(entity: WKMsgEntity) {
        spanBuilder.setSpan(
            StyleSpan(Typeface.BOLD),
            entity.offset,
            (entity.offset + entity.length),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun addColor(entity: WKMsgEntity) {
        var value = entity.value
        if (!value.startsWith("#")) {
            value = "#$value"
        }
        spanBuilder.setSpan(
            ForegroundColorSpan(Color.parseColor(value)),
            entity.offset,
            (entity.offset + entity.length),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun addItalic(entity: WKMsgEntity) {
        spanBuilder.setSpan(
            StyleSpan(Typeface.ITALIC),
            entity.offset,
            (entity.offset + entity.length),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun addUnderline(entity: WKMsgEntity) {
        spanBuilder.setSpan(
            WMUnderlineSpan(),
            entity.offset,
            (entity.offset + entity.length),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun addStrikethrough(entity: WKMsgEntity) {
        spanBuilder.setSpan(
            StrikethroughSpan(),
            entity.offset,
            (entity.offset + entity.length),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun addFont(entity: WKMsgEntity) {
        val size = entity.value
        spanBuilder.setSpan(
            AbsoluteSizeSpan(size.toInt(), true),
            entity.offset,
            (entity.offset + entity.length),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun addLink(entity: WKMsgEntity, context: Context) {
        val clickSpan = object : ClickableSpan() {
            override fun onClick(p0: View) {
                val url =
                    getDisplayContent().substring(entity.offset, entity.offset + entity.length)
                val intent = Intent(context, WKWebViewActivity::class.java)
                intent.putExtra("url", url)
                context.startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                //  super.updateDrawState(ds)
            }
        }
        spanBuilder.setSpan(
            clickSpan,
            entity.offset,
            (entity.offset + entity.length),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun addMentions(chatAdapter: ChatAdapter, context: Context) {
        var allOffset = 0
        for (entity in entities) {
            if (entity.type.equals(RichStyles.mention)) {
                //addMention(entity, context, chatAdapter)

//                val uid = entity.value
                var showName = ""

                val start = entity.offset + allOffset
                val end = entity.offset + entity.length + allOffset

                val channel =
                    WKIM.getInstance().channelManager.getChannel(
                        entity.value,
                        WKChannelType.PERSONAL
                    )
                if (channel != null) {
                    showName = if (!TextUtils.isEmpty(channel.channelRemark))
                        channel.channelRemark
                    else channel.channelName
                }
                if (TextUtils.isEmpty(showName)) {
                    showName = getDisplayContent().substring(
                        entity.offset,
                        (entity.offset + entity.length)
                    )
                }
                if (!showName.startsWith("@")) showName = "@$showName"
                val oldName: String =
                    getDisplayContent().substring(entity.offset, entity.offset + entity.length)
                if (!TextUtils.isEmpty(showName)) {
                    if (!showName.startsWith("@")) showName = "@$showName"
                }
                showName = "$showName "

                val nameSpan = SpannableStringBuilder()
                nameSpan.append(showName)
                nameSpan.setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    showName.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                var groupID = ""
                if (chatAdapter.conversationContext.chatChannelInfo.channelType == WKChannelType.GROUP) {
                    groupID = chatAdapter.conversationContext.chatChannelInfo.channelID
                }
                val clickSpan = NormalClickableSpan(
                    false,
                    Theme.colorAccount,
                    NormalClickableContent(NormalClickableContent.NormalClickableTypes.Other, ""),
                    object : NormalClickableSpan.IClick {
                        override fun onClick(view: View) {
                            EndpointManager.getInstance().invoke(
                                EndpointSID.userDetailView,
                                UserDetailMenu(context, entity.value, groupID)
                            )
                        }
                    })
                nameSpan.setSpan(clickSpan, 0, showName.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spanBuilder.replace(start, end, nameSpan)

                if (!TextUtils.isEmpty(oldName)) {
                    val diff = showName.length - oldName.length
                    allOffset += diff
                }
            }
        }
    }

    private fun addMention(entity: WKMsgEntity, context: Context, chatAdapter: ChatAdapter) {
        var showName = ""
        val channel =
            WKIM.getInstance().channelManager.getChannel(
                entity.value,
                WKChannelType.PERSONAL
            )
        if (channel != null) {
            showName = if (!TextUtils.isEmpty(channel.channelRemark))
                channel.channelRemark
            else channel.channelName
        }
        if (TextUtils.isEmpty(showName)) {
            showName = getDisplayContent().substring(entity.offset, (entity.offset + entity.length))
        }
        if (!showName.startsWith("@")) showName = "@$showName"

        val nameSpan = SpannableStringBuilder()
        nameSpan.append(showName)
        nameSpan.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            showName.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        var groupID = ""
        if (chatAdapter.conversationContext.chatChannelInfo.channelType == WKChannelType.GROUP) {
            groupID = chatAdapter.conversationContext.chatChannelInfo.channelID
        }
        val clickSpan = NormalClickableSpan(
            false,
            Theme.colorAccount,
            NormalClickableContent(NormalClickableContent.NormalClickableTypes.Other, ""),
            object : NormalClickableSpan.IClick {
                override fun onClick(view: View) {
                    EndpointManager.getInstance().invoke(
                        EndpointSID.userDetailView,
                        UserDetailMenu(context, entity.value, groupID)
                    )
                }
            })
        nameSpan.setSpan(clickSpan, 0, showName.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spanBuilder.replace(entity.offset, (entity.offset + entity.length), nameSpan)
    }

    private fun addImg(
        entity: WKMsgEntity,
        clientMsgNO: String,
        context: Context,
        chatAdapter: ChatAdapter
    ) {

        val valueJson = JSONObject(entity.value)
        val width = valueJson.optInt("width")
        val height = valueJson.optInt("height")
        val url = valueJson.optString("url")
        val wH =
            ImageUtils.getInstance().getImageWidthAndHeightToTalk(width, height)

        val localPath = WKConstants.imageDir + url.hashCode()

        val file = File(localPath)
        if (file.exists()) {
            var bitmap = BitmapFactory.decodeFile(localPath)
            bitmap = WMToolImage.imageScale(bitmap, wH[0], wH[1])
            val textBuilder = SpannableStringBuilder("\ufffc")
            val alignImageSpan = object : AlignImageSpan(
                context,
                WKApiConfig.getShowUrl(url),
                width,
                height,
                bitmap,
                ALIGN_BOTTOM
            ) {
                override fun onClick(view: View?) {
                    val imageView = ImageView(context)
                    val list = ArrayList<ImageView>()
                    list.add(imageView)
                    val tempList = ArrayList<Any>()
                    tempList.add(WKApiConfig.getShowUrl(url))
                    WKDialogUtils.getInstance().showImagePopup(
                        context,
                        tempList,
                        list,
                        imageView,
                        0,
                        ArrayList(),
                        null,
                        null
                    )
                }

            }

            textBuilder.setSpan(
                alignImageSpan,
                0,
                1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spanBuilder.replace(entity.offset, (entity.offset + entity.length), textBuilder)
        } else {
            val bgBitmap =
                Theme.createBitmap(
                    wH[0],
                    wH[1],
                    context.getColor(R.color.homeColor)
                )
            val imageSpan = object : AlignImageSpan(
                context,
                url,
                width,
                height,
                bgBitmap,
                ALIGN_BOTTOM
            ) {
                override fun onClick(view: View?) {
                }
            }

            val text = SpannableStringBuilder(" ")
            text.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spanBuilder.replace(entity.offset, (entity.offset + entity.length), text)
            WKDownloader.instance.download(WKApiConfig.getShowUrl(url), localPath, object :
                WKProgressManager.IProgress {
                override fun onProgress(tag: Any?, progress: Int) {
                }

                override fun onSuccess(tag: Any?, path: String?) {
                    var index = 0
                    val size = chatAdapter.data.size
                    while (index < size) {
                        if (chatAdapter.data[index].wkMsg.clientMsgNO.equals(
                                clientMsgNO
                            )
                        ) {
                            chatAdapter.notifyItemChanged(
                                index,
                                chatAdapter.data[index]
                            )
                            break
                        }
                        index++
                    }
                }

                override fun onFail(tag: Any?, msg: String?) {
                }

            })
        }

    }


    override fun describeContents(): Int {
        return 0
    }


    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
    }

    companion object CREATOR : Parcelable.Creator<RichTextContent> {
        override fun createFromParcel(parcel: Parcel): RichTextContent {
            return RichTextContent(parcel)
        }

        override fun newArray(size: Int): Array<RichTextContent?> {
            return arrayOfNulls(size)
        }
    }

}