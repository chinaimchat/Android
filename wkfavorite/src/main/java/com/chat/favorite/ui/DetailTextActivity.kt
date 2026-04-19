package com.chat.favorite.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.chat.base.base.WKBaseActivity
import com.chat.base.emoji.MoonUtil
import com.chat.base.endpoint.EndpointManager
import com.chat.base.endpoint.EndpointSID
import com.chat.base.endpoint.entity.ChatChooseContacts
import com.chat.base.endpoint.entity.ChooseChatMenu
import com.chat.base.entity.PopupMenuItem
import com.chat.base.utils.WKDialogUtils
import com.chat.base.utils.WKReader
import com.chat.base.utils.WKToastUtils
import com.chat.favorite.R
import com.chat.favorite.WKFavoriteApplication
import com.chat.favorite.databinding.ActDetailTextLayoutBinding
import com.google.android.material.snackbar.Snackbar
import com.xinbida.wukongim.WKIM
import com.xinbida.wukongim.entity.WKChannel
import com.xinbida.wukongim.msgmodel.WKTextContent

class DetailTextActivity : WKBaseActivity<ActDetailTextLayoutBinding>() {
    override fun getViewBinding(): ActDetailTextLayoutBinding {
        return ActDetailTextLayoutBinding.inflate(layoutInflater)
    }

    override fun setTitle(titleTv: TextView) {
        titleTv.setText(R.string.favorite_detail)
    }

    override fun initPresenter() {
        wkVBinding.refreshLayout.setEnableOverScrollDrag(true)
        wkVBinding.refreshLayout.setEnableLoadMore(false)
        wkVBinding.refreshLayout.setEnableRefresh(false)
    }


    override fun initView() {
        val entity = WKFavoriteApplication.instance.detailFavorite
        val content = entity.payload!!["content"] as String
        MoonUtil.identifyFaceExpression(
            this,
            wkVBinding.contentTv,
            content,
            MoonUtil.DEF_SCALE
        )
        wkVBinding.contentTv.movementMethod = LinkMovementMethod.getInstance()
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun initListener() {

        val list: MutableList<PopupMenuItem> = ArrayList()
        list.add(
            PopupMenuItem(
                getString(R.string.favorite_copy),
                R.mipmap.msg_copy,
                object : PopupMenuItem.IClick {
                    override fun onClick() {
                        val cm =
                            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val mClipData =
                            ClipData.newPlainText(
                                "Label",
                                wkVBinding.contentTv.text.toString()
                            )
                        cm.setPrimaryClip(mClipData)
                        WKToastUtils.getInstance()
                            .showToastNormal(getString(R.string.favorite_copyed))

                    }
                })
        )
        list.add(
            PopupMenuItem(
                getString(R.string.forward),
                R.mipmap.msg_forward,
                object : PopupMenuItem.IClick {
                    override fun onClick() {

                        val mTextContent =
                            WKTextContent(wkVBinding.contentTv.text.toString())
                        EndpointManager.getInstance()
                            .invoke(
                                EndpointSID.showChooseChatView, ChooseChatMenu(
                                    ChatChooseContacts { list1: List<WKChannel>? ->
                                        if (WKReader.isNotEmpty(list1)) {
                                            for (mChannel in list1!!) {
                                                WKIM.getInstance().msgManager.send(
                                                    mTextContent,
                                                    mChannel
                                                )
                                            }
                                            val viewGroup =
                                                this@DetailTextActivity.findViewById<View>(
                                                    android.R.id.content
                                                )
                                                    .rootView as ViewGroup
                                            Snackbar.make(
                                                viewGroup,
                                                getString(R.string.str_forward),
                                                1000
                                            )
                                                .setAction(
                                                    ""
                                                ) { }
                                                .show()
                                        }
                                    }, mTextContent
                                )
                            )

                    }
                })
        )
        WKDialogUtils.getInstance().setViewLongClickPopup(wkVBinding.contentTv,list)
    }
}