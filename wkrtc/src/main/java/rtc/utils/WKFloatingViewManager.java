package rtc.utils;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.TextView;

import rtc.WKRTCApplication;
import rtc.WKRTCCallType;
import rtc.conference.MeetingActivity;
import rtc.p2p.CallActivity;

import com.chat.rtc.R;
import com.yhao.floatwindow.FloatWindow;
import com.yhao.floatwindow.MoveType;
import com.yhao.floatwindow.Screen;
import com.yhao.floatwindow.ViewStateListener;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.lang.ref.WeakReference;

/**
 * 5/13/21 4:25 PM
 */
public class WKFloatingViewManager {


    private WKFloatingViewManager() {
    }

    private static class LiMFloatingViewManagerBinder {
        static final WKFloatingViewManager mFloatingViewManager = new WKFloatingViewManager();
    }

    public static WKFloatingViewManager getInstance() {
        return LiMFloatingViewManagerBinder.mFloatingViewManager;
    }

    SurfaceViewRenderer surfaceViewRenderer;
    public WeakReference<View> timeView;

    public void showFloatingView(boolean isP2PCall) {
        View view = LayoutInflater.from(WKRTCApplication.getInstance().getContext()).inflate(R.layout.floating_call_layout, null, false);
        timeView = new WeakReference<>(view.findViewById(R.id.timeView));
        TextView timeTv = view.findViewById(R.id.timeTv);

        surfaceViewRenderer = view.findViewById(R.id.surfaceView);
        if (isP2PCall) {
            //
            if (P2PDataProvider.getInstance().remoteStream != null && P2PDataProvider.getInstance().callType == WKRTCCallType.video) {
                surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                surfaceViewRenderer.setEnableHardwareScaler(true);
                surfaceViewRenderer.setZOrderMediaOverlay(true);
                surfaceViewRenderer.init(WKRTCApplication.getInstance().getRootEglBase().getEglBaseContext(), null);
                P2PDataProvider.getInstance().remoteStream.attach(surfaceViewRenderer);
                timeView.get().setVisibility(View.GONE);
                surfaceViewRenderer.setVisibility(View.VISIBLE);
            }
//            if (P2PDataProvider.getInstance().callType == WKRTCCallType.video){
//                surfaceViewRenderer.setVisibility(View.VISIBLE);
//            }else {
//                surfaceViewRenderer.setVisibility(View.GONE);
//            }
        } else {
            surfaceViewRenderer.setVisibility(View.GONE);
            timeView.get().setVisibility(View.VISIBLE);
        }

        WKRTCManager.getInstance().addTimerListener("floatingView", (time, timeText) -> {
            new Handler(Looper.getMainLooper()).post(() -> timeTv.setText(timeText));
        });
        FloatWindow
                .with(WKRTCApplication.getInstance().getContext())
                .setView(view)
                .setX(Screen.width, 0.75f)
                .setY(Screen.height, 0.3f)
                .setMoveType(MoveType.slide, 0, 0)
                .setMoveStyle(500, new BounceInterpolator())
                .setDesktopShow(true).setViewStateListener(new ViewStateListener() {
                    @Override
                    public void onPositionUpdate(int x, int y) {

                    }

                    @Override
                    public void onShow() {
                        WKRTCManager.getInstance().isCalling = true;
                    }

                    @Override
                    public void onHide() {

                    }

                    @Override
                    public void onDismiss() {
                        if (surfaceViewRenderer != null) {
                            surfaceViewRenderer = null;
                        }
                        if (timeView != null) {
                            timeView = null;
                        }
                    }

                    @Override
                    public void onMoveAnimStart() {

                    }

                    @Override
                    public void onMoveAnimEnd() {

                    }

                    @Override
                    public void onBackToDesktop() {

                    }
                })
                .build();
        view.setOnClickListener(v -> {
            if (P2PDataProvider.getInstance().remoteStream != null && surfaceViewRenderer != null && P2PDataProvider.getInstance().callType == WKRTCCallType.video) {
                P2PDataProvider.getInstance().remoteStream.detach(surfaceViewRenderer);
            }
            gotoCall(isP2PCall);
        });

    }

    public void onSwitchAudio() {
        if (surfaceViewRenderer != null)
            surfaceViewRenderer.setVisibility(View.GONE);
        if (timeView != null && timeView.get() != null)
            timeView.get().setVisibility(View.VISIBLE);
    }

    private void gotoCall(boolean isP2PCall) {
        Intent intent;
        if (isP2PCall) {
            intent = new Intent(WKRTCApplication.getInstance().getContext(), CallActivity.class);
            intent.putExtra("toName", P2PDataProvider.getInstance().toName);
            intent.putExtra("toUID", P2PDataProvider.getInstance().toUID);
            intent.putExtra("loginUID", P2PDataProvider.getInstance().loginUID);
            intent.putExtra("callType", P2PDataProvider.getInstance().callType);
            intent.putExtra("isCreate", P2PDataProvider.getInstance().isCreate);
        } else {
            intent = new Intent(WKRTCApplication.getInstance().getContext(), MeetingActivity.class);
            intent.putExtra("roomID", MeetingDataProvider.getInstance().roomID);
            intent.putExtra("token", MeetingDataProvider.getInstance().token);
            intent.putExtra("loginUID", MeetingDataProvider.getInstance().loginUID);
            intent.putExtra("channelID", MeetingDataProvider.getInstance().channelID);
            intent.putExtra("channelType", MeetingDataProvider.getInstance().channelType);
        }
        intent.putExtra("isRestart", true);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        WKRTCApplication.getInstance().getContext().startActivity(intent);
        FloatWindow.destroy();

    }
}
