package com.chat.label.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import com.chat.base.base.WKBaseActivity
import com.chat.base.endpoint.EndpointManager
import com.chat.base.endpoint.entity.ChooseContactsMenu
import com.chat.label.WKLabelApplication
import com.chat.label.R
import com.chat.label.adapter.LabelMemberAdapter
import com.chat.label.databinding.ActLabelDetailLayoutBinding
import com.chat.label.entity.Label
import com.chat.label.entity.LabelMember
import com.chat.label.service.LabelContact
import com.chat.label.service.LabelPresenter
import com.xinbida.wukongim.entity.WKChannel
import com.xinbida.wukongim.entity.WKChannelType

/**
 * 2020-11-03 12:29
 * 标签详情
 */
class LabelDetailActivity : WKBaseActivity<ActLabelDetailLayoutBinding>(),
    LabelContact.ILabelView {
    var sureBtn: Button? = null
    lateinit var adapter: LabelMemberAdapter
    var id: String? = ""
    var name: String? = ""
    private lateinit var presenter: LabelPresenter
    private var list: ArrayList<WKChannel>? = null
    override fun setTitle(titleTv: TextView?) {
        if (!TextUtils.isEmpty(name))
            titleTv?.text = name
        else titleTv?.setText(R.string.str_label_create_title)
    }

    override fun initPresenter() {
        id = intent.getStringExtra("id")
        name = intent.getStringExtra("name")
        list = intent.getParcelableArrayListExtra("list")
        presenter = LabelPresenter(this)
    }

    override fun getRightBtnText(titleRightBtn: Button?): String {
        sureBtn = titleRightBtn
        sureBtn!!.alpha = 0.2f
        return getString(R.string.str_save)
    }

    override fun rightButtonClick() {
        super.rightButtonClick()
        val name = wkVBinding.nameEt.text.toString()
        val uids = ArrayList<String>()
        for (member in adapter.data) {
            if (member.uid != "-1" && member.uid != "-2") {
                uids.add(member.uid)
            }
        }
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(
                name.replace("\\s".toRegex(), "")
            )
        ) {
            showToast(R.string.str_label_is_null)
            return
        }
        if (uids.isNotEmpty()) {
            loadingPopup.show()
            if (!TextUtils.isEmpty(id)) {
                presenter.updateLabel(id!!, name, uids)
            } else presenter.addLabel(name, uids)
        }
    }

    override fun initView() {
        adapter = LabelMemberAdapter()
        wkVBinding.recyclerView.layoutManager = GridLayoutManager(this, 5)
        wkVBinding.recyclerView.adapter = adapter
        if (!TextUtils.isEmpty(id)) {
            presenter.getLabelDetail(id!!)
        }
        val members = ArrayList<LabelMember>()
        if (list != null && list!!.size > 0) {
            for (channel in list!!) run {
                val member = LabelMember()
                member.name = channel.channelName
                member.uid = channel.channelID
                if (!TextUtils.isEmpty(channel.channelRemark))
                    member.remark = channel.channelRemark
                members.add(member)
            }
        }
        val memberAdd = LabelMember()
        memberAdd.uid = "-1"
        members.add(memberAdd)
        val memberDelete = LabelMember()
        memberDelete.uid = "-2"
        members.add(memberDelete)
        adapter.setList(members)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initListener() {
        wkVBinding.deleteBtn.setOnClickListener { _ ->
            run {
                showDialog(getString(R.string.str_label_delete_tips), fun(it: Int) {
                    if (it == 1) {
                        loadingPopup.show()
                        presenter.deleteLabel(id!!)
                    }
                })
            }
        }
        wkVBinding.nameEt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                val text = wkVBinding.nameEt.text.toString().replace(" ", "")
                sureBtn!!.isEnabled = !TextUtils.isEmpty(text)
                if (TextUtils.isEmpty(text)) {
                    sureBtn!!.alpha = 0.2f
                } else sureBtn!!.alpha = 1f
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

        })
        wkVBinding.recyclerView.setOnTouchListener { _, _ ->
            var isAdd = false
            for (member1 in adapter.data) {
                if (!TextUtils.isEmpty(member1.uid) && member1.isShowDelete) {
                    isAdd = true
                }
                member1.isShowDelete = false
            }
            if (isAdd) {
                val memberAdd = LabelMember()
                memberAdd.uid = "-1"
                adapter.addData(memberAdd)

                val memberDelete = LabelMember()
                memberDelete.uid = "-2"
                adapter.addData(memberDelete)
                adapter.notifyItemRangeChanged(0, adapter.itemCount)
            }
            true
        }


        adapter.addChildClickViewIds(R.id.avatarView)
        adapter.setOnItemChildClickListener { adapter1, _, position ->

            val member = adapter1.getItem(position) as LabelMember
            if (member.uid == "-1") {
                // 加人
                chooseMembers()
            } else if (member.uid == "-2") {
                //减人
                adapter.removeAt(adapter.data.size - 1)
                adapter.removeAt(adapter.data.size - 1)
                for (member1 in adapter.data) {
                    member1.isShowDelete = true
                }
                adapter.notifyItemRangeChanged(0, adapter.itemCount)
                checkMembers()
            } else {
                if (member.isShowDelete) {
                    adapter.removeAt(position)
                    checkMembers()
                }
            }
        }
    }

    private fun checkMembers() {
        if (adapter.data.size == 0) {
            val memberAdd = LabelMember()
            memberAdd.uid = "-1"
            adapter.addData(memberAdd)
            sureBtn!!.alpha = 0.2f
        }
    }

    private fun chooseMembers() {
        val channels = ArrayList<WKChannel>()
        for (member in adapter.data) {
            if (member.uid != "-1" && member.uid != "-2") {
                val channel = WKChannel()
                channel.channelID = member.uid
                channel.channelType = WKChannelType.PERSONAL
                channels.add(channel)
            }
        }
        EndpointManager.getInstance().invoke(
            "choose_contacts",
            ChooseContactsMenu(
                Int.MAX_VALUE,
                false, false,
                channels,
                object : ChooseContactsMenu.IChooseBack {
                    override fun onBack(selectedList: List<WKChannel>?) {
                        // todo
                        val members = ArrayList<LabelMember>()
                        if (selectedList != null && selectedList.isNotEmpty()) {
                            for (channel in selectedList) run {
                                val member = LabelMember()
                                if (!TextUtils.isEmpty(channel.channelName))
                                    member.name = channel.channelName
                                member.uid = channel.channelID
                                if (!TextUtils.isEmpty(channel.channelRemark))
                                    member.remark = channel.channelRemark
                                members.add(member)
                            }
                        }
                        if (members.size > 0) {
                            sureBtn!!.alpha = 1f
                        }
                        val memberAdd = LabelMember()
                        memberAdd.uid = "-1"
                        members.add(memberAdd)
                        val memberDelete = LabelMember()
                        memberDelete.uid = "-2"
                        members.add(memberDelete)

                        adapter.setList(members)

                    }
                })
        )
    }

    override fun setLabels(list: List<Label>) {
    }

    override fun setLabelDetail(label: Label) {
        wkVBinding.nameEt.setText(label.name)
        wkVBinding.nameEt.setSelection(label.name.length)
        wkVBinding.deleteBtn.visibility = View.VISIBLE
        label.members?.let { adapter.addData(0, it) }
    }

    override fun setAddLabelResult() {
        loadingPopup.dismiss()
        val intent = Intent()
        intent.putExtra("type", 2)
        setResult(Activity.RESULT_OK, intent)
        WKLabelApplication.instance.setLabelSaved()
        finish()
    }

    override fun setDeleteLabelResult(id: String) {
        loadingPopup.dismiss()
        val intent = Intent()
        intent.putExtra("type", 3)
        intent.putExtra("id", id)
        setResult(Activity.RESULT_OK, intent)
        finish()
        finish()
    }

    override fun showError(msg: String?) {
    }

    override fun hideLoading() {
    }

    override fun getViewBinding(): ActLabelDetailLayoutBinding {
        return ActLabelDetailLayoutBinding.inflate(layoutInflater)
    }

}