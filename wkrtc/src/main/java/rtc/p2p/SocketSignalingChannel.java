/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package rtc.p2p;

import static owt.p2p.OwtP2PError.P2P_CONN_SERVER_UNKNOWN;

import android.text.TextUtils;


import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import io.socket.client.Socket;
import io.socket.emitter.Emitter.Listener;
import owt.base.ActionCallback;
import owt.base.Const;
import owt.base.OwtError;
import owt.p2p.OwtP2PError;
import owt.p2p.SignalingChannelInterface;
import rtc.utils.WKLogger;
import rtc.utils.WKRTCManager;

/**
 * Socket.IO implementation of P2P signaling channel.
 */
public class SocketSignalingChannel implements SignalingChannelInterface {
    private static final String TAG = "OWT-SocketClient";
    private final String CLIENT_CHAT_TYPE = "owt-message";
    private final String SERVER_AUTHENTICATED = "server-authenticated";
    private final String FORCE_DISCONNECT = "server-disconnect";
    private final String CLIENT_TYPE = "&clientType=";
    private final String CLIENT_TYPE_VALUE = "Android";
    private final String CLIENT_VERSION = "&clientVersion=";
    private final String CLIENT_VERSION_VALUE = Const.CLIENT_VERSION;

    private final int MAX_RECONNECT_ATTEMPTS = 5;
    private int reconnectAttempts = 0;
    private Socket socketIOClient;
    private List<SignalingChannelObserver> signalingChannelObservers;
    private ActionCallback<String> connectCallback;

