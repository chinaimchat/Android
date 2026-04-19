package com.chat.base.act;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.chat.base.R;
import com.chat.base.WKBaseApplication;
import com.chat.base.config.WKConfig;
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
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder;
import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.entity.WKChannel;
import com.xinbida.wukongim.entity.WKMsg;
import com.xinbida.wukongim.entity.WKSendOptions;
import com.xinbida.wukongim.msgmodel.WKMessageContent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        detailPlayer.getBackButton().setVisibility(View.GONE);
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

    private String appendTokenToUrl(String url, String token) {
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(token)) return url;
        try {
            if (url.contains("token=")) return url;
            String encodedToken = java.net.URLEncoder.encode(token, "UTF-8");
            if (url.contains("?")) {
                return url + "&token=" + encodedToken;
            } else {
                return url + "?token=" + encodedToken;
            }
        } catch (Exception e) {
            return url;
        }
    }

    @Override
    public void onPlayError(String url, Object... objects) {
        Log.e("PlayVideoActivity", "onPlayError url=" + url);
        WKToastUtils.getInstance().showToastNormal("视频无法播放");
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

        // 后端对视频/封面链接可能要求 token 头；GSY/IJK 走自身网络层，不会自动复用 OkHttp 的拦截器。
        Map<String, String> headers = new HashMap<>();
        String token = WKConfig.getInstance().getToken();
        if (TextUtils.isEmpty(token)) {
            token = WKConfig.getInstance().getImToken();
        }
        if (TextUtils.isEmpty(token)) {
            token = WKConfig.getInstance().getUserInfo().token;
        }
        if (TextUtils.isEmpty(token)) {
            token = WKConfig.getInstance().getUserInfo().im_token;
        }
        if (!TextUtils.isEmpty(token)) {
            headers.put("token", token);
            headers.put("Authorization", "Bearer " + token);
        }

        // 尽量复用你们 OkHttp 的 CommonRequestParamInterceptor 公共头，避免文件预览接口额外拦截
        headers.put("model", Build.MODEL);
        headers.put("os", "Android");
        headers.put("appid", WKBaseApplication.getInstance().appID);
        headers.put("version", WKBaseApplication.getInstance().versionName);
        headers.put("package", WKBaseApplication.getInstance().packageName);

        String playUrlForRequest = playUrl;
        if (playUrlForRequest != null && (playUrlForRequest.startsWith("http") || playUrlForRequest.startsWith("HTTP"))) {
            playUrlForRequest = appendTokenToUrl(playUrlForRequest, token);
        }

        return new GSYVideoOptionBuilder()
                .setThumbImageView(imageView)
                .setUrl(playUrlForRequest)
                .setMapHeadData(headers)
                .setCacheWithPlay(false)
                .setVideoTitle("")
                .setIsTouchWiget(true)
                //.setAutoFullWithSize(true)
                .setRotateViewAuto(false)
                .setLockLand(false)
                .setShowFullAnimation(false)//打开动画
                .setNeedLockFull(true)
                // 从开头开始播放，避免 seekRatio=1 导致直接跳到结尾
                .setSeekRatio(0);
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

    private void showSaveDialog(String url) {
        List<BottomSheetItem> list = new ArrayList<>();
        list.add(new BottomSheetItem(getString(R.string.save_img), R.mipmap.msg_download, () -> {
            checkPermissions(url);
        }));
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

    private void saveToAlbum(String url) {

        // 保存视频
        if (!url.startsWith("http") && !url.startsWith("HTTP")) {
            File file = new File(url.replaceAll("file:///", ""));
            save(file);
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
                    save(file);
                }

                @Override
                public void onFail(@Nullable Object tag, @Nullable String msg) {
                    WKToastUtils.getInstance().showToastNormal(getString((R.string.download_err)));
                }
            });
        }

    }

    private void save(File file) {
        boolean result = WKFileUtils.getInstance().saveVideoToAlbum(PlayVideoActivity.this, file.getAbsolutePath());
        if (result) {
            WKToastUtils.getInstance().showToastNormal(getString(R.string.saved_album));
        }
    }

    private void checkPermissions(String url) {
        String desc = String.format(
                getString(R.string.file_permissions_des),
                getString(R.string.app_name)
        );
        if (Build.VERSION.SDK_INT < 33) {
            WKPermissions.getInstance().checkPermissions(new WKPermissions.IPermissionResult() {
                                                             @Override
                                                             public void onResult(boolean result) {
                                                                 if (result) {
                                                                     saveToAlbum(url);
                                                                 }
                                                             }

                                                             @Override
                                                             public void clickResult(boolean isCancel) {

                                                             }
                                                         },
                    this,
                    desc,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            );
        } else {
            WKPermissions.getInstance().checkPermissions(
                    new WKPermissions.IPermissionResult() {
                        @Override
                        public void onResult(boolean result) {
                            if (result) {
                                saveToAlbum(url);
                            }
                        }

                        @Override
                        public void clickResult(boolean isCancel) {

                        }
                    },
                    this,
                    desc,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_IMAGES
            );
        }
    }
}
