package com.chat.richeditor.ui

import android.content.Intent
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import com.chat.base.base.WKBaseActivity
import com.chat.base.endpoint.EndpointManager
import com.chat.base.ui.Theme
import com.chat.base.utils.SoftKeyboardUtils
import com.chat.base.utils.StringUtils
import com.chat.richeditor.WKRichApplication
import com.chat.richeditor.R
import com.chat.richeditor.databinding.EditLayoutBinding
import com.chat.richeditor.msg.RichTextContent
import com.xinbida.wukongim.entity.WKMentionInfo

class EditActivity : WKBaseActivity<EditLayoutBinding>() {
    lateinit var rightTv: TextView
    override fun getViewBinding(): EditLayoutBinding {
        return EditLayoutBinding.inflate(layoutInflater)
    }

    override fun setTitle(titleTv: TextView?) {
        titleTv!!.setText(
            R.string.rich_text
        )
    }

    override fun initPresenter() {
    }

    override fun hideStatusBar(): Boolean {
        return true
    }

    override fun initView() {
        SoftKeyboardUtils.getInstance()
            .showSoftKeyBoard(this@EditActivity, wkVBinding.textEditor.editText)
    }

    override fun initListener() {
        EndpointManager.getInstance().setMethod("choose_group_member") {
            SoftKeyboardUtils.getInstance().hideSoftKeyboard(this@EditActivity)
            val intent = Intent(this@EditActivity, ChooseGroupMemberActivity::class.java)
            previewResultLac.launch(intent)
        }
        wkVBinding.textEditor.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                var content = p0.toString().replace(" ", "").trim()
                content = StringUtils.replaceBlank(content)
                if (!TextUtils.isEmpty(content)) {
                    rightTv.setTextColor(
                        Theme.colorAccount
                    )
                } else {
                    rightTv.setTextColor(
                        Theme.colorAccountDisable
                    )
                }
            }

        })
    }

    override fun getRightTvText(textView: TextView?): String {
        this.rightTv = textView!!
        textView.setTextColor(Theme.colorAccountDisable)
        return getString(R.string.str_send)
    }

    override fun rightLayoutClick() {
        super.rightLayoutClick()
        val content = wkVBinding.textEditor.editText.text.toString()
        val tempContent = StringUtils.replaceBlank(content.trim())
        if (TextUtils.isEmpty(tempContent)) {
            return
        }
        val contentJsonArr = wkVBinding.textEditor.contentJsonArr
        val richTextContent = RichTextContent()
        richTextContent.content = content
        richTextContent.entities = contentJsonArr
        val list = wkVBinding.textEditor.mentions
        if (list != null && list.size > 0) {
            val mentionInfo = WKMentionInfo()
            val uidList: MutableList<String> = ArrayList()
            var i = 0
            val size = list.size
            while (i < size) {
                if (list[i].equals("-1", ignoreCase = true)) {
                    richTextContent.mentionAll = 1 //remind all
                } else {
                    uidList.add(list[i])
                }
                i++
            }
            mentionInfo.uids = uidList
            richTextContent.mentionInfo = mentionInfo
        }
        WKRichApplication.instance.sendMsg(richTextContent)
        finish()
    }

    override fun finish() {
        super.finish()
        EndpointManager.getInstance().remove("choose_group_member")
        SoftKeyboardUtils.getInstance().hideSoftKeyboard(this@EditActivity)
    }

    private var previewResultLac = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.data != null && result.resultCode == RESULT_OK) {
            val showName = result.data!!.getStringExtra("showName")
            val uid = result.data!!.getStringExtra("uid")
            if (!TextUtils.isEmpty(showName)) {
                wkVBinding.textEditor.addMentionSpan("@$showName", uid)
                SoftKeyboardUtils.getInstance()
                    .showSoftKeyBoard(this@EditActivity, wkVBinding.textEditor.editText)
            }
        }
    }


}