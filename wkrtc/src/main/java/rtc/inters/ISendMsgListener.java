package rtc.inters;


public interface ISendMsgListener {
    void sendRTCMsg(String uid, String message, ISendMsgBack iSendMsgBack);

    void sendHangUpMsg(String uid, int second,int callType,int isCaller);

    void sendRefuse(String channelID, byte channelType,int callType);

    void sendMultiRefuse(String roomID);

    void sendMultiHangup(String roomID);

    void sendMultiJoined(String roomID);
    // void sendMissed(String channelID, byte channelType, int callType);

    void sendCancel(String uid, int callType);

    void sendSwitchAudio(String fromUID, String toUID);

    void sendSwitchVideo(String fromUID, String toUID);

    void sendSwitchVideoRespond(String fromUID, String toUID, int status);

    void sendAccept(String fromUID, String toUID, int callType);
}
