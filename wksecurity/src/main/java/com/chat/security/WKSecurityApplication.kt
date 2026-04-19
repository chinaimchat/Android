package com.chat.security

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.chat.base.WKBaseApplication
import com.chat.base.config.WKConfig
import com.chat.base.config.WKSharedPreferencesUtil
import com.chat.base.endpoint.EndpointCategory
import com.chat.base.endpoint.EndpointHandler
import com.chat.base.endpoint.EndpointManager
import com.chat.base.endpoint.entity.ChatSettingCellMenu
import com.chat.base.endpoint.entity.PersonalInfoMenu
import com.chat.base.ui.components.SwitchView
import com.chat.base.utils.ActManagerUtils
import com.chat.base.utils.WKTimeUtils
import com.chat.security.service.SecurityModel
import com.chat.security.ui.ChatPwdActivity
import com.chat.security.ui.CheckLockScreenPwdActivity
import com.chat.security.ui.DisconnectScreenSaverActivity
import com.chat.security.ui.SecurityPrivacyActivity
import com.xinbida.wukongim.WKIM
import com.xinbida.wukongim.entity.WKChannelExtras

class WKSecurityApplication private constructor() {
    private object SingletonInstance {
        val INSTANCE = WKSecurityApplication()
    }

    companion object {
        val instance: WKSecurityApplication
            get() = SingletonInstance.INSTANCE
    }

    fun init() {
        EndpointManager.getInstance().setMethod(
            "security", EndpointCategory.personalCenter, 3
        ) {
            PersonalInfoMenu(
                R.mipmap.icon_security,
                WKBaseApplication.getInstance().context.getString(R.string.security_and_privacy),
                0
            ) {
                val intent =
                    Intent(
                        WKBaseApplication.getInstance().context,
                        SecurityPrivacyActivity::class.java
                    )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                WKBaseApplication.getInstance().context.startActivity(intent)
            }
        }
        EndpointManager.getInstance().setMethod(
            "add_security_module"
        ) { true }
        // 聊天设置的聊天密码view
        EndpointManager.getInstance().setMethod("chat_pwd_view", object : EndpointHandler {
            override fun invoke(`object`: Any?): Any? {
                if (`object` is ChatSettingCellMenu) {
                    return getChatPwdView(
                        `object`.parentLayout.context,
                        `object`.channelID,
                        `object`.channelType,
                        `object`.parentLayout
                    )
                }
                return null
            }
        })
        EndpointManager.getInstance().setMethod("show_set_chat_pwd") {
            val intent =
                Intent(
                    WKBaseApplication.getInstance().context,
                    ChatPwdActivity::class.java
                )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            WKBaseApplication.getInstance().context.startActivity(intent)
            null
        }

        /*        EndpointManager.getInstance().setMethod("choose_video_call_members", object -> {
            ChooseVideoCallMembersMenu callMembersMenu = (ChooseVideoCallMembersMenu) object;
            WKUIKitApplication.this.iChooseMembersBack = callMembersMenu.iChooseBack;
            Intent intent = new Intent(getContext(), ChooseVideoCallMembersActivity.class);
            intent.putExtra("channelID", callMembersMenu.channelID);
            intent.putExtra("channelType", callMembersMenu.channelType);
            intent.putStringArrayListExtra("selectedUIDs", (ArrayList<String>) callMembersMenu.selectedList);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            mContext.get().startActivity(intent);
            return null;
        });*/EndpointManager.getInstance()
            .setMethod("chow_check_lock_screen_pwd") {
                val lockTime =
                    WKSharedPreferencesUtil.getInstance().getLong("lock_start_time")
                val nowTime = WKTimeUtils.getInstance().currentSeconds
                val time = nowTime - lockTime
                val userInfoEntity = WKConfig.getInstance().userInfo
                var overtime = false
                val afterTime = userInfoEntity.lock_after_minute * 60
                if (time >= afterTime) {
                    overtime = true
                }
                val isTop = ActManagerUtils.getInstance().isActivityTop(
                    CheckLockScreenPwdActivity::class.java.name,
                    WKBaseApplication.getInstance().context
                )
                if (!TextUtils.isEmpty(userInfoEntity.lock_screen_pwd)
                    && overtime && !isTop
                    && !TextUtils.isEmpty(WKConfig.getInstance().token)
                ) {
                    val intent = Intent(
                        WKBaseApplication.getInstance().context,
                        CheckLockScreenPwdActivity::class.java
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    WKBaseApplication.getInstance().context.startActivity(intent)
                }
                null
            }
        EndpointManager.getInstance().setMethod("show_disconnect_screen") { `object`: Any? ->
            val context = `object` as Context?
            if (context != null) {
                val className: String =
                    DisconnectScreenSaverActivity::class.java.name
                if (WKConfig.getInstance()
                        .userInfo.setting.offline_protection == 1 && !ActManagerUtils.getInstance()
                        .isActivityTop(className, context)
                ) {
                    val intent =
                        Intent(context, DisconnectScreenSaverActivity::class.java)
                    context.startActivity(intent)
                }
            }
            null
        }
    }

    private fun getChatPwdView(
        context: Context,
        channelId: String,
        channelType: Byte,
        parentView: ViewGroup
    ): View {
        val view: View = LayoutInflater.from(context)
            .inflate(R.layout.item_chat_pwd_layuout, parentView, false)
        val chatPwdSwitchView = view.findViewById<SwitchView>(R.id.chatPwdSwitchView)
        chatPwdSwitchView.setOnCheckedChangeListener { compoundButton, isChecked ->
            if (compoundButton.isPressed) {
                if (TextUtils.isEmpty(WKConfig.getInstance().userInfo.chat_pwd)) {
                    EndpointManager.getInstance().invoke("show_set_chat_pwd", null)
                    chatPwdSwitchView.isChecked = !isChecked
                } else {
                    SecurityModel().updateChannelChatPwd(
                        channelId,
                        channelType,
                        if (isChecked) 1 else 0, null
                    )
                }
            }
        }
        val channel = WKIM.getInstance().channelManager.getChannel(channelId, channelType)
        if (channel?.remoteExtraMap != null && channel.remoteExtraMap.containsKey(WKChannelExtras.chatPwdOn)) {
            val `object` = channel.remoteExtraMap[WKChannelExtras.chatPwdOn]
            if (`object` != null) {
                chatPwdSwitchView.isChecked = `object` as Int == 1
            }
        }
        return view
    }
}