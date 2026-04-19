package com.chat.pinned.message.entity

class PinnedMessage {
    var message_id: String = ""
    var message_seq: Long = 0
    var channel_id: String = ""
    var channel_type: Int = 0
    var is_deleted: Int = 0
    var version: Long = 0
    var created_at: String = ""
    var updated_at: String = ""
}