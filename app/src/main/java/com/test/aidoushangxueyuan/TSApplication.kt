package com.test.aidoushangxueyuan

import android.app.Activity
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.text.TextUtils
import android.util.Log
import androidx.multidex.MultiDexApplication
import com.chat.base.WKBaseApplication
import com.chat.base.config.WKApiConfig
import com.chat.base.config.WKConfig
import com.chat.base.config.WKConstants
import com.chat.base.config.WKSharedPreferencesUtil
import com.chat.base.endpoint.EndpointManager
import com.chat.base.ui.Theme
import com.chat.base.utils.ActManagerUtils
import com.chat.base.utils.WKPlaySound
import com.chat.base.utils.WKTimeUtils
import com.chat.advanced.WKAdvancedApplication
import com.chat.base.utils.language.WKMultiLanguageUtil
import com.chat.customerservice.WKCustomerServiceApplication
import com.chat.favorite.WKFavoriteApplication
import com.chat.file.WKFileApplication
import com.chat.groupmanage.WKGroupManageApplication
import com.chat.imgeditor.WKImageEditorApplication

import com.chat.login.WKLoginApplication

import com.chat.pinned.message.WKPinnedMessageApplication
import com.chat.push.WKPushApplication
import com.chat.richeditor.WKRichApplication
import com.chat.scan.WKScanApplication
import com.chat.security.WKSecurityApplication
import com.chat.sticker.WKStickerApplication
import com.chat.uikit.ImConnectionForegroundController
import com.chat.uikit.TabActivity
import com.chat.video.WKVideoApplication
import com.chat.wallet.WKWalletApplication
import com.chat.uikit.WKUIKitApplication
import com.chat.uikit.chat.manager.WKIMUtils
import com.chat.uikit.user.service.UserModel
import kotlin.system.exitProcess

class TSApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        val processName = getProcessName(this, Process.myPid())
        if (processName != null) {
            val defaultProcess = processName == getAppPackageName()
            if (defaultProcess) {
                initAll()
            }
        }
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(p0: Activity, p1: Bundle?) {
                lockPortraitSafely(p0)
            }

            override fun onActivityStarted(p0: Activity) {
                WKUIKitApplication.getInstance().setAppInForeground(true)
            }

            override fun onActivityResumed(p0: Activity) {
                ActManagerUtils.getInstance().currentActivity = p0
            }

            override fun onActivityPaused(p0: Activity) {
            }

            override fun onActivityStopped(p0: Activity) {
            }

            override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
            }

            override fun onActivityDestroyed(p0: Activity) {
            }
        })
    }

    /**
     * 全应用强制竖屏。<application android:screenOrientation> 不是合法属性会被忽略，
     * 所以这里在每个 Activity 的 onCreate 回调里兜底 setRequestedOrientation。
     * Android 8.0 (API 26) 透明主题下会抛 IllegalStateException，捕获即可，
     * 透明 Activity 本身就跟随父页方向，不强行设置不会出现横屏。
     */
    private fun lockPortraitSafely(activity: Activity) {
        try {
            if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        } catch (t: Throwable) {
            Log.w("TSApplication", "lockPortrait failed for ${activity.javaClass.simpleName}", t)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (applicationContext != null && applicationContext.resources != null && applicationContext.resources.configuration != null && applicationContext.resources.configuration.uiMode != newConfig.uiMode) {
            WKMultiLanguageUtil.getInstance().setConfiguration()
            Theme.applyTheme()
            killAppProcess()
        }
    }

    private fun killAppProcess() {
        ActManagerUtils.getInstance().clearAllActivity()
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(WKMultiLanguageUtil.getInstance().attachBaseContext(base))
    }

    private fun initAll() {
        WKMultiLanguageUtil.getInstance().init(this)
        WKBaseApplication.getInstance().init(getAppPackageName(), this)
        Theme.applyTheme()
        initApi()
        initFeatureModules()
        WKLoginApplication.getInstance().init(this)
        WKScanApplication.getInstance().init(this)
        WKUIKitApplication.getInstance().init(this)
        WKPushApplication.getInstance().init(getAppPackageName(), this)
        addAppFrontBack()
        addListener()
    }

    /**
     * 多域名入口：从 [com.chat.base.net.ApiHostPool] 取当前首选 host 拼 API 根，
     * 首次启动会从 11 个候选域中随机挑一个（等价入口分流）；运行期由
     * DomainFalloverInterceptor 按池顺序自动切换健康域名，保持与 Web / iOS 一致。
     * 不再把任何主域写死在此处。
     */
    private fun initApi() {
        val apiURL = com.chat.base.net.ApiHostPool.defaultApiBaseURL()
        WKSharedPreferencesUtil.getInstance().putSP("api_base_url", apiURL)
        WKApiConfig.initBaseURLIncludeIP(apiURL)
    }

    /** 注册各模块的聊天加号、消息类型等；否则只有 UIKit 内置功能。 */
    private fun initFeatureModules() {
        WKGroupManageApplication.getInstance().init()
        WKFileApplication.getInstance().init(this)
        // WKMomentsApplication.getInstance().init(this) // 模块已停用
        WKWalletApplication.getInstance().init(this)
        WKVideoApplication.getInstance().init(this)
        WKAdvancedApplication.instance.init()
        WKStickerApplication.instance.init()
        WKSecurityApplication.instance.init()
        WKFavoriteApplication.instance.init()
        WKCustomerServiceApplication.instance.init(WKBaseApplication.getInstance().appID)
        // WKLabelApplication.instance.init(this) // 模块已停用
        WKRichApplication.instance.init()
        WKImageEditorApplication.getInstance().init()
        WKPinnedMessageApplication.init()
    }

    private fun getAppPackageName(): String {
        return packageName  // 动态获取实际的 applicationId
    }

    private fun getProcessName(cxt: Context, pid: Int): String? {
        val am = cxt.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val runningApps = am.runningAppProcesses ?: return null
        for (app in runningApps) {
            if (app.pid == pid) {
                return app.processName
            }
        }
        return null
    }

    private fun addAppFrontBack() {
        val helper = AppFrontBackHelper()
        helper.register(this, object : AppFrontBackHelper.OnAppStatusListener {
            override fun onFront() {
                ImConnectionForegroundController.stop(this@TSApplication)
                WKUIKitApplication.getInstance().setAppInForeground(true)
                if (!TextUtils.isEmpty(WKConfig.getInstance().token)) {
                    val keepBackgroundNotifications =
                        WKConfig.getInstance().userInfo?.setting?.new_msg_notice == 1
                    if (WKBaseApplication.getInstance().disconnect) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            EndpointManager.getInstance()
                                .invoke("chow_check_lock_screen_pwd", null)
                        }, 1000)
                    }
                    if (!keepBackgroundNotifications) {
                        WKIMUtils.getInstance().initIMListener()
                    }
                    WKUIKitApplication.getInstance().startChat()
                    UserModel.getInstance().getOnlineUsers()

                }
            }

            override fun onBack() {
                WKUIKitApplication.getInstance().setAppInForeground(false)
                val result = EndpointManager.getInstance().invoke("rtc_is_calling", null)
                var isCalling = false
                if (result != null) {
                    isCalling = result as Boolean
                }
                val keepBackgroundNotifications =
                    WKConfig.getInstance().userInfo?.setting?.new_msg_notice == 1
                val imFgsEnabled = WKSharedPreferencesUtil.getInstance().getBoolean(
                    ImConnectionForegroundController.PREF_IM_FGS_KEEP_ALIVE,
                    true
                )
                val shouldStopConn =
                    WKBaseApplication.getInstance().disconnect && !isCalling && !keepBackgroundNotifications
                if (shouldStopConn) {
                    if (imFgsEnabled) {
                        ImConnectionForegroundController.startIfEnabled(this@TSApplication)
                    } else {
                        WKUIKitApplication.getInstance().stopConn()
                    }
                } else if (imFgsEnabled && !TextUtils.isEmpty(WKConfig.getInstance().token)) {
                    ImConnectionForegroundController.startIfEnabled(this@TSApplication)
                }
                if (!keepBackgroundNotifications) {
                    WKIMUtils.getInstance().removeListener()
                }
                WKSharedPreferencesUtil.getInstance()
                    .putLong("lock_start_time", WKTimeUtils.getInstance().currentSeconds)

            }
        })
    }

    private fun addListener() {
        createNotificationChannel()
        EndpointManager.getInstance().setMethod("main_show_home_view") { `object` ->
            if (`object` != null) {
                val from = `object` as Int
                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("from", from)
                startActivity(intent)
            }
            null
        }
        EndpointManager.getInstance().setMethod("show_tab_home") {
            val intent = Intent(applicationContext, TabActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            null
        }

        EndpointManager.getInstance().setMethod("play_new_msg_Media") {
            WKPlaySound.getInstance().playRecordMsg(R.raw.newmsg)
            null
        }
    }


    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = applicationContext.getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(
                createMessageChannel(
                    WKConstants.newMsgChannelID,
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
            notificationManager.createNotificationChannel(
                createMessageChannel(
                    WKConstants.newMsgInAppChannelID,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        createNotificationRTCChannel()
    }

    private fun createNotificationRTCChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = applicationContext.getString(R.string.new_rtc_notification)
            val description = applicationContext.getString(R.string.new_rtc_notification_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(WKConstants.newRTCChannelID, name, importance)
            channel.description = description
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(0, 100, 100, 100, 100, 100)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channel.setSound(
                Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.newrtc),
                Notification.AUDIO_ATTRIBUTES_DEFAULT
            )
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = applicationContext.getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createMessageChannel(channelId: String, importance: Int): NotificationChannel {
        val name: CharSequence = applicationContext.getString(R.string.new_msg_notification)
        val description = applicationContext.getString(R.string.new_msg_notification_desc)
        return NotificationChannel(channelId, name, importance).apply {
            this.description = description
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(
                Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.newmsg),
                Notification.AUDIO_ATTRIBUTES_DEFAULT
            )
        }
    }

}
