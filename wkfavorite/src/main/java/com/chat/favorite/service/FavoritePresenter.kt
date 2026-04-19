package com.chat.favorite.service

import com.chat.favorite.entity.FavoriteEntity
import com.chat.base.net.HttpResponseCode
import java.lang.ref.WeakReference

class FavoritePresenter(view: FavoriteContract.View) : FavoriteContract.Presenter {
    override fun getList(page: Int) {
        FavoriteModel().list(page, object : FavoriteModel.IList {
            override fun onResult(code: Int, msg: String?, list: List<FavoriteEntity>?) {
                if (favoriteView.get() != null) {
                    if (code == HttpResponseCode.success.toInt()) {
                        favoriteView.get()?.setList(list)
                    } else favoriteView.get()?.showError(msg)
                }
            }

        })
    }

    override fun showLoading() {
    }


    private var favoriteView: WeakReference<FavoriteContract.View> =
        WeakReference(view)
}