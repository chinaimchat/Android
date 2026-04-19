package rtc.utils;

import static org.webrtc.PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
import static owt.base.MediaCodecs.AudioCodec.OPUS;
import static owt.base.MediaCodecs.AudioCodec.PCMU;
import static owt.base.MediaCodecs.VideoCodec.H264;
import static owt.base.MediaCodecs.VideoCodec.VP8;
import static owt.base.MediaCodecs.VideoCodec.VP9;

import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;

import rtc.WKRTCApplication;
import com.yhao.floatwindow.FloatWindow;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnection;
import org.webrtc.RTCStatsReport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import owt.base.ActionCallback;
import owt.base.AudioCodecParameters;
import owt.base.LocalStream;
import owt.base.MediaConstraints;
import owt.base.OwtError;
import owt.base.VideoEncodingParameters;
import owt.conference.ConferenceClient;
import owt.conference.ConferenceClientConfiguration;
import owt.conference.ConferenceInfo;
import owt.conference.Participant;
import owt.conference.Publication;
import owt.conference.PublishOptions;
import owt.conference.RemoteMixedStream;
import owt.conference.RemoteStream;
import owt.conference.SubscribeOptions;
import owt.conference.Subscription;
import owt.utils.OwtVideoCapturer;
import rtc.conference.HttpUtils;
import rtc.conference.ItemRemoteStream;
import rtc.conference.WKRTCStatusReport;
import rtc.inters.ILocalListener;

public class MeetingDataProvider implements ConferenceClient.ConferenceClientObserver {
    private final String tag = "MeetingDataProvider";

    private MeetingDataProvider() {
    }

    @Override
    public void onStreamAdded(RemoteStream remoteStream) {
        addRemoteStream(remoteStream, null);
    }

    @Override
    public void onParticipantJoined(Participant participant) {
        String uid = participant.userId;
        boolean isAdd = true;
        for (int j = 0; j < list.size(); j++) {
            if (!TextUtils.isEmpty(list.get(j).uid)
                    && !TextUtils.isEmpty(uid) && list.get(j).uid.equals(uid)) {
                list.get(j).participant = participant;
                isAdd = false;
                break;
            }
        }
        if (isAdd) {
            list.add(new ItemRemoteStream(uid, null, participant));
            iMeetingListener.addItem(null, uid);
        }
    }

