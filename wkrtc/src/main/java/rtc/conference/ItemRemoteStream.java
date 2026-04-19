package rtc.conference;

import owt.conference.Participant;
import owt.conference.RemoteStream;

/**
 * 5/8/21 7:10 PM
 */
public class ItemRemoteStream {
    public String uid;
    //远程流
    public RemoteStream remoteStream;
    //参与者
    public Participant participant;

    public ItemRemoteStream(String uid, RemoteStream remoteStream, Participant participant) {
        this.uid = uid;
        this.remoteStream = remoteStream;
        this.participant = participant;
    }
}
