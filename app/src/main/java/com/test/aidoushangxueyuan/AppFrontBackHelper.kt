package com.test.aidoushangxueyuan

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * 应用前后台监听：通过 Activity 启动/停止计数判断，
 * 计数由 0→1 视为进入前台，由 1→0 视为进入后台。
 */
class AppFrontBackHelper {

    private var listener: OnAppStatusListener? = null

    private val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        private var activityStartCount = 0

        override fun onActivityStarted(activity: Activity) {
            if (++activityStartCount == 1) listener?.onFront()
        }

        override fun onActivityStopped(activity: Activity) {
            if (--activityStartCount == 0) listener?.onBack()
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    fun register(application: Application, listener: OnAppStatusListener) {
        this.listener = listener
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    fun unregister(application: Application) {
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    interface OnAppStatusListener {
        fun onFront()
        fun onBack()
    }
}