    // Socket.IO events.
    private Listener onConnectErrorCallback = args -> {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            if (connectCallback != null) {
                connectCallback.onFailure(
                        new OwtError(P2P_CONN_SERVER_UNKNOWN.value, "connect failed"));
                connectCallback = null;
            } else {
                for (SignalingChannelObserver observer : signalingChannelObservers) {
                    observer.onServerDisconnected();
                }
            }
            reconnectAttempts = 0;
        }
    };

    private Listener onErrorCallback = args -> {
        if (connectCallback != null) {
            Pattern pattern = Pattern.compile("[0-9]*");
            if (pattern.matcher(args[0].toString()).matches()) {
                connectCallback.onFailure(
                        new OwtError(OwtP2PError.get(Integer.parseInt((String) args[0])).value,
                                "Server error"));
            } else {
                connectCallback.onFailure(new OwtError(args[0].toString()));
            }
        }
    };

    private Listener onReconnectingCallback = args -> {
        reconnectAttempts++;
    };

    private Listener onDisconnectCallback = args -> {
        for (SignalingChannelObserver observer : signalingChannelObservers) {
            observer.onServerDisconnected();
        }
    };

    // P2P server events.
    private Listener onServerAuthenticatedCallback = args -> {
        if (connectCallback != null) {
            connectCallback.onSuccess(args[0].toString());
            connectCallback = null;
        }
    };

    private Listener onForceDisconnectCallback = args -> {
        if (socketIOClient != null) {
            socketIOClient.on(Socket.EVENT_DISCONNECT, onDisconnectCallback);
            socketIOClient.io().reconnection(false);
        }
    };

    private Listener onMessageCallback = args -> {
        JSONObject argumentJsonObject = (JSONObject) args[0];
        for (SignalingChannelObserver observer : signalingChannelObservers) {
            try {
                observer.onMessage(argumentJsonObject.getString("from"),
                        argumentJsonObject.getString("data"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    public SocketSignalingChannel(IPublish iPublish) {
        this.signalingChannelObservers = new ArrayList<>();
        this.actionCallbackMap = new ConcurrentHashMap<>();
        this.iPublish = iPublish;
        WKRTCManager.getInstance().addMsgAck((clientSeq, isSuccess) -> {

            if (actionCallbackMap.get(clientSeq) != null) {
                if (isSuccess) {
                    Objects.requireNonNull(actionCallbackMap.get(clientSeq)).onSuccess(null);
                } else {
                    Objects.requireNonNull(actionCallbackMap.get(clientSeq)).onFailure(new OwtError("发送消息失败"));
                }
            } else {
                WKLogger.e(TAG,"发送消息回执中无该ID-->"+clientSeq);
            }

        });
    }

    @Override
    public void addObserver(SignalingChannelObserver observer) {
        this.signalingChannelObservers.add(observer);
    }

    @Override
    public void removeObserver(SignalingChannelObserver observer) {
        this.signalingChannelObservers.remove(observer);
    }


    @Override
    public void connect(String userInfo, ActionCallback<String> callback) {
        connectCallback = callback;
        callback.onSuccess(userInfo);
//        JSONObject loginObject;
//        String token;
//        String url;
//        try {
//            connectCallback = callback;
//            loginObject = new JSONObject(userInfo);
//            token = URLEncoder.encode(loginObject.getString("token"), "UTF-8");
//            url = loginObject.getString("host");
//            url += "?token=" + token + CLIENT_TYPE + CLIENT_TYPE_VALUE + CLIENT_VERSION
//                    + CLIENT_VERSION_VALUE;
//            if (!isValid(url)) {
//                callback.onFailure(new OwtError(P2P_CLIENT_ILLEGAL_ARGUMENT.value, "Invalid URL"));
//                return;
//            }
//            IO.Options opt = new IO.Options();
//            opt.forceNew = true;
//            opt.reconnection = true;
//            opt.reconnectionAttempts = MAX_RECONNECT_ATTEMPTS;
//            if (socketIOClient != null) {
//                Log.d(TAG, "stop reconnecting the former url");
//                socketIOClient.disconnect();
//            }
//            socketIOClient = IO.socket(url, opt);
//
//            socketIOClient.on(Socket.EVENT_CONNECT_ERROR, onConnectErrorCallback)
//                    .on(Socket.EVENT_ERROR, onErrorCallback)
//                    .on(Socket.EVENT_RECONNECTING, onReconnectingCallback)
//                    .on(CLIENT_CHAT_TYPE, onMessageCallback)
//                    .on(SERVER_AUTHENTICATED, onServerAuthenticatedCallback)
//                    .on(FORCE_DISCONNECT, onForceDisconnectCallback);
//
//            socketIOClient.connect();
//
//        } catch (JSONException e) {
//            if (callback != null) {
//                callback.onFailure(new OwtError(P2P_CLIENT_ILLEGAL_ARGUMENT.value, e.getMessage()));
//            }
//        } catch (URISyntaxException e) {
//            if (callback != null) {
//                callback.onFailure(new OwtError(P2P_CLIENT_ILLEGAL_ARGUMENT.value, e.getMessage()));
//            }
//        } catch (UnsupportedEncodingException e) {
//            if (callback != null) {
//                callback.onFailure(new OwtError(P2P_CLIENT_ILLEGAL_ARGUMENT.value, e.getMessage()));
//            }
//        }
    }

    private boolean isValid(String urlString) {
        try {
            URL url = new URL(urlString);
            return url.getPort() <= 65535;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    @Override
    public void disconnect() {
//        if (socketIOClient != null) {
//            Log.d(TAG, "Socket IO Disconnect.");
//            socketIOClient.on(Socket.EVENT_DISCONNECT, onDisconnectCallback);
//            socketIOClient.disconnect();
//            socketIOClient = null;
//        }
        for (SignalingChannelObserver observer : signalingChannelObservers) {
            observer.onServerDisconnected();
        }
    }

    private final ConcurrentHashMap<Long, ActionCallback<Void>> actionCallbackMap;

    private final IPublish iPublish;

    @Override
    public void sendMessage(String peerId, String message, final ActionCallback<Void> callback) {
        //  Log.e("发送消息", "--->" + peerId);
//        Log.e("发送消息内容", message);
        if (!TextUtils.isEmpty(message)) {
            try {
                JSONObject jsonObject = new JSONObject(message);
                JSONObject dataJson = jsonObject.optJSONObject("data");
                if (dataJson != null && dataJson.has("type")) {
                    String type = dataJson.optString("type");
                    if (type.equals("answer")) {
                        // 推流
                        iPublish.onPublish();
                    }
                }
            } catch (JSONException e) {
                WKLogger.e(TAG,"发送rtc消息解析错误");
            }
        }
        WKLogger.e(TAG,"发送rtc的数据");
//        if (!WKRTCManager.getInstance().isCalling) {
//            WKLogger.e(TAG,"发送rtc消息断开");
//            disconnect();
//            return;
//        }
        WKRTCManager.getInstance().getSendMsgListener().sendRTCMsg(peerId, message, clientSeq -> {
            actionCallbackMap.put(clientSeq, callback);
        });
//        WKRTCApplication.getInstance().sendRTCMsg(peerId, message, clientSeq -> {
//            actionCallbackMap.put(clientSeq, callback);
//        });
        // callback.onSuccess(null);
//        if (socketIOClient == null) {
//            Log.d(TAG, "socketIOClient is not established.");
//            return;
//        }
//        JSONObject jsonObject = new JSONObject();
//        try {
//            jsonObject.put("to", peerId);
//            jsonObject.put("data", message);
//            socketIOClient.emit(CLIENT_CHAT_TYPE, jsonObject, (Ack) args -> {
//                if (args == null || args.length != 0) {
//                    if (callback != null) {
//                        callback.onFailure(new OwtError("Failed to send message."));
//                    }
//                } else {
//                    if (callback != null) {
//                        callback.onSuccess(null);
//                    }
//                }
//            });
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }

    public interface IPublish {
        void onPublish();
    }
}
