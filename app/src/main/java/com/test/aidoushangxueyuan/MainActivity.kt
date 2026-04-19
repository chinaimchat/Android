package com.test.aidoushangxueyuan

import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.view.View
import androidx.core.content.ContextCompat
import android.app.Activity
import com.chat.base.WKBaseApplication
import com.chat.base.base.WKBaseActivity
import com.chat.base.config.WKApiConfig
import com.chat.base.config.WKConfig
import com.chat.base.config.WKSharedPreferencesUtil
import com.chat.base.ui.components.NormalClickableContent
import com.chat.base.ui.components.NormalClickableSpan
import com.chat.base.utils.WKDialogUtils
import com.chat.login.ui.PerfectUserInfoActivity
import com.chat.login.ui.WKLoginActivity
import com.chat.uikit.TabActivity
import com.test.aidoushangxueyuan.databinding.ActivityMainBinding
import com.xinbida.wukongim.WKIM

class MainActivity : WKBaseActivity<ActivityMainBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // `core-splashscreen` 在不同版本里 Kotlin 的入口（扩展/静态/Companion）可能不一致，
        // 这里用反射方式避免编译期找不到 `installSplashScreen`。
        tryInstallSplashScreen(this)
        super.onCreate(savedInstanceState)
    }

    private fun tryInstallSplashScreen(activity: Activity) {
        val targetClasses = arrayOf(
            "androidx.core.splashscreen.SplashScreen",     // 可能是静态/Companion
            "androidx.core.splashscreen.SplashScreenKt"    // 可能是 Kotlin 顶层函数所在类
        )
        for (className in targetClasses) {
            try {
                val clazz = Class.forName(className)
                // 1) 先找类上同名方法：installSplashScreen( Activity ... )
                runCatching {
                    val method = clazz.methods.firstOrNull { it.name == "installSplashScreen" && it.parameterTypes.size == 1 }
                    if (method != null) {
                        val instance = if (java.lang.reflect.Modifier.isStatic(method.modifiers)) null else null
                        method.invoke(instance, activity)
                        return
                    }
                }

                // 2) 再找 Companion 实例的方法（如果存在）
                val companionObj = runCatching { clazz.getField("Companion").get(null) }.getOrNull()
                if (companionObj != null) {
                    val method = companionObj.javaClass.methods.firstOrNull { it.name == "installSplashScreen" && it.parameterTypes.size == 1 }
                    if (method != null) {
                        method.invoke(companionObj, activity)
                        return
                    }
                }
            } catch (_: Throwable) {
                // try next
            }
        }
    }

    override fun getViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun initView() {
        super.initView()
        val isShowDialog: Boolean =
            WKSharedPreferencesUtil.getInstance().getBoolean("show_agreement_dialog")
        if (isShowDialog) {
            showDialog()
        } else gotoApp()
    }

    private fun gotoApp() {
        val from = intent.getIntExtra("from", 0)
        if (TextUtils.isEmpty(WKConfig.getInstance().token)) {
            startActivity(loginIntent(from))
            finish()
            return
        }
        if (TextUtils.isEmpty(WKConfig.getInstance().userInfo.name)) {
            startActivity(Intent(this, PerfectUserInfoActivity::class.java))
            finish()
            return
        }
        val publicRSAKey = WKIM.getInstance().cmdManager.rsaPublicKey
        if (TextUtils.isEmpty(publicRSAKey)) {
            startActivity(loginIntent(from))
            finish()
            return
        }
        startActivity(Intent(this, TabActivity::class.java))
        finish()
    }

    private fun loginIntent(from: Int) =
        Intent(this, WKLoginActivity::class.java).putExtra("from", from)

    private fun showDialog() {
        val content = getString(R.string.dialog_content)
        val linkSpan = SpannableStringBuilder()
        linkSpan.append(content)
        val userAgreementIndex = content.indexOf(getString(R.string.main_user_agreement))
        linkSpan.setSpan(
            NormalClickableSpan(
                true,
                ContextCompat.getColor(this, R.color.blue),
                NormalClickableContent(NormalClickableContent.NormalClickableTypes.Other, ""),
                object : NormalClickableSpan.IClick {
                    override fun onClick(view: View) {
                        showWebView(
                            WKApiConfig.baseWebUrl + "user_agreement.html"
                        )
                    }
                }), userAgreementIndex, userAgreementIndex + getString(R.string.main_user_agreement).length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val privacyPolicyIndex = content.indexOf(getString(R.string.main_privacy_policy))
        linkSpan.setSpan(
            NormalClickableSpan(true,
                ContextCompat.getColor(this, R.color.blue),
                NormalClickableContent(NormalClickableContent.NormalClickableTypes.Other, ""),
                object : NormalClickableSpan.IClick {
                    override fun onClick(view: View) {
                        showWebView(WKApiConfig.baseWebUrl + "privacy_policy.html")
                    }
                }), privacyPolicyIndex, privacyPolicyIndex + getString(R.string.main_privacy_policy).length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        WKDialogUtils.getInstance().showDialog(
            this,
            getString(R.string.dialog_title),
            linkSpan,
            false,
            getString(R.string.disagree),
            getString(R.string.agree),
            0,
            0
        ) { index ->
            if (index == 1) {
                WKSharedPreferencesUtil.getInstance()
                    .putBoolean("show_agreement_dialog", false)
                WKBaseApplication.getInstance().init(
                    WKBaseApplication.getInstance().packageName,
                    WKBaseApplication.getInstance().application
                )
                gotoApp()
            } else {
                finish()
            }
        }
    }
}
