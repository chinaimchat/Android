package com.chat.base.net;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 多域名故障切换拦截器。
 *
 * <p>仅当原请求 host 属于 {@link ApiHostPool} 池内时生效。
 * 按 {@link ApiHostPool#orderedHosts()} 顺序依次尝试，遇到以下情形切换到下一个：
 * <ul>
 *   <li>网络级异常：{@link IOException}（超时 / 连不上 / DNS 失败 / TLS 失败）</li>
 *   <li>HTTP 5xx</li>
 * </ul>
 *
 * <p><b>不会对 4xx 切换</b>——4xx 是业务错（参数错 / 未登录 / 权限不足 / 资源不存在），
 * 切到其他 host 结果一样，还会把所有候选全探测一遍浪费时间。</p>
 *
 * <p>切换成功后，通过 {@link ApiHostPool#savePreferredHost(String)} 把当前 host
 * 写成"下次首选"，让后续请求直接命中，无试错开销。</p>
 *
 * <p>只对 <b>应用层拦截器</b>（{@code addInterceptor}）位置生效；
 * 不要加到 {@code addNetworkInterceptor}，因为网络层 redirect 后 {@code chain.proceed}
 * 无法被多次调用。</p>
 */
public class DomainFalloverInterceptor implements Interceptor {

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request origRequest = chain.request();
        HttpUrl origUrl = origRequest.url();
        String origHost = origUrl.host();

        // 池外域名（CDN、第三方、OAuth、IP 直连等）不改写，直接走原请求。
        if (!ApiHostPool.isPoolHost(origHost)) {
            return chain.proceed(origRequest);
        }

        List<String> hosts = ApiHostPool.orderedHosts();
        IOException lastNetErr = null;
        Response lastServerErr = null;

        for (int i = 0; i < hosts.size(); i++) {
            String host = hosts.get(i);
            Request req;
            if (host.equals(origHost)) {
                req = origRequest;
            } else {
                HttpUrl newUrl = origUrl.newBuilder().host(host).build();
                req = origRequest.newBuilder().url(newUrl).build();
            }

            // 前一轮留下的 5xx 响应必须关闭，OkHttp 要求响应体被消费或关闭。
            if (lastServerErr != null) {
                closeQuietly(lastServerErr);
                lastServerErr = null;
            }

            try {
                Response response = chain.proceed(req);
                if (response.code() >= 500 && response.code() <= 599) {
                    lastServerErr = response;
                    continue;
                }
                // 2xx / 3xx / 4xx 都算"这个 host 是活的"——记下首选、直接返回。
                ApiHostPool.savePreferredHost(host);
                return response;
            } catch (IOException e) {
                lastNetErr = e;
                // 接着试下一个 host
            }
        }

        if (lastServerErr != null) {
            // 所有候选都是 5xx，把最后一个返回给上层（保留响应体让业务自行处理）。
            return lastServerErr;
        }
        if (lastNetErr != null) {
            throw lastNetErr;
        }
        // 理论上不会走到这里
        throw new IOException("DomainFalloverInterceptor: no host attempted");
    }

    private static void closeQuietly(Response r) {
        try {
            r.close();
        } catch (Throwable ignore) {
        }
    }
}
