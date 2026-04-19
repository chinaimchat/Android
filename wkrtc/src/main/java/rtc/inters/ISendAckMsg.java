package rtc.inters;

/**
 * 5/12/21 2:42 PM
 */
public interface ISendAckMsg {
    void msgAck(long clientSeq, boolean isSuccess);
}
