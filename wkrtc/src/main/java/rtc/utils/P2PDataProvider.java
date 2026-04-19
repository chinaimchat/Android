package rtc.utils;

import rtc.WKRTCApplication;
import rtc.WKRTCCallType;
import rtc.inters.ILocalListener;
import com.yhao.floatwindow.FloatWindow;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.SurfaceViewRenderer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import owt.base.ActionCallback;
import owt.base.LocalStream;
import owt.base.MediaConstraints;
import owt.base.OwtError;
import owt.p2p.P2PClient;
import owt.p2p.Publication;
import owt.p2p.RemoteStream;
import owt.utils.OwtVideoCapturer;

public class P2PDataProvider implements P2PClient.P2PClientObserver {
    private final String tag = "P2PDataProvider";

    private P2PDataProvider() {
    }

    @Override
    public void onServerDisconnected() {

    }

    @Override
    public void onStreamAdded(RemoteStream remoteStream) {
        WKLogger.e(tag, "收到远程流信息了-->" + remoteStream.audioTrackId());
        this.remoteStream = remoteStream;

        ip2PListener.onStreamAdded();
        remoteStream.addObserver(new owt.base.RemoteStream.StreamObserver() {
            @Override
            public void onEnded() {
                exit();
            }

            @Override
            public void onUpdated() {
            }
        });
    }

    @Override
    public void onDataReceived(String peerId, String message) {

    }

    private static class P2PDataProviderBinder {
        final static P2PDataProvider provider = new P2PDataProvider();
    }

    public static P2PDataProvider getInstance() {
        return P2PDataProviderBinder.provider;
    }

    private IP2PListener ip2PListener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    public LocalStream localStream;
    public RemoteStream remoteStream;
    private P2PClient p2PClient;
    private OwtVideoCapturer capturer;
    //    private Publication publication;
    public String loginUID, toUID, toName;
    public int callType;
    public boolean isCreate;
    private boolean isAccept = false;

    public void addP2PListener(IP2PListener ip2PListener) {
        this.ip2PListener = ip2PListener;

    }

    public void init(String loginUID, String toUID, String toName, int callType, boolean isCreate, boolean isConnect, SurfaceViewRenderer surfaceViewRenderer) {

        this.isCreate = isCreate;
        this.loginUID = loginUID;
        this.toName = toName;
        this.toUID = toUID;
        this.callType = callType;
        initP2PClient();
        executor.execute(() -> {
            if (capturer == null) {
                capturer = OwtVideoCapturer.create(320, 240, 30, true, true);
                localStream = new LocalStream(capturer,
                        new MediaConstraints.AudioTrackConstraints());
            }
            if (callType == WKRTCCallType.audio) {
                localStream.disableVideo();
            }
            localStream.attach(surfaceViewRenderer);
            if (isCreate || callType == WKRTCCallType.video || isConnect)
                connect();
        });
        initLocalListener();
    }

    private void initP2PClient() {
        p2PClient = WKRTCApplication.getInstance().getP2PClient();
        p2PClient.addObserver(this);
    }

