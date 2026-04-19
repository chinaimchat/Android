package com.chat.base.config;

import android.net.Uri;
import android.text.TextUtils;

import java.util.Locale;

import com.xinbida.wukongim.entity.WKChannelType;

/**
 * 2019-11-20 10:11
 * api地址
 * <p><b>相对路径拼接约定（与 Web 一致）：</b>{@link #initBaseURL} / {@link #initBaseURLIncludeIP} 传入的 {@code apiURL}
 * 必须是带协议、正确主机与端口的根地址，例如 {@code http://192.168.1.1:8090} 或已带版本后缀的 {@code http://IP:8090/v1}（末尾 {@code /v1} 会被去掉再统一拼出 {@code .../v1/}）。
 * App 与 Web 必须使用同一套 API 主机与端口；若配置错误，易出现缺端口、错主机，或拼接异常形如 {@code .../v1file/...}（路径粘连）等资源加载问题。</p>
 */
public class WKApiConfig {
    public static String baseUrl = "";
    public static String baseWebUrl = "";

    /**
     * @param apiURL 形如 {@code http://主机:端口} 或 {@code http://主机:端口/v1}，须含 {@code http(s)://} 与端口（若环境非默认端口）
     */
    public static void initBaseURL(String apiURL) {
        String base = stripV1Suffix(apiURL);
        baseUrl = base + "/v1/";
        baseWebUrl = base + "/web/";
    }

    /**
     * @param apiURL 同 {@link #initBaseURL}；须与 Web 端配置的 API 根一致，否则相对路径拼出的资源 URL 会错。
     */
    public static void initBaseURLIncludeIP(String apiURL) {
        String base = stripV1Suffix(apiURL);
        baseUrl = base + "/v1/";
        baseWebUrl = base + "/web/";
    }

    /**
     * 去除 apiURL 末尾可能已包含的 "/v1" 或 "/v1/"，
     * 防止调用方传入带版本路径的地址时拼出 /v1/v1/ 的重复路径。
     */
    private static String stripV1Suffix(String apiURL) {
        if (TextUtils.isEmpty(apiURL)) return apiURL;
        if (apiURL.endsWith("/v1/")) {
            return apiURL.substring(0, apiURL.length() - 4);
        }
        if (apiURL.endsWith("/v1")) {
            return apiURL.substring(0, apiURL.length() - 3);
        }
        return apiURL;
    }

    public static String getAvatarUrl(String uid) {
        return baseUrl + "users/" + uid + "/avatar";
    }

    public static String getGroupUrl(String groupId) {
        return baseUrl + "groups/" + groupId + "/avatar";
    }

    public static String getShowAvatar(String channelID, byte channelType) {
        return channelType == WKChannelType.PERSONAL ? getAvatarUrl(channelID) : getGroupUrl(channelID);
    }

