package rtc.conference;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
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
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.chat.rtc.R;

import rtc.WKRTCApplication;
import rtc.utils.RTCAudioPlayer;
import rtc.utils.WKFloatingViewManager;
import rtc.utils.WKLogger;
import rtc.utils.WKRTCManager;
import rtc.view.MultiVideoChatLayout;

import org.webrtc.RTCStats;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import owt.base.MediaConstraints;
import owt.conference.Participant;
import owt.conference.RemoteStream;
import rtc.utils.BtnClickUtils;
import rtc.utils.WKRTCCommonUtils;
import rtc.utils.MeetingDataProvider;

public class MeetingActivity extends AppCompatActivity implements MeetingDataProvider.IMeetingListener {
    private final String tag = "MeetingActivity";
    private final int OVERLAY_PERMISSION_REQUEST_CODE = 1024;
    private String roomID, token;
    private String channelID;
    private byte channelType;
    private String loginUID;
    private View hangupIv;
    private boolean isOpenCamera = false;
    private boolean isOpenSpeaker = false;
    private boolean isOpenMicrophone = true;
    private boolean isRestart = false;
    private TextView timeTv;
    private View switchCameraView;
    private SurfaceViewRenderer bigSurfaceView;
    private MultiVideoChatLayout allView;
    private AppCompatImageView microphoneIv, speakerIv, cameraIv;
    private View microphoneView, speakerView, cameraView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (WKRTCManager.getInstance().isShowAnimation)
            overridePendingTransition(R.anim.top_in, R.anim.top_silent);
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        WKRTCManager.getInstance().isCalling = true;
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
        isRestart = getIntent().getBooleanExtra("isRestart", false);
        initView();
        initListener();
        if (isRestart) {
            addView(MeetingDataProvider.getInstance().list);
        } else {
            List<ItemRemoteStream> list = new ArrayList<>();
            // 选择的成员
            if (getIntent().hasExtra("uids")) {
                List<String> uidList = getIntent().getStringArrayListExtra("uids");
                if (uidList != null && uidList.size() > 0) {
                    for (int i = 0; i < uidList.size(); i++) {
                        list.add(new ItemRemoteStream(uidList.get(i), null, null));
                        MeetingDataProvider.getInstance().startCountDownTimer(uidList.get(i));
                    }
                }
            }
            MeetingDataProvider.getInstance().init(loginUID, channelID, channelType, roomID, token);
            MeetingDataProvider.getInstance().list = list;
            addView(list);
            requestPermission();
        }
        MeetingDataProvider.getInstance().addMeetingListener(this);
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
        WKRTCManager.getInstance().addTimerListener("multiCall", (time, timeText) -> runOnUiThread(() -> timeTv.setText(timeText)));
        findViewById(R.id.switchCameraIv).setOnClickListener(v -> MeetingDataProvider.getInstance().capturer.switchCamera());
        findViewById(R.id.minimizeIv).setOnClickListener(v -> showFloatingView());
        hangupIv.setOnClickListener(v -> {
            if (BtnClickUtils.getInstance().isCanClick(R.id.hangUpIv)) {
                hangupIv.setEnabled(false);
                MeetingDataProvider.getInstance().hangUp();
            }
        });
        cameraView.setOnClickListener(v -> {
            if (MeetingDataProvider.getInstance().localStream == null || MeetingDataProvider.getInstance().publication == null)
                return;
            isOpenCamera = !isOpenCamera;
            updateCameraStatus();
            bigSurfaceView.setVisibility(View.GONE);
            if (isOpenCamera) {
                MeetingDataProvider.getInstance().localStream.enableVideo();
                MeetingDataProvider.getInstance().publication.unmute(MediaConstraints.TrackKind.VIDEO, null);
            } else {
                MeetingDataProvider.getInstance().localStream.disableVideo();
                MeetingDataProvider.getInstance().publication.mute(MediaConstraints.TrackKind.VIDEO, null);
            }
            switchCameraView.setVisibility(isOpenCamera ? View.VISIBLE : View.GONE);
        });
        speakerView.setOnClickListener(v -> {
            if (MeetingDataProvider.getInstance().localStream == null || MeetingDataProvider.getInstance().publication == null)
                return;
            isOpenSpeaker = !isOpenSpeaker;
            updateSpeakerStatus();
        });
        microphoneView.setOnClickListener(v -> {
            if (MeetingDataProvider.getInstance().localStream == null || MeetingDataProvider.getInstance().publication == null)
                return;
            isOpenMicrophone = !isOpenMicrophone;
            updateMicrophoneStatus();
            if (isOpenMicrophone) {
                MeetingDataProvider.getInstance().localStream.enableAudio();
                MeetingDataProvider.getInstance().publication.unmute(MediaConstraints.TrackKind.AUDIO, null);
            } else {
                MeetingDataProvider.getInstance().localStream.disableAudio();
                MeetingDataProvider.getInstance().publication.mute(MediaConstraints.TrackKind.AUDIO, null);
            }
        });
        findViewById(R.id.addIv).setOnClickListener(v -> {
            if (WKRTCManager.getInstance().getIChooseMembers() != null) {
                List<String> selectedUIDs = new ArrayList<>();
                for (int i = 0; i < MeetingDataProvider.getInstance().list.size(); i++) {
                    if (!TextUtils.isEmpty(MeetingDataProvider.getInstance().list.get(i).uid)) {
                        selectedUIDs.add(MeetingDataProvider.getInstance().list.get(i).uid);
                    }
                }
                WKRTCManager.getInstance().getIChooseMembers().chooseMembers(MeetingActivity.this, roomID, channelID, channelType, selectedUIDs, UIDs -> {
                    // TODO: 5/7/21
                    if (WKRTCCommonUtils.isEmpty(UIDs)) return;
                    for (int i = 0; i < UIDs.size(); i++) {
                        boolean isAdd = true;
                        for (int j = 0; j < MeetingDataProvider.getInstance().list.size(); j++) {
                            if (!TextUtils.isEmpty(MeetingDataProvider.getInstance().list.get(j).uid) && !TextUtils.isEmpty(UIDs.get(i))
                                    && MeetingDataProvider.getInstance().list.get(j).uid.equals(UIDs.get(i))) {
                                isAdd = false;
                                break;
                            }
                        }
                        if (isAdd) {
                            MeetingDataProvider.getInstance().list.add(new ItemRemoteStream(UIDs.get(i), null, null));
                            allView.addView(getSurfaceView(UIDs.get(i), null));
                            MeetingDataProvider.getInstance().startCountDownTimer(UIDs.get(i));
                        }
                    }
                });
            }
        });
    }

    private void requestPermission() {
        String[] permissions = new String[]{Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO};

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(MeetingActivity.this,
                    permission) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MeetingActivity.this,
                        permissions,
                        100);
