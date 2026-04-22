package com.chat.base.net;

import android.util.Log;

import com.chat.base.WKBaseApplication;
import com.chat.base.utils.WKNetUtil;

import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.X509TrustManager;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import okhttp3.HttpUrl;

/**
 * 2020-07-17 14:55
 */
public class OkHttpUtils {
    private OkHttpUtils() {
    }

    private static class OkHttpUtilsBinder {
        final static OkHttpUtils okHttp = new OkHttpUtils();
    }

    public static OkHttpUtils getInstance() {
        return OkHttpUtilsBinder.okHttp;
    }

    private OkHttpClient sOkHttpClient;
    //缓存天数
    private static final long CACHE_STALE_SEC = 60 * 60 * 24 * 2;

    public OkHttpClient getOkHttpClient() {
        if (sOkHttpClient == null) {
            synchronized (OkHttpUtils.class) {
                Cache cache = new Cache(new File(WKBaseApplication.getInstance().getContext().getCacheDir(), "HttpCache"),
                        1024 * 1024 * 100);
                if (sOkHttpClient == null) {
                    sOkHttpClient = new OkHttpClient.Builder().cache(cache)
                            .connectTimeout(60 * 10, TimeUnit.SECONDS)
                            .readTimeout(60 * 10, TimeUnit.SECONDS)
                            .writeTimeout(60 * 10, TimeUnit.SECONDS).connectionPool(new ConnectionPool(5, 5, TimeUnit.SECONDS))
                            .sslSocketFactory(SSLSocketClient.getSSLSocketFactory(), new X509TrustManager() {
                                @Override
                                public void checkClientTrusted(X509Certificate[] chain, String authType) {

                                }

                                @Override
                                public void checkServerTrusted(X509Certificate[] chain, String authType) {

                                }

                                @Override
                                public X509Certificate[] getAcceptedIssuers() {
                                    return new X509Certificate[0];
                                }
                            })
                            .hostnameVerifier(SSLSocketClient.getHostnameVerifier())
                            // 多域名故障切换必须置于最外层：失败时整个链（含通用头、缓存）重试下一个 host。
                            .addInterceptor(new DomainFalloverInterceptor())
                            .addInterceptor(mRewriteCacheControlInterceptor)
                            .addInterceptor(new CommonRequestParamInterceptor())
                            .addNetworkInterceptor(mRewriteCacheControlInterceptor)
                            .addInterceptor(new LogInterceptor()).build();
                }
            }
        }
        return sOkHttpClient;
    }

    private volatile OkHttpClient sBareAssetClient;

    /**
     * 文件预览链：首跳 {@code /v1/file/preview/...} 需带业务头；302 至对象存储等不带（避免破坏预签名）。
     */
    private static boolean filePreviewGatewayHopNeedsAuthHeaders(@Nullable HttpUrl url) {
        if (url == null) {
            return false;
        }
        String p = url.encodedPath();
        return p != null && p.contains("/v1/file/preview");
    }

    /**
     * 无应用层拦截器、无缓存，用于拉取 OSS/MinIO 预签名等 URL（仅 OkHttp 内置 Bridge）。
     */
    @NotNull
    private OkHttpClient getBareAssetClient() {
        if (sBareAssetClient == null) {
            synchronized (OkHttpUtils.class) {
                if (sBareAssetClient == null) {
                    sBareAssetClient = new OkHttpClient.Builder()
                            .connectTimeout(60 * 10, TimeUnit.SECONDS)
                            .readTimeout(60 * 10, TimeUnit.SECONDS)
                            .writeTimeout(60 * 10, TimeUnit.SECONDS)
                            .sslSocketFactory(SSLSocketClient.getSSLSocketFactory(), new X509TrustManager() {
                                @Override
                                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                                }

                                @Override
                                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                                }

                                @Override
                                public X509Certificate[] getAcceptedIssuers() {
                                    return new X509Certificate[0];
                                }
                            })
                            .hostnameVerifier(SSLSocketClient.getHostnameVerifier())
                            .build();
                }
            }
        }
        return sBareAssetClient;
    }

    /**
     * <b>适用范围（与聊天发图上传无关）</b>：仅用于需要「先打业务预览网关、再 302 到存储」的 <b>GET 拉取图片字节</b>，
     * 例如钱包收款码等走 {@code /v1/file/preview/...} 的场景。
     * <p><b>禁止</b>用于 {@code file/upload}（含 {@code /api/v1/file/upload}）的签发或 multipart POST：
     * 聊天/附件上传必须走主 {@link #getOkHttpClient()} + {@link com.chat.base.net.ud.WKUploader}，全局加头、不得在本方法里剥头或换 bare 发上传。</p>
     * <p>禁止用主 Client 自动跟随重定向：部分存储对首跳与跟随请求头敏感。做法：路径含 {@code /v1/file/preview} 的跳
     * 用带拦截器的 {@code followRedirects(false)} Client；后续跳用 bare Client。</p>
     *
     * @return 成功时的响应体；非 2xx、无 Location、超限重定向、或 URL 为上传接口时返回 null
     */
    @Nullable
    public byte[] fetchGatewayThenBareRedirect(@NotNull String startUrl) throws IOException {
        HttpUrl current = HttpUrl.parse(startUrl);
        if (current == null) {
            return null;
        }
        String startPath = current.encodedPath();
        if (startPath != null && startPath.contains("file/upload")) {
            return null;
        }
        OkHttpClient main = getOkHttpClient();
        OkHttpClient authNoRedirect = main.newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
        OkHttpClient bare = getBareAssetClient();

        for (int hop = 0; hop < 10; hop++) {
            boolean needsAuth = filePreviewGatewayHopNeedsAuthHeaders(current);
            OkHttpClient client = needsAuth ? authNoRedirect : bare;
            Request request = new Request.Builder().url(current).build();
            Response response = client.newCall(request).execute();
            try {
                if (response.isRedirect()) {
                    String loc = response.header("Location");
                    if (loc == null) {
                        return null;
                    }
                    HttpUrl next = response.request().url().resolve(loc);
                    if (next == null) {
                        return null;
                    }
                    current = next;
                    continue;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    return null;
                }
                return response.body().bytes();
            } finally {
                response.close();
            }
        }
        return null;
    }

    private final Interceptor mRewriteCacheControlInterceptor = chain -> {
        Request request = chain.request();
        if (!WKNetUtil.isNetworkAvailable(WKBaseApplication.getInstance().getContext())) {
            request = request.newBuilder()
                    .cacheControl(CacheControl.FORCE_CACHE)
                    .build();
            Log.e("无网络连接：", "------->");
        }
        Response originalResponse = chain.proceed(request);
        if (WKNetUtil.isNetworkAvailable(WKBaseApplication.getInstance().getContext())) {
            //有网的时候读接口上的@Headers里的配置，你可以在这里进行统一的设置
            String cacheControl = request.cacheControl().toString();
            return originalResponse.newBuilder()
                    .header("Cache-Control", cacheControl)
                    .removeHeader("Pragma")
                    .build();
        } else {
            return originalResponse.newBuilder()
                    .header("Cache-Control", "public, only-if-cached, max-stale=" + CACHE_STALE_SEC)
                    .removeHeader("Pragma")
                    .build();
        }
    };

}