    /**
     * 给头像/通用 URL 拼上 {@code ?v=<cacheKey>}，目的是<b>把头像缓存版本带进 URL 自身</b>：
     * <ul>
     *     <li>当对方更新头像时 server 会下发 {@code wk_userAvatarUpdate} CMD，本地把
     *     {@code WKChannel.avatarCacheKey} 改成新 UUID。但有些 Adapter（会话列表、联系人列表、
     *     聊天气泡）持有的是 {@code WKChannel} 的<b>旧实例引用</b>，DB 里的新 key 没同步到这个引用上；
     *     即使 Glide 用 {@code MyGlideUrlWithId} 把 cacheKey 当作磁盘 key，旧引用还是会拿到旧 key
     *     而命中之前缓存的「默认头像」字节，UI 上就一直是默认头像。</li>
     *     <li>把 cache key 拼到 URL 里以后，URL 字符串本身随 key 变化，Glide 内存/磁盘缓存、
     *     OkHttp 协议缓存都会自然失效；而服务端 {@code GET /v1/users/{uid}/avatar} 也支持
     *     {@code ?v=}（{@code chinaim-server} 仅做透传/日志，不影响响应字节），
     *     不会触发任何业务副作用。</li>
     * </ul>
     * <p>规则：</p>
     * <ul>
     *     <li>{@code url} / {@code cacheKey} 任一为空 → 原样返回；</li>
     *     <li>URL 已经含 {@code v=}（query 中或 fragment 之前）→ 不重复追加，原样返回，避免覆盖
     *     调用方自己拼好的版本号；</li>
     *     <li>否则按是否已有 {@code ?} 选择 {@code ?v=} 或 {@code &v=}，并对 {@code cacheKey}
     *     做 {@link Uri#encode} 防止特殊字符破坏 URL。</li>
     * </ul>
     */
    public static String appendAvatarCacheKey(String url, String cacheKey) {
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(cacheKey)) {
            return url;
        }
        // 仅对 http(s) URL 追加；本地文件路径（file://、绝对路径等）原样返回，
        // 避免给 Glide 拿到 `/storage/...avatar?v=...` 这种被当 query 处理失败的怪路径。
        String lower = url.toLowerCase(Locale.US);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return url;
        }
        int q = url.indexOf('?');
        if (q < 0) {
            return url + "?v=" + Uri.encode(cacheKey);
        }
        // 已含 v=（在 query 部分中），就不再追加
        String query = url.substring(q + 1);
        if (query.startsWith("v=") || query.contains("&v=")) {
            return url;
        }
        return url + "&v=" + Uri.encode(cacheKey);
    }

    /**
     * 相对路径一律拼到 {@link #baseUrl}；已是 {@code http(s)} 则原样返回（与官方唐僧叨叨原始逻辑一致）。
     * <p>依赖 {@link #baseUrl} 已由 {@link #initBaseURL} 用正确 {@code apiURL} 初始化；与 Web 的 apiURL 须同主机、同端口（例如 {@code http://IP:8090/v1} 类配置），否则仍可能出现错链、缺端口或路径粘连等问题。</p>
     * <p>若后端只返回 {@code sticker/...} 而无 {@code file/preview/}，需后端改返回路径或路由，否则易 HTTP 404。</p>
     * <p><b>与后端的约定：</b>{@link #baseUrl} 以 {@code /v1/} 结尾；若接口返回的相对路径以 {@code /} 开头（如 {@code /file/preview/...}），
     * 客户端会拼成 {@code .../v1//file/...}，部分网关会 404。应在后端统一返回<b>无前导斜杠</b>的路径（如 {@code file/preview/sticker/...}），
     * 或在网关层将请求路径中的连续 {@code //} 规范为单 {@code /}。</p>
     * <p>恢复「预览分流」版：见 {@code wkbase/backup/WKApiConfig.java.showurl-with-preview-routing-20260328}。</p>
     */
    public static String getShowUrl(String url) {
        if (TextUtils.isEmpty(url) || url.startsWith("http") || url.startsWith("HTTP")) {
            return url;
        } else {
            return baseUrl + url;
        }
    }

    /**
     * 将服务端返回的「文件预览」相对路径转为完整 URL，供 Glide 等加载。
     * <p>与 {@code GET /v1/wallet/recharge/channels} 的 {@code qr_image_url} 及表情包等资源约定一致：</p>
     * <pre>{@code 完整地址 = baseUrl + "file/preview/" + 相对路径}</pre>
     * <p>若相对路径已以 {@code file/preview/} 开头，则只拼 {@link #baseUrl}，不再重复前置 {@code file/preview/}；
     * 否则拼为 {@code baseUrl + file/preview/ + 相对路径}。路径中含中文等对每段做 {@link Uri#encode}。</p>
     * <p>已是 {@code http(s)} 或 {@code data:image} 的地址原样返回；{@link #baseUrl} 已含末尾 {@code /v1/}，勿再重复拼版本号。</p>
     */
    public static String getFilePreviewShowUrl(String relativePath) {
        if (TextUtils.isEmpty(relativePath)) {
            return relativePath;
        }
        String p = relativePath.trim();
        if (p.startsWith("http://") || p.startsWith("https://") || p.startsWith("HTTP")
                || p.startsWith("data:image")) {
            return collapseDuplicateV1InUrl(p);
        }
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        p = stripLeadingV1FromRelativePath(p);
        // 服务端若已返回「file/preview/...」，勿再前置一段 file/preview，否则成 .../file/preview/file/preview/... 导致 404
        String lower = p.toLowerCase(Locale.US);
        String pathForEncode;
        if (lower.startsWith("file/preview/") || lower.equals("file/preview")) {
            pathForEncode = p;
        } else {
            pathForEncode = "file/preview/" + p;
        }
        String encoded = encodePathSegmentsForUrl(pathForEncode);
        return collapseDuplicateV1InUrl(baseUrl + encoded);
    }

    /** 对路径按 {@code /} 分段编码，保留斜杠，避免中文文件名导致加载失败。 */
    private static String encodePathSegmentsForUrl(String path) {
        if (TextUtils.isEmpty(path)) {
            return path;
        }
        String[] segs = path.split("/", -1);
        StringBuilder sb = new StringBuilder(path.length() + 8);
        for (int i = 0; i < segs.length; i++) {
            if (i > 0) {
                sb.append('/');
            }
            if (segs[i].isEmpty()) {
                continue;
            }
            sb.append(Uri.encode(segs[i]));
        }
        return sb.toString();
    }

    /**
     * 充值渠道收款码图：相对路径走 {@link #getFilePreviewShowUrl}，其余走 {@link #getShowUrl}，避免误伤非预览存储的相对路径。
     */
    public static String getRechargeChannelQrImageLoadUrl(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return raw;
        }
        String t = normalizeJsonWrappedUrl(raw.trim());
        if (t.startsWith("http://") || t.startsWith("https://") || t.startsWith("HTTP")
                || t.startsWith("data:image")) {
            return collapseDuplicateV1InUrl(t);
        }
        String lower = t.toLowerCase(Locale.US);
        if (lower.startsWith("file/preview") || lower.contains("recharge_qr")) {
            return getFilePreviewShowUrl(t);
        }
        return getShowUrl(t);
    }

    /** 去掉 JSON 字符串首尾引号等，避免接口把 URL 当字符串序列化后带引号导致 Glide/OkHttp 请求失败 */
    private static String normalizeJsonWrappedUrl(String t) {
        if (TextUtils.isEmpty(t)) {
            return t;
        }
        if ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'"))) {
            return t.substring(1, t.length() - 1).trim();
        }
        return t;
    }

    /**
     * {@link #baseUrl} 已含 {@code /v1/}；相对路径若再带 {@code v1/} 会拼成双 v1，先去掉一层。
     */
    private static String stripLeadingV1FromRelativePath(String path) {
        if (TextUtils.isEmpty(path)) {
            return path;
        }
        String t = path;
        while (true) {
            String lower = t.toLowerCase(Locale.US);
            if (lower.startsWith("v1/")) {
                t = t.substring(3);
                continue;
            }
            break;
        }
        return t;
    }

    /**
     * 兜底：折叠路径中的 {@code /v1/v1/} 为 {@code /v1/}；{@code ?} 之后不处理。
     */
    private static String collapseDuplicateV1InUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        int q = url.indexOf('?');
        String main = q >= 0 ? url.substring(0, q) : url;
        String suffix = q >= 0 ? url.substring(q) : "";
        String out = main;
        while (out.contains("/v1/v1/")) {
            out = out.replace("/v1/v1/", "/v1/");
        }
        if (out.endsWith("/v1/v1")) {
            out = out.substring(0, out.length() - 3);
        }
        return out + suffix;
    }

}
