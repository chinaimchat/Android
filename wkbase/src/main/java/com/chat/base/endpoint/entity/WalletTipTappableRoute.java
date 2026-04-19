package com.chat.base.endpoint.entity;

import android.content.Context;

/**
 * 系统提示（1011/1012）句末「红包」「转账」点击后打开详情；由钱包模块注册 {@code wallet_tip_open_detail} 处理。
 */
public final class WalletTipTappableRoute {

    public static final String ENDPOINT_SID = "wallet_tip_open_detail";

    public final Context context;
    public final String packetNo;
    public final String channelId;
    public final byte channelType;
    public final String transferNo;

    private WalletTipTappableRoute(
            Context context,
            String packetNo,
            String channelId,
            byte channelType,
            String transferNo
    ) {
        this.context = context;
        this.packetNo = packetNo;
        this.channelId = channelId;
        this.channelType = channelType;
        this.transferNo = transferNo;
    }

    public static WalletTipTappableRoute redPacket(Context ctx, String packetNo, String channelId, byte channelType) {
        return new WalletTipTappableRoute(ctx, packetNo, channelId, channelType, null);
    }

    public static WalletTipTappableRoute transfer(Context ctx, String transferNo) {
        return new WalletTipTappableRoute(ctx, null, null, (byte) 0, transferNo);
    }
}
