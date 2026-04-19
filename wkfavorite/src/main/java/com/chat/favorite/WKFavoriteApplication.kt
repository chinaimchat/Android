package com.chat.favorite

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import com.alibaba.fastjson.JSONObject
import com.google.android.material.snackbar.Snackbar
import com.chat.base.WKBaseApplication
import com.chat.base.config.WKApiConfig
import com.chat.base.endpoint.EndpointCategory
import com.chat.base.endpoint.EndpointHandler
import com.chat.base.endpoint.EndpointManager
import com.chat.base.endpoint.entity.ChatItemPopupMenu
import com.chat.base.endpoint.entity.ChatItemPopupMenu.IPopupItemClick
import com.chat.base.endpoint.entity.PersonalInfoMenu
import com.chat.base.msg.IConversationContext
import com.chat.base.msgitem.WKContentType
import com.chat.base.net.HttpResponseCode
import com.chat.base.utils.WKToastUtils
import com.chat.favorite.entity.FavoriteEntity
import com.chat.favorite.service.FavoriteModel
import com.chat.favorite.ui.FavoriteListActivity
import com.xinbida.wukongim.entity.WKMsg
import com.xinbida.wukongim.msgmodel.WKImageContent
import com.xinbida.wukongim.msgmodel.WKTextContent
import java.lang.ref.WeakReference

class WKFavoriteApplication {
    private lateinit var context: WeakReference<Context>
    lateinit var detailFavorite: FavoriteEntity

    private object SingletonInstance {
        val INSTANCE = WKFavoriteApplication()
    }

    companion object {
        val instance: WKFavoriteApplication
            get() = SingletonInstance.INSTANCE
    }

    fun init() {
        this.context = WeakReference<Context>(WKBaseApplication.getInstance().context)
        initListener()
    }


    private fun initListener() {
        EndpointManager.getInstance().setMethod(
            "favorite_item", EndpointCategory.wkChatPopupItem, 50
        ) { `object`: Any ->
            val mMsg1 =
                `object` as WKMsg
            if (mMsg1.type == WKContentType.WK_TEXT || mMsg1.type == WKContentType.WK_IMAGE) {
                return@setMethod ChatItemPopupMenu(R.mipmap.msg_fave,
                    context.get()!!.getString(R.string.favorite),
                    object : IPopupItemClick {
                        override fun onClick(
                            mMsg: WKMsg,
                            iConversationContext: IConversationContext
                        ) {
                            val jsonObject =
                                JSONObject()
                            if (mMsg.type == WKContentType.WK_TEXT) {
                                val textMsgModel =
                                    mMsg.baseContentMsgModel as WKTextContent
                                var content = textMsgModel.content
                                if (mMsg.remoteExtra.contentEditMsgModel != null) {
                                    content =
                                        mMsg.remoteExtra.contentEditMsgModel.getDisplayContent()
                                }
                                jsonObject["content"] = content
                            } else if (mMsg.type == WKContentType.WK_IMAGE) {
                                val imageContent =
                                    mMsg.baseContentMsgModel as WKImageContent
                                jsonObject["content"] = WKApiConfig.getShowUrl(imageContent.url)
                                jsonObject["width"] = imageContent.width
                                jsonObject["height"] = imageContent.height
                            }
                            var uniqueKey = mMsg.messageID
                            var authorUID = ""
                            var authorName = ""
                            if (TextUtils.isEmpty(uniqueKey)) uniqueKey = mMsg.clientMsgNO
                            if (mMsg.from != null) {
                                authorUID = mMsg.from.channelID
                                authorName = mMsg.from.channelName
                            }
                            addFavorite(
                                mMsg.type,
                                uniqueKey,
                                authorUID,
                                authorName,
                                jsonObject,
                                iConversationContext.chatActivity
                            )

                        }
                    })
            }
            null
        }
        EndpointManager.getInstance().setMethod(
            "favorite_list", EndpointCategory.personalCenter, 100
        ) {
            PersonalInfoMenu(
                R.mipmap.icon_followed,
                context.get()!!.getString(R.string.my_favorite),
                0
            ) {
                val intent = Intent(context.get(), FavoriteListActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.get()!!.startActivity(intent)
            }
        }
        EndpointManager.getInstance().setMethod(
            "favorite_add"
        ) { `object` ->
            val hashMap = `object` as HashMap<*, *>
            val type = hashMap["type"] as Int
            val authorUID = hashMap["author_uid"] as String
            val authorName = hashMap["author_name"] as String
            val uniqueKey = hashMap["unique_key"] as String
            val payload = hashMap["payload"] as JSONObject
            val activity = hashMap["activity"] as Activity
            addFavorite(type, uniqueKey, authorUID, authorName, payload, activity)
            null
        }
    }

    fun addFavorite(
        type: Int,
        uniqueKey: String,
        authorUID: String,
        authorName: String,
        jsonObject: JSONObject,
        activity: Activity
    ) {
        FavoriteModel()
            .add(
                type,
                uniqueKey,
                authorUID,
                authorName,
                jsonObject
            ) { code: Int, msg: String? ->
                if (code == HttpResponseCode.success.toInt()) {
                    val viewGroup =
                        (activity
                            .findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(
                            0
                        )
                    Snackbar.make(
                        viewGroup,
                        activity
                            .getString(R.string.favorite_added),
                        2000
                    )
                        .setAction(
                            activity
                                .getString(R.string.favorite_check)
                        ) {
                            val intent = Intent(
                                activity,
                                FavoriteListActivity::class.java
                            )
                            activity
                                .startActivity(intent)
                        }
                        .show()
                } else {
                    WKToastUtils.getInstance().showToastNormal(msg)
                }
            }
    }
}