package rtc.p2p;

import static android.app.NotificationManager.INTERRUPTION_FILTER_ALL;
import static android.app.NotificationManager.INTERRUPTION_FILTER_NONE;
import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.chat.rtc.R;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.util.HashMap;
import java.util.Objects;

import rtc.WKRTCApplication;
import rtc.WKRTCCallType;
import rtc.utils.WKFloatingViewManager;
import rtc.utils.WKRTCCommonUtils;
import rtc.utils.P2PDataProvider;
import rtc.utils.RTCAudioPlayer;
import rtc.utils.WKRTCManager;

public class CallActivity extends AppCompatActivity {
    private final int OVERLAY_PERMISSION_REQUEST_CODE = 1024;
    private AppCompatImageView avatarIv;
    private TextView nameTv, timeTv;
    private TextView waitingAnswerTv;
    private SurfaceViewRenderer smallSurfaceView;
    private SurfaceViewRenderer fullSurfaceView;
    private float dX, dY;
    private View answerLayout, videoLayout, audioLayout, muteLayout, videoAnswerLayout, coverView;
    private AppCompatImageView answerIV, switchCameraIV, hangUpIV, switchAudioIV, switchVideoIV, muteIV, speakerIV, audioAnswerIV, videoAnswerIV;
    private boolean isCreate;

