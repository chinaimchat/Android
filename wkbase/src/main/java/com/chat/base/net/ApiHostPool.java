package com.chat.base.net;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chat.base.config.WKSharedPreferencesUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

/**
 * 多域名入口池 + 偏好持久化。
 *
 * <p>与 Web/Manager 侧保持一致，11 个候选域名全部指向同一后端，任一域名均可作为主入口。
 * 与 Nginx 的 302 分流 + 客户端列表重试方案配套：
 * 客户端冷启动随机挑一个作为初始 host；任一请求失败时由
 * {@link DomainFalloverInterceptor} 顺序尝试下一个；成功后把 host 回写为偏好。</p>
 *
 * <p>存储键：{@code preferred_api_host}（仅 scheme+host，不含路径）。</p>
 */
public final class ApiHostPool {

    private static final String PREF_KEY = "preferred_api_host";

    /**
     * 候选域名（与 server_name / split_clients / Web 列表保持一致）。
     * 任何一个都能独立承载全部功能（API/文件/WS）。
     */
    public static final List<String> HOSTS = Collections.unmodifiableList(Arrays.asList(
            "coolapq.com",
            "nykjh.com",
            "lwijf.com",
            "lhqrx.com",
            "lqxybw.cn",
            "vowjyo.cn",
            "pifqtq.cn",
            "xegjzf.cn",
            "hailsv.cn",
            "wvyexex.cn",
            "xwxxkxl.cn"
    ));

    private static final Random RANDOM = new Random();

    private ApiHostPool() {
    }

    /** 当前首选 host；无缓存时随机挑一个（等价于入口分流）。 */
    @NonNull
    public static String getPreferredHost() {
        String cached = WKSharedPreferencesUtil.getInstance().getSP(PREF_KEY);
        if (!TextUtils.isEmpty(cached) && HOSTS.contains(cached)) {
            return cached;
        }
        String picked = HOSTS.get(RANDOM.nextInt(HOSTS.size()));
        WKSharedPreferencesUtil.getInstance().putSP(PREF_KEY, picked);
        return picked;
    }

    /** 把最近一次请求成功的 host 记为首选，下次优先走它，减少每次启动的试错开销。 */
    public static void savePreferredHost(@Nullable String host) {
        if (TextUtils.isEmpty(host) || !HOSTS.contains(host)) {
            return;
        }
        WKSharedPreferencesUtil.getInstance().putSP(PREF_KEY, host);
    }

    /**
     * 返回以 {@link #getPreferredHost()} 排在首位的候选序列（去重）。
     * 失败切换时按此顺序依次尝试。
     */
    @NonNull
    public static List<String> orderedHosts() {
        String preferred = getPreferredHost();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        seen.add(preferred);
        seen.addAll(HOSTS);
        return new ArrayList<>(seen);
    }

    /** 判断 host 是否属于池内，用于拦截器只改写"我们自己"的域名，不误伤第三方 URL。 */
    public static boolean isPoolHost(@Nullable String host) {
        if (TextUtils.isEmpty(host)) {
            return false;
        }
        return HOSTS.contains(host);
    }

    /**
     * 默认 API base URL（含 scheme，不含 /v1）——供首次启动写入 {@code api_base_url}
     * 与初始化 {@link com.chat.base.config.WKApiConfig}。
     */
    @NonNull
    public static String defaultApiBaseURL() {
        return "https://" + getPreferredHost();
    }
}
