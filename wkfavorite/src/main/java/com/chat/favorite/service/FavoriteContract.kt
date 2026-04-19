package com.chat.favorite.service

import com.chat.favorite.entity.FavoriteEntity
import com.chat.base.base.WKBasePresenter
import com.chat.base.base.WKBaseView

class FavoriteContract {
    interface Presenter : WKBasePresenter {
        fun getList(page: Int)
    }

    interface View : WKBaseView {
        fun setList(list: List<FavoriteEntity>?)
    }
}