package com.chat.base.workplace;
import android.text.TextUtils;
import android.util.Log;

import com.chat.base.act.WKWebViewActivity;

import java.lang.ref.WeakReference;

/**
 * Workplace Web bubble pending state.
 *
 * Life-cycle:
 * - WorkplaceFragment opens WKWebViewActivity with Intent extras.
 * - WKWebViewActivity sets pending state when finishing.
 * - TabActivity checks pending state and shows bubble.
 * - Bubble two-step click:
 *   1) show X (no navigation)
 *   2) open web again, then clear pending; next close will re-set via extras
 */
public class WorkplaceWebBubbleStore {
    private static volatile WorkplaceWebBubbleStore instance;

    private String url;
    private String icon;

    private boolean pending;
    private boolean xVisible;
    private WeakReference<WKWebViewActivity> activityRef;

    private WorkplaceWebBubbleStore() {
    }

    public static WorkplaceWebBubbleStore getInstance() {
        if (instance == null) {
            synchronized (WorkplaceWebBubbleStore.class) {
                if (instance == null) {
                    instance = new WorkplaceWebBubbleStore();
                }
            }
        }
        return instance;
    }

    public synchronized void setPending(String url, String icon) {
        this.url = url;
        this.icon = icon;
        this.pending = !TextUtils.isEmpty(url);
        this.xVisible = false;
        Log.d("WorkplaceBubble", "store setPending pending=" + this.pending + ", url=" + url);
    }

    public synchronized void setPending(String url, String icon, WKWebViewActivity activity) {
        setPending(url, icon);
        bindActivity(activity);
    }

    public synchronized boolean hasPending() {
        return pending && !TextUtils.isEmpty(url);
    }

    public synchronized String getUrl() {
        return url;
    }

    public synchronized String getIcon() {
        return icon;
    }

    public synchronized boolean isXVisible() {
        return xVisible;
    }

    public synchronized void setXVisible(boolean xVisible) {
        this.xVisible = xVisible;
    }

    public synchronized void bindActivity(WKWebViewActivity activity) {
        if (activity == null) {
            activityRef = null;
            return;
        }
        activityRef = new WeakReference<>(activity);
    }

    public synchronized WKWebViewActivity getBoundActivity() {
        return activityRef != null ? activityRef.get() : null;
    }

    public synchronized boolean hasLiveSession() {
        WKWebViewActivity activity = getBoundActivity();
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    public synchronized void unbindActivity(WKWebViewActivity activity) {
        WKWebViewActivity current = getBoundActivity();
        if (activity == null || current == activity) {
            activityRef = null;
        }
    }

    public synchronized void clear() {
        Log.d("WorkplaceBubble", "store clear");
        url = null;
        icon = null;
        pending = false;
        xVisible = false;
        activityRef = null;
    }
}

