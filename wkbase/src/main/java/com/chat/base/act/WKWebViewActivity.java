package com.chat.base.act;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.chat.base.R;
import com.chat.base.app.WKAppModel;
import com.chat.base.base.WKBaseActivity;
import com.chat.base.config.WKApiConfig;
import com.chat.base.config.WKBinder;
import com.chat.base.config.WKConfig;
import com.chat.base.databinding.ActWebvieiwLayoutBinding;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.EndpointSID;
import com.chat.base.endpoint.entity.ChatChooseContacts;
import com.chat.base.endpoint.entity.ChatViewMenu;
import com.chat.base.endpoint.entity.ChooseChatMenu;
import com.chat.base.entity.AppInfo;
import com.chat.base.entity.AuthInfo;
import com.chat.base.entity.PopupMenuItem;
import com.chat.base.glide.GlideUtils;
import com.chat.base.jsbrigde.CallBackFunction;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.base.ui.components.AvatarView;
import com.chat.base.ui.components.BottomSheet;
import com.chat.base.utils.WKDialogUtils;
import com.chat.base.utils.WKLogUtils;
import com.chat.base.utils.WKToastUtils;
import com.chat.base.workplace.WorkplaceWebBubbleStore;
import com.google.gson.JsonObject;
import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.entity.WKChannel;
import com.xinbida.wukongim.entity.WKChannelType;
import com.xinbida.wukongim.entity.WKSendOptions;
import com.xinbida.wukongim.msgmodel.WKTextContent;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * 2019-11-21 13:25
 */

@SuppressLint("JavascriptInterface")
public class WKWebViewActivity extends WKBaseActivity<ActWebvieiwLayoutBinding> {
    TextView titleTv;
    private final int FILE_CHOOSER_RESULT_CODE = 101;
    ValueCallback<Uri> mUploadMessage;
    ValueCallback<Uri[]> mUploadCallbackAboveL;
    private String channelID;
    private byte channelType;

    @Override
    protected ActWebvieiwLayoutBinding getViewBinding() {
        return ActWebvieiwLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        this.titleTv = titleTv;
    }

    @Override
    protected void initPresenter() {
        if (getIntent().hasExtra("channelID"))
            channelID = getIntent().getStringExtra("channelID");
        if (getIntent().hasExtra("channelType"))
            channelType = getIntent().getByteExtra("channelType", (byte) 0);
    }

    @Override
    protected int getBackResourceID(ImageView backIv) {
        return R.mipmap.ic_close_white;
    }

    @Override
    protected void backListener(int type) {
        if (minimizeWorkplaceBubbleIfNeeded()) {
            return;
        }
        super.backListener(type);
    }

    @Override
    protected int getRightIvResourceId(ImageView imageView) {
        return R.mipmap.ic_ab_other;
    }

    @Override
    protected void rightLayoutClick() {
        super.rightLayoutClick();

        List<PopupMenuItem> list = new ArrayList<>();
        list.add(new PopupMenuItem(getString(R.string.copy_url), R.mipmap.search_links, () -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData mClipData = ClipData.newPlainText("Label", wkVBinding.webView.getUrl());
            assert cm != null;
            cm.setPrimaryClip(mClipData);
            WKToastUtils.getInstance().showToastNormal(getString(R.string.copyed));
        }));
        list.add(new PopupMenuItem(getString(R.string.forward), R.mipmap.msg_forward, () -> {
            WKTextContent textContent = new WKTextContent(wkVBinding.webView.getUrl());
            EndpointManager.getInstance().invoke(EndpointSID.showChooseChatView, new ChooseChatMenu(new ChatChooseContacts(new ChatChooseContacts.IChoose() {
                @Override
                public void onResult(List<WKChannel> list) {
                    for (WKChannel channel : list) {
                        WKSendOptions options = new WKSendOptions();
                        options.setting.receipt = channel.receipt;
                        WKIM.getInstance().getMsgManager().sendWithOptions(textContent, channel,options);
                    }
                }
            }), textContent));
        }));