    private int callType;
    private String loginUID;
    private String toUID, toName;
    private AudioManager audioManager;
    private boolean isConnect = false;
    private long totalDuration;
    private boolean isRestart = false;
    private boolean isAttachLocal = true;
    private Vibrator vibrator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        isRestart = getIntent().getBooleanExtra("isRestart", false);
        isCreate = getIntent().getBooleanExtra("isCreate", false);
        if (isCreate && !isRestart) {
            overridePendingTransition(R.anim.top_in, R.anim.top_silent);
        }
        WKRTCManager.getInstance().isCalling = true;
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Window window = getWindow();
        setContentView(R.layout.act_p2p_call_layout);
        WKRTCCommonUtils.setStatusBarColor(window, ContextCompat.getColor(this, R.color.color232323), 0);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        toName = getIntent().getStringExtra("toName");
        toUID = getIntent().getStringExtra("toUID");
        loginUID = getIntent().getStringExtra("loginUID");
        callType = getIntent().getIntExtra("callType", 1);
        initView();

    }

    private void initView() {
        coverView = findViewById(R.id.coverView);
        videoAnswerLayout = findViewById(R.id.videoAnswerLayout);
        videoAnswerIV = findViewById(R.id.videoAnswerIV);
        audioAnswerIV = findViewById(R.id.audioAnswerIV);
        answerLayout = findViewById(R.id.answerLayout);
        muteLayout = findViewById(R.id.muteLayout);
        audioLayout = findViewById(R.id.audioLayout);
        videoLayout = findViewById(R.id.videoLayout);
        answerIV = findViewById(R.id.answerIV);
        speakerIV = findViewById(R.id.speakerIV);
        muteIV = findViewById(R.id.muteIV);
        switchVideoIV = findViewById(R.id.switchVideoIV);
        switchCameraIV = findViewById(R.id.switchCameraIV);
        switchAudioIV = findViewById(R.id.switchAudioIV);
        hangUpIV = findViewById(R.id.hangUpIV);
        waitingAnswerTv = findViewById(R.id.waitingAnswerTv);
        timeTv = findViewById(R.id.timeTv);
        nameTv = findViewById(R.id.nameTv);
        avatarIv = findViewById(R.id.avatarIv);

        smallSurfaceView = findViewById(R.id.smallRenderer);
        smallSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        smallSurfaceView.setEnableHardwareScaler(true);
        smallSurfaceView.setOnTouchListener(touchListener);
        smallSurfaceView.setZOrderMediaOverlay(true);
        smallSurfaceView.init(WKRTCApplication.getInstance().getRootEglBase().getEglBaseContext(), null);


        fullSurfaceView = findViewById(R.id.fullRenderer);
        fullSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        fullSurfaceView.setEnableHardwareScaler(true);
        fullSurfaceView.setZOrderMediaOverlay(false);
        fullSurfaceView.init(WKRTCApplication.getInstance().getRootEglBase().getEglBaseContext(), null);
        nameTv.setText(toName);
        WKRTCManager.getInstance().showAvatar(this, toUID, avatarIv, 80, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ViewCompat.setTransitionName(hangUpIV, "hangup");
            ViewCompat.setTransitionName(nameTv, "nameTv");
            ViewCompat.setTransitionName(avatarIv, "avatarIv");
        }
        isConnect = getIntent().getBooleanExtra("is_connect", false);
        if (callType == WKRTCCallType.audio) {
            // audioManager.setSpeakerphoneOn(false);//是否从扬声器播出
            //音频通话
            fullSurfaceView.setVisibility(View.GONE);
            smallSurfaceView.setVisibility(View.GONE);
            avatarIv.setVisibility(View.VISIBLE);
            nameTv.setVisibility(View.VISIBLE);
            if (!isRestart) {
                if (!isConnect) {
                    RTCAudioPlayer.getInstance().play(this, "lim_rtc_call.mp3", true);
                    if (vibrator.hasVibrator() && !isCreate) {
                        long[] pattern = {0, 500, 1000};
                        vibrator.vibrate(pattern, 0);
                    }
                } else {
                    WKRTCManager.getInstance().getSendMsgListener().sendSwitchAudio(loginUID, toUID);
                }
            }
        } else {
            //  audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.setSpeakerphoneOn(true);
            if (!isRestart) {
                if (!isConnect) {
                    RTCAudioPlayer.getInstance().play(this, "lim_rtc_call.mp3", true);
                }
            }
        }

        if (isCreate) {
            waitingAnswerTv.setVisibility(View.VISIBLE);
            countDownTimer.start();
        } else {
            waitingAnswerTv.setVisibility(View.GONE);
            if (callType == WKRTCCallType.audio) {
                answerLayout.setVisibility(View.VISIBLE);
            } else {
                waitingAnswerTv.setVisibility(View.VISIBLE);
                waitingAnswerTv.setText(R.string.invitation_video_call);
                coverView.setVisibility(View.VISIBLE);
                videoAnswerLayout.setVisibility(View.VISIBLE);
            }
        }

        if (isRestart) {
            if (P2PDataProvider.getInstance().localStream != null) {
                P2PDataProvider.getInstance().localStream.attach(fullSurfaceView);
            }
            if (P2PDataProvider.getInstance().remoteStream != null) {
                if (callType == WKRTCCallType.video) {
                    P2PDataProvider.getInstance().remoteStream.attach(smallSurfaceView);
                }
                answerLayout.setVisibility(View.GONE);

                if (callType == WKRTCCallType.video) {
                    nameTv.setVisibility(View.GONE);
                    avatarIv.setVisibility(View.GONE);
                    audioLayout.setVisibility(View.GONE);
                    videoLayout.setVisibility(View.VISIBLE);
                } else {
                    videoLayout.setVisibility(View.GONE);
                    audioLayout.setVisibility(View.VISIBLE);
                    nameTv.setVisibility(View.VISIBLE);
                    avatarIv.setVisibility(View.VISIBLE);
                }
            }
            initListener();
        } else {
            requestPermission();
        }
    }

    private void initListener() {
        audioAnswerIV.setOnClickListener(view -> {
            // 语音接听
            if (vibrator != null) {
                vibrator.cancel();
            }
            RTCAudioPlayer.getInstance().stopPlay();
            videoAnswerLayout.setVisibility(View.GONE);
            coverView.setVisibility(View.GONE);
            WKRTCManager.getInstance().getSendMsgListener().sendAccept(loginUID, toUID, WKRTCCallType.audio);
            P2PDataProvider.getInstance().publish("语音接听");
//            P2PDataProvider.getInstance().localStream.disableVideo();

            callType = WKRTCCallType.audio;
            P2PDataProvider.getInstance().callType = WKRTCCallType.audio;
            fullSurfaceView.setVisibility(View.GONE);
            smallSurfaceView.setVisibility(View.GONE);
            audioLayout.setVisibility(View.VISIBLE);
            videoLayout.setVisibility(View.GONE);
            nameTv.setVisibility(View.VISIBLE);
            avatarIv.setVisibility(View.VISIBLE);
            WKRTCManager.getInstance().getSendMsgListener().sendSwitchAudio(loginUID, toUID);
        });
        videoAnswerIV.setOnClickListener(view -> {
            if (vibrator != null) {
                vibrator.cancel();
            }
            RTCAudioPlayer.getInstance().stopPlay();
            videoAnswerLayout.setVisibility(View.GONE);
            coverView.setVisibility(View.GONE);
            WKRTCManager.getInstance().getSendMsgListener().sendAccept(loginUID, toUID, WKRTCCallType.video);
            P2PDataProvider.getInstance().publish("视频接听");
        });
        answerIV.setOnClickListener(v -> {
            // 接听
            if (vibrator != null) {
                vibrator.cancel();
            }
            RTCAudioPlayer.getInstance().stopPlay();
            answerLayout.setVisibility(View.GONE);
            P2PDataProvider.getInstance().connect();
            WKRTCManager.getInstance().getSendMsgListener().sendAccept(loginUID, toUID, WKRTCCallType.audio);
            P2PDataProvider.getInstance().publish("语音通话");
        });
        switchCameraIV.setOnClickListener(v -> {
            P2PDataProvider.getInstance().switchCamera();
        });
        switchAudioIV.setOnClickListener(v -> {
            if (P2PDataProvider.getInstance().remoteStream == null || P2PDataProvider.getInstance().localStream == null)
                return;
            callType = WKRTCCallType.audio;
            P2PDataProvider.getInstance().callType = WKRTCCallType.audio;
            fullSurfaceView.setVisibility(View.GONE);
            smallSurfaceView.setVisibility(View.GONE);
            audioLayout.setVisibility(View.VISIBLE);
            videoLayout.setVisibility(View.GONE);
            nameTv.setVisibility(View.VISIBLE);
            avatarIv.setVisibility(View.VISIBLE);
            WKRTCManager.getInstance().getSendMsgListener().sendSwitchAudio(loginUID, toUID);
        });
        switchVideoIV.setOnClickListener(view -> {
            if (P2PDataProvider.getInstance().remoteStream == null || P2PDataProvider.getInstance().localStream == null)
                return;
            WKRTCManager.getInstance().getSendMsgListener().sendSwitchVideo(loginUID, toUID);
            Toast.makeText(this, getString(R.string.request_video_send), Toast.LENGTH_SHORT).show();
        });
        // 静音
        muteIV.setOnClickListener(v -> {
            if (P2PDataProvider.getInstance().remoteStream == null || P2PDataProvider.getInstance().localStream == null)
                return;
            HashMap<String, String> hashMap = P2PDataProvider.getInstance().localStream.getAttributes();
            if (hashMap == null) hashMap = new HashMap<>();
            String rtc_mute = "open";
            if (hashMap.containsKey("rtc_mute")) {
                rtc_mute = hashMap.get("rtc_mute");
            }

            if (!TextUtils.isEmpty(rtc_mute) && TextUtils.equals(rtc_mute, "open")) {
                P2PDataProvider.getInstance().localStream.disableAudio();
                muteIV.setImageResource(R.mipmap.ic_mute_hover);
                hashMap.put("rtc_mute", "close");
            } else {
                P2PDataProvider.getInstance().localStream.enableAudio();
                muteIV.setImageResource(R.mipmap.ic_mute);
                hashMap.put("rtc_mute", "open");
            }
            P2PDataProvider.getInstance().localStream.setAttributes(hashMap);
        });
        // 免提
        speakerIV.setOnClickListener(v -> {
            if (P2PDataProvider.getInstance().remoteStream == null || P2PDataProvider.getInstance().localStream == null)
                return;
            //    audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.setSpeakerphoneOn(!audioManager.isSpeakerphoneOn());
            if (audioManager.isSpeakerphoneOn()) {
                speakerIV.setImageResource(R.mipmap.ic_handfree_hover);
            } else speakerIV.setImageResource(R.mipmap.ic_handfree);
        });
        // 小窗口
        findViewById(R.id.minimizeIv).setOnClickListener(v -> {
            if (vibrator != null) {
                vibrator.cancel();
            }
            showFloatingView();
        });
        smallSurfaceView.setOnClickListener(v -> {
            if (P2PDataProvider.getInstance().remoteStream == null || P2PDataProvider.getInstance().localStream == null)
                return;
            if (isAttachLocal) {
                P2PDataProvider.getInstance().localStream.detach(fullSurfaceView);
                P2PDataProvider.getInstance().remoteStream.detach(smallSurfaceView);

                P2PDataProvider.getInstance().localStream.attach(smallSurfaceView);
                P2PDataProvider.getInstance().remoteStream.attach(fullSurfaceView);
            } else {
                P2PDataProvider.getInstance().localStream.detach(smallSurfaceView);
                P2PDataProvider.getInstance().remoteStream.detach(fullSurfaceView);

                P2PDataProvider.getInstance().localStream.attach(fullSurfaceView);
                P2PDataProvider.getInstance().remoteStream.attach(smallSurfaceView);
            }
            isAttachLocal = !isAttachLocal;
        });
        hangUpIV.setOnClickListener(v -> {
            activeHangup();
        });
        WKRTCManager.getInstance().addTimerListener("p2pCall", (time, timeText) -> {
            runOnUiThread(() -> {
                totalDuration = time;
                timeTv.setText(timeText);
            });
        });
        P2PDataProvider.getInstance().addP2PListener(new P2PDataProvider.IP2PListener() {
            @Override
            public void hangup() {
                CallActivity.this.hangup();
            }

            @Override
            public void onStreamAdded() {
                RTCAudioPlayer.getInstance().stopPlay();
                runOnUiThread(() -> {
                    if (smallSurfaceView != null && callType == WKRTCCallType.video) {
                        P2PDataProvider.getInstance().remoteStream.attach(smallSurfaceView);
                        switchType(WKRTCCallType.video);
                    }

                    waitingAnswerTv.setVisibility(View.GONE);
                    answerLayout.setVisibility(View.GONE);
                    muteLayout.setVisibility(View.VISIBLE);
                    if (callType == WKRTCCallType.video) {
                        nameTv.setVisibility(View.GONE);
                        avatarIv.setVisibility(View.GONE);
                        audioLayout.setVisibility(View.GONE);
                        videoLayout.setVisibility(View.VISIBLE);
                        audioManager.setSpeakerphoneOn(true);
                    } else {
                        audioManager.setSpeakerphoneOn(false);
                        videoLayout.setVisibility(View.GONE);
                        audioLayout.setVisibility(View.VISIBLE);
                        nameTv.setVisibility(View.VISIBLE);
                        avatarIv.setVisibility(View.VISIBLE);
                    }
                    WKRTCManager.getInstance().startTimer();
                });
            }

            @Override
            public void onShowDialog() {
                if (!CallActivity.this.isFinishing() && !CallActivity.this.isDestroyed()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(CallActivity.this);
                    builder.setMessage(getString(R.string.request_video));
                    builder.setPositiveButton(getString(R.string.agree), (dialog, which) -> {
                        dialog.dismiss();
                        runOnUiThread(() -> {
                            P2PDataProvider.getInstance().remoteStream.attach(smallSurfaceView);
                            P2PDataProvider.getInstance().localStream.enableVideo();
                            switchType(WKRTCCallType.video);
                        });
                        WKRTCManager.getInstance().getSendMsgListener().sendSwitchVideoRespond(loginUID, toUID, 1);
                    });
                    builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                        dialog.dismiss();
                        WKRTCManager.getInstance().getSendMsgListener().sendSwitchVideoRespond(loginUID, toUID, 0);
                    });
                    builder.show();
                }
            }

            @Override
            public void onConnection(int status) {
                runOnUiThread(() -> {
                    if (status == 0) {
                        waitingAnswerTv.setText(R.string.connecting);
                    } else if (status == 1) {
                        if (isCreate)
                            waitingAnswerTv.setText(R.string.waiting_answer_other);
                        else {
                            if (callType == WKRTCCallType.video) {
                                waitingAnswerTv.setText(R.string.invitation_video_call);
                            }
                        }
                    } else {
                        waitingAnswerTv.setText(R.string.connection_err);
                    }
                });
            }

            @Override
            public void onSwitchVideoRespond(String uid, int status) {
                if (status == 0)
                    runOnUiThread(() -> Toast.makeText(CallActivity.this, getString(R.string.other_refuse_switch_video), Toast.LENGTH_SHORT).show());
                else {
                    runOnUiThread(() -> {
                        P2PDataProvider.getInstance().remoteStream.attach(smallSurfaceView);
                        CallActivity.this.switchType(WKRTCCallType.video);
                    });
                }
            }

            @Override
            public void switchType(int callType) {
                runOnUiThread(() -> CallActivity.this.switchType(callType));
            }


        });
    }


    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }


    private CountDownTimer countDownTimer = new CountDownTimer(1000 * 30, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            if (P2PDataProvider.getInstance().remoteStream == null) {
                WKRTCManager.getInstance().getSendMsgListener().sendCancel(toUID, callType);
                hangup();
            }
        }
    };


    boolean isClick = false;
    long startTime;
    private final View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (v.getId() == R.id.smallRenderer) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isClick = false;
                        startTime = System.currentTimeMillis();
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        isClick = true;
                        v.animate()
                                .x(event.getRawX() + dX)
                                .y(event.getRawY() + dY)
                                .setDuration(0)
                                .start();
                        break;
                    case MotionEvent.ACTION_UP:
                        long endTime = System.currentTimeMillis();
                        isClick = (endTime - startTime) > 0.1 * 1000L;
                        v.animate()
                                .x(event.getRawX()
                                        + dX)
                                .y(event.getRawY() + dY)
                                .setDuration(10)
                                .start();
                        break;
                }
            }
            return isClick;
        }

    };

    private void activeHangup() {
        RTCAudioPlayer.getInstance().stopPlay();
        if (!isCreate) {
            if (P2PDataProvider.getInstance().localStream == null || P2PDataProvider.getInstance().remoteStream == null) {
                // 拒绝
                //  WKRTCManager.getInstance().getSaveMsgListener().onRefuse(isCreate, toUID, LiMRTCCallType.audio);
                WKRTCManager.getInstance().getSendMsgListener().sendRefuse(toUID, (byte) 1, callType);
                new Handler(Objects.requireNonNull(Looper.myLooper())).postDelayed(() -> {
                    RTCAudioPlayer.getInstance().play(CallActivity.this, "lim_rtc_hangup.wav", false);
                    finish();
                }, 500);
            } else {
                WKRTCManager.getInstance().getSendMsgListener().sendHangUpMsg(toUID, (int) (totalDuration / 1000), callType, isCreate ? 1 : 0);
            }
        } else {
            if (P2PDataProvider.getInstance().remoteStream == null) {
                WKRTCManager.getInstance().getSendMsgListener().sendCancel(toUID, callType);
            } else {
                WKRTCManager.getInstance().getSendMsgListener().sendHangUpMsg(toUID, (int) (totalDuration / 1000), callType, isCreate ? 1 : 0);
            }
        }
        hangup();
    }

    private void hangup() {
        if (fullSurfaceView != null) {
            fullSurfaceView.release();
            fullSurfaceView = null;
        }
        if (smallSurfaceView != null) {
            smallSurfaceView.release();
            smallSurfaceView = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
        P2PDataProvider.getInstance().hangup();
        audioManager.setSpeakerphoneOn(false);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        stopTimer();
        WKRTCManager.getInstance().stopTimer();
        finish();
    }

    private void requestPermission() {
        String[] permissions = new String[]{Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO};

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(CallActivity.this,
                    permission) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(CallActivity.this,
                        permissions,
                        100);
                // 发送取消消息
//                WKRTCManager.getInstance().getSendMsgListener().sendCancel(toUID, callType);
//                hangup();
                return;
            }
        }
        initListener();
        P2PDataProvider.getInstance().init(loginUID, toUID, toName, callType, isCreate, isConnect, fullSurfaceView);
        if (!isCreate) {
            runOnUiThread(() -> {
                answerIV.setVisibility(View.VISIBLE);
            });
        }

    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100
                && grantResults.length == 2
                && grantResults[0] == PERMISSION_GRANTED
                && grantResults[1] == PERMISSION_GRANTED) {
            initListener();
            P2PDataProvider.getInstance().init(loginUID, toUID, toName, callType, isCreate, isConnect, fullSurfaceView);

            if (!isCreate) {
                runOnUiThread(() -> {
                    answerIV.setVisibility(View.VISIBLE);
                });
                //  P2PDataProvider.getInstance().publish("ui 通过");
            }
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.open_camera_microphone));
            builder.setPositiveButton(getString(R.string.rtc_go_to_setting), (dialog, which) -> {
                dialog.dismiss();
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + CallActivity.this.getPackageName()));
                startActivity(intent);
                activeHangup();
