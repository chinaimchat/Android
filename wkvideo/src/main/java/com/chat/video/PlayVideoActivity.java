package com.chat.video;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.chat.base.WKBaseApplication;
import com.chat.base.config.WKConstants;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.EndpointSID;
import com.chat.base.endpoint.entity.ChatChooseContacts;
import com.chat.base.endpoint.entity.ChooseChatMenu;
import com.chat.base.entity.BottomSheetItem;
import com.chat.base.glide.GlideUtils;
import com.chat.base.net.ud.WKDownloader;
import com.chat.base.net.ud.WKProgressManager;
import com.chat.base.utils.WKDialogUtils;
import com.chat.base.utils.WKFileUtils;
import com.chat.base.utils.WKPermissions;
import com.chat.base.utils.WKReader;
import com.chat.base.utils.WKTimeUtils;
import com.chat.base.utils.WKToastUtils;
import com.chat.base.utils.systembar.WKStatusBarUtils;
import com.google.android.material.snackbar.Snackbar;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder;
import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.entity.WKChannel;
import com.xinbida.wukongim.entity.WKMsg;
import com.xinbida.wukongim.entity.WKSendOptions;
import com.xinbida.wukongim.msgmodel.WKMessageContent;
import com.xinbida.wukongim.msgmodel.WKVideoContent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 2020-03-11 11:54
 * 播放视频
 */
public class PlayVideoActivity extends GSYBaseActivityDetail<VideoPlayer> {

