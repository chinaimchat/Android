package rtc.inters;

/**
 * 5/12/21 11:03 AM
 * 事件
 */
public interface ISaveMsgListener {

    void onHangUp(boolean isCreator, String toUID, String time, int callType);

    void onRefuse(boolean isCreator, String toUID, int callType);

    void onMissed(boolean isCreator, String toUID, int callType);

    void onCancel(boolean isCreator, String toUID, int callType);
}
