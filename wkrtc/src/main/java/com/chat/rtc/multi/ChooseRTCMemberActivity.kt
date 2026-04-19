package com.chat.rtc.multi

import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chat.base.base.WKBaseActivity
import com.chat.base.config.WKConfig
import com.chat.base.config.WKSystemAccount
import com.chat.base.net.HttpResponseCode
import com.chat.base.utils.HanziToPinyin
import com.chat.base.utils.WKReader
import com.chat.base.utils.WKToastUtils
import com.chat.rtc.R
import com.chat.rtc.WKUIRTCApplication
import com.chat.rtc.databinding.ChooseCallMembersLayoutBinding
import com.chat.rtc.service.RTCModel
import com.xinbida.wukongim.WKIM
import com.xinbida.wukongim.entity.WKChannel
import com.xinbida.wukongim.entity.WKChannelMember
import com.xinbida.wukongim.entity.WKChannelType
import java.util.Locale
import java.util.Objects

class ChooseRTCMemberActivity : WKBaseActivity<ChooseCallMembersLayoutBinding>() {

    private var channelID: String? = null
    private var channelType: Byte = 0
    private var selectedList: List<String>? = null
    private var maxSelectCount = 9
    private var allList: List<GroupMemberEntity>? = null
    var adapter: ChooseMemberAdapter? = null
    private var selectedAdapter: ChooseUserSelectedAdapter? = null
    private var rightBtn: TextView? = null

    override fun getViewBinding(): ChooseCallMembersLayoutBinding {
        return ChooseCallMembersLayoutBinding.inflate(layoutInflater)
    }

    override fun initPresenter() {
        super.initPresenter()
        channelID = intent.getStringExtra("channelID")
        channelType = intent.getByteExtra("channelType", WKChannelType.GROUP)
        selectedList = intent.getStringArrayListExtra("selectedList")
        if (selectedList != null) {
            maxSelectCount = 9 - selectedList!!.size
        }
    }

    override fun getRightBtnText(titleRightBtn: Button?): String {
        rightBtn = titleRightBtn
        return getString(R.string.sure)
    }

    override fun rightButtonClick() {
        super.rightButtonClick()

        if (selectedAdapter!!.data.size > 0) {
            val uidList: MutableList<String> = java.util.ArrayList()
            var i = 0
            val size = selectedAdapter!!.data.size
            while (i < size) {
                if (selectedAdapter!!.data[i].member != null && !TextUtils.isEmpty(
                        selectedAdapter!!.data[i].member.memberUID
                    )
                ) {
                    uidList.add(selectedAdapter!!.data[i].member.memberUID)
                }
                i++
            }
            if (WKUIRTCApplication.iChooseMembersBack != null && !TextUtils.isEmpty(WKUIRTCApplication.roomID) && WKReader.isNotEmpty(uidList)) {
                showTitleRightLoading()
                showOrHideRightBtn(false)
                RTCModel.getInstance().invokeJoinRoom(
                    WKUIRTCApplication.roomID,
                    uidList
                ) { code, msg ->
                    showOrHideRightBtn(true)
                    hideTitleRightLoading()
                    if (code == HttpResponseCode.success.toInt()) {
                        WKUIRTCApplication.iChooseMembersBack!!.onBack(uidList)
                        finish()
                    } else {
                        showToast(msg)
                    }
                }

            }

        }
    }

