package com.chat.label.service

import com.chat.base.base.WKBasePresenter
import com.chat.base.base.WKBaseView
import com.chat.label.entity.Label

/**
 * 2020-11-03 11:24
 * 标签
 */
class LabelContact {
    interface ILabelPresenter : WKBasePresenter {
        fun addLabel(name: String, uids: List<String>)
        fun updateLabel(id: String, name: String, uids: List<String>)
        fun getLabels()
        fun getLabelDetail(id: String)
        fun deleteLabel(id: String)
    }

    interface ILabelView : WKBaseView {
        fun setLabels(list: List<Label>)
        fun setLabelDetail(label: Label)
        fun setAddLabelResult()
        fun setDeleteLabelResult(id: String)
    }
}