    @Override
    public void onMessageReceived(String message, String from, String to) {
        if (!TextUtils.isEmpty(message)) {
            try {
                JSONObject jsonObject = new JSONObject(message);
                String cmd = "";
                if (jsonObject.has("cmd")) {
                    cmd = jsonObject.optString("cmd");
                }
                if (TextUtils.isEmpty(cmd)) return;
                if (cmd.equals("removeParticipant")) {
                    String uid = jsonObject.optString("uid");
                    for (int i = 0, size = list.size(); i < size; i++) {
                        if (!TextUtils.isEmpty(uid) && !TextUtils.isEmpty(list.get(i).uid) && list.get(i).uid.equals(uid)) {
                            list.remove(i);
                            iMeetingListener.removeView(uid);
                            if (list.size() == 1) {
                                hangUp();
                            }
                            break;
                        }
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onServerDisconnected() {

        WKRTCManager.getInstance().stopTimer();
        FloatWindow.destroy();
        if (capturer != null) {
            capturer.stopCapture();
            capturer.dispose();
            capturer = null;
        }

        if (localStream != null) {
            localStream.dispose();
            localStream = null;
        }

        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).remoteStream != null) {
//                    list.get(i).remoteStream.detach(surfaceViewMap.get(list.get(i).uid));
                    list.get(i).remoteStream.disableAudio();
                    list.get(i).remoteStream = null;
                }
            }
            list.clear();
//            surfaceViewMap.clear();
        }
        if (timerMap != null && timerMap.size() > 0) {
            for (Map.Entry<String, CountDownTimer> entry : timerMap.entrySet()) {
                if (entry.getValue() != null) {
                    entry.getValue().cancel();
                }
            }
            timerMap.clear();
        }
        conferenceClient.removeObserver(this);
        publication = null;
        conferenceClient = null;
        iMeetingListener.hangup();

    }

    private static class MultiDataProviderBinder {
        final static MeetingDataProvider provider = new MeetingDataProvider();
    }

    public static MeetingDataProvider getInstance() {
        return MultiDataProviderBinder.provider;
    }

    private IMeetingListener iMeetingListener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ConferenceClient conferenceClient;
    public String roomID, token, loginUID, channelID;
    public byte channelType = 2;
    public LocalStream localStream;
    public OwtVideoCapturer capturer;
    public Publication publication;
    public List<ItemRemoteStream> list;
    public RemoteStream bigRemoteStream;
    private ConcurrentHashMap<String, WKRTCStatusReport> rtcStatusMap;
    //未进入通话的倒计时
    private ConcurrentHashMap<String, CountDownTimer> timerMap;
    private Timer checkRTCStatusReportTimer;

    private void startCheckRTCStatusReportTimer() {
        if (rtcStatusMap != null && list != null && rtcStatusMap.size() > 0 && list.size() > 0) {
            if (checkRTCStatusReportTimer == null) {
                checkRTCStatusReportTimer = new Timer();
            }
            checkRTCStatusReportTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    iMeetingListener.checkRTCStatusReport(rtcStatusMap, list);
                }
            }, 1000, 1000);
        }
    }

    public void addMeetingListener(IMeetingListener iMeetingListener) {
        this.iMeetingListener = iMeetingListener;
    }

    public void init(String loginUID, String channelID, byte channelType, String roomID, String token) {
        this.loginUID = loginUID;
        this.channelID = channelID;
        this.channelType = channelType;
        this.roomID = roomID;
        this.token = token;
    }

    private void initConferenceClient() {
        PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder(
                "turn:" + WKRTCApplication.getInstance().turnIP + ":3478?transport=udp").setUsername("user").setPassword(
                "passwd").createIceServer();
        PeerConnection.IceServer iceServer1 = PeerConnection.IceServer.builder(
                "stun:stun1.l.google.com:19302").createIceServer();
        PeerConnection.IceServer iceServer2 = PeerConnection.IceServer.builder(
                "stun:stun2.l.google.com:19302").createIceServer();
        PeerConnection.IceServer iceServer3 = PeerConnection.IceServer.builder(
                "stun:stunserver.org").createIceServer();
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(iceServer);
        iceServers.add(iceServer1);
        iceServers.add(iceServer2);
        iceServers.add(iceServer3);
        PeerConnection.RTCConfiguration rtcConfiguration = new PeerConnection.RTCConfiguration(
                iceServers);
        HttpUtils.setUpINSECURESSLContext();
        rtcConfiguration.continualGatheringPolicy = GATHER_CONTINUALLY;
        ConferenceClientConfiguration configuration
                = ConferenceClientConfiguration.builder()
                .setHostnameVerifier(HttpUtils.hostnameVerifier)
                .setSSLContext(HttpUtils.sslContext)
                .setRTCConfiguration(rtcConfiguration)
                .build();
        conferenceClient = new ConferenceClient(configuration);
        conferenceClient.addObserver(this);
    }

    public void start() {
        initConferenceClient();
        initListener();
        joinRoom();
    }

    private void initListener() {
        WKRTCManager.getInstance().addLocalListener(new ILocalListener() {

            @Override
            public void onReceivedRTCMsg(String uid, String message) {

            }

            @Override
            public void onHangUp(String channelID, byte channelType, int second) {

            }

            @Override
            public void onRefuse(String channelID, byte channelType, String uid) {
                if (channelID.equals(MeetingDataProvider.this.channelID) && channelType == MeetingDataProvider.this.channelType && !TextUtils.isEmpty(uid)) {
                    removeUser(uid);
                }
            }

            @Override
            public void onSwitchVideoRespond(String uid, int status) {

            }

            @Override
            public void onCancel(String uid) {

            }

            @Override
            public void onPublish() {

            }

            @Override
            public void onMultiRefuse(String roomID, String uid) {
                removeUser(uid);
            }
        });
    }


    private void joinRoomSuccess() {

        executor.execute(() -> {
            if (conferenceClient != null && conferenceClient.info() != null && conferenceClient.info().getRemoteStreams().size() > 0) {

                for (int i = 0; i < conferenceClient.info().getRemoteStreams().size(); i++) {
                    RemoteStream remoteStream = conferenceClient.info().getRemoteStreams().get(i);
                    if (remoteStream instanceof RemoteMixedStream
                            && ((RemoteMixedStream) remoteStream).view.equals("common")) {
                        continue;
                    }

                    String uid = "";
                    if (remoteStream.getAttributes() != null) {
                        uid = remoteStream.getAttributes().get("from");
                    }
                    Participant participant = null;
                    conferenceClient.info().getParticipants();
                    for (Participant participant1 : conferenceClient.info().getParticipants()) {
                        if (participant1.userId.equals(uid)) {
                            participant = participant1;
                            break;
                        }
                    }
                    addRemoteStream(remoteStream, participant);
                }

            } else {
                iMeetingListener.addView(list);
            }
        });
        //推流
        publish();
    }


    private void publish() {
        executor.execute(() -> {
            if (capturer == null) {
                capturer = OwtVideoCapturer.create(160, 120, 30, true, true);
                localStream = new LocalStream(capturer, new MediaConstraints.AudioTrackConstraints());
            }
            // 默认关闭视频
//            localStream.enableVideo();
            localStream.disableVideo();
            localStream.enableAudio();
            PublishOptions options;
            VideoEncodingParameters h264 = new VideoEncodingParameters(H264);
            VideoEncodingParameters vp8 = new VideoEncodingParameters(VP8);
            VideoEncodingParameters vp9 = new VideoEncodingParameters(VP9);
            options = PublishOptions.builder()
                    .addVideoParameter(vp8)
                    .addVideoParameter(h264)
                    .addVideoParameter(vp9)
                    .build();
            ActionCallback<Publication> callback = new ActionCallback<Publication>() {
                @Override
                public void onSuccess(final Publication result) {
                    publication = result;
                    WKRTCManager.getInstance().getSendMsgListener().sendMultiJoined(roomID);
                    WKRTCManager.getInstance().getSendMsgListener().sendMultiJoined(roomID);
                }

                @Override
                public void onFailure(final OwtError error) {

                }
            };
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("from", loginUID);
            localStream.setAttributes(hashMap);
            conferenceClient.publish(localStream, options, callback);
        });
    }

    public void joinRoom() {
        executor.execute(() -> conferenceClient.join(token, new ActionCallback<ConferenceInfo>() {
            @Override
            public void onSuccess(ConferenceInfo conferenceInfo) {
                joinRoomSuccess();
            }

            @Override
            public void onFailure(OwtError e) {
                WKLogger.e(tag, "加入房间错误" + e.errorMessage);
            }
        }));

    }

    public void hangUp() {
        if (conferenceClient != null) {
            executor.execute(() -> conferenceClient.leave());
        }
        unPublish();
        WKRTCManager.getInstance().isCalling = false;
        WKRTCManager.getInstance().getSendMsgListener().sendMultiHangup(roomID);
        if (iMeetingListener != null) {
            iMeetingListener.hangup();
        }
        if (checkRTCStatusReportTimer != null) {
            checkRTCStatusReportTimer.cancel();
            checkRTCStatusReportTimer = null;
        }
    }

    private void unPublish() {
        executor.execute(() -> {
            if (publication != null)
                publication.stop();
//            localStream.detach(localRenderer);
            if (capturer != null) {
                capturer.stopCapture();
                capturer.dispose();
                capturer = null;
            }
            if (localStream != null) {
                localStream.dispose();
                localStream = null;
            }
        });

    }

    public void startCountDownTimer(String uid) {

        CountDownTimer downTimer = new CountDownTimer(30 * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                removeUser(uid);
            }
        };
        downTimer.start();
        if (timerMap == null) timerMap = new ConcurrentHashMap<>();
        timerMap.put(uid, downTimer);

    }

    private void removeUser(String uid) {
        if (timerMap.containsKey(uid)) {
            CountDownTimer timer = timerMap.get(uid);
            if (timer != null) {
                timer.cancel();
            }
        }
        timerMap.remove(uid);
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                if (!TextUtils.isEmpty(list.get(i).uid) && list.get(i).uid.equals(uid)) {
                    list.remove(i);
                    iMeetingListener.removeView(uid);
//                    allView.removeViewAt(i);
                    if (list.size() == 1) {
                        hangUp();
                    }
                    break;
                }
            }
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("uid", uid);
            jsonObject.put("cmd", "removeParticipant");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (conferenceClient != null)
            conferenceClient.send(jsonObject.toString(), new ActionCallback<Void>() {
                @Override
                public void onSuccess(Void result) {

                }

                @Override
                public void onFailure(OwtError error) {

                }
            });
    }


    private void addRemoteStream(RemoteStream remoteStream, Participant participant) {
        String uid = "";
        HashMap<String, String> hashMap = remoteStream.getAttributes();
        if (hashMap != null && hashMap.containsKey("from")) {
            uid = hashMap.get("from");
        }
        String finalUid = uid;
        executor.execute(() -> {
            SubscribeOptions.VideoSubscriptionConstraints videoOption =
                    SubscribeOptions.VideoSubscriptionConstraints.builder()
                            .build();

            SubscribeOptions.AudioSubscriptionConstraints audioOption =
                    SubscribeOptions.AudioSubscriptionConstraints.builder()
                            .addCodec(new AudioCodecParameters(OPUS))
                            .addCodec(new AudioCodecParameters(PCMU))
                            .build();

            SubscribeOptions options = SubscribeOptions.builder(true, true)
                    .setAudioOption(audioOption)
                    .setVideoOption(videoOption)
                    .build();
            remoteStream.addObserver(new owt.base.RemoteStream.StreamObserver() {
                @Override
                public void onEnded() {

                    for (int i = 0, size = list.size(); i < size; i++) {
                        if (!TextUtils.isEmpty(list.get(i).uid) && !TextUtils.isEmpty(finalUid) && list.get(i).uid.equals(finalUid)) {
                            list.remove(i);
                            iMeetingListener.removeView(finalUid);
                            //allView.removeViewAt(i);
                            break;
                        }
                    }
                    if (list.size() <= 1) {
                        hangUp();
                    }


                }

                @Override
                public void onUpdated() {
                }
            });

            conferenceClient.subscribe(remoteStream, options, new ActionCallback<Subscription>() {
                @Override
                public void onSuccess(Subscription result) {

                    if (!TextUtils.isEmpty(finalUid) && !TextUtils.isEmpty(loginUID)) {
                        if (loginUID.equals(finalUid)) {
                            remoteStream.disableAudio();
                        }
                        if (!loginUID.equals(finalUid)) {
                            WKRTCManager.getInstance().startTimer();
                        }
                    }
                    if (!TextUtils.isEmpty(finalUid) && timerMap != null && timerMap.size() > 0) {
                        CountDownTimer countDownTimer = timerMap.get(finalUid);
                        if (countDownTimer != null) countDownTimer.cancel();
                        timerMap.remove(finalUid);
                    }

                    result.getStats(new ActionCallback<>() {
                        @Override
                        public void onSuccess(RTCStatsReport result) {
                            if (rtcStatusMap == null) rtcStatusMap = new ConcurrentHashMap<>();
                            if (!TextUtils.isEmpty(finalUid)) {
                                rtcStatusMap.put(finalUid, new WKRTCStatusReport(result, 0f));
                            }
                            //startCheckRTCStatusReportTimer();
                        }

                        @Override
                        public void onFailure(OwtError error) {

                        }
                    });
                    result.addObserver(new Subscription.SubscriptionObserver() {
                        @Override
                        public void onEnded() {
                        }

                        @Override
                        public void onError(OwtError error) {

                        }

                        @Override
                        public void onMute(MediaConstraints.TrackKind trackKind) {
                            if (!TextUtils.isEmpty(finalUid)) {
                                iMeetingListener.onMute(list, finalUid, trackKind);
                            }


                        }

                        @Override
                        public void onUnmute(MediaConstraints.TrackKind trackKind) {
                            if (!TextUtils.isEmpty(finalUid)) {
                                iMeetingListener.onUnmute(list, finalUid, trackKind);
                            }


                        }
                    });
                    iMeetingListener.resetView(list, finalUid, participant, remoteStream);
                }

                @Override
                public void onFailure(OwtError error) {
                    Log.e("订阅失败", error.errorMessage);
                }
            });
        });
    }


    public interface IMeetingListener {
        void removeView(String uid);

        void onUnmute(List<ItemRemoteStream> list, String uid, MediaConstraints.TrackKind trackKind);

        void onMute(List<ItemRemoteStream> list, String uid, MediaConstraints.TrackKind trackKind);

        void resetView(List<ItemRemoteStream> list, String uid, Participant participant, RemoteStream remoteStream);

        void hangup();

        void addView(List<ItemRemoteStream> list);

        void addItem(RemoteStream remoteStream, String uid);

        void checkRTCStatusReport(ConcurrentHashMap<String, WKRTCStatusReport> rtcStatusMap, List<ItemRemoteStream> list);
    }
}
