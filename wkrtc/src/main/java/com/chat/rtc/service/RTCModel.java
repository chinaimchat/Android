package com.chat.rtc.service;

import com.alibaba.fastjson.JSONObject;
import com.chat.base.base.WKBaseModel;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.net.ICommonListener;
import com.chat.base.net.IRequestResultListener;
import com.chat.base.net.entity.CommonResponse;
import com.chat.rtc.entity.RTCToken;
import com.chat.rtc.entity.VideoRoom;

import java.util.List;

import io.reactivex.rxjava3.annotations.NonNull;

/**
 * 5/7/21 6:19 PM
 */
public class RTCModel extends WKBaseModel {
    private RTCModel() {

    }

    private static class LiMVideoCallModelBinder {
        static final RTCModel model = new RTCModel();
    }

    public static RTCModel getInstance() {
        return LiMVideoCallModelBinder.model;
    }

    public void createRoomID(String name, String channelID, byte channelType, List<String> uids, final IGetRoomID iGetRoomID) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("channel_id", channelID);
        jsonObject.put("channel_type", channelType);
        jsonObject.put("uids", uids);
        jsonObject.put("invite_on", 1);
        request(createService(RTCService.class).getRoomID(jsonObject), new IRequestResultListener<VideoRoom>() {
            @Override
            public void onSuccess(VideoRoom result) {
                iGetRoomID.onBack(result.room_id, HttpResponseCode.success, "");
            }

            @Override
            public void onFail(int code, String msg) {
                iGetRoomID.onBack("", code, msg);
            }
        });
    }

    public void getVideoCallToken(String roomID, IGetVideoCallToken iGetVideoCallToken) {
        request(createService(RTCService.class).getRTCToken(roomID), new IRequestResultListener<RTCToken>() {
            @Override
            public void onSuccess(RTCToken result) {
                iGetVideoCallToken.onBack(result.token, HttpResponseCode.success, "");
            }

            @Override
            public void onFail(int code, String msg) {
                iGetVideoCallToken.onBack("", code, msg);
            }
        });
    }

    public void inviteP2PCall(int callType, String toUID, final ICommonListener iCommonListener) {
        JSONObject j = new JSONObject();
        j.put("call_type", callType);
        j.put("to_uid", toUID);
        request(createService(RTCService.class).inviteP2PCall(j), new IRequestResultListener<CommonResponse>() {
            @Override
            public void onSuccess(CommonResponse result) {
                if (iCommonListener != null)
                    iCommonListener.onResult(HttpResponseCode.success, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                if (iCommonListener != null)
                    iCommonListener.onResult(code, msg);
            }
        });
    }

    public void acceptP2PCall(int callType, String fromUID, final ICommonListener iCommonListener) {
        JSONObject j = new JSONObject();
        j.put("call_type", callType);
        j.put("from_uid", fromUID);
        request(createService(RTCService.class).acceptP2PCall(j), new IRequestResultListener<CommonResponse>() {
            @Override
            public void onSuccess(CommonResponse result) {
                if (iCommonListener != null)
                    iCommonListener.onResult(HttpResponseCode.success, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                if (iCommonListener != null)
                    iCommonListener.onResult(code, msg);
            }
        });
    }

    public void multiRefuse(String roomID, final ICommonListener iCommonListener) {
        request(createService(RTCService.class).multiRefuse(roomID), new IRequestResultListener<CommonResponse>() {
            @Override
            public void onSuccess(CommonResponse result) {
                if (iCommonListener != null)
                    iCommonListener.onResult(HttpResponseCode.success, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                if (iCommonListener != null)
                    iCommonListener.onResult(code, msg);
            }
        });
    }

    public void invokeJoinRoom(String roomID, List<String> uids, @NonNull final ICommonListener iCommonListener) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("uids", uids);
        request(createService(RTCService.class).invokeJoinRoom(roomID, jsonObject), new IRequestResultListener<CommonResponse>() {
            @Override
            public void onSuccess(CommonResponse result) {
                iCommonListener.onResult(result.status, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                iCommonListener.onResult(code, msg);
            }
        });
    }

    public void multiJoined(String roomID, final ICommonListener iCommonListener) {
        request(createService(RTCService.class).multiJoined(roomID), new IRequestResultListener<CommonResponse>() {
            @Override
            public void onSuccess(CommonResponse result) {
                if (iCommonListener != null)
                    iCommonListener.onResult(HttpResponseCode.success, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                if (iCommonListener != null)
                    iCommonListener.onResult(code, msg);
            }
        });
    }

    public void multiHangup(String roomID, final ICommonListener iCommonListener) {
        request(createService(RTCService.class).multiHangup(roomID), new IRequestResultListener<CommonResponse>() {
            @Override
            public void onSuccess(CommonResponse result) {
                if (iCommonListener != null)
                    iCommonListener.onResult(HttpResponseCode.success, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                if (iCommonListener != null)
                    iCommonListener.onResult(code, msg);
            }
        });
    }

    public void refuseP2PCall(int callType, String fromUID, final ICommonListener iCommonListener) {
        JSONObject j = new JSONObject();
        j.put("uid", fromUID);
        j.put("call_type", callType);
        request(createService(RTCService.class).refuseP2PCall(j), new IRequestResultListener<CommonResponse>() {
            @Override
            public void onSuccess(CommonResponse result) {
                if (iCommonListener != null)
                    iCommonListener.onResult(HttpResponseCode.success, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                if (iCommonListener != null)
                    iCommonListener.onResult(code, msg);
            }
        });
    }

    public void cancelP2PCall(int callType, String fromUID, final ICommonListener iCommonListener) {
        JSONObject j = new JSONObject();
        j.put("uid", fromUID);
        j.put("call_type", callType);
        request(createService(RTCService.class).cancelP2PCall(j), new IRequestResultListener<CommonResponse>() {
            @Override
            public void onSuccess(CommonResponse result) {
                if (iCommonListener != null)
                    iCommonListener.onResult(HttpResponseCode.success, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                if (iCommonListener != null)
                    iCommonListener.onResult(code, msg);
            }
        });
    }

    public void hangupP2PCall(String fromUID, int second, int callType, int isCaller, final ICommonListener iCommonListener) {
        JSONObject j = new JSONObject();
        j.put("uid", fromUID);
        j.put("second", second);
        j.put("call_type", callType);
        j.put("is_caller", isCaller);
        request(createService(RTCService.class).hangupP2PCall(j), new IRequestResultListener<CommonResponse>() {
            @Override
            public void onSuccess(CommonResponse result) {
                if (iCommonListener != null)
                    iCommonListener.onResult(HttpResponseCode.success, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                if (iCommonListener != null)
                    iCommonListener.onResult(code, msg);
            }
        });
    }

    public interface IGetRoomID {
        void onBack(String roomID, int code, String msg);
    }

    public interface IGetVideoCallToken {
        void onBack(String token, int code, String msg);
    }
}
