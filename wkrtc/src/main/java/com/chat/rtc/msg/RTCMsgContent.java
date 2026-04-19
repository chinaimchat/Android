package com.chat.rtc.msg;

import com.chat.base.WKBaseApplication;
import com.chat.rtc.R;
import rtc.WKRTCCallType;
import com.xinbida.wukongim.msgmodel.WKMessageContent;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 5/17/21 11:21 AM
 */
public class RTCMsgContent extends WKMessageContent {
    public int callType;
    public int second;
    public int resultType;//1：正常通话2：取消3：拒绝

    public RTCMsgContent() {
        type = WKRTCType.WK_P2P_CALL;
    }

    @Override
    public WKMessageContent decodeMsg(JSONObject jsonObject) {
        if (jsonObject.has("call_type"))
            callType = jsonObject.optInt("call_type");
        if (jsonObject.has("second"))
            second = jsonObject.optInt("second");
        if (jsonObject.has("result_type"))
            resultType = jsonObject.optInt("result_type");
        return this;
    }

    @Override
    public JSONObject encodeMsg() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("call_type", callType);
            jsonObject.put("second", second);
            jsonObject.put("result_type", resultType);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public String getSearchableWord() {
        return WKBaseApplication.getInstance().getContext().getString(callType == WKRTCCallType.video ? R.string.last_msg_audio_call : R.string.last_msg_voice_call);
    }

    @Override
    public String getDisplayContent() {
        return WKBaseApplication.getInstance().getContext().getString(callType == WKRTCCallType.video ? R.string.last_msg_audio_call : R.string.last_msg_voice_call);
    }
}
