package com.chat.label.service

import com.chat.base.net.HttpResponseCode
import com.chat.label.entity.Label
import java.lang.ref.WeakReference

/**
 *
 * 2020-11-03 11:26
 * 标签
 */
class LabelPresenter constructor(view: LabelContact.ILabelView) : LabelContact.ILabelPresenter {
    override fun deleteLabel(id: String) {
        LabelModel().delete(id, object : LabelModel.ILabelCommon {
            override fun onResult(code: Int, msg: String) {
                labelview.get()!!.hideLoading()
                if (code == HttpResponseCode.success.toInt())
                    labelview.get()!!.setDeleteLabelResult(id)
                else {
                    labelview.get()!!.showError(msg)
                }
            }
        })
    }

    override fun updateLabel(id: String, name: String, uids: List<String>) {
        LabelModel().update(id, name, uids, object : LabelModel.ILabelCommon {
            override fun onResult(code: Int, msg: String) {
                if (code == HttpResponseCode.success.toInt())
                    labelview.get()!!.setAddLabelResult()
                else {
                    labelview.get()!!.hideLoading()
                    labelview.get()!!.showError(msg)
                }
            }
        })
    }

    override fun addLabel(name: String, uids: List<String>) {
        LabelModel().add(name, uids, object : LabelModel.ILabelCommon {
            override fun onResult(code: Int, msg: String) {
                if (code == HttpResponseCode.success.toInt())
                    labelview.get()!!.setAddLabelResult()
                else {
                    labelview.get()!!.hideLoading()
                    labelview.get()!!.showError(msg)
                }
            }
        })
    }

    override fun getLabelDetail(id: String) {
        LabelModel().detail(id, object : LabelModel.ILabelDetail {
            override fun onResult(code: Int, msg: String, label: Label) {
                labelview.get()!!.hideLoading()
                if (code == HttpResponseCode.success.toInt())
                    labelview.get()!!.setLabelDetail(label)
                else {
                    labelview.get()!!.showError(msg)
                }
            }
        })
    }

    override fun getLabels() {
        LabelModel().getLabels(object : LabelModel.IGetLabels {
            override fun onResult(code: Int, msg: String, list: List<Label>) {
                labelview.get()!!.hideLoading()
                if (code == HttpResponseCode.success.toInt())
                    labelview.get()!!.setLabels(list)
                else {
                    labelview.get()!!.showError(msg)
                }
            }

        })
    }

    override fun showLoading() {}

    private var labelview: WeakReference<LabelContact.ILabelView> = WeakReference(view)

}