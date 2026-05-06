package com.chat.base.net;

import android.text.TextUtils;
import android.util.Log;

import com.chat.base.config.WKBinder;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

/**
 * Debug-only: dump Workplace API headers + raw response for backend triage.
 * <p>
 * Why: Android may be filtered by backend based on headers (os/appid/version/token).
 */
public class WorkplaceDebugInterceptor implements Interceptor {

    private static final String TAG = "WorkplaceDebug";

    private static boolean isWorkplaceEndpoint(Request request) {
        if (request == null || request.url() == null) return false;
        String path = request.url().encodedPath();
        if (TextUtils.isEmpty(path)) return false;
        // Both are used by Workplace page.
        return path.endsWith("/workplace/category") || path.contains("/workplace/categorys/");
    }

    private static String maskToken(String token) {
        if (TextUtils.isEmpty(token)) return "";
        int len = token.length();
        if (len <= 10) return "***";
        return token.substring(0, 4) + "..." + token.substring(len - 4);
    }

    private static String safeHeader(Request request, String name) {
        if (request == null) return "";
        String v = request.header(name);
        return v == null ? "" : v;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();
        if (!WKBinder.isDebug || !isWorkplaceEndpoint(request)) {
            return chain.proceed(request);
        }

        // Dump key request headers (the ones backend commonly filters on).
        String token = safeHeader(request, "token");
        String os = safeHeader(request, "os");
        String appid = safeHeader(request, "appid");
        String version = safeHeader(request, "version");
        String pkg = safeHeader(request, "package");
        String model = safeHeader(request, "model");

        Log.d(TAG, "==== Workplace request ====");
        Log.d(TAG, "url=" + request.url());
        Log.d(TAG, String.format(Locale.getDefault(),
                "headers{os=%s, appid=%s, version=%s, package=%s, model=%s, token=%s}",
                os, appid, version, pkg, model, maskToken(token)));

        // Optional: log request body if it exists and is repeatable.
        RequestBody requestBody = request.body();
        if (requestBody != null && !requestBody.isOneShot()) {
            MediaType type = requestBody.contentType();
            Charset charset = type == null ? StandardCharsets.UTF_8 : (type.charset(StandardCharsets.UTF_8));
            Buffer buffer = new Buffer();
            try {
                requestBody.writeTo(buffer);
                String bodyStr = buffer.readString(charset);
                if (!TextUtils.isEmpty(bodyStr)) {
                    Log.d(TAG, "request_body=" + bodyStr);
                }
            } catch (Exception e) {
                Log.w(TAG, "request_body_read_failed", e);
            }
        }

        Response response = chain.proceed(request);
        ResponseBody body = response.body();
        if (body == null) {
            Log.d(TAG, "response(code=" + response.code() + "): <empty body>");
            return response;
        }

        MediaType mediaType = body.contentType();
        boolean isTextBody = mediaType != null
                && ("application".equals(mediaType.type()) || "text".equals(mediaType.type()));
        if (!isTextBody) {
            Log.d(TAG, "response(code=" + response.code() + "): [binary " + mediaType + "]");
            return response;
        }

        String content = body.string();
        Log.d(TAG, "response_code=" + response.code());
        Log.d(TAG, "response_raw=" + content);
        Log.d(TAG, "==== Workplace response end ====");

        return response.newBuilder()
                .body(ResponseBody.create(content, mediaType))
                .build();
    }
}

