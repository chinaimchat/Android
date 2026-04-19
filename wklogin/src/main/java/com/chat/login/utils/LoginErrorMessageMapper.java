package com.chat.login.utils;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.chat.login.R;

import java.util.Locale;

public final class LoginErrorMessageMapper {

    private LoginErrorMessageMapper() {
    }

    public static String map(@NonNull Context context, String rawMsg) {
        if (TextUtils.isEmpty(rawMsg)) {
            return rawMsg;
        }
        String lower = rawMsg.toLowerCase(Locale.ROOT);
        if (lower.contains("手机号不合法")
                || lower.contains("手机号码不合法")
                || lower.contains("illegal mobile phone number")
                || lower.contains("invalid mobile phone number")
                || lower.contains("phone number is invalid")) {
            return context.getString(R.string.account_input_error);
        }
        return rawMsg;
    }
}
