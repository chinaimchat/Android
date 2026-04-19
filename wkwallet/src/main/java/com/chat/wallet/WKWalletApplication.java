package com.chat.wallet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.chat.base.config.WKConfig;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.ChatFunctionMenu;
import com.chat.base.endpoint.entity.PersonalInfoMenu;
import com.chat.base.endpoint.entity.WalletTipTappableRoute;
import com.chat.base.msg.IConversationContext;
import com.chat.base.msgitem.WKContentType;
import com.chat.base.msgitem.WKMsgItemViewManager;
import com.chat.base.utils.WKReader;
import com.chat.base.utils.WKToastUtils;
import com.chat.wallet.msg.WKRedPacketContent;
import com.chat.wallet.msg.WKRedPacketProvider;
import com.chat.wallet.msg.WKTransferContent;
import com.chat.wallet.msg.WKTransferProvider;
import com.chat.wallet.msg.WKWalletBalanceSyncContent;
import com.chat.wallet.msg.WKWalletBalanceSyncProvider;
import com.chat.wallet.receive.WalletReceiveQrContract;
import com.chat.wallet.redpacket.RedPacketDetailActivity;
import com.chat.wallet.redpacket.SendRedPacketActivity;
import com.chat.wallet.transfer.TransferActivity;
import com.chat.wallet.transfer.TransferDetailActivity;
import com.chat.wallet.util.RechargeChannelsCache;
import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.entity.WKChannelType;
import com.xinbida.wukongim.entity.WKMsg;

import java.lang.ref.WeakReference;
import java.util.Map;

public class WKWalletApplication {

    private WKWalletApplication() {
    }

    private static class Holder {
        static final WKWalletApplication INSTANCE = new WKWalletApplication();
    }

    public static WKWalletApplication getInstance() {
        return Holder.INSTANCE;
    }

    private WeakReference<Context> mContext;

    public Context getContext() {
        return mContext != null ? mContext.get() : null;
    }

    public void init(Context context) {
        mContext = new WeakReference<>(context);

        registerContentModels();
        registerEndpoints();
        registerPersonalCenterMenus();
        registerChatFunctionMenus();
        registerMsgProviders();
        registerWalletBalanceImSyncListener();
    }

    private void registerContentModels() {
        WKIM.getInstance().getMsgManager().registerContentMsg(WKRedPacketContent.class);
        WKIM.getInstance().getMsgManager().registerContentMsg(WKTransferContent.class);
        WKIM.getInstance().getMsgManager().registerContentMsg(WKWalletBalanceSyncContent.class);
    }

