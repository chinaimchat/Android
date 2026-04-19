package com.chat.base.utils;

import android.text.TextUtils;

import java.util.Locale;

public final class WKErrorMessageNormalizer {

    private WKErrorMessageNormalizer() {
    }

    public static String normalize(String rawMsg) {
        if (TextUtils.isEmpty(rawMsg)) {
            return rawMsg;
        }
        String lower = rawMsg.toLowerCase(Locale.ROOT);
        if (lower.contains("手机号不合法")
                || lower.contains("手机号码不合法")
                || lower.contains("illegal mobile phone number")
                || lower.contains("invalid mobile phone number")
                || lower.contains("phone number is invalid")) {
            if (Locale.getDefault().getLanguage().startsWith("zh")) {
                return "账号输入有误，请检查后重试";
            }
            return "Invalid account input. Please check and try again";
        }
        return rawMsg;
    }
}
