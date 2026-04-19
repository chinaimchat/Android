package com.chat.rtc.msg;



import com.xinbida.wukongim.msgmodel.WKMessageContent;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 5/11/21 2:41 PM
 */
public class RTCDataContent extends WKMessageContent {
    public RTCDataContent() {
        type = WKRTCType.wk_video_call_data;
    }

    public RTCDataContent(String content) {
        type = WKRTCType.wk_video_call_data;
        this.content = content;
    }


    @Override
    public JSONObject encodeMsg() {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("content", content);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public WKMessageContent decodeMsg(JSONObject jsonObject) {
        if (jsonObject != null) {
            if (jsonObject.has("content")) {
                this.content = jsonObject.optString("content");
            }
        }
        return this;
    }
}
