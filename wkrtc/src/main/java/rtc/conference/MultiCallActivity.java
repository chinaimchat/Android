package rtc.conference;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static org.webrtc.PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
import static owt.base.MediaCodecs.AudioCodec.OPUS;
import static owt.base.MediaCodecs.AudioCodec.PCMU;
import static owt.base.MediaCodecs.VideoCodec.VP9;

import android.Manifest;
import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.chat.rtc.R;
import com.yhao.floatwindow.FloatWindow;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnection;
import org.webrtc.RTCStats;
import org.webrtc.RTCStatsReport;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import rtc.RTCBaseActivity;
import rtc.WKRTCApplication;
import rtc.inters.ILocalListener;
import rtc.utils.BtnClickUtils;
import rtc.utils.WKRTCCommonUtils;
import rtc.utils.RTCAudioPlayer;
import rtc.utils.WKLogger;
import rtc.utils.WKRTCManager;
import rtc.view.MultiVideoChatLayout;

/**
 * 5/6/21 6:22 PM
 * 多人
 */
public class MultiCallActivity extends RTCBaseActivity implements ConferenceClient.ConferenceClientObserver {
    private final String tag = "MultiCallActivity";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView timeTv;
    private View switchCameraView;
    private SurfaceViewRenderer bigSurfaceView;
    private MultiVideoChatLayout allView;
    private AppCompatImageView microphoneIv, speakerIv, cameraIv;
    private View microphoneView, speakerView, cameraView;
    private ConferenceClient conferenceClient;
    private ConferenceInfo conferenceInfo;
    private LocalStream localStream;
    private OwtVideoCapturer capturer;
    private Publication publication;
    private List<ItemRemoteStream> list;
    //    private ConcurrentHashMap<String, SurfaceViewRenderer> surfaceViewMap;
    private ConcurrentHashMap<String, WKRTCStatusReport> rtcStatusMap;
    //未进入通话的倒计时
    private ConcurrentHashMap<String, CountDownTimer> timerMap;
    private String token;

