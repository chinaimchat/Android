package com.chat.uikit.utils

import android.Manifest
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.FragmentActivity
import com.chat.base.endpoint.EndpointManager
import com.chat.base.ui.Theme
import com.chat.base.utils.WKDialogUtils
import com.chat.base.utils.rxpermissions.RxPermissions
import com.chat.uikit.R

object NotificationPermissionHelper {

    @JvmStatic
    fun ensureNotificationPermission(activity: FragmentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val desc = activity.getString(
                R.string.notification_permissions_desc,
                activity.getString(R.string.app_name)
            )
            RxPermissions(activity).request(Manifest.permission.POST_NOTIFICATIONS).subscribe { granted ->
                if (!granted) {
                    WKDialogUtils.getInstance().showDialog(
                        activity,
                        activity.getString(com.chat.base.R.string.authorization_request),
                        desc,
                        true,
                        activity.getString(R.string.cancel),
                        activity.getString(R.string.to_set),
                        0,
                        Theme.colorAccount
                    ) { index ->
                        if (index == 1) {
                            EndpointManager.getInstance().invoke("show_open_notification_dialog", activity)
                        }
                    }
                }
            }
            return
        }
        if (!NotificationManagerCompat.from(activity).areNotificationsEnabled()) {
            EndpointManager.getInstance().invoke("show_open_notification_dialog", activity)
        }
    }
}