//                MeetingDataProvider.getInstance().hangUp();
//                finish();
                return;
            }
        }

        MeetingDataProvider.getInstance().start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100
                && grantResults.length == 2
                && grantResults[0] == PERMISSION_GRANTED
                && grantResults[1] == PERMISSION_GRANTED) {
            MeetingDataProvider.getInstance().start();
        } else {
            MeetingDataProvider.getInstance().hangUp();
            finish();
        }
    }


    private View getSurfaceView(String uid, RemoteStream remoteStream) {
        View view = LayoutInflater.from(MeetingActivity.this).inflate(R.layout.item_surfaceview, null);
        view.setTag(uid);
        View loadingView = view.findViewById(R.id.loadingView);
        View loadingLayout = view.findViewById(R.id.loadingLayout);
        AppCompatImageView avatarIv = view.findViewById(R.id.avatarIv);
        SurfaceViewRenderer surfaceView = view.findViewById(R.id.surfaceView);
        surfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        surfaceView.setEnableHardwareScaler(true);
        surfaceView.init(WKRTCApplication.getInstance().getRootEglBase().getEglBaseContext(), null);

//        surfaceViewMap.put(uid, surfaceView);
        if (remoteStream == null) {
            loadingView.setVisibility(View.VISIBLE);
            loadingLayout.setVisibility(View.VISIBLE);
        } else {
            remoteStream.attach(surfaceView);
            loadingView.setVisibility(View.GONE);
            loadingLayout.setVisibility(View.GONE);
        }
        avatarIv.setVisibility(View.VISIBLE);
        surfaceView.setVisibility(View.GONE);
        WKRTCManager.getInstance().showAvatar(MeetingActivity.this, uid, avatarIv, avatarIv.getWidth(), false);

        // 点击切换到大视图
        surfaceView.setOnClickListener(v -> {
            for (int i = 0; i < MeetingDataProvider.getInstance().list.size(); i++) {
                if (!TextUtils.isEmpty(uid) && MeetingDataProvider.getInstance().list.get(i).uid.equals(uid)) {
                    if (MeetingDataProvider.getInstance().list.get(i).remoteStream != null) {
                        MeetingDataProvider.getInstance().bigRemoteStream = MeetingDataProvider.getInstance().list.get(i).remoteStream;
                        MeetingDataProvider.getInstance().bigRemoteStream.attach(bigSurfaceView);
                        break;
                    }
                }
            }
            bigSurfaceView.setVisibility(View.VISIBLE);
            bigSurfaceView.setOnClickListener(v1 -> {
                bigSurfaceView.setVisibility(View.GONE);
                if (MeetingDataProvider.getInstance().bigRemoteStream != null) {
                    MeetingDataProvider.getInstance().bigRemoteStream.detach(bigSurfaceView);
                }
            });
        });
        return view;
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


    private int currVolume = 0;

    protected void openSpeaker() {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setSpeakerphoneOn(true);
            currVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
            if (!audioManager.isSpeakerphoneOn()) {
                //setSpeakerphoneOn() only work when audio mode set to MODE_IN_CALL.
                audioManager.setMode(AudioManager.MODE_IN_CALL);
                audioManager.setSpeakerphoneOn(true);
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                        AudioManager.STREAM_VOICE_CALL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭扬声器
     */
    protected void closeSpeaker() {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                if (audioManager.isSpeakerphoneOn()) {
                    audioManager.setSpeakerphoneOn(false);
                    audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, currVolume,
                            AudioManager.STREAM_VOICE_CALL);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeView(String uid) {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed() && allView.getChildCount() > 0 && !TextUtils.isEmpty(uid)) {
                for (int i = 0, size = allView.getChildCount(); i < size; i++) {
                    View view = allView.getChildAt(i);
                    if (view != null && view.getTag() != null && view.getTag().equals(uid)) {
                        allView.removeViewAt(i);
                        break;
                    }
                }

            }
        });

    }

    @Override
    public void onUnmute(List<ItemRemoteStream> list, String uid, MediaConstraints.TrackKind trackKind) {
        runOnUiThread(() -> {
            if (!TextUtils.isEmpty(uid) && !isFinishing() && !isDestroyed()) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).uid.equals(uid)) {
                        //开启摄像头
                        if (trackKind == MediaConstraints.TrackKind.VIDEO) {
                            allView.getChildAt(i).findViewById(R.id.avatarIv).setVisibility(View.GONE);
                            allView.getChildAt(i).findViewById(R.id.surfaceView).setVisibility(View.VISIBLE);
                        } else if (trackKind == MediaConstraints.TrackKind.AUDIO) {
                            //开启声音
                            if (uid.equals(loginUID)) {
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

    @Override
    public void onMute(List<ItemRemoteStream> list, String uid, MediaConstraints.TrackKind trackKind) {
        runOnUiThread(() -> {
            if (!TextUtils.isEmpty(uid) && !isFinishing() && !isDestroyed()) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).uid.equals(uid)) {
                        //关闭摄像头
                        if (trackKind == MediaConstraints.TrackKind.VIDEO) {
                            AppCompatImageView imageView = allView.getChildAt(i).findViewById(R.id.avatarIv);
                            imageView.setVisibility(View.VISIBLE);
                            WKRTCManager.getInstance().showAvatar(MeetingActivity.this, uid, imageView, imageView.getWidth(), false);
                            allView.getChildAt(i).findViewById(R.id.surfaceView).setVisibility(View.GONE);
                        } else if (trackKind == MediaConstraints.TrackKind.AUDIO) {
                            if (uid.equals(loginUID)) {
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
    public void resetView(List<ItemRemoteStream> list, String uid, Participant participant, RemoteStream remoteStream) {
        runOnUiThread(() -> {
            if (!TextUtils.isEmpty(uid) && !isFinishing() && !isDestroyed()) {
                boolean isAdd = true;
                for (int j = 0; j < list.size(); j++) {
                    if (!TextUtils.isEmpty(list.get(j).uid)
                            && !TextUtils.isEmpty(uid) && list.get(j).uid.equals(uid)) {
                        boolean isAttach = list.get(j).remoteStream == null;
                        list.get(j).remoteStream = remoteStream;
                        if (list.get(j).participant == null)
                            list.get(j).participant = participant;
                        if (isAttach) {
                            if (allView != null && allView.getChildAt(j) != null) {
                                SurfaceViewRenderer surfaceViewRenderer = allView.getChildAt(j).findViewById(R.id.surfaceView);
                                list.get(j).remoteStream.attach(surfaceViewRenderer);
                            }
                        }
                        if (allView != null && allView.getChildAt(j) != null) {
                            allView.getChildAt(j).findViewById(R.id.loadingView).setVisibility(View.GONE);
                            allView.getChildAt(j).findViewById(R.id.loadingLayout).setVisibility(View.GONE);

                        }
                        isAdd = false;
                        break;
                    }
                }
                if (isAdd) {
                    Log.e("重新添加用户了", "-->");
                    list.add(new ItemRemoteStream(uid, remoteStream, participant));
                    allView.addView(getSurfaceView(uid, remoteStream));
                }
            }
        });
    }

    @Override
    public void addView(List<ItemRemoteStream> list) {
        runOnUiThread(() -> {
            if (!isDestroyed() && !isFinishing()) {
                allView.removeAllViews();
                for (int i = 0; i < list.size(); i++) {
                    allView.addView(getSurfaceView(list.get(i).uid, list.get(i).remoteStream));
                }
            }
        });
    }

    @Override
    public void addItem(RemoteStream remoteStream, String uid) {
        if (isDestroyed() || isFinishing()) return;
        runOnUiThread(() -> {
            boolean isAdd = true;
            for (int i = 0, size = allView.getChildCount(); i < size; i++) {
                View view = allView.getChildAt(i);
                if (view.getTag() != null && view.getTag().equals(uid)) {
                    isAdd = false;
                    break;
                }
            }
            if (isAdd) {
                allView.addView(getSurfaceView(uid, remoteStream));
            }
        });

    }

    @Override
    public void hangup() {
        if (!isDestroyed() && !isFinishing()) {
            RTCAudioPlayer.getInstance().play(MeetingActivity.this, "lim_rtc_hangup.wav", false);
            finish();
        }
    }


    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.top_silent, R.anim.top_out);
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
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + MeetingActivity.this.getPackageName()));
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
            show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showFloatingView();
        }
        return true;
//        return super.onKeyDown(keyCode, event);
    }


    private void show() {
        if (MeetingDataProvider.getInstance().localStream == null || MeetingDataProvider.getInstance().publication == null)
            return;
        if (isOpenCamera) {
            isOpenCamera = false;
            updateCameraStatus();
            bigSurfaceView.setVisibility(View.GONE);
            if (isOpenCamera) {
                MeetingDataProvider.getInstance().localStream.enableVideo();
                MeetingDataProvider.getInstance().publication.unmute(MediaConstraints.TrackKind.VIDEO, null);
            } else {
                MeetingDataProvider.getInstance().localStream.disableVideo();
                MeetingDataProvider.getInstance().publication.mute(MediaConstraints.TrackKind.VIDEO, null);
            }
            switchCameraView.setVisibility(isOpenCamera ? View.VISIBLE : View.GONE);
        }
        WKFloatingViewManager.getInstance().showFloatingView(false);
        finish();
    }


    @Override
    public void checkRTCStatusReport(ConcurrentHashMap<String, WKRTCStatusReport> rtcStatusMap, List<ItemRemoteStream> list) {
        if (isFinishing() || isDestroyed()) return;
        if (rtcStatusMap != null && !rtcStatusMap.isEmpty()) {
            for (Map.Entry<String, WKRTCStatusReport> rtcStatsEntry : rtcStatusMap.entrySet()) {
                Map<String, RTCStats> statsMap = rtcStatsEntry.getValue().rtcStatsReport.getStatsMap();
                for (Map.Entry<String, RTCStats> entry : statsMap.entrySet()) {
                    String mapKey = entry.getKey();
                    RTCStats mapValue = entry.getValue();
                    WKLogger.e(tag, "声音：" + mapValue.toString());
//                    if (mapValue.getMembers().containsKey("mediaType")) {
//                        String mediaType = (String) mapValue.getMembers().get("mediaType");
//                        if (!TextUtils.isEmpty(mediaType) && mediaType.equals("audio")){
//                            Object ssrc  = mapValue.getMembers().get("ssrc");
//                            Log.e("查询的音频",ssrc+"");
//                        }
//                    }
                    //  totalSamplesReceived totalAudioEnergy
                    if (mapValue.getMembers() != null && mapValue.getMembers().containsKey("totalSamplesReceived")) {
                        Object object = mapValue.getMembers().get("totalSamplesReceived");

                        if (object != null) {
                            BigInteger totalAudioEnergy = (BigInteger) object;
                            runOnUiThread(() -> {
                                boolean isShowVoice = totalAudioEnergy.intValue() > 0;
//                                if (totalAudioEnergy - rtcStatsEntry.getValue().lastTotalAudioEnergy >= 0.0005) {
//                                    isShowVoice = true;
//                                }
                                if (WKRTCCommonUtils.isNotEmpty(list)) {
                                    for (int i = 0, size = list.size(); i < size; i++) {
                                        if (!TextUtils.isEmpty(list.get(i).uid) && list.get(i).uid.equals(rtcStatsEntry.getKey())) {
                                            for (int j = 0; j < allView.getChildCount(); j++) {
                                                if (allView.getChildAt(j) != null && allView.getChildAt(j).getTag() != null && allView.getChildAt(j).getTag().equals(list.get(i).uid)) {
                                                    allView.getChildAt(j).findViewById(R.id.speakerIv).setVisibility(isShowVoice ? View.VISIBLE : View.GONE);
                                                    break;
                                                }
                                            }

                                            break;
                                        }
                                    }
                                }
                            });

//                            rtcStatsEntry.getValue().lastTotalAudioEnergy = totalAudioEnergy;
//                            rtcStatusMap.put(mapKey, rtcStatsEntry.getValue());
                        }
                        break;
                    }
                }
            }

        }
    }
}
