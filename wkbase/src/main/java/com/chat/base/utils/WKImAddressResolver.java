package com.chat.base.utils;

import android.text.TextUtils;

import com.alibaba.fastjson.JSONObject;

import java.util.Locale;

/**
 * IM 集群地址解析：与 iOS WKImTcpAddrFromAPIPayload / WKParseImHostPort 对齐，
 * 兼容 tcp_addr 在根节点、data、result 内及 camelCase，并按最后一个冒号拆分 host/port。
 */
public final class WKImAddressResolver {

    private WKImAddressResolver() {
    }

    public static final class HostPort {
        public final String host;
        public final int port;

        public HostPort(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }

    /**
     * 将接口返回体（Map/JSONObject/JavaBean 等）统一成 JSONObject 再取地址。
     */
    public static String resolveTcpAddr(Object payload) {
        if (payload == null) {
            return null;
        }
        JSONObject root;
        if (payload instanceof JSONObject) {
            root = (JSONObject) payload;
        } else {
            try {
                root = (JSONObject) JSONObject.toJSON(payload);
            } catch (Exception e) {
                return null;
            }
        }
        return resolveTcpAddrFromRoot(root);
    }

    private static String resolveTcpAddrFromRoot(JSONObject root) {
        if (root == null || root.isEmpty()) {
            return null;
        }
        String addr = firstNonEmptyString(root, "tcp_addr", "tcpAddr");
        if (TextUtils.isEmpty(addr)) {
            Object data = root.get("data");
            addr = tcpAddrFromNested(data);
        }
        if (TextUtils.isEmpty(addr)) {
            Object res = root.get("result");
            addr = tcpAddrFromNested(res);
        }
        return normalizeTcpAddr(addr);
    }

    private static String tcpAddrFromNested(Object nested) {
        if (nested == null) {
            return null;
        }
        if (nested instanceof JSONObject) {
            return firstNonEmptyString((JSONObject) nested, "tcp_addr", "tcpAddr");
        }
        if (nested instanceof String) {
            String s = ((String) nested).trim();
            if (s.startsWith("{")) {
                try {
                    JSONObject o = JSONObject.parseObject(s);
                    return firstNonEmptyString(o, "tcp_addr", "tcpAddr");
                } catch (Exception ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String firstNonEmptyString(JSONObject o, String... keys) {
        if (o == null) {
            return null;
        }
        for (String k : keys) {
            String v = objectToTrimmedString(o.get(k));
            if (!TextUtils.isEmpty(v)) {
                return v;
            }
        }
        return null;
    }

    private static String objectToTrimmedString(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof String) {
            return ((String) v).trim();
        }
        return String.valueOf(v).trim();
    }

    private static String normalizeTcpAddr(String addr) {
        if (TextUtils.isEmpty(addr)) {
            return null;
        }
        String s = addr.trim();
        String lower = s.toLowerCase(Locale.US);
        if (lower.startsWith("tcp://")) {
            s = s.substring(6);
        }
        return s.length() > 0 ? s : null;
    }

    /**
     * 按最后一个冒号拆分 host 与端口，避免 tcp://host:port 或 IPv6 多冒号时误解析。
     */
    public static HostPort parseHostPort(String raw) {
        String s = normalizeTcpAddr(raw);
        if (TextUtils.isEmpty(s)) {
            return null;
        }
        int idx = s.lastIndexOf(':');
        if (idx <= 0 || idx >= s.length() - 1) {
            return null;
        }
        String host = s.substring(0, idx).trim();
        String portStr = s.substring(idx + 1).trim();
        int slash = portStr.indexOf('/');
        if (slash >= 0) {
            portStr = portStr.substring(0, slash).trim();
        }
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            return null;
        }
        if (host.isEmpty() || port <= 0 || port > 65535) {
            return null;
        }
        return new HostPort(host, port);
    }
}