        list.add(new PopupMenuItem(getString(R.string.refresh), R.mipmap.tool_rotate, () -> {
            wkVBinding.webView.reload();
        }));
        list.add(new PopupMenuItem(getString(R.string.open_system_browser), R.mipmap.msg_openin, () -> {
            Uri uri = Uri.parse(wkVBinding.webView.getUrl());
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }));
        ImageView rightIV = findViewById(R.id.titleRightIv);
        WKDialogUtils.getInstance().showScreenPopup(rightIV, list);
    }

    @Override
    protected void initView() {
        initWebViewSetting();
        WorkplaceWebBubbleStore.getInstance().bindActivity(this);
        String url = resolveUrlFromIntent(getIntent());
        assert url != null;
        Log.e("加载的URL", url);
        wkVBinding.webView.loadUrl(url);
    }

    /**
     * Resolves the {@code url} extra the same way for first load and for {@link WorkplaceWebViewActivity#onNewIntent}.
     */
    protected String resolveUrlFromIntent(Intent intent) {
        if (intent == null) {
            return null;
        }
        String url = intent.getStringExtra("url");
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        if (!url.startsWith("http") && !url.startsWith("HTTP") && !url.startsWith("file")) {
            url = "http://" + url;
        }
        if (url.equals(WKApiConfig.baseWebUrl + "report.html")) {
            String wk_theme_pref = Theme.getTheme();
            url = String.format("%s?uid=%s&token=%s&mode=%s", url, WKConfig.getInstance().getUid(), WKConfig.getInstance().getToken(), wk_theme_pref);
        }

        // Some server middleware reads token from query/header/cookie.
        // WebView does not reuse OkHttp headers, so we need to append token for QR code endpoints.
        // e.g. /v1/qrcode/{uuid}
        if (url.contains("/api/v1/qrcode/") || url.contains("/v1/qrcode/")) {
            String token = WKConfig.getInstance().getToken();
            if (!TextUtils.isEmpty(token) && !url.contains("token=")) {
                try {
                    String uid = WKConfig.getInstance().getUid();
                    String encodedToken = URLEncoder.encode(token, "UTF-8");
                    String encodedUid = URLEncoder.encode(uid == null ? "" : uid, "UTF-8");
                    String joiner = url.contains("?") ? "&" : "?";
                    url = url + joiner + "uid=" + encodedUid + "&token=" + encodedToken;
                } catch (Exception ignored) {
                }
            }
        }
        return url;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebViewSetting() {
        WebSettings webSettings = wkVBinding.webView.getSettings();
        webSettings.setJavaScriptEnabled(true); // 设置支持javascript脚本
        webSettings.setUseWideViewPort(true);
        webSettings.setPluginState(WebSettings.PluginState.ON);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setDefaultTextEncodingName("UTF-8");
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
//        webSettings.setAppCacheEnabled(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false); // 禁止保存表单
        webSettings.setDomStorageEnabled(true);
//        webSettings.setAppCacheMaxSize(1024 * 1024 * 8);
        //webSettings.setAllowFileAccess(true);
        webSettings.setAllowUniversalAccessFromFileURLs(false);
        webSettings.setAllowFileAccessFromFileURLs(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(0);
        }
        if (WKBinder.isDebug && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        // 禁止网页缩放，避免系统右下角出现放大/缩小控件。
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        wkVBinding.webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        applyWebViewAppearance();
    }

    /**
     * Web 页随主题 / 系统深色模式：H5 的 prefers-color-scheme 与整页反色策略与 App 一致。
     */
    private void applyWebViewAppearance() {
        WebSettings webSettings = wkVBinding.webView.getSettings();
        boolean dark = Theme.isEffectiveDarkMode(this);
        wkVBinding.webView.setBackgroundColor(ContextCompat.getColor(this, R.color.homeColor));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(webSettings, dark);
                return;
            }
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(webSettings, dark
                    ? WebSettingsCompat.FORCE_DARK_ON
                    : WebSettingsCompat.FORCE_DARK_OFF);
        }
    }

    @Override
    protected boolean supportSlideBack() {
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (minimizeWorkplaceBubbleIfNeeded()) {
                return true;
            } else if (wkVBinding.webView.canGoBack()) {
                wkVBinding.webView.goBack();
                return true;
            } else return super.onKeyDown(keyCode, event);
        } else
            return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (minimizeWorkplaceBubbleIfNeeded()) {
            return;
        }
        if (wkVBinding.webView.canGoBack()) {
            wkVBinding.webView.goBack();
            return;
        }
        super.onBackPressed();
    }


