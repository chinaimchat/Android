package com.chat.uikit.user.service;

import com.chat.base.base.WKBaseModel;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.net.IRequestResultListener;
import com.chat.uikit.enity.InviteCodeEntity;

public class InviteCodeModel extends WKBaseModel {
    private InviteCodeModel() {
    }

    private static class InviteCodeModelBinder {
        static final InviteCodeModel model = new InviteCodeModel();
    }

    public static InviteCodeModel getInstance() {
        return InviteCodeModelBinder.model;
    }

    public interface IInviteCodeResult {
        void onResult(int code, String msg, InviteCodeEntity entity);
    }

    public interface ICommonResult {
        void onResult(int code, String msg);
    }

    public void getInviteCode(IInviteCodeResult callback) {
        request(createService(InviteCodeService.class).getInviteCode(), new IRequestResultListener<>() {
            @Override
            public void onSuccess(InviteCodeEntity result) {
                if (callback != null) {
                    callback.onResult(HttpResponseCode.success, "", result);
                }
            }

            @Override
            public void onFail(int code, String msg) {
                if (callback != null) {
                    callback.onResult(code, msg, null);
                }
            }
        });
    }

    public void switchInviteStatus(ICommonResult callback) {
        request(createService(InviteCodeService.class).switchInviteStatus(), new IRequestResultListener<>() {
            @Override
            public void onSuccess(com.chat.base.net.entity.CommonResponse result) {
                if (callback != null) {
                    callback.onResult(result.status, result.msg);
                }
            }

            @Override
            public void onFail(int code, String msg) {
                if (callback != null) {
                    callback.onResult(code, msg);
                }
            }
        });
    }
}
