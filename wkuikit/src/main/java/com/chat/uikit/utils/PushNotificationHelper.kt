package com.chat.uikit.utils

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.chat.base.WKBaseApplication
import com.chat.base.config.WKConstants
import com.chat.base.utils.NotificationCompatUtil
import com.chat.uikit.NotifyRelayActivity
import com.chat.uikit.R
import com.chat.uikit.TabActivity
import com.chat.uikit.chat.manager.WKIMUtils

object PushNotificationHelper {
    // 使用独立的新渠道 ID，规避旧渠道被系统/历史配置静音后无法代码覆盖的问题。
    private const val NEW_MSG_BACKGROUND_SOUND_CHANNEL_ID = "wk_new_msg_notification_sound_v1"

    /** 应用前台内的聊天消息通知。 */
    private val MESSAGE_IN_APP = NotificationCompatUtil.Channel(
        channelId = WKConstants.newMsgInAppChannelID,
        name = WKBaseApplication.getInstance().context.getString(R.string.new_msg_notification),
        importance = NotificationManager.IMPORTANCE_DEFAULT
    )

    /** 应用退到桌面后台后，使用高优先级横幅通知。 */
    private val MESSAGE_BACKGROUND = NotificationCompatUtil.Channel(
        channelId = NEW_MSG_BACKGROUND_SOUND_CHANNEL_ID,
        name = WKBaseApplication.getInstance().context.getString(R.string.new_msg_notification),
        importance = NotificationManager.IMPORTANCE_HIGH,
        lockScreenVisibility = NotificationCompat.VISIBILITY_PUBLIC,
        vibrate = longArrayOf(0, 250),
        sound = Uri.parse("android.resource://" + WKBaseApplication.getInstance().context.packageName + "/" + R.raw.newmsg)
    )

    /** 通知渠道-@提醒消息(重要性级别-紧急：发出提示音，并以浮动通知的形式显示 & 锁屏显示 & 振动0.25s )*/
    private val MENTION = NotificationCompatUtil.Channel(
        channelId = WKConstants.newMsgChannelID,
        name = WKBaseApplication.getInstance().context.getString(R.string.new_msg_notification),
        importance = NotificationManager.IMPORTANCE_HIGH,
        lockScreenVisibility = NotificationCompat.VISIBILITY_PUBLIC,
        vibrate = longArrayOf(0, 250),
        sound = Uri.parse("android.resource://" + WKBaseApplication.getInstance().context.packageName + "/" + R.raw.newmsg)
    )

    /** 通知渠道-系统通知(重要性级别-中：无提示音) */
    private val NOTICE = NotificationCompatUtil.Channel(
        channelId = WKConstants.newMsgChannelID,
        name = WKBaseApplication.getInstance().context.getString(R.string.new_msg_notification),
        importance = NotificationManager.IMPORTANCE_LOW
    )

    /** 通知渠道-音视频通话(重要性级别-紧急：发出提示音，并以浮动通知的形式显示 & 锁屏显示 & 振动4s停2s再振动4s ) */
    private val CALL = NotificationCompatUtil.Channel(
        channelId = WKConstants.newRTCChannelID,
        name = WKBaseApplication.getInstance().context.getString(R.string.new_rtc_notification),
        importance = NotificationManager.IMPORTANCE_HIGH,
        lockScreenVisibility = NotificationCompat.VISIBILITY_PUBLIC,
        vibrate = longArrayOf(0, 4000, 2000, 4000),
        sound = Uri.parse("android.resource://" + WKBaseApplication.getInstance().context.packageName + "/" + R.raw.newrtc)
    )