    private String roomID;
    private String channelID;
    private byte channelType;
    private String loginUID;
    private View hangupIv;
    private boolean isOpenCamera = false;
    private boolean isOpenSpeaker = false;
    private boolean isOpenMicrophone = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (WKRTCManager.getInstance().isShowAnimation)
            overridePendingTransition(R.anim.top_in, R.anim.top_silent);
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.act_multi_call_layout);
        WKRTCCommonUtils.setStatusBarColor(window, ContextCompat.getColor(this, R.color.color232323), 0);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        roomID = getIntent().getStringExtra("roomID");
        token = getIntent().getStringExtra("token");
        loginUID = getIntent().getStringExtra("loginUID");
        channelID = getIntent().getStringExtra("channelID");
        channelType = getIntent().getByteExtra("channelType", (byte) 2);
        list = new ArrayList<>();
        // 选择的成员
        if (getIntent().hasExtra("uids")) {
            List<String> uidList = getIntent().getStringArrayListExtra("uids");
            if (WKRTCCommonUtils.isNotEmpty(uidList)) {
                for (int i = 0; i < uidList.size(); i++) {
                    list.add(new ItemRemoteStream(uidList.get(i), null, null));
                    startCountDownTimer(uidList.get(i));
                }
            }
        }
        initView();
        initListener();
        addView();
        initConferenceClient();
        joinRoom();
    }

    private void initView() {

        bigSurfaceView = findViewById(R.id.bigSurfaceView);
        bigSurfaceView.init(WKRTCApplication.getInstance().getRootEglBase().getEglBaseContext(), null);
        bigSurfaceView.setEnableHardwareScaler(true);
        bigSurfaceView.setZOrderMediaOverlay(false);

        Resources resources = getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();

        int mScreenWidth = dm.widthPixels;
        bigSurfaceView.getLayoutParams().height = bigSurfaceView.getLayoutParams().width = mScreenWidth;

        switchCameraView = findViewById(R.id.switchCameraView);
        timeTv = findViewById(R.id.timeTv);

        microphoneIv = findViewById(R.id.microphoneIv);
        microphoneView = findViewById(R.id.microphoneView);
        speakerIv = findViewById(R.id.speakerIv);
        speakerView = findViewById(R.id.speakerView);
        cameraIv = findViewById(R.id.cameraIv);
        cameraView = findViewById(R.id.cameraView);
        hangupIv = findViewById(R.id.hangUpIv);
        allView = findViewById(R.id.allView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ViewCompat.setTransitionName(hangupIv, "hangup");
        }
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
                if (channelID.equals(MultiCallActivity.this.channelID) && channelType == MultiCallActivity.this.channelType && !TextUtils.isEmpty(uid)) {
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
                // TODO: 2022/11/2
                removeUser(uid);
            }
        });
        // 计时监听
        WKRTCManager.getInstance().addTimerListener("multiCall", (time, timeText) -> runOnUiThread(() -> timeTv.setText(timeText)));
        findViewById(R.id.switchCameraIv).setOnClickListener(v -> capturer.switchCamera());
        findViewById(R.id.minimizeIv).setOnClickListener(v -> showFloatingView(null, false));
        hangupIv.setOnClickListener(v -> {
            if (BtnClickUtils.getInstance().isCanClick(R.id.hangUpIv)) {
                hangupIv.setEnabled(false);
                hangUp();
            }
        });
        cameraView.setOnClickListener(v -> {
            if (localStream == null) return;
            isOpenCamera = !isOpenCamera;
            updateCameraStatus();
            bigSurfaceView.setVisibility(View.GONE);
            if (isOpenCamera) {
                localStream.enableVideo();
                publication.unmute(MediaConstraints.TrackKind.VIDEO, null);
            } else {
                localStream.disableVideo();
                publication.mute(MediaConstraints.TrackKind.VIDEO, null);
            }
            switchCameraView.setVisibility(isOpenCamera ? View.VISIBLE : View.GONE);
        });
        speakerView.setOnClickListener(v -> {
            isOpenSpeaker = !isOpenSpeaker;
            updateSpeakerStatus();
        });
        microphoneView.setOnClickListener(v -> {
            if (localStream == null) return;
            isOpenMicrophone = !isOpenMicrophone;
            updateMicrophoneStatus();
            if (isOpenMicrophone) {
                localStream.enableAudio();
                publication.unmute(MediaConstraints.TrackKind.AUDIO, null);
            } else {
                localStream.disableAudio();
                publication.mute(MediaConstraints.TrackKind.AUDIO, null);
            }
        });
        findViewById(R.id.addIv).setOnClickListener(v -> {
            if (WKRTCManager.getInstance().getIChooseMembers() != null) {
                List<String> selectedUIDs = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    if (!TextUtils.isEmpty(list.get(i).uid)) {
                        selectedUIDs.add(list.get(i).uid);
                    }
                }
                WKRTCManager.getInstance().getIChooseMembers().chooseMembers(MultiCallActivity.this, roomID, channelID, channelType, selectedUIDs, UIDs -> {
                    // TODO: 5/7/21
                    if (UIDs == null || UIDs.size() == 0) return;
                    for (int i = 0; i < UIDs.size(); i++) {
                        boolean isAdd = true;
                        for (int j = 0; j < list.size(); j++) {
                            if (!TextUtils.isEmpty(list.get(j).uid) && !TextUtils.isEmpty(UIDs.get(i))
                                    && list.get(j).uid.equals(UIDs.get(i))) {
                                isAdd = false;
                                break;
                            }
                        }
                        if (isAdd) {
                            list.add(new ItemRemoteStream(UIDs.get(i), null, null));
                            allView.addView(getSurfaceView(UIDs.get(i), null));
                            startCountDownTimer(UIDs.get(i));
                        }
                    }
                });
            }
        });
    }

    private void initConferenceClient() {
//        HttpUtils.setUpINSECURESSLContext();
//        ConferenceClientConfiguration configuration
//                = ConferenceClientConfiguration.builder()
//                .setHostnameVerifier(HttpUtils.hostnameVerifier)
//                .setSSLContext(HttpUtils.sslContext)
//                .build();
        //turn:example.com?transport=tcp
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

    private void joinRoom() {
        executor.execute(() -> conferenceClient.join(token, new ActionCallback<ConferenceInfo>() {
            @Override
            public void onSuccess(ConferenceInfo conferenceInfo) {
                MultiCallActivity.this.conferenceInfo = conferenceInfo;
                requestPermission();
            }

            @Override
            public void onFailure(OwtError e) {
                WKLogger.e(tag, "加入房间错误" + e.errorMessage);
            }
        }));

    }

    private void requestPermission() {
        String[] permissions = new String[]{Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO};

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(MultiCallActivity.this,
                    permission) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MultiCallActivity.this,
                        permissions,
                        100);
                return;
            }
        }

        onConnectSucceed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100
                && grantResults.length == 2
                && grantResults[0] == PERMISSION_GRANTED
                && grantResults[1] == PERMISSION_GRANTED) {
            onConnectSucceed();
        }
    }


    private void hangUp() {
        executor.execute(() -> conferenceClient.leave());
        unPublish();
        WKRTCManager.getInstance().isCalling = false;
        WKRTCManager.getInstance().getSendMsgListener().sendMultiHangup(roomID);
    }

    private void onConnectSucceed() {

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

            } else addView();
        });
        //推流
        publish();
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

    private void publish() {
        executor.execute(() -> {
            boolean vga = true;
//            boolean vga = settingsFragment == null || settingsFragment.resolutionVGA;
            if (capturer == null) {
                capturer = OwtVideoCapturer.create(160, 120, 30, true, true);
                localStream = new LocalStream(capturer, new MediaConstraints.AudioTrackConstraints());
            }
            // 默认关闭视频
//            localStream.enableVideo();
            localStream.disableVideo();
            PublishOptions options;
//            VideoEncodingParameters h264 = new VideoEncodingParameters(H264);
//            VideoEncodingParameters vp8 = new VideoEncodingParameters(VP8);
            VideoEncodingParameters vp9 = new VideoEncodingParameters(VP9);
            options = PublishOptions.builder()
//                    .addVideoParameter(vp8)
//                    .addVideoParameter(h264)
//                    .addVideoParameter(vp9)
                    .build();
            ActionCallback<Publication> callback = new ActionCallback<Publication>() {
                @Override
                public void onSuccess(final Publication result) {
                    publication = result;
//                    try {
//                        JSONArray mixBody = new JSONArray();
//                        JSONObject body = new JSONObject();
//                        body.put("op", "add");
//                        body.put("path", "/info/inViews");
//                        body.put("value", "common");
//                        mixBody.put(body);
//
////                        String serverUrl = "https://49.235.106.135:3004";
//                        String uri = LiMRTCApplication.getInstance().serverUrl
//                                + "/rooms/" + conferenceInfo.id()
//                                + "/streams/" + result.id();
//                        HttpUtils.request(uri, "PATCH", mixBody.toString(), true);
//
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
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

    private void addRemoteStream(RemoteStream remoteStream, Participant participant) {
        String uid = "";
        HashMap<String, String> hashMap = remoteStream.getAttributes();
        if (hashMap != null && hashMap.containsKey("from")) {
            uid = hashMap.get("from");
        }
//        int width = remoteStream.publicationSettings.videoPublicationSettings.resolutionWidth;
//        int height = remoteStream.publicationSettings.videoPublicationSettings.resolutionHeight;
//
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
                    runOnUiThread(() -> {
                        for (int i = 0, size = list.size(); i < size; i++) {
                            if (!TextUtils.isEmpty(list.get(i).uid) && !TextUtils.isEmpty(finalUid) && list.get(i).uid.equals(finalUid)) {
                                list.remove(i);
                                allView.removeViewAt(i);
                                break;
                            }
                        }
                        if (list.size() <= 1) {
                            hangUp();
                        }
                    });

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
                            runOnUiThread(() -> WKRTCManager.getInstance().startTimer());
//                            if (localStream != null && isOpenCamera) {
//                                JSONObject jsonObject = new JSONObject();
//                                try {
//                                    jsonObject.put("cmd", "enableVideo");
//                                } catch (JSONException e) {
//                                    e.printStackTrace();
//                                }
//                                conferenceClient.send(jsonObject.toString(), new ActionCallback<Void>() {
//                                    @Override
//                                    public void onSuccess(Void result) {
//
//                                    }
//
//                                    @Override
//                                    public void onFailure(OwtError error) {
//
//                                    }
//                                });
//                            }
                        }
                    }
                    if (!TextUtils.isEmpty(finalUid) && timerMap != null && timerMap.size() > 0) {
                        CountDownTimer countDownTimer = timerMap.get(finalUid);
                        if (countDownTimer != null) countDownTimer.cancel();
                        timerMap.remove(finalUid);
                    }
                    result.getStats(new ActionCallback<RTCStatsReport>() {
                        @Override
                        public void onSuccess(RTCStatsReport result) {
                            if (rtcStatusMap == null) rtcStatusMap = new ConcurrentHashMap<>();
                            if (!TextUtils.isEmpty(finalUid)) {
                                rtcStatusMap.put(finalUid, new WKRTCStatusReport(result, 0f));
                            }
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
                            runOnUiThread(() -> {
                                if (!TextUtils.isEmpty(finalUid)) {
                                    for (int i = 0; i < list.size(); i++) {
                                        if (list.get(i).uid.equals(finalUid)) {
                                            //关闭摄像头
                                            if (trackKind == MediaConstraints.TrackKind.VIDEO) {
                                                AppCompatImageView imageView = allView.getChildAt(i).findViewById(R.id.avatarIv);
                                                imageView.setVisibility(View.VISIBLE);
                                                WKRTCManager.getInstance().showAvatar(MultiCallActivity.this, finalUid, imageView, imageView.getWidth(), false);
                                                allView.getChildAt(i).findViewById(R.id.surfaceView).setVisibility(View.GONE);
                                            } else if (trackKind == MediaConstraints.TrackKind.AUDIO) {
                                                if (finalUid.equals(loginUID)) {
                                                    allView.getChildAt(i).findViewById(R.id.closeMicrophoneIv).setVisibility(View.VISIBLE);
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }

                            });

                        }

                        @Override
                        public void onUnmute(MediaConstraints.TrackKind trackKind) {
                            runOnUiThread(() -> {
                                if (!TextUtils.isEmpty(finalUid)) {
                                    for (int i = 0; i < list.size(); i++) {
                                        if (list.get(i).uid.equals(finalUid)) {
                                            //开启摄像头
                                            if (trackKind == MediaConstraints.TrackKind.VIDEO) {
                                                allView.getChildAt(i).findViewById(R.id.avatarIv).setVisibility(View.GONE);
                                                allView.getChildAt(i).findViewById(R.id.surfaceView).setVisibility(View.VISIBLE);
                                            } else if (trackKind == MediaConstraints.TrackKind.AUDIO) {
                                                //开启声音
                                                if (finalUid.equals(loginUID)) {
                                                    //本人
                                                    allView.getChildAt(i).findViewById(R.id.closeMicrophoneIv).setVisibility(View.GONE);
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }

                            });

                        }
                    });

                    runOnUiThread(() -> {
                        boolean isAdd = true;
                        for (int j = 0; j < list.size(); j++) {
                            if (!TextUtils.isEmpty(list.get(j).uid)
                                    && !TextUtils.isEmpty(finalUid) && list.get(j).uid.equals(finalUid)) {
                                boolean isAttach = list.get(j).remoteStream == null;
                                list.get(j).remoteStream = remoteStream;
                                if (list.get(j).participant == null)
                                    list.get(j).participant = participant;
                                if (isAttach) {
                                    Log.e("绑定了试图", "-->");
                                    SurfaceViewRenderer surfaceViewRenderer = allView.getChildAt(j).findViewById(R.id.surfaceView);
                                    list.get(j).remoteStream.attach(surfaceViewRenderer);
                                }
                                allView.getChildAt(j).findViewById(R.id.loadingView).setVisibility(View.GONE);
                                isAdd = false;
                                break;
                            }
                        }
                        if (isAdd) {
                            list.add(new ItemRemoteStream(finalUid, remoteStream, participant));
                            allView.addView(getSurfaceView(finalUid, remoteStream));
                        }
                    });

                }

                @Override
                public void onFailure(OwtError error) {
                    Log.e("订阅失败", error.errorMessage);
                }
            });
        });
    }

    @Override
    public void onStreamAdded(RemoteStream remoteStream) {
        addRemoteStream(remoteStream, null);
        //   startTimer();
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
        if (isAdd)
            list.add(new ItemRemoteStream(uid, null, participant));
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
                    runOnUiThread(() -> {
                        for (int i = 0, size = list.size(); i < size; i++) {
                            if (!TextUtils.isEmpty(uid) && !TextUtils.isEmpty(list.get(i).uid) && list.get(i).uid.equals(uid)) {
                                list.remove(i);
                                allView.removeViewAt(i);
                                if (list.size() == 1) {
                                    hangUp();
                                }
                                break;
                            }
                        }
                    });

                } else if (cmd.equals("enableVideo")) {
                    // 开启视频
//                    if (list != null && list.size() > 0) {
//                        runOnUiThread(() -> {
//                            for (int i = 0, size = list.size(); i < size; i++) {
//                                if (list.get(i).participant != null && list.get(i).participant.id.equals(from)) {
//                                    list.get(i).remoteStream.enableVideo();
//                                    allView.getChildAt(i).findViewById(R.id.avatarIv).setVisibility(View.GONE);
//                                    // allView.getChildAt(i).findViewById(R.id.surfaceView).setVisibility(View.VISIBLE);
//
//                                    SurfaceViewRenderer surfaceViewRenderer = surfaceViewMap.get(list.get(i).uid);
//                                    if (surfaceViewRenderer != null)
//                                        surfaceViewRenderer.setVisibility(View.VISIBLE);
//                                    break;
//                                }
//                            }
//                        });
//
//                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onServerDisconnected() {
        WKRTCManager.getInstance().stopTimer();
        RTCAudioPlayer.getInstance().play(MultiCallActivity.this, "lim_rtc_hangup.wav", false);
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
                    list.get(i).remoteStream.enableAudio();
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
        conferenceInfo = null;
        conferenceClient = null;
        runOnUiThread(this::finish);
    }

    private void addView() {
        runOnUiThread(() -> {
//            if (surfaceViewMap == null) {
//                surfaceViewMap = new ConcurrentHashMap<>();
//            }

            allView.removeAllViews();
            for (int i = 0; i < list.size(); i++) {
                allView.addView(getSurfaceView(list.get(i).uid, list.get(i).remoteStream));
            }
        });
    }

    private View getSurfaceView(String uid, RemoteStream remoteStream) {
        View view = LayoutInflater.from(MultiCallActivity.this).inflate(R.layout.item_surfaceview, null);
        View loadingView = view.findViewById(R.id.loadingView);
        AppCompatImageView avatarIv = view.findViewById(R.id.avatarIv);
        SurfaceViewRenderer surfaceView = view.findViewById(R.id.surfaceView);
        surfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        surfaceView.setEnableHardwareScaler(true);
        surfaceView.init(WKRTCApplication.getInstance().getRootEglBase().getEglBaseContext(), null);

//        surfaceViewMap.put(uid, surfaceView);
        if (remoteStream == null) {
            loadingView.setVisibility(View.VISIBLE);
        } else {
            remoteStream.attach(surfaceView);
            loadingView.setVisibility(View.GONE);
        }
        avatarIv.setVisibility(View.VISIBLE);
        surfaceView.setVisibility(View.GONE);
        WKRTCManager.getInstance().showAvatar(MultiCallActivity.this, uid, avatarIv,avatarIv.getWidth(),false);
        // 点击切换到大视图
        surfaceView.setOnClickListener(v -> {
            for (int i = 0; i < list.size(); i++) {
                if (!TextUtils.isEmpty(uid) && list.get(i).uid.equals(uid)) {
                    if (list.get(i).remoteStream != null) {
                        list.get(i).remoteStream.attach(bigSurfaceView);
                    }
                }
            }
            bigSurfaceView.setVisibility(View.VISIBLE);
            bigSurfaceView.setOnClickListener(v1 -> {
                bigSurfaceView.setVisibility(View.GONE);
            });
        });
        return view;
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.top_silent, R.anim.top_out);
    }


    private void checkRTCStatusReport() {
        if (rtcStatusMap != null && rtcStatusMap.size() > 0) {
            for (Map.Entry<String, WKRTCStatusReport> rtcStatsEntry : rtcStatusMap.entrySet()) {
                Map<String, RTCStats> statsMap = rtcStatsEntry.getValue().rtcStatsReport.getStatsMap();
                for (Map.Entry<String, RTCStats> entry : statsMap.entrySet()) {
                    String mapKey = entry.getKey();
                    RTCStats mapValue = entry.getValue();
                    if (mapValue.getMembers() != null && mapValue.getMembers().containsKey("totalAudioEnergy")) {
                        Object object = mapValue.getMembers().get("totalAudioEnergy");

                        if (object != null) {
                            double totalAudioEnergy = (double) object;
                            boolean isShowVoice = false;
                            if (totalAudioEnergy - rtcStatsEntry.getValue().lastTotalAudioEnergy >= 0.0005) {
                                isShowVoice = true;
                            }
                            if (list != null && list.size() > 0) {
                                for (int i = 0, size = list.size(); i < size; i++) {
                                    if (TextUtils.isEmpty(list.get(i).uid) && list.get(i).uid.equals(rtcStatsEntry.getKey())) {
                                        allView.getChildAt(i).findViewById(R.id.speakerIv).setVisibility(isShowVoice ? View.VISIBLE : View.GONE);
                                        break;
                                    }
                                }
                            }
                            rtcStatsEntry.getValue().lastTotalAudioEnergy = totalAudioEnergy;
                            rtcStatusMap.put(mapKey, rtcStatsEntry.getValue());
                        }
                        break;
                    }
                }
            }

        }
    }

    // 邀请用户超过30s未进入通话就挂断移除对应成员
    private void startCountDownTimer(String uid) {
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
                    allView.removeViewAt(i);
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

    private void updateCameraStatus() {
        cameraView.setBackgroundResource(isOpenCamera ? R.drawable.view_bg_e9e9e9 : R.drawable.view_bg_1a1a1a);
        cameraIv.setImageResource(isOpenCamera ? R.mipmap.icon_camera : R.mipmap.icon_close_camera);
    }

    public void updateSpeakerStatus() {
        speakerView.setBackgroundResource(isOpenSpeaker ? R.drawable.view_bg_e9e9e9 : R.drawable.view_bg_1a1a1a);
        speakerIv.setImageResource(isOpenSpeaker ? R.mipmap.icon_speaker : R.mipmap.icon_close_speaker);
        if (isOpenSpeaker) {
            openSpeaker();
        } else closeSpeaker();
    }


    public void updateMicrophoneStatus() {
        microphoneView.setBackgroundResource(isOpenMicrophone ? R.drawable.view_bg_e9e9e9 : R.drawable.view_bg_1a1a1a);
        microphoneIv.setImageResource(isOpenMicrophone ? R.mipmap.icon_microphone : R.mipmap.icon_close_microphone);
    }

    @Override
    protected void onPause() {
        super.onPause();
        WKRTCManager.getInstance().removeTimeListener("multiCall");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK)
            showFloatingView(null, false);
        return true;
    }
}