//    @Override
//    protected void backListener(int type) {
//        // super.backListener(type);
//        if (wkVBinding.webView.canGoBack()) {
//            wkVBinding.webView.goBack();
//        } else {
//            super.onBackPressed();
//        }
//    }

    @Override
    protected void initListener() {
        wkVBinding.webView.registerHandler("quit", (var1, var2) -> {
            if (!minimizeWorkplaceBubbleIfNeeded()) {
                finish();
            }
        });
        wkVBinding.webView.registerHandler("auth", (data, function) -> {
            if (!TextUtils.isEmpty(data)) {
                try {
                    JSONObject jsonObject = new JSONObject(data);
                    String appId = jsonObject.optString("app_id");
                    if (!TextUtils.isEmpty(appId)) {
                        getAppInfo(appId, function);
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

            }
            Log.e("需要授权的信息", data);
        });
        wkVBinding.webView.registerHandler("getChannel", (data, function) -> {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("channelID", channelID);
            jsonObject.addProperty("channelType", channelType);
            function.onCallBack(jsonObject.toString());
        });
        wkVBinding.webView.registerHandler("showConversation", (data, function) -> {
            if (!TextUtils.isEmpty(data)) {
                try {
                    JSONObject jsonObject = new JSONObject(data);
                    String channelID = jsonObject.optString("channel_id");
                    byte channelType = (byte) jsonObject.optInt("channel_type");
                    EndpointManager.getInstance().invoke(EndpointSID.chatView, new ChatViewMenu(WKWebViewActivity.this, channelID, channelType, 0, true));
                    finish();
                } catch (JSONException e) {
                    WKLogUtils.e("显示最近会话页面错误");
                }
            }
        });

        wkVBinding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!TextUtils.isEmpty(getIntent().getStringExtra("workplace_bubble_url")) && !TextUtils.isEmpty(url)) {
                    WorkplaceWebBubbleStore.getInstance().setPending(url,
                            getIntent().getStringExtra("workplace_bubble_icon"),
                            WKWebViewActivity.this);
                }
            }
        });

        wkVBinding.webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView webView, String s) {
                super.onReceivedTitle(webView, s);
                if (!TextUtils.isEmpty(s) && !"about:blank".equals(s)) {
                    titleTv.setText(s);
                }
            }

            @Override
            public void onProgressChanged(WebView webView, int i) {
                super.onProgressChanged(webView, i);
                if (i > 99) {
                    wkVBinding.progress.setVisibility(View.GONE);
//                    hideLoadingDialog();
                } else {
                    wkVBinding.progress.setVisibility(View.VISIBLE);
                    wkVBinding.progress.setProgress(i);
                }
            }

//            @Override
//            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
//                mUploadMessage = uploadMsg;
//                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
//                i.addCategory(Intent.CATEGORY_OPENABLE);
//                i.setType("*/*");
//                WKWebViewActivity.this.startActivityForResult(Intent.createChooser(i, "File Browser"), FILE_CHOOSER_RESULT_CODE);
//            }

            // For Android 5.0+
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {

                mUploadCallbackAboveL = filePathCallback;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                startActivityForResult(
                        Intent.createChooser(i, "File Browser"),
                        FILE_CHOOSER_RESULT_CODE);
                return true;
            }

        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (null == mUploadMessage && null == mUploadCallbackAboveL) return;
            Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();
            if (mUploadCallbackAboveL != null) {
                onActivityResultAboveL(requestCode, resultCode, data);
            } else if (mUploadMessage != null) {
                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            }
        }

    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void onActivityResultAboveL(int requestCode, int resultCode, Intent data) {
        if (requestCode != FILE_CHOOSER_RESULT_CODE
                || mUploadCallbackAboveL == null) {
            return;
        }
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
            } else {
                String dataString = data.getDataString();
                ClipData clipData = data.getClipData();
                if (clipData != null) {
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                }
                if (dataString != null)
                    results = new Uri[]{Uri.parse(dataString)};
            }
        }
        mUploadCallbackAboveL.onReceiveValue(results);
        mUploadCallbackAboveL = null;
    }


    @SuppressLint("NewApi")
    @Override
    protected void onPause() {
        wkVBinding.webView.onPause();
        super.onPause();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        wkVBinding.webView.onResume();
        super.onResume();
    }

    @Override
    public void finish() {
        // Write pending data before TabActivity.onResume, otherwise bubble may miss this round.
        cacheWorkplaceBubblePendingIfNeeded();
        super.finish();
    }

    @Override
    protected void onDestroy() {
        // Fallback in case finish() path is skipped.
        cacheWorkplaceBubblePendingIfNeeded();
        WorkplaceWebBubbleStore.getInstance().unbindActivity(this);
        super.onDestroy();
    }

    private boolean minimizeWorkplaceBubbleIfNeeded() {
        String bubbleUrl = getIntent().getStringExtra("workplace_bubble_url");
        String bubbleIcon = getIntent().getStringExtra("workplace_bubble_icon");
        if (TextUtils.isEmpty(bubbleUrl)) {
            return false;
        }
        cacheWorkplaceBubblePendingIfNeeded();
        openWorkplaceTabFromBubble();
        return true;
    }

    private void openWorkplaceTabFromBubble() {
        // TabActivity is singleTask: starting it while WKWebViewActivity sits on top clears/finishes
        // this activity, which breaks "minimize web into bubble" (live WebView instance).
        // WorkplaceWebViewActivity runs in its own task and only moves that task to the back.
        if (this instanceof WorkplaceWebViewActivity) {
            moveTaskToBack(true);
            return;
        }
        Intent intent = new Intent();
        intent.setClassName(getPackageName(), "com.chat.uikit.TabActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void cacheWorkplaceBubblePendingIfNeeded() {
        // Only workplace web flow should trigger bubble; it is identified by non-empty extras.
        String bubbleUrl = getIntent().getStringExtra("workplace_bubble_url");
        String bubbleIcon = getIntent().getStringExtra("workplace_bubble_icon");
        if (!TextUtils.isEmpty(bubbleUrl)) {
            Log.d("WorkplaceBubble", "cache pending in WKWebViewActivity, url=" + bubbleUrl);
            WorkplaceWebBubbleStore.getInstance().setPending(bubbleUrl, bubbleIcon, this);
        } else {
            Log.d("WorkplaceBubble", "skip cache in WKWebViewActivity, empty bubble url");
        }
    }

    private void getAppInfo(String appId, CallBackFunction function) {
        WKAppModel.Companion.getInstance().getAppInfo(appId, (code, msg, appInfo) -> {
            if (code == HttpResponseCode.success) {
                authDialog(appInfo, function);
            } else {
                if (!TextUtils.isEmpty(msg)) {
                    showToast(msg);
                }
            }
        });
    }

    private void authDialog(AppInfo appInfo, CallBackFunction function) {
        View authView = LayoutInflater.from(this).inflate(R.layout.auth_dialog_layout, getViewBinding().webView, false);
        TextView appName = authView.findViewById(R.id.appNameTv);
        AvatarView appIV = authView.findViewById(R.id.appIV);
        TextView nameTv = authView.findViewById(R.id.nameTv);
        TextView descTv = authView.findViewById(R.id.descTv);
        AvatarView avatarView = authView.findViewById(R.id.avatarView);
        descTv.setText(String.format(getString(R.string.str_request_desc), getString(R.string.app_name)));
        appIV.setSize(30f);
        appName.setText(appInfo.getApp_name());
        GlideUtils.getInstance().showImg(this, WKApiConfig.getShowUrl(appInfo.getApp_logo()), appIV.imageView);
        avatarView.setSize(40f);
        WKChannel loginChannel = WKIM.getInstance().getChannelManager().getChannel(WKConfig.getInstance().getUid(), WKChannelType.PERSONAL);
        avatarView.showAvatar(loginChannel);
        nameTv.setText(loginChannel.channelName);
        BottomSheet bottomSheet = new BottomSheet(this, true);
        bottomSheet.setCustomView(authView);
        authView.findViewById(R.id.cancelBtn).setOnClickListener(v -> {
            bottomSheet.setDelegate(null);
            bottomSheet.dismiss();
        });
        authView.findViewById(R.id.sureBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WKAppModel.Companion.getInstance().getAuthCode(appInfo.getApp_id(), new WKAppModel.IAuth() {
                    @Override
                    public void onResult(int code, @Nullable String msg, @Nullable AuthInfo authInfo) {
                        if (authInfo != null) {
                            JSONObject json = new JSONObject();
                            try {
                                json.put("code", authInfo.getAuthcode());
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                            function.onCallBack(json.toString());
                            bottomSheet.setDelegate(null);
                            bottomSheet.dismiss();
                        }
                    }
                });
            }
        });
        bottomSheet.setOpenNoDelay(false);
        bottomSheet.setDelegate(new BottomSheet.BottomSheetDelegateInterface() {

            @Override
            public void onOpenAnimationStart() {

            }

            @Override
            public void onOpenAnimationEnd() {

            }

            @Override
            public boolean canDismiss() {
                return false;
            }
        });
        bottomSheet.show();
    }
}
