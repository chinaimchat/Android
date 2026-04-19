package com.chat.base.entity

class UserOnlineStatus {
    companion object {
        const val APP = 0
        const val Web = 1
        const val PC = 2
        /** 服务端隐藏分端，界面只保留是否在线 */
        const val Hidden = 3

        @JvmStatic
        fun showPeerDeviceBreakdown(deviceFlag: Int, cfg: WKAPPConfig?): Boolean {
            if (deviceFlag == Hidden) return false
            val on = cfg?.show_device_online_on
            return on == null || on != 0
        }
    }
}