package com.chat.rtc.msg;

import com.xinbida.wukongim.msgmodel.WKMessageContent;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 6/11/21 4:10 PM
 */
public class SwitchVideoRespondContent extends WKMessageContent {
    public int status;

    public SwitchVideoRespondContent() {
        type = WKRTCType.wk_video_switch_video_respond;
    }

    @Override
    public JSONObject encodeMsg() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("status", status);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public WKMessageContent decodeMsg(JSONObject jsonObject) {
        if (jsonObject.has("status")) status = jsonObject.optInt("status");
        return this;
    }
}
