package com.chat.label

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.chat.base.WKBaseApplication
import com.chat.base.endpoint.EndpointCategory
import com.chat.base.endpoint.EndpointManager
import com.chat.base.endpoint.entity.ChooseLabelEntity
import com.chat.base.endpoint.entity.ChooseLabelMenu
import com.chat.base.endpoint.entity.ContactsMenu
import com.chat.base.endpoint.entity.SaveLabelMenu
import com.chat.label.ui.LabelActivity
import com.chat.label.ui.LabelDetailActivity
import com.chat.label.entity.Label
import com.chat.label.service.LabelModel
import com.xinbida.wukongim.entity.WKChannel
import com.xinbida.wukongim.entity.WKChannelType

/**
 * 2020-11-02 17:00
 * 标签
 */
class WKLabelApplication private constructor() {
    private object SingletonInstance {
        val INSTANCE = WKLabelApplication()
    }

    companion object {
        val instance: WKLabelApplication
            get() = SingletonInstance.INSTANCE
    }

    fun init(context: Context) {
        val appModule = WKBaseApplication.getInstance().getAppModuleWithSid("label")
        if (!WKBaseApplication.getInstance().appModuleIsInjection(appModule)) return

        addListener(context)
    }

    private fun addListener(mContext: Context) {
        EndpointManager.getInstance().setMethod(
            EndpointCategory.mailList + "_label",
            EndpointCategory.mailList,
            80
        ) {
            ContactsMenu("label", R.mipmap.icon_label, mContext.getString(R.string.str_label)) {
                val intent = Intent(mContext, LabelActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                mContext.startActivity(intent)

            }
        }
        EndpointManager.getInstance().setMethod("save_label") { `object` ->
            saveLabelMenu = `object` as SaveLabelMenu
            val intent = Intent(saveLabelMenu!!.context, LabelDetailActivity::class.java)
            intent.putParcelableArrayListExtra(
                "list",
                saveLabelMenu!!.list as java.util.ArrayList<out Parcelable>
            )
            saveLabelMenu!!.context.startActivity(intent)
        }
        EndpointManager.getInstance().setMethod("choose_label") { `object` ->
            ChooseLabelMenu = `object` as ChooseLabelMenu
            LabelModel().getLabels(object : LabelModel.IGetLabels {
                override fun onResult(code: Int, msg: String, list: List<Label>) {
                    val labels = ArrayList<ChooseLabelEntity>()
                    for (label in list) run {
                        val entity = ChooseLabelEntity()
                        entity.labelId = label.id
                        entity.labelName = label.name
                        val members = ArrayList<WKChannel>()
                        for (member in label.members!!) {
                            val channel = WKChannel()
                            channel.channelID = member.uid
                            channel.channelType = WKChannelType.PERSONAL
                            channel.channelName = member.name
                            channel.channelRemark = member.remark
                            members.add(channel)
                        }
                        entity.members = members
                        labels.add(entity)
                    }
                    ChooseLabelMenu!!.iChooseLabel.onResult(labels)
                }

            })

            null
        }
    }

    private var ChooseLabelMenu: ChooseLabelMenu? = null
    private var saveLabelMenu: SaveLabelMenu? = null

    fun setLabelSaved() {
        if (saveLabelMenu != null) {
            saveLabelMenu = null
            EndpointManager.getInstance().invoke("refresh_label_list", null)
        }
    }
}




