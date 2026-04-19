package rtc.conference;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.chat.rtc.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import rtc.utils.WKRTCCommonUtils;
import rtc.utils.RTCAudioPlayer;
import rtc.utils.WKRTCManager;

/**
 * 5/8/21 10:36 AM
 */
public class MultiCallWaitingAnswerActivity extends AppCompatActivity {
    private List<String> uids;
    private String fromUID;
    private String fromName;
    private String channelID;
    private byte channelType;
    private String token;
    private String loginUID;
    private String roomID;
    private AudioManager audioManager;
    private Vibrator vibrator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        overridePendingTransition(R.anim.top_in, R.anim.top_silent);
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.act_waiting_answer_layout);
        WKRTCCommonUtils.setStatusBarColor(window, ContextCompat.getColor(this, R.color.color232323), 0);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        fromUID = getIntent().getStringExtra("fromUID");
        fromName = getIntent().getStringExtra("fromName");
        channelID = getIntent().getStringExtra("channelID");
        loginUID = getIntent().getStringExtra("loginUID");
        channelType = getIntent().getByteExtra("channelType", (byte) 2);
        token = getIntent().getStringExtra("token");
        uids = getIntent().getStringArrayListExtra("uids");
        roomID = getIntent().getStringExtra("roomID");
        initView();
        startCountDownTimer();
    }

    private void initView() {
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(true);//是否从扬声器播出
        if (vibrator.hasVibrator()) {
            long[] pattern = {0, 500, 1000};
            vibrator.vibrate(pattern, 0);
        }
        RTCAudioPlayer.getInstance().play(this, "lim_rtc_receive.mp3", true);

        ImageView avatarIv = findViewById(R.id.avatarIv);
        WKRTCManager.getInstance().showAvatar(this, fromUID, avatarIv, 80, true);
        View hangUpIv = findViewById(R.id.hangUpIv);
        TextView nameTv = findViewById(R.id.nameTv);
        TextView otherTv = findViewById(R.id.otherTv);
        nameTv.setText(fromName);
        LinearLayout userLayout = findViewById(R.id.userLayout);
        LinearLayout userLayout1 = findViewById(R.id.userLayout1);
        userLayout1.removeAllViews();
        userLayout.removeAllViews();
        if (uids != null && uids.size() > 0) {
            for (int i = 0; i < uids.size(); i++) {
                if (i > 3) {
                    userLayout1.addView(addUser(userLayout1, uids.get(i)));
                } else userLayout.addView(addUser(userLayout, uids.get(i)));
            }
        } else otherTv.setVisibility(View.GONE);
        hangUpIv.setOnClickListener(v -> {
            onCancel(true);
        });
        findViewById(R.id.answerIv).setOnClickListener(v -> {
            RTCAudioPlayer.getInstance().stopPlay();
//            Intent intent = new Intent(MultiCallWaitingAnswerActivity.this, MultiCallActivity.class);
            Intent intent = new Intent(MultiCallWaitingAnswerActivity.this, MeetingActivity.class);
            intent.putExtra("token", token);
            intent.putExtra("channelID", channelID);
            intent.putExtra("channelType", channelType);
            intent.putExtra("loginUID", loginUID);
            intent.putExtra("roomID", roomID);
            intent.putStringArrayListExtra("uids", new ArrayList<>(uids));
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                @SuppressWarnings("unchecked") ActivityOptionsCompat activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(MultiCallWaitingAnswerActivity.this, new Pair<>(hangUpIv, "hangup"));
//                startActivity(intent, activityOptions.toBundle());
//            } else {
            startActivity(intent);
//            }
            if (downTimer != null) {
                downTimer.cancel();
                downTimer = null;
            }
            finish();
        });
    }

    @Override
    public void finish() {
        super.finish();
        if (vibrator != null) {
            vibrator.cancel();
        }
        overridePendingTransition(R.anim.top_silent, R.anim.top_out);
        new Handler(Looper.getMainLooper()).postDelayed(() -> RTCAudioPlayer.getInstance().stopPlay(), 1000);
    }

    private View addUser(LinearLayout linearLayout, String uid) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_waiting_answer_user_layout, linearLayout, false);
        ImageView avatarIv = view.findViewById(R.id.avatarIv);
        WKRTCManager.getInstance().showAvatar(this, uid, avatarIv, 40, true);
        return view;
    }

    private void onCancel(boolean isCancel) {
        if (vibrator != null) {
            vibrator.cancel();
        }
        if (downTimer != null) {
            downTimer.cancel();
        }
        RTCAudioPlayer.getInstance().stopPlay();
        if (isCancel)
            WKRTCManager.getInstance().getSendMsgListener().sendMultiRefuse(roomID);
//        else
//            WKRTCManager.getInstance().getSaveMsgListener().onMissed(channelID, channelType, LiMRTCCallType.audio);
        new Handler(Objects.requireNonNull(Looper.myLooper())).postDelayed(() -> {
            RTCAudioPlayer.getInstance().play(MultiCallWaitingAnswerActivity.this, "lim_rtc_hangup.wav", false);
            finish();
        }, 500);
        WKRTCManager.getInstance().isCalling = false;
    }

    CountDownTimer downTimer;

    // 邀请用户超过30s未进入通话就挂断移除对应成员
    private void startCountDownTimer() {
        downTimer = new CountDownTimer(30 * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                onCancel(false);
            }
        };
        downTimer.start();
    }
}
