package com.chat.rtc.msg;

import com.chat.base.msgitem.WKContentType;
import com.xinbida.wukongim.msgmodel.WKMessageContent;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 5/17/21 12:28 PM
 * 多人音视频
 */
public class MultiMsgContent extends WKMessageContent {
    public MultiMsgContent() {
        type = WKContentType.videoCallGroup;
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
        content = jsonObject.optString("content");
        return this;
    }

    @Override
    public String getDisplayContent() {
        return content;
    }

    @Override
    public String getSearchableWord() {
        return content;
    }
}