    public void connect() {
        ip2PListener.onConnection(0);
        isAccept = true;
        executor.execute(() -> {
            JSONObject loginObj = new JSONObject();
            try {
                loginObj.put("uid", loginUID);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            p2PClient.addAllowedRemotePeer(toUID);

            p2PClient.connect(loginObj.toString(), new ActionCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    WKLogger.e(tag, "rtc连接成功了" + result);
//                    if (!isCreate) {
//                        publish("rtc连接成功");
//                    }
                    ip2PListener.onConnection(1);
                }

                @Override
                public void onFailure(OwtError error) {
                    WKLogger.e(tag,"rtc连接失败了"+error.errorMessage);
                    new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                              WKLogger.e(tag,"重连异常");
                            }
                            connect();
                        }
                    }.start();
                    ip2PListener.onConnection(2);
                }
            });
        });
    }


    // 发布本地流
    public synchronized void publish(String from) {
        if (localStream == null) {
            WKLogger.e(tag,"localStream存在为空的信息");
            return;
        }
//        if (publication != null || localStream == null){
//
//            if (publication != null){
//                Log.e("publication存在不为空的信息","-->");
//            }
//            if (localStream != null){
//                Log.e("localStream存在不为空的信息","-->");
//            }
//            return;
//        }
//          p2PClient.addAllowedRemotePeer(toUID);
        if (localStream.getMediaStream() == null) {
            WKLogger.e(tag,"本地流是空");
        }
        WKLogger.e(tag,"去发布流信息");
        executor.execute(
                () -> p2PClient.publish(toUID, localStream, new ActionCallback<Publication>() {
                    @Override
                    public void onSuccess(Publication result) {
//                        publication = result;
                        WKLogger.e(tag,"去发布流信息成功了");
                    }

                    @Override
                    public void onFailure(OwtError error) {
                        WKLogger.e(tag,"去发布流信息失败了" + error.errorMessage);
                        new Thread() {
                            @Override
                            public void run() {
                                super.run();
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                publish("失败");
                            }
                        }.start();

                    }
                }));

    }

    private void initLocalListener() {
        WKRTCManager.getInstance().addLocalListener(new ILocalListener() {
            @Override
            public void onReceivedRTCMsg(String uid, String message) {
                if (p2PClient != null) {
                    p2PClient.onMessage(uid, message);
                }
            }

            @Override
            public void onHangUp(String channelID, byte channelType, int second) {
                if (channelID.equals(toUID) && channelType == 1) {
                    exit();
                }
            }

            @Override
            public void onRefuse(String channelID, byte channelType, String uid) {
                if (channelType == 1 && channelID.equals(toUID)) {
                    exit();
                }
            }

            @Override
            public void onRequestSwitchVideo(String uid) {
                ip2PListener.onShowDialog();

            }

            @Override
            public void onSwitchVideoRespond(String uid, int status) {
                ip2PListener.onSwitchVideoRespond(uid, status);
                if (status == 1) {
                    callType = WKRTCCallType.video;
                    localStream.enableVideo();

                }
            }

            @Override
            public void onCancel(String uid) {
                if (uid.equals(toUID)) {
                    exit();
                }
            }

            @Override
            public void onPublish() {
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
//                            getStatus();
                        if (isCreate) {
                            publish("事件成功");
                        }
                    }
                }.start();
            }

            @Override
            public void onSwitchAudio(String uid) {
                if (uid.equals(toUID)) {
                    callType = WKRTCCallType.audio;
                    //音频通话
                    ip2PListener.switchType(callType);
                }
            }


            @Override
            public void onAccept(String uid, int callType) {
                if (!isAccept && uid.equals(loginUID)) {
                    exit();
                }
            }
        });

    }

    private void exit() {
        hangup();
        ip2PListener.hangup();
    }

    public void hangup() {
        WKRTCManager.getInstance().isCalling = false;
//        if (publication != null) {
//            publication.stop();
//            publication = null;
//        }
        if (capturer != null) {
            capturer.stopCapture();
            capturer.dispose();
            capturer = null;
        }
        if (localStream != null) {
            localStream.dispose();
            localStream = null;
        }
        if (remoteStream != null) {
            remoteStream.disposed();
            remoteStream = null;
        }
        RTCAudioPlayer.getInstance().stopPlay();
        FloatWindow.destroy();

        WKRTCApplication.getInstance().getRootEglBase().releaseSurface();
        if (p2PClient != null) {
            executor.execute(() -> {
                p2PClient.removeObserver(this);
                p2PClient.onServerDisconnected();
                p2PClient.stop(toUID);
                p2PClient.disconnect();
                p2PClient.closeInternal();
            });
        }

    }

    public void switchCamera() {
        if (remoteStream == null || localStream == null)
            return;
        capturer.switchCamera();
    }

    public interface IP2PListener {

        void hangup();

        void onStreamAdded();

        void onSwitchVideoRespond(String uid, int status);

        void switchType(int callType);

        void onShowDialog();

        void onConnection(int status);
    }
}
