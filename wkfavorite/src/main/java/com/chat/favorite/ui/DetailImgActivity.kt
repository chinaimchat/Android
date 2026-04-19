package com.chat.favorite.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.chat.base.base.WKBaseActivity
import com.chat.base.config.WKApiConfig
import com.chat.base.endpoint.EndpointManager
import com.chat.base.endpoint.EndpointSID
import com.chat.base.endpoint.entity.ChatChooseContacts
import com.chat.base.endpoint.entity.ChooseChatMenu
import com.chat.base.entity.ImagePopupBottomSheetItem
import com.chat.base.entity.PopupMenuItem
import com.chat.base.glide.GlideUtils
import com.chat.base.utils.ImageUtils
import com.chat.base.utils.WKDialogUtils
import com.chat.base.views.CustomImageViewerPopup.IImgPopupMenu
import com.chat.favorite.R
import com.chat.favorite.WKFavoriteApplication
import com.chat.favorite.databinding.ActDetailImgLayoutBinding
import com.google.android.material.snackbar.Snackbar
import com.xinbida.wukongim.WKIM
import com.xinbida.wukongim.entity.WKChannel
import com.xinbida.wukongim.msgmodel.WKImageContent

class DetailImgActivity : WKBaseActivity<ActDetailImgLayoutBinding>() {
    override fun getViewBinding(): ActDetailImgLayoutBinding {
        return ActDetailImgLayoutBinding.inflate(layoutInflater)
    }


    override fun setTitle(titleTv: TextView) {
        titleTv.setText(R.string.favorite_detail)
    }

    override fun initView() {
        wkVBinding.refreshLayout.setEnableOverScrollDrag(true)
        wkVBinding.refreshLayout.setEnableLoadMore(false)
        wkVBinding.refreshLayout.setEnableRefresh(false)

        val content = WKFavoriteApplication.instance.detailFavorite.payload!!["content"] as String
        GlideUtils.getInstance()
            .showImg(this, WKApiConfig.getShowUrl(content), wkVBinding.imageView)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initListener() {

        val content = WKFavoriteApplication.instance.detailFavorite.payload!!["content"] as String
        val list: MutableList<PopupMenuItem> = ArrayList()
        list.add(
            PopupMenuItem(getString(R.string.favorite_save), R.mipmap.msg_download,
                object : PopupMenuItem.IClick {
                    override fun onClick() {
                        Glide.with(view)
                            .asBitmap()
                            .load(WKApiConfig.getShowUrl(content))
                            .into(object :
                                CustomTarget<Bitmap?>(SIZE_ORIGINAL, SIZE_ORIGINAL) {
                                override fun onResourceReady(
                                    resource: Bitmap,
                                    transition: Transition<in Bitmap?>?
                                ) {
                                    ImageUtils.getInstance().saveBitmap(
                                        this@DetailImgActivity,
                                        resource,
                                        true,
                                        null
                                    )
                                    showToast(getString(R.string.saved_album))
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {}
                            })


                    }
                })
        )
        list.add(
            PopupMenuItem(
                getString(R.string.forward),
                R.mipmap.msg_forward,
                object : PopupMenuItem.IClick {
                    override fun onClick() {
                        forward(
                            content
                        )
                    }
                })
        )
        WKDialogUtils.getInstance().setViewLongClickPopup(wkVBinding.imageView, list)
        wkVBinding.imageView.setOnClickListener {
            showImg()
        }
    }

    private fun forward(url: String) {
        var width = 129
        var height = 229
        if (WKFavoriteApplication.instance.detailFavorite.payload!!.containsKey("width")) {
            val `object`: String =
                WKFavoriteApplication.instance.detailFavorite.payload!!["width"].toString() + ""
            if (!TextUtils.isEmpty(`object`)) {
                val wdb = `object`.toDouble()
                width = wdb.toInt()
            }
        }
        if (WKFavoriteApplication.instance.detailFavorite.payload!!.containsKey("height")) {
            val `object`: String =
                WKFavoriteApplication.instance.detailFavorite.payload!!["height"].toString() + ""
            if (!TextUtils.isEmpty(`object`)) {
                val hdb = `object`.toDouble()
                height = hdb.toInt()
            }
        }
        val imageContent = WKImageContent()
        imageContent.url = url
        imageContent.width = width
        imageContent.height = height
        EndpointManager.getInstance().invoke(
            EndpointSID.showChooseChatView,
            ChooseChatMenu(ChatChooseContacts { list1: List<WKChannel>? ->
                if (!list1.isNullOrEmpty()) {
                    for (channel in list1) {
                        WKIM.getInstance().msgManager
                            .send(
                                imageContent,
                                channel
                            )
                    }
                    val viewGroup =
                        this@DetailImgActivity.findViewById<View>(android.R.id.content)
                            .rootView as ViewGroup
                    Snackbar.make(viewGroup, getString(R.string.str_forward), 1000)
                        .setAction("") { }
                        .show()
                }
            }, imageContent)
        )
    }

    private fun showImg() {
        val uri: String
        val content = WKFavoriteApplication.instance.detailFavorite.payload!!["content"] as String
        uri = WKApiConfig.getShowUrl(content)
        //查看大图
        val tempImgList: MutableList<Any> = ArrayList()
        val imageViewList: MutableList<ImageView> = ArrayList()
        imageViewList.add(wkVBinding.imageView)
        tempImgList.add(WKApiConfig.getShowUrl(uri))
        val index = 0
        val bottomEntity1 = ImagePopupBottomSheetItem(
            getString(R.string.forward),
            R.mipmap.msg_forward,
            object : ImagePopupBottomSheetItem.IBottomSheetClick {
                override fun onClick(index: Int) {
                    forward(uri)
                }
            })
        val list: MutableList<ImagePopupBottomSheetItem> = ArrayList()
        list.add(bottomEntity1)
        WKDialogUtils.getInstance().showImagePopup(
            this,
            tempImgList,
            imageViewList,
            wkVBinding.imageView,
            index,
            list,
            object : IImgPopupMenu {
                override fun onForward(position: Int) {

                }

                override fun onFavorite(position: Int) {}
                override fun onShowInChat(position: Int) {}
            },
            null
        )
    }
}