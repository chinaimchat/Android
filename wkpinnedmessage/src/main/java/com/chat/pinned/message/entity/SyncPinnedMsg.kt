package com.chat.pinned.message.entity

import com.xinbida.wukongim.entity.WKSyncRecent

class SyncPinnedMsg {
    lateinit var pinned_messages: List<PinnedMessage>
    lateinit var messages: List<WKSyncRecent>
}