    /**
     * 显示聊天消息；点击通知进入对应会话 [com.chat.uikit.chat.ChatActivity]。
     * @param channelId 会话 channel id
     * @param channelType 会话类型（单聊/群等）
     */
    /**
     * 离线推送（FCM/HMS/小米/OPPO 等 data）：始终使用高重要性渠道，保证 heads-up/横幅，不依赖厂商系统 notification。
     * @param notifyTag 与服务端 notify_id 一致，用于 setGroup 折叠同一会话
     */
    fun notifyOfflinePush(
        context: Context,
        id: Int,
        notifyTag: String?,
        title: String?,
        text: String?,
        channelId: String,
        channelType: Byte
    ) {
        val chatIntent = WKIMUtils.getInstance().buildChatIntentForNotification(context, channelId, channelType)
        val intent = NotifyRelayActivity.relayIntent(context, chatIntent)
        val builder = NotificationCompatUtil.createNotificationBuilder(
            context,
            MESSAGE_BACKGROUND,
            title,
            text,
            intent
        )
        // 后台离线推送要每条都可提示声音，不能只在首次出现时响铃。
        builder.setOnlyAlertOnce(false)
        builder.setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(text)
        )
        builder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
        val notification = buildDefaultConfig(builder)
        if (!notifyTag.isNullOrEmpty()) {
            NotificationCompatUtil.notify(context, notifyTag, id, notification)
        } else {
            NotificationCompatUtil.notify(context, id, notification)
        }
    }

    fun notifyMessage(
        context: Context,
        id: Int,
        title: String?,
        text: String?,
        channelId: String,
        channelType: Byte
    ) {
        val chatIntent = WKIMUtils.getInstance().buildChatIntentForNotification(context, channelId, channelType)
        val intent = NotifyRelayActivity.relayIntent(context, chatIntent)
        // 组织内强提醒模式：前后台统一走高优先级有声渠道，确保每条消息都可弹窗提示。
        val notifChannel = MESSAGE_BACKGROUND

        val builder = NotificationCompatUtil.createNotificationBuilder(
            context,
            notifChannel,
            title,
            text,
            intent
        )
        // 每条都允许提示音/横幅，避免同会话更新时不再响铃。
        builder.setOnlyAlertOnce(false)

        // 默认情况下，通知的文字内容会被截断以放在一行。如果您想要更长的通知，可以使用 setStyle() 添加样式模板来启用可展开的通知。
        builder.setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(text)
        )
        builder.setCategory(NotificationCompat.CATEGORY_MESSAGE)

        NotificationCompatUtil.notify(context, id, buildDefaultConfig(builder));
    }

    /**
     * 显示@提醒消息
     * @param context 上下文
     * @param id      通知的唯一ID
     * @param title   标题
     * @param text    正文文本
     */
    fun notifyMention(
        context: Context,
        id: Int,
        title: String?,
        text: String?
    ) {
        val intent = NotifyRelayActivity.relayIntent(context, TabActivity::class.java)

        val builder = NotificationCompatUtil.createNotificationBuilder(
            context,
            MENTION,
            title,
            text,
            intent
        )

        // 默认情况下，通知的文字内容会被截断以放在一行。如果您想要更长的通知，可以使用 setStyle() 添加样式模板来启用可展开的通知。
        builder.setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(text)
        )
        builder.setCategory(NotificationCompat.CATEGORY_MESSAGE)

        NotificationCompatUtil.notify(context, id, buildDefaultConfig(builder));
    }

    /**
     * 显示系统通知
     * @param context 上下文
     * @param id      通知的唯一ID
     * @param title   标题
     * @param text    正文文本
     */
    fun notifyNotice(
        context: Context,
        id: Int,
        title: String?,
        text: String?
    ) {
        val intent = NotifyRelayActivity.relayIntent(context, TabActivity::class.java)
        val builder = NotificationCompatUtil.createNotificationBuilder(
            context,
            NOTICE,
            title,
            text,
            intent
        )

        NotificationCompatUtil.notify(context, id, buildDefaultConfig(builder));
    }

    /**
     * 显示音视频通话
     * @param context 上下文
     * @param id      通知的唯一ID
     * @param title   标题
     * @param text    正文文本
     */
    fun notifyCall(
        context: Context,
        id: Int,
        title: String?,
        text: String?
    ) {
        val intent = NotifyRelayActivity.relayIntent(context, TabActivity::class.java)
        val builder = NotificationCompatUtil.createNotificationBuilder(
            context,
            CALL,
            title,
            text,
            intent
        )
        NotificationCompatUtil.notify(context, id, buildDefaultConfig(builder));
    }

    /**
     * 构建应用通知的默认配置
     * @param builder 构建器
     */
    private fun buildDefaultConfig(builder: NotificationCompat.Builder): Notification {
        builder.setSmallIcon(R.mipmap.ic_logo)
        return builder.build()
    }
}
