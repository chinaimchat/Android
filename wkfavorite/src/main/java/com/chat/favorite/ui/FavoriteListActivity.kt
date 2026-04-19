package com.chat.favorite.ui

import android.content.Intent
import android.view.View
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chat.favorite.R
import com.chat.favorite.databinding.ActFavoriteListLayoutBinding
import com.chat.favorite.entity.FavoriteEntity
import com.chat.favorite.service.FavoriteContract
import com.chat.favorite.service.FavoritePresenter
import com.chat.base.base.WKBaseActivity
import com.chat.base.ui.Theme
import com.chat.base.utils.WKReader
import com.chat.favorite.WKFavoriteApplication
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener

class FavoriteListActivity : WKBaseActivity<ActFavoriteListLayoutBinding>(),
    FavoriteContract.View {
    private lateinit var adapter: FavoriteAdapter
    private lateinit var presenter: FavoritePresenter
    private var page = 1
    override fun getViewBinding(): ActFavoriteListLayoutBinding {
        return ActFavoriteListLayoutBinding.inflate(layoutInflater)
    }

    override fun initPresenter() {
        presenter = FavoritePresenter(this)
    }

    override fun setTitle(titleTv: TextView) {
        titleTv.setText(R.string.my_favorite)
    }

    override fun initView() {
        adapter = FavoriteAdapter(ArrayList())
        initAdapter(wkVBinding.recyclerView, adapter)
        presenter.getList(page)
    }

    override fun initListener() {
        wkVBinding.spinKit.setColor(Theme.colorAccount)
        wkVBinding.refreshLayout.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onLoadMore(refreshLayout: RefreshLayout) {
                page++
                presenter.getList(page)
            }

            override fun onRefresh(refreshLayout: RefreshLayout) {
                page = 1
                presenter.getList(page)
            }
        })
        adapter.addChildClickViewIds(R.id.contentLayout)
        adapter.addChildClickViewIds(R.id.contentTv)
        adapter.setOnItemChildClickListener { _: BaseQuickAdapter<*, *>, _: View?, position: Int ->
            WKFavoriteApplication.instance.detailFavorite  = adapter.data[position]
//            WKFavoriteApplication.instance.detailFavorite = entity
            val intent: Intent = if (WKFavoriteApplication.instance.detailFavorite.type == 1) {
                Intent(this, DetailTextActivity::class.java)
            } else {
                Intent(this, DetailImgActivity::class.java)
            }
            startActivity(intent)
        }
    }

    override fun setList(list: List<FavoriteEntity>?) {
        wkVBinding.refreshLayout.finishRefresh()
        if (page == 1) {
            adapter.setList(list)
            if (WKReader.isEmpty(list)) {
                adapter.addData(FavoriteEntity())
            }
        } else {
            if (WKReader.isEmpty(list)) {
                wkVBinding.refreshLayout.finishLoadMoreWithNoMoreData()
            } else {
                wkVBinding.refreshLayout.finishLoadMore()
                adapter.addData(list!!)
            }
        }
    }

    override fun showError(msg: String?) {
    }

    override fun hideLoading() {
    }
}