    VideoPlayer detailPlayer;
    String playUrl;
    String coverImg;
    private String clientMsgNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.act_play_video_layout);

        detailPlayer = findViewById(R.id.player);
        //增加title
        detailPlayer.getTitleTextView().setVisibility(View.GONE);
        // 显示返回键：全屏播放时系统返回可能被 GSY backFromWindowFull 吞掉且无 UI 出口，导致无法退出
        detailPlayer.getBackButton().setVisibility(View.VISIBLE);
        detailPlayer.getBackButton().setOnClickListener(v -> finish());
        initView();
        initVideoBuilderMode();
        detailPlayer.startPlayLogic();
    }

    private void initView() {
        if (getIntent().hasExtra("clientMsgNo"))
            clientMsgNo = getIntent().getStringExtra("clientMsgNo");
        coverImg = getIntent().getStringExtra("coverImg");
        String url = getIntent().getStringExtra("url");
        if (TextUtils.isEmpty(url)) {
            WKToastUtils.getInstance().showToast(getString(R.string.video_deleted));
            finish();
            return;
        }
        playUrl = url;
        if (!url.startsWith("HTTP") && !url.startsWith("http")) {
            playUrl = "file:///" + url;
        }
        detailPlayer.setLongClick(() -> {
            if (!TextUtils.isEmpty(clientMsgNo)) {
                WKMsg msg = WKIM.getInstance().getMsgManager().getWithClientMsgNO(clientMsgNo);
                if (msg.flame == 1) return;
            }
            showSaveDialog(playUrl);
        });

        cacheVideoIfNeeded();


        Window window = getWindow();
        if (window == null) return;
        WKStatusBarUtils.transparentStatusBar(window);
//        WKStatusBarUtils.setDarkMode(window);
        WKStatusBarUtils.setStatusBarColor(window, ContextCompat.getColor(this, R.color.black), 0);
        WKStatusBarUtils.setLightMode(window);

        if (!TextUtils.isEmpty(clientMsgNo)) {
            WKIM.getInstance().getMsgManager().addOnRefreshMsgListener("play_video", (msg, b) -> {
                if (msg != null && !TextUtils.isEmpty(msg.clientMsgNO) && msg.clientMsgNO.equals(clientMsgNo)) {
                    if (msg.remoteExtra.revoke == 1) {
                        WKToastUtils.getInstance().showToast(getString(R.string.can_not_play_video_with_revoke));
                        finish();
                    }
                }
            });
        }
    }

    @Override
    public VideoPlayer getGSYVideoPlayer() {
        return detailPlayer;
    }

    @Override
    public GSYVideoOptionBuilder getGSYVideoOptionBuilder() {
        //内置封面可参考SampleCoverVideo
        ImageView imageView = new ImageView(this);
        ViewCompat.setTransitionName(detailPlayer, "coverIv");
        GlideUtils.getInstance().showImg(this, coverImg, imageView);
        return new GSYVideoOptionBuilder()
                .setThumbImageView(imageView)
                .setUrl(playUrl)
                .setCacheWithPlay(false)
                .setVideoTitle("")
                .setIsTouchWiget(true)
                //.setAutoFullWithSize(true)
                .setRotateViewAuto(false)
                .setLockLand(false)
                .setShowFullAnimation(false)//打开动画
                .setNeedLockFull(true)
                .setSeekRatio(1);
    }

    @Override
    public void onPlayError(String url, Object... objects) {
        Log.e("PlayVideoActivity", "onPlayError url=" + url);
        WKToastUtils.getInstance().showToastNormal(getString(R.string.video_deleted));
    }

    @Override
    public void clickForFullScreen() {

    }


    /**
     * 是否启动旋转横屏，true表示启动
     */
    @Override
    public boolean getDetailOrientationRotateAuto() {
        return true;
    }

    @Override
    public void onBackPressed() {
        if (orientationUtils != null) {
            orientationUtils.backToProtVideo();
        }
        if (GSYVideoManager.backFromWindowFull(this)) {
            return;
        }
        finish();
    }

    private void showSaveDialog(String url) {
        List<BottomSheetItem> list = new ArrayList<>();
        list.add(new BottomSheetItem(getString(R.string.save_img), R.mipmap.msg_download, () -> checkSavePermission(url)));
        if (!TextUtils.isEmpty(clientMsgNo)) {
            list.add(new BottomSheetItem(getString(R.string.forward), R.mipmap.msg_forward, () -> {

                if (!TextUtils.isEmpty(clientMsgNo)) {
                    WKMsg msg = WKIM.getInstance().getMsgManager().getWithClientMsgNO(clientMsgNo);
                    if (msg != null && msg.baseContentMsgModel != null) {
                        EndpointManager.getInstance().invoke(EndpointSID.showChooseChatView, new ChooseChatMenu(new ChatChooseContacts(list1 -> {
                            WKMessageContent msgContent = msg.baseContentMsgModel;
                            if (WKReader.isNotEmpty(list1)) {
                                for (WKChannel channel : list1) {
                                    msgContent.mentionAll = 0;
                                    msgContent.mentionInfo = null;
                                    WKSendOptions options = new WKSendOptions();
                                    options.setting.receipt = channel.receipt;
//                                    setting.signal = 0;
                                    WKIM.getInstance().getMsgManager().sendWithOptions(
                                            msgContent,
                                            channel, options
                                    );
                                }
                                View viewGroup = findViewById(android.R.id.content);
                                Snackbar.make(viewGroup, getString(R.string.str_forward), 1000).setAction("", view -> {
                                }).show();
                            }
                        }), msg.baseContentMsgModel));
                    }
                }

            }));
        }
        WKDialogUtils.getInstance().showBottomSheet(this, getString(R.string.wk_video), false, list);
    }

    private void checkSavePermission(String url) {
        String desc = String.format(getString(R.string.file_permissions_des), getString(R.string.app_name));
        WKPermissions.IPermissionResult callback = new WKPermissions.IPermissionResult() {
            @Override
            public void onResult(boolean result) {
                if (result) saveToAlbum(url);
            }

            @Override
            public void clickResult(boolean isCancel) {
            }
        };
        if (Build.VERSION.SDK_INT >= 33) {
            WKPermissions.getInstance().checkPermissions(callback, this, desc,
                    Manifest.permission.READ_MEDIA_VIDEO);
        } else {
            WKPermissions.getInstance().checkPermissions(callback, this, desc,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    private void saveToAlbum(String url) {
        if (!url.startsWith("http") && !url.startsWith("HTTP")) {
            File file = new File(url.replaceAll("file:///", ""));
            if (WKFileUtils.getInstance().saveVideoToAlbum(this, file.getAbsolutePath())) {
                WKToastUtils.getInstance().showToastNormal(getString(R.string.saved_album));
            }
        } else {
            String fileDir = Objects.requireNonNull(getExternalFilesDir("video")).getAbsolutePath() + WKBaseApplication.getInstance().getFileDir() + "/";
            WKFileUtils.getInstance().createFileDir(fileDir);
            String filePath = fileDir + WKTimeUtils.getInstance().getCurrentMills() + ".mp4";
            WKDownloader.Companion.getInstance().download(url, filePath, new WKProgressManager.IProgress() {
                @Override
                public void onProgress(@Nullable Object tag, int progress) {
                }

                @Override
                public void onSuccess(@Nullable Object tag, @Nullable String path) {
                    File file = new File(filePath.replaceAll("file:///", ""));
                    if (WKFileUtils.getInstance().saveVideoToAlbum(PlayVideoActivity.this, file.getAbsolutePath())) {
                        WKToastUtils.getInstance().showToastNormal(getString(R.string.saved_album));
                    }
                }

                @Override
                public void onFail(@Nullable Object tag, @Nullable String msg) {
                    WKToastUtils.getInstance().showToastNormal(getString(R.string.download_err));
                }
            });
        }
    }

    private void cacheVideoIfNeeded() {
        if (TextUtils.isEmpty(clientMsgNo)) return;
        if (playUrl == null || (!playUrl.startsWith("http") && !playUrl.startsWith("HTTP"))) return;

        WKMsg msg = WKIM.getInstance().getMsgManager().getWithClientMsgNO(clientMsgNo);
        if (msg == null || msg.flame == 1) return;
        if (!(msg.baseContentMsgModel instanceof WKVideoContent)) return;
        WKVideoContent videoContent = (WKVideoContent) msg.baseContentMsgModel;
        if (!TextUtils.isEmpty(videoContent.localPath)) {
            File localFile = new File(videoContent.localPath);
            if (localFile.exists() && localFile.length() > 0) return;
        }

        String fileDir = WKConstants.videoDir + msg.channelType + "/" + msg.channelID + "/";
        WKFileUtils.getInstance().createFileDir(fileDir);
        String filePath = fileDir + msg.clientMsgNO + ".mp4";

        File cached = new File(filePath);
        if (cached.exists() && cached.length() > 0) {
            videoContent.localPath = filePath;
            WKIM.getInstance().getMsgManager().updateContentAndRefresh(clientMsgNo, videoContent, false);
            return;
        }

        WKDownloader.Companion.getInstance().download(playUrl, filePath, new WKProgressManager.IProgress() {
            @Override
            public void onProgress(@Nullable Object tag, int progress) {
            }

            @Override
            public void onSuccess(@Nullable Object tag, @Nullable String path) {
                videoContent.localPath = filePath;
                WKIM.getInstance().getMsgManager().updateContentAndRefresh(clientMsgNo, videoContent, false);
            }

            @Override
            public void onFail(@Nullable Object tag, @Nullable String failMsg) {
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        if (!TextUtils.isEmpty(clientMsgNo)) {
            WKIM.getInstance().getMsgManager().removeRefreshMsgListener("play_video");
            WKMsg msg = WKIM.getInstance().getMsgManager().getWithClientMsgNO(clientMsgNo);
            if (msg != null && msg.flame == 1 && msg.viewed == 0) {
                WKIM.getInstance().getMsgManager().updateViewedAt(1, WKTimeUtils.getInstance().getCurrentMills(), clientMsgNo);
                EndpointManager.getInstance().invoke("video_viewed", clientMsgNo);
            }
        }
    }
}
