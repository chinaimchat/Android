package com.chat.rtc.entity

class RtcOfflineMsg(
    val channelId: String,
    val channelType: Byte,
    val invokeUID: String,
    val callType: Int
)