    private void registerEndpoints() {
        Context ctx = mContext.get();
        if (ctx == null) return;

        // 登出 / 401 时由 wkuikit、wkbase 调用，避免展示上一账号的充值渠道缓存
        EndpointManager.getInstance().setMethod("wk_wallet_logout_cleanup", object -> {
            RechargeChannelsCache.clear();
            return null;
        });

        EndpointManager.getInstance().setMethod(WalletTipTappableRoute.ENDPOINT_SID, object -> {
            if (!(object instanceof WalletTipTappableRoute)) {
                return null;
            }
            WalletTipTappableRoute r = (WalletTipTappableRoute) object;
            Context c = r.context;
            if (c == null) {
                return null;
            }
            try {
                if (!TextUtils.isEmpty(r.packetNo)) {
                    Intent i = new Intent(c, RedPacketDetailActivity.class);
                    i.putExtra("packet_no", r.packetNo);
                    if (!TextUtils.isEmpty(r.channelId)) {
                        i.putExtra("channel_id", r.channelId);
                        i.putExtra("channel_type", (int) r.channelType);
                    }
                    if (!(c instanceof Activity)) {
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                    c.startActivity(i);
                } else if (!TextUtils.isEmpty(r.transferNo)) {
                    Intent i = new Intent(c, TransferDetailActivity.class);
                    i.putExtra("transfer_no", r.transferNo);
                    if (!(c instanceof Activity)) {
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                    c.startActivity(i);
                }
            } catch (Exception ignored) {
            }
            return null;
        });

        EndpointManager.getInstance().setMethod("wk_scan_handle_mtp", object -> {
            if (!(object instanceof Map)) {
                return false;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) object;
            Object actObj = map.get("activity");
            Object uriObj = map.get("uri");
            if (!(actObj instanceof AppCompatActivity) || !(uriObj instanceof String)) {
                return false;
            }
            AppCompatActivity activity = (AppCompatActivity) actObj;
            String raw = (String) uriObj;
            if (!raw.startsWith("mtp://")) {
                return false;
            }
            String toUid = WalletReceiveQrContract.parseRecipientUid(raw);
            if (toUid == null) {
                return false;
            }
            String myUid = WKConfig.getInstance().getUid();
            if (toUid.equals(myUid)) {
                WKToastUtils.getInstance().showToast(activity.getString(R.string.wallet_receive_qr_scan_self));
                return true;
            }
            Intent intent = new Intent(activity, TransferActivity.class);
            intent.putExtra("channel_type", WKChannelType.PERSONAL);
            intent.putExtra("to_uid", toUid);
            intent.putExtra("from_wallet_receive_qr", true);
            activity.startActivity(intent);
            return true;
        });
    }

    private void registerMsgProviders() {
        WKMsgItemViewManager.getInstance().addChatItemViewProvider(
                WKContentType.WK_REDPACKET, new WKRedPacketProvider());
        WKMsgItemViewManager.getInstance().addChatItemViewProvider(
                WKContentType.WK_TRANSFER, new WKTransferProvider());
        WKMsgItemViewManager.getInstance().addChatItemViewProvider(
                WKContentType.WK_WALLET_BALANCE_SYNC, new WKWalletBalanceSyncProvider());
    }

    private void registerPersonalCenterMenus() {
        Context ctx = getContext();
        if (ctx == null) {
            return;
        }
        EndpointManager.getInstance().setMethod("wallet_personal_center", EndpointCategory.personalCenter, 1000,
                object -> new PersonalInfoMenu(R.drawable.ic_wallet_menu, ctx.getString(R.string.wallet_title), 2, () -> {
                    Intent intent = new Intent(ctx, com.chat.wallet.ui.WalletActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(intent);
                }));
    }

    private void registerChatFunctionMenus() {
        Context ctx = getContext();
        if (ctx == null) {
            return;
        }
        EndpointManager.getInstance().setMethod("wallet_chat_redpacket", EndpointCategory.chatFunction, 110,
                object -> buildRedPacketMenu(ctx, object));
        EndpointManager.getInstance().setMethod("wallet_chat_transfer", EndpointCategory.chatFunction, 111,
                object -> buildTransferMenu(ctx, object));
    }

    private ChatFunctionMenu buildRedPacketMenu(Context ctx, Object object) {
        if (!(object instanceof IConversationContext)) {
            return null;
        }
        IConversationContext conversationContext = (IConversationContext) object;
        return new ChatFunctionMenu("wallet_redpacket", com.chat.base.R.drawable.icon_func_wallet_redpacket,
                ctx.getString(R.string.redpacket_send), iConversationContext -> {
            Intent intent = new Intent(iConversationContext.getChatActivity(), SendRedPacketActivity.class);
            intent.putExtra("channel_id", conversationContext.getChatChannelInfo().channelID);
            intent.putExtra("channel_type", (int) conversationContext.getChatChannelInfo().channelType);
            iConversationContext.getChatActivity().startActivity(intent);
        });
    }

    private ChatFunctionMenu buildTransferMenu(Context ctx, Object object) {
        if (!(object instanceof IConversationContext)) {
            return null;
        }
        IConversationContext conversationContext = (IConversationContext) object;
        if (conversationContext.getChatChannelInfo() == null) {
            return null;
        }
        byte channelType = conversationContext.getChatChannelInfo().channelType;
        if (channelType != WKChannelType.PERSONAL && channelType != WKChannelType.GROUP) {
            return null;
        }
        return new ChatFunctionMenu("wallet_transfer", com.chat.base.R.drawable.icon_func_wallet_transfer,
                ctx.getString(R.string.transfer_send), iConversationContext -> {
            Intent intent = new Intent(iConversationContext.getChatActivity(), TransferActivity.class);
            intent.putExtra("channel_id", conversationContext.getChatChannelInfo().channelID);
            intent.putExtra("channel_type", (int) conversationContext.getChatChannelInfo().channelType);
            if (conversationContext.getChatChannelInfo().channelType == WKChannelType.PERSONAL) {
                intent.putExtra("to_uid", conversationContext.getChatChannelInfo().channelID);
            }
            iConversationContext.getChatActivity().startActivity(intent);
        });
    }

    private void registerWalletBalanceImSyncListener() {
        WKIM.getInstance().getMsgManager().addOnNewMsgListener("wallet_balance_im_sync", msgList -> {
            if (!WKReader.isNotEmpty(msgList)) {
                return;
            }
            for (int i = 0, n = msgList.size(); i < n; i++) {
                WKMsg m = msgList.get(i);
                if (m != null && m.type == WKContentType.WK_WALLET_BALANCE_SYNC) {
                    WalletIMBalanceSyncHandler.handle(m);
                }
            }
        });
    }
}