//                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
            });
            builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                dialog.dismiss();
                activeHangup();
            });
            builder.show();

        }

    }

    private void showFloatingView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //判断是否拥有悬浮窗权限，无则跳转悬浮窗权限授权页面
            if (Settings.canDrawOverlays(this)) {
                show();
            } else {
                showDialog();
            }
        } else {
            show();
        }
    }


    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.open_floating_desc));
        builder.setPositiveButton(getString(R.string.open), (dialog, which) -> {
            dialog.dismiss();
            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + CallActivity.this.getPackageName()));
            } else {
                intent = new Intent(Settings.ACTION_APN_SETTINGS, Uri.parse("package:" + CallActivity.this.getPackageName()));
            }
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            showFloatingView();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && P2PDataProvider.getInstance().remoteStream != null) {
            showFloatingView();
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            boolean isSet = true;
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int filter = notificationManager.getCurrentInterruptionFilter();
                if (!notificationManager.isNotificationPolicyAccessGranted() && filter == INTERRUPTION_FILTER_NONE) {
                    startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                    isSet = false;
                }
            }
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !notificationManager.isNotificationPolicyAccessGranted()) {
//                startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
//            } else {
//                int volume = audioManager.getStreamVolume(AudioManager.MODE_IN_CALL);
//                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.MODE_IN_CALL);
//                float value = (float) volume / maxVolume;
//                audioManager.adjustStreamVolume(AudioManager.MODE_IN_CALL, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
//
//                if (P2PDataProvider.getInstance().remoteStream != null) {
//                    if (value < 0) {
//                        value = 0.f;
//                    }
//                    if (value > 1) {
//                        value = 1.f;
//                    }
//                    P2PDataProvider.getInstance().remoteStream.setVolume(value);
//                }
//            }
            if (isSet) {

                int volume = audioManager.getStreamVolume(AudioManager.MODE_IN_CALL);
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.MODE_IN_CALL);
                float value = (float) volume / maxVolume;
                audioManager.adjustStreamVolume(AudioManager.MODE_IN_CALL, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);

                if (P2PDataProvider.getInstance().remoteStream != null) {
                    if (value < 0) {
                        value = 0.f;
                    }
                    if (value > 1) {
                        value = 1.f;
                    }
                    P2PDataProvider.getInstance().remoteStream.setVolume(value);
                }

            }

            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            boolean isSet = true;
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int filter = notificationManager.getCurrentInterruptionFilter();
                if (!notificationManager.isNotificationPolicyAccessGranted() && filter != INTERRUPTION_FILTER_ALL) {
                    startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                    isSet = false;
                }
            }


            if (isSet) {
                int volume = audioManager.getStreamVolume(AudioManager.MODE_IN_CALL);
                if (volume <= 1) {
                    audioManager.setStreamVolume(AudioManager.MODE_IN_CALL, volume, AudioManager.FLAG_SHOW_UI);
                    return true;
                }
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.MODE_IN_CALL);

                float value = (float) volume / maxVolume;
                audioManager.adjustStreamVolume(AudioManager.MODE_IN_CALL, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                if (P2PDataProvider.getInstance().remoteStream != null) {
                    if (value <= 0) {
                        value = 0.1f;
                    }
                    if (value > 1) {
                        value = 1.f;
                    }
                    P2PDataProvider.getInstance().remoteStream.setVolume(value);
                }
            }
            return true;
        }
        return true;
