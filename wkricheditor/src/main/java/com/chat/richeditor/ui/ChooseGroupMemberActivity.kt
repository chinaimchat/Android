package com.chat.richeditor.ui

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.chat.base.base.WKBaseActivity
import com.chat.base.config.WKConfig
import com.chat.base.ui.Theme
import com.chat.base.utils.SoftKeyboardUtils
import com.chat.base.utils.singleclick.SingleClickUtil
import com.chat.richeditor.WKRichApplication
import com.chat.richeditor.R
import com.chat.richeditor.databinding.ActChooseMemberLayoutRichBinding
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener
import com.xinbida.wukongim.WKIM
import com.xinbida.wukongim.entity.WKChannelExtras
import com.xinbida.wukongim.entity.WKChannelMember
import com.xinbida.wukongim.entity.WKChannelType

class ChooseGroupMemberActivity : WKBaseActivity<ActChooseMemberLayoutRichBinding>() {
    private lateinit var adapter: ChooseAdapter
    private lateinit var channelID: String
    private var channelType: Byte = WKChannelType.GROUP
    private var searchKey = ""
    private var page = 1
    private var groupType = 0
    override fun getViewBinding(): ActChooseMemberLayoutRichBinding {
        overridePendingTransition(R.anim.bottom_in, R.anim.bottom_silent)
        return ActChooseMemberLayoutRichBinding.inflate(layoutInflater)
    }

    override fun setTitle(titleTv: TextView) {
        titleTv.text = getText(R.string.choose_member)
    }

    override fun initPresenter() {
        val channel = WKRichApplication.instance.iConversationContext.chatChannelInfo
        channelID = channel.channelID
        channelType = channel.channelType
        if (channel.remoteExtraMap != null && channel.remoteExtraMap.containsKey(
                WKChannelExtras.groupType
            )
        ) {
            val groupTypeObject = channel.remoteExtraMap[WKChannelExtras.groupType]
            if (groupTypeObject is Int) {
                groupType = groupTypeObject
            }
        }
    }

    override fun initView() {
        Theme.setColorFilter(this, wkVBinding.searchIv, R.color.color999)
        adapter = ChooseAdapter()
        initAdapter(wkVBinding.recyclerView, adapter)
    }

    override fun initListener() {
        wkVBinding.refreshLayout.setEnableRefresh(false)
        wkVBinding.refreshLayout.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onLoadMore(refreshLayout: RefreshLayout) {
                page++
                getData()
            }

            override fun onRefresh(refreshLayout: RefreshLayout) {}
        })
        adapter.setOnItemClickListener { adapter, view1, position ->
            SingleClickUtil.determineTriggerSingleClick(view1) {
                val groupMember = adapter.data[position] as WKChannelMember
                val showName = groupMember.memberName
//            if (TextUtils.isEmpty(showName))
//                showName = groupMember.channelMember.memberName
                val intent = Intent()
                intent.putExtra("showName", showName)
                intent.putExtra("uid", groupMember.memberUID)
                setResult(RESULT_OK, intent)
                finish()
            }
        }

        wkVBinding.searchET.imeOptions = EditorInfo.IME_ACTION_SEARCH
        wkVBinding.searchET.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                SoftKeyboardUtils.getInstance().hideSoftKeyboard(this)
            }
            false
        }
        wkVBinding.searchET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                searchUser(editable.toString())
            }
        })
    }

    override fun initData() {
        super.initData()
        getData()
    }

    private fun searchUser(content: String) {
        searchKey = content
        page = 1
        wkVBinding.refreshLayout.setEnableLoadMore(true)
        getData()
    }

    private fun getData() {
        WKIM.getInstance().channelMembersManager.getWithPageOrSearch(
            channelID,
            channelType,
            searchKey,
            page,
            100
        ) { list, b ->
            if (groupType == 0) resortData(list) else {
                if (b) {
                    resortData(list)
                }
            }
        }
    }

    private fun resortData(list: List<WKChannelMember>) {
        val loginUID = WKConfig.getInstance().uid
        val tempList = ArrayList<WKChannelMember>()
        for (member in list.iterator()) {
            if (member.memberUID != loginUID) {
                tempList.add(member)
            }
        }
        if (page == 1) {
            adapter.setList(tempList)
        } else {
            adapter.addData(tempList)
        }
        wkVBinding.refreshLayout.finishRefresh()
        wkVBinding.refreshLayout.finishLoadMore()
        if (tempList.isEmpty()) {
            wkVBinding.refreshLayout.setEnableLoadMore(false)
            wkVBinding.refreshLayout.finishLoadMoreWithNoMoreData()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.bottom_silent, R.anim.bottom_out)
    }
}