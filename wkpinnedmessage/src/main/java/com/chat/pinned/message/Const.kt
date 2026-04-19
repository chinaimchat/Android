package com.chat.pinned.message

class Const {
    companion object {
        const val cmdMessageDeleted = "messageDeleted"
        const val cmdSyncPinnedMessage = "syncPinnedMessage"
        const val allowMemberPinnedMessage = "allow_member_pinned_message"

        fun getHideChannelPinnedMsgKey(channelId: String, channelType: Byte): String {
            return String.format("hide_pin_msg_%s_%s", channelId, channelType)
        }
    }
}