package com.chat.base.act;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;

/**
 * Workplace "发现" 外链专用：独立 taskAffinity，避免回到 TabActivity（singleTask）时系统清栈把 Web 页销毁。
 * 缩小进悬浮球时用 {@link #moveTaskToBack(boolean)}，展开时复合同一 Activity / WebView 实例。
 */
public class WorkplaceWebViewActivity extends WKWebViewActivity {

    public static final String EXTRA_BUBBLE_RESTORE = "workplace_bubble_restore";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getBooleanExtra(EXTRA_BUBBLE_RESTORE, false)) {
            return;
        }
        if (wkVBinding == null) {
            return;
        }
        String url = resolveUrlFromIntent(intent);
        if (!TextUtils.isEmpty(url)) {
            wkVBinding.webView.loadUrl(url);
        }
    }
}