//        return super.onKeyDown(keyCode, event);
    }


    private void switchType(int callType) {
        P2PDataProvider.getInstance().callType = callType;
        CallActivity.this.callType = callType;
        muteLayout.setVisibility(View.VISIBLE);
        if (callType == WKRTCCallType.audio) {
            fullSurfaceView.setVisibility(View.GONE);
            smallSurfaceView.setVisibility(View.GONE);
            avatarIv.setVisibility(View.VISIBLE);
            nameTv.setVisibility(View.VISIBLE);
            audioLayout.setVisibility(View.VISIBLE);
            videoLayout.setVisibility(View.GONE);

            //  audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.setSpeakerphoneOn(true);
            if (audioManager.isSpeakerphoneOn()) {
                speakerIV.setImageResource(R.mipmap.ic_handfree_hover);
            } else speakerIV.setImageResource(R.mipmap.ic_handfree);
        } else {
            fullSurfaceView.setVisibility(View.VISIBLE);
            smallSurfaceView.setVisibility(View.VISIBLE);
            avatarIv.setVisibility(View.GONE);
            nameTv.setVisibility(View.GONE);
            audioLayout.setVisibility(View.GONE);
            videoLayout.setVisibility(View.VISIBLE);
        }
    }

    private void show() {
        WKFloatingViewManager.getInstance().showFloatingView(true);
        finish();
    }


    @Override
    public void finish() {
        super.finish();
        WKRTCManager.getInstance().isCalling = false;
    }
}
