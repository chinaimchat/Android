package rtc;

import static owt.base.MediaCodecs.VideoCodec.H264;
import static owt.base.MediaCodecs.VideoCodec.H265;
import static owt.base.MediaCodecs.VideoCodec.VP8;
import static owt.base.MediaCodecs.VideoCodec.VP9;

import android.content.Context;

import rtc.utils.WKRTCManager;

import org.webrtc.EglBase;
import org.webrtc.Logging;
import org.webrtc.PeerConnection;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import owt.base.ContextInitialization;
import owt.base.VideoEncodingParameters;
import owt.p2p.P2PClient;
import owt.p2p.P2PClientConfiguration;
import owt.p2p.RemoteStream;
import rtc.p2p.SocketSignalingChannel;
import rtc.utils.WKLogger;

/**
 * 4/30/21 2:21 PM
 * rtc
 */
public class WKRTCApplication {
    private WKRTCApplication() {
    }

    private static final class LiMRTCApplicationBinder {
        private static final WKRTCApplication rtc = new WKRTCApplication();
    }

    public static WKRTCApplication getInstance() {
        return LiMRTCApplicationBinder.rtc;
    }

    private WeakReference<Context> context;
    private EglBase rootEglBase;

    //    public String serverUrl = "https://49.235.106.135:3004";
    public String turnIP = "175.27.245.108";
    List<PeerConnection.IceServer> trunList = new ArrayList<>();

    public Context getContext() {
        return context.get();
    }

    public RemoteStream remoteStream;

    public EglBase getRootEglBase() {
        return rootEglBase;
    }

    public void setDebug(boolean isDebug) {
        WKLogger.isDebug = isDebug;
    }

    public void initModule(Context context, List<PeerConnection.IceServer> trunList) {
//        this.serverUrl = url;
//        this.turnIP = turnIP;
        this.trunList = trunList;
        if (trunList == null) {
            this.trunList = new ArrayList<>();
        }
        this.context = new WeakReference<>(context);
        rootEglBase = EglBase.create();
        ContextInitialization.create()
                .setApplicationContext(context)
                .addIgnoreNetworkType(ContextInitialization.NetworkType.LOOPBACK)
                .setVideoHardwareAccelerationOptions(
                        rootEglBase.getEglBaseContext(),
                        rootEglBase.getEglBaseContext())
                .initialize();
        // initP2PClient();
        Logging.enableLogToDebugOutput(Logging.Severity.LS_ERROR);
    }

//    P2PClient p2PClient;

    public P2PClient getP2PClient() {
//        if (p2PClient == null) initP2PClient();
//        return p2PClient;
        return initP2PClient();
    }

    private P2PClient initP2PClient() {

//        PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder(
//                "turn:" + turnIP + ":3478?transport=udp").setUsername("tsdd").setPassword(
//                "tsddpwd").createIceServer();
////        PeerConnection.IceServer iceServer11 = PeerConnection.IceServer.builder(
////                "turn:162.209.218.50:3478?transport=udp").setUsername("user").setPassword(
////                "passwd").createIceServer();
//        PeerConnection.IceServer iceServer0 = PeerConnection.IceServer.builder(
//                "stun:stun.qq.com").createIceServer();
//        PeerConnection.IceServer iceServer1 = PeerConnection.IceServer.builder(
//                "stun:stun1.l.google.com:19302").createIceServer();
//        PeerConnection.IceServer iceServer2 = PeerConnection.IceServer.builder(
//                "stun:stun2.l.google.com:19302").createIceServer();
//        PeerConnection.IceServer iceServer3 = PeerConnection.IceServer.builder(
//                "stun:stunserver.org").createIceServer();
//        PeerConnection.IceServer iceServer4 = PeerConnection.IceServer.builder(
//                "stun:stun.xten.com").createIceServer();
//        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
//        iceServers.add(iceServer);
//        iceServers.add(iceServer0);
//        iceServers.add(iceServer1);
//        iceServers.add(iceServer2);
//        iceServers.add(iceServer3);
//        iceServers.add(iceServer4);
//        iceServers.add(iceServer11);
        PeerConnection.RTCConfiguration rtcConfiguration = new PeerConnection.RTCConfiguration(
                trunList);
        rtcConfiguration.bundlePolicy = PeerConnection.BundlePolicy.MAXCOMPAT;
        rtcConfiguration.useMediaTransport = true;
//        rtcConfiguration.maxIPv6Networks = 2;
        VideoEncodingParameters h264 = new VideoEncodingParameters(H264);
        VideoEncodingParameters h265 = new VideoEncodingParameters(H265);
        VideoEncodingParameters vp8 = new VideoEncodingParameters(VP8);
        VideoEncodingParameters vp9 = new VideoEncodingParameters(VP9);
        P2PClientConfiguration configuration = P2PClientConfiguration.builder()
                .addVideoParameters(h264)
                .addVideoParameters(vp8)
                .addVideoParameters(vp9)
                .addVideoParameters(h265)
                .setRTCConfiguration(rtcConfiguration)
                .build();
        return new P2PClient(configuration, new SocketSignalingChannel(() -> WKRTCManager.getInstance().getIRTCListener().onPublish()));
    }

}
