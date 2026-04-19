package com.chat.uikit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * 透明中转 Activity：通知 / 前台服务通知点击先进入此处，再转发到目标界面，
 * 减少冷启动闪屏、并统一「互拉」到前台的 Intent 路径。
 */
class NotifyRelayActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dispatchForward()
    }

    private fun dispatchForward() {
        val forwardClassName = intent.getStringExtra(EXTRA_FORWARD_CLASS)
        val forward: Intent = if (!forwardClassName.isNullOrEmpty()) {
            try {
                Intent(this, Class.forName(forwardClassName)).apply {
                    intent.extras?.let { putExtras(it) }
                }
            } catch (_: ClassNotFoundException) {
                fallbackLaunchIntent()
            }
        } else {
            fallbackLaunchIntent()
        }
        forward.removeExtra(EXTRA_FORWARD_CLASS)
        forward.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
        if (forward.resolveActivity(packageManager) != null) {
            startActivity(forward)
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun fallbackLaunchIntent(): Intent {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        return launch ?: Intent(this, TabActivity::class.java)
    }

    companion object {
        const val EXTRA_FORWARD_CLASS = "notify_relay_forward_class"

        /**
         * @param forwardClass 最终要打开的 Activity，例如 [TabActivity]
         */
        fun relayIntent(context: Context, forwardClass: Class<*>): Intent {
            return Intent(context, NotifyRelayActivity::class.java).apply {
                putExtra(EXTRA_FORWARD_CLASS, forwardClass.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }

        /**
         * 将已配置好 Component 与 extras 的 Intent（如进入 [com.chat.uikit.chat.ChatActivity]）包一层中转。
         */
        fun relayIntent(context: Context, targetIntent: Intent): Intent {
            val component = targetIntent.component
                ?: return relayIntent(context, TabActivity::class.java)
            return Intent(context, NotifyRelayActivity::class.java).apply {
                putExtra(EXTRA_FORWARD_CLASS, component.className)
                targetIntent.extras?.let { putExtras(it) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }
}
