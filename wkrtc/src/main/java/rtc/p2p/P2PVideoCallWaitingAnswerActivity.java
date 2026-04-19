package rtc.p2p;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import com.chat.rtc.R;

import rtc.WKRTCApplication;
import rtc.WKRTCCallType;
import rtc.inters.ILocalListener;
import rtc.utils.P2PDataProvider;
import rtc.utils.WKRTCManager;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import owt.base.LocalStream;
import owt.base.MediaConstraints;
import owt.utils.OwtVideoCapturer;
import rtc.utils.WKRTCCommonUtils;
import rtc.utils.RTCAudioPlayer;
import rtc.utils.WKLogger;

/**
 * 5/10/21 5:12 PM
 * 呼叫中
 */
public class P2PVideoCallWaitingAnswerActivity extends AppCompatActivity {
    private final String tag = "P2PVideoCallWaitingAnswerActivity";
    private AppCompatImageView avatarIv;
    private TextView nameTv;
    private String fromUID;
    private String fromName;
    private String loginUID;
    private SurfaceViewRenderer fullRenderer;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
//    private OwtVideoCapturer capturer;
//    private LocalStream localStream;
    private View hangUpIv;
    private CountDownTimer downTimer;
    private boolean isAccept = false;
    private Vibrator vibrator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        overridePendingTransition(R.anim.top_in, R.anim.top_silent);
        super.onCreate(savedInstanceState);
        WKRTCManager.getInstance().isCalling = true;
        Window window = getWindow();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.act_p2p_call_waiting_answer_layout);
        WKRTCCommonUtils.setStatusBarColor(window, ContextCompat.getColor(this, R.color.color232323), 0);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        fromUID = getIntent().getStringExtra("toUID");
        fromName = getIntent().getStringExtra("toName");
        loginUID = getIntent().getStringExtra("loginUID");
        initView();
        initListener();
        startCountDownTimer();
    }

    private void initView() {
        if (vibrator.hasVibrator()) {
            long[] pattern = {0, 500, 1000};
            vibrator.vibrate(pattern, 0);
        }
        RTCAudioPlayer.getInstance().play(this, "lim_rtc_receive.mp3", true);
        hangUpIv = findViewById(R.id.hangUpIv);
        fullRenderer = findViewById(R.id.fullRenderer);
        fullRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        fullRenderer.setEnableHardwareScaler(true);
        fullRenderer.init(WKRTCApplication.getInstance().getRootEglBase().getEglBaseContext(), null);
        nameTv = findViewById(R.id.nameTv);
        avatarIv = findViewById(R.id.avatarIv);
        nameTv.setText(fromName);
        WKRTCManager.getInstance().showAvatar(this, fromUID, avatarIv,80,true);
        //渲染本地流信息
        P2PDataProvider.getInstance().init(loginUID,fromUID,fromName,WKRTCCallType.video,true,true,fullRenderer);
//        executor.execute(() -> {
//            if (capturer == null) {
//                capturer = OwtVideoCapturer.create(1280, 720, 30, true, true);
//                localStream = new LocalStream(capturer,
//                        new MediaConstraints.AudioTrackConstraints());
//            }
//            localStream.attach(fullRenderer);
//        });
    }

    private void initListener() {
        P2PDataProvider.getInstance().addP2PListener(new P2PDataProvider.IP2PListener() {
            @Override
            public void hangup() {

            }

            @Override
            public void onStreamAdded() {

            }

            @Override
            public void onSwitchVideoRespond(String uid, int status) {

            }

            @Override
            public void switchType(int callType) {

            }

            @Override
            public void onShowDialog() {

            }

            @Override
            public void onConnection(int status) {

            }
        });
        hangUpIv.setOnClickListener(v -> {
            if (vibrator != null) {
                vibrator.cancel();
            }
            RTCAudioPlayer.getInstance().stopPlay();
            new Handler(Objects.requireNonNull(Looper.myLooper())).postDelayed(() -> {
                // WKRTCManager.getInstance().getSaveMsgListener().onRefuse(false, fromUID, LiMRTCCallType.video);
                WKRTCManager.getInstance().getSendMsgListener().sendRefuse(fromUID, (byte) 1, WKRTCCallType.video);
                RTCAudioPlayer.getInstance().play(P2PVideoCallWaitingAnswerActivity.this, "lim_rtc_hangup.wav", false);
                finish();
            }, 500);
            WKRTCManager.getInstance().isCalling = false;
            P2PDataProvider.getInstance().hangup();
        });
        findViewById(R.id.answerAudioIv).setOnClickListener(v -> {
            //接听语音
            gotoCall(WKRTCCallType.audio);
        });
        findViewById(R.id.answerIv).setOnClickListener(v -> {
            //接听视频
            gotoCall(WKRTCCallType.video);
        });
        WKRTCManager.getInstance().addLocalListener(new ILocalListener() {
            @Override
            public void onReceivedRTCMsg(String uid, String message) {

            }

            @Override
            public void onHangUp(String channelID, byte channelType, int second) {

            }

            @Override
            public void onRefuse(String channelID, byte channelType, String uid) {

            }

            @Override
            public void onSwitchVideoRespond(String uid, int status) {

            }

            @Override
            public void onCancel(String uid) {
                // 对方挂断
                stopTimer();
                WKLogger.e(tag, "对方取消呼叫");
                RTCAudioPlayer.getInstance().stopPlay();
                new Handler(Objects.requireNonNull(Looper.myLooper())).postDelayed(() -> {
                    RTCAudioPlayer.getInstance().play(P2PVideoCallWaitingAnswerActivity.this, "lim_rtc_hangup.wav", false);
                    finish();
                }, 500);
                P2PDataProvider.getInstance().hangup();
            }

            @Override
            public void onPublish() {

            }

            @Override
            public void onSwitchAudio(String uid) {

            }

            @Override
            public void onAccept(String uid, int callType) {
                if (!isAccept && uid.equals(loginUID)) {
                    stopTimer();
                    Log.e("在其他设备接听", "--->");
                    RTCAudioPlayer.getInstance().stopPlay();
                    new Handler(Objects.requireNonNull(Looper.myLooper())).postDelayed(() -> {
                        RTCAudioPlayer.getInstance().play(P2PVideoCallWaitingAnswerActivity.this, "lim_rtc_hangup.wav", false);
                        finish();
                    }, 500);
                }
            }

        });
    }

    private void gotoCall(int callType) {
        if (vibrator != null) {
            vibrator.cancel();
        }
        isAccept = true;
        stopTimer();
        WKRTCManager.getInstance().getSendMsgListener().sendAccept(loginUID, fromUID, callType);
        RTCAudioPlayer.getInstance().stopPlay();
//        Intent intent = new Intent(this, P2PCallActivity.class);
        Intent intent = new Intent(this, CallActivity.class);
        intent.putExtra("callType", callType);
        intent.putExtra("loginUID", loginUID);
        intent.putExtra("toUID", fromUID);
        intent.putExtra("toName", fromName);
        intent.putExtra("isCreate", false);
        intent.putExtra("is_connect", true);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            @SuppressWarnings("unchecked") ActivityOptionsCompat activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(P2PVideoCallWaitingAnswerActivity.this,
//                    new Pair<>(hangUpIv, "hangup"), new Pair<>(avatarIv, "avatarIv"), new Pair<>(nameTv, "nameTv"));
//            startActivity(intent, activityOptions.toBundle());
//        } else {
        startActivity(intent);
//        }
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.top_silent, R.anim.top_out);
//        if (localStream != null) {
//            localStream.detach(fullRenderer);
//        }
//        if (capturer != null) {
//            capturer.dispose();
//            capturer = null;
//        }

    }

    private void stopTimer() {
        if (vibrator != null) {
            vibrator.cancel();
        }
        if (downTimer != null) {
            downTimer.cancel();
            downTimer = null;
        }
    }

    private void startCountDownTimer() {
        downTimer = new CountDownTimer(30 * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                //  WKRTCManager.getInstance().getSaveMsgListener().onMissed(false, fromUID, LiMRTCCallType.video);
            }
        };
        downTimer.start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
