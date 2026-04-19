package rtc.inters;

/**
 * 5/12/21 11:12 AM 本地使用
 */
public interface ILocalListener {
    void onReceivedRTCMsg(String uid, String message);

    void onHangUp(String channelID, byte channelType, int second);

    void onRefuse(String channelID, byte channelType, String uid);

    void onSwitchVideoRespond(String uid, int status);

    void onCancel(String uid);

    void onPublish();

    default void onMultiRefuse(String roomID, String uid) {
    }

    default void onAccept(String uid, int callType) {
    }

    default void onRequestSwitchVideo(String uid) {
    }

    default void onSwitchAudio(String uid) {
    }

}