    override fun initView() {
        adapter = ChooseMemberAdapter()
        wkVBinding.recyclerView.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )
        wkVBinding.recyclerView.adapter = adapter
        (Objects.requireNonNull(wkVBinding.selectUserRecyclerView.itemAnimator) as DefaultItemAnimator).supportsChangeAnimations =
            false
        selectedAdapter = ChooseUserSelectedAdapter(object : ChooseUserSelectedAdapter.IGetEdit {
            override fun onDeleted(uid: String) {
                var i = 0
                val size = adapter!!.data.size
                while (i < size) {
                    if (adapter!!.data[i].member.memberUID == uid && adapter!!.data[i].isCanCheck == 1) {
                        adapter!!.data[i].checked = if (adapter!!.data[i].checked == 1) 0 else 1
                        adapter!!.notifyItemChanged(i, adapter!!.data[i])
                        break
                    }
                    i++
                }
                checkSelect()
            }

            override fun searchUser(key: String) {
                this@ChooseRTCMemberActivity.searchUser(key)
            }
        })
        wkVBinding.selectUserRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        wkVBinding.selectUserRecyclerView.adapter = selectedAdapter
        val entity = GroupMemberEntity()
        entity.itemType = 1
        selectedAdapter!!.addData(entity)
    }

    override fun initListener() {
        selectedAdapter!!.setOnItemClickListener { _: BaseQuickAdapter<*, *>?, _: View?, position: Int ->
            val userEntity =
                selectedAdapter!!.getItem(position)
            if (userEntity != null && userEntity.itemType == 0) {
                if (!userEntity.isSetDelete) {
                    userEntity.isSetDelete = true
                    selectedAdapter!!.notifyItemChanged(position, userEntity)
                    return@setOnItemClickListener
                }
                var isRemove = false
                var i = 0
                val size = adapter!!.data.size
                while (i < size) {
                    if (adapter!!.data[i].member.memberUID.equals(
                            userEntity.member.memberUID,
                            ignoreCase = true
                        ) && adapter!!.data[i].isCanCheck == 1
                    ) {
                        adapter!!.data[i].checked =
                            if (adapter!!.data[i].checked == 1) 0 else 1
                        adapter!!.notifyItemChanged(i, adapter!!.data[i])
                        isRemove = true
                        break
                    }
                    i++
                }
                if (isRemove) {
                    var i1 = 0
                    val size1 = selectedAdapter!!.data.size
                    while (i1 < size1) {
                        selectedAdapter!!.data[i1].isSetDelete = false
                        i1++
                    }
                    selectedAdapter!!.removeAt(position)
                    checkSelect()
                }
            }
        }
        adapter!!.setOnItemClickListener { adapter: BaseQuickAdapter<*, *>, _: View?, position: Int ->
            val memberEntity =
                adapter.getItem(position) as GroupMemberEntity?
            if (memberEntity != null) {
                if (maxSelectCount != -1 && selectedAdapter!!.itemCount >= maxSelectCount) {
                    val content =
                        String.format(getString(R.string.call_max_count), maxSelectCount)
                    WKToastUtils.getInstance().showToastNormal(content)
                    return@setOnItemClickListener
                }
                if (memberEntity.isCanCheck == 1) {
                    memberEntity.checked = if (memberEntity.checked == 1) 0 else 1
                    adapter.notifyItemChanged(position, memberEntity)
                    checkSelect()
                    if (memberEntity.checked == 1) {
                        val friendEntity =
                            GroupMemberEntity()
                        friendEntity.member = WKChannelMember()
                        friendEntity.member.memberUID = memberEntity.member.memberUID
                        friendEntity.member.memberName = memberEntity.member.memberName
                        friendEntity.member.memberRemark = memberEntity.member.memberRemark
                        selectedAdapter!!.addData(selectedAdapter!!.data.size - 1, friendEntity)
                        wkVBinding.selectUserRecyclerView.scrollToPosition(selectedAdapter!!.data.size - 1)
                    } else {
                        var i = 0
                        val size = selectedAdapter!!.data.size
                        while (i < size) {
                            if (selectedAdapter!!.data[i].itemType == 0 && selectedAdapter!!.data[i].member.memberUID == memberEntity.member.memberUID
                            ) {
                                selectedAdapter!!.removeAt(i)
                                break
                            }
                            i++
                        }
                    }
                }
            }
        }
    }

    override fun initData() {
        getData()
    }

    private fun searchUser(content: String) {
        if (TextUtils.isEmpty(content)) {
            adapter!!.setList(allList)
            return
        }
        val tempList: MutableList<GroupMemberEntity> = ArrayList()
        var i = 0
        val size = allList!!.size
        while (i < size) {
            if ((!TextUtils.isEmpty(allList!![i].member.memberName) && allList!![i].member.memberName.lowercase(
                    Locale.getDefault()
                )
                    .contains(content.lowercase(Locale.getDefault())) || !TextUtils.isEmpty(allList!![i].member.memberRemark) && allList!![i].member.memberRemark.lowercase(
                    Locale.getDefault()
                )
                    .contains(content.lowercase(Locale.getDefault()))
                        || content.contains(
                    allList!![i].pying.lowercase(
                        Locale.getDefault()
                    )
                ))
            ) {
                tempList.add(allList!![i])
            }
            i++
        }
        adapter!!.setList(tempList)
    }

    private fun getData() {
        val memberList = WKIM.getInstance().channelMembersManager.getMembers(channelID, channelType)
        val uidList = ArrayList<String>()
        for (member in memberList) {
            if (member != null && TextUtils.isEmpty(member.memberUID)) {
                uidList.add(member.memberUID)
            }
        }
        val channelList = WKIM.getInstance().channelManager.getWithChannelIdsAndChannelType(
            uidList,
            WKChannelType.PERSONAL
        )
        val list: MutableList<GroupMemberEntity> = ArrayList()
        var i = 0
        val size = memberList.size
        while (i < size) {
            if (TextUtils.isEmpty(memberList[i].memberUID) || (memberList[i].memberUID == WKSystemAccount.system_team) || (memberList[i].memberUID == WKSystemAccount.system_file_helper)) {
                i++
                continue
            }
            if (memberList[i].memberUID != WKConfig.getInstance().uid) {
                val entity = GroupMemberEntity()
                entity.member = memberList[i]
                var channel: WKChannel? = null
                if (WKReader.isNotEmpty(channelList)) {
                    for (channel1 in channelList) {
                        if (channel1.channelID.equals(memberList[i].memberUID) && channel1.channelType == WKChannelType.PERSONAL) {
                            channel = channel1
                            break
                        }
                    }
                }
                if (channel != null && !TextUtils.isEmpty(channel.channelRemark)) {
                    entity.member.memberRemark = channel.channelRemark
                }
                var showName = entity.member.memberRemark
                if (TextUtils.isEmpty(showName)) showName = entity.member.memberName
                if (!TextUtils.isEmpty(showName)) {
                    if (isStartNum(showName)) {
                        entity.pying = "#"
                    } else entity.pying =
                        HanziToPinyin.getInstance().getPY(showName)
                } else entity.pying = "#"
                entity.isCanCheck = 1
                if (selectedList != null && selectedList!!.isNotEmpty()) {
                    var j = 0
                    val len = selectedList!!.size
                    while (j < len) {
                        if ((selectedList!![j] == entity.member.memberUID)) {
                            entity.checked = 1
                            entity.isCanCheck = 0
                            break
                        }
                        j++
                    }
                }
                list.add(entity)
            }
            i++
        }
        allList = list
        adapter!!.setList(allList)
    }


    private fun checkSelect() {
        var count = 0
        val list: List<GroupMemberEntity> = adapter!!.data
        var i = 0
        val size = list.size
        while (i < size) {
            if (list[i].checked == 1 && list[i].isCanCheck == 1) {
                count++
            }
            i++
        }
        if (count > 0) {
            rightBtn!!.text = String.format("%s(%s)", getString(R.string.sure), count)
            rightBtn!!.isEnabled = true
            rightBtn!!.alpha = 1f
        } else {
            rightBtn!!.setText(R.string.sure)
            rightBtn!!.isEnabled = false
            rightBtn!!.alpha = 0.2f
        }
    }

    private fun isStartNum(str: String): Boolean {
        val temp = str.substring(0, 1)
        return Character.isDigit(temp[0])
    }

}