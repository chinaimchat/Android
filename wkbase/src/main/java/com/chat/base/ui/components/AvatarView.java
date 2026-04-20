package com.chat.base.ui.components;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.chat.base.R;
import com.chat.base.config.WKApiConfig;
import com.chat.base.config.WKConstants;
import com.chat.base.glide.GlideUtils;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.LayoutHelper;
import com.chat.base.utils.WKTimeUtils;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.entity.WKChannel;

import java.io.File;

public class AvatarView extends FrameLayout {
    public ShapeableImageView imageView;
    public TextView defaultAvatarTv;
    public View spotView;
    public TextView onlineTv;

    public AvatarView(Context context) {
        super(context);
        init();
    }

    public AvatarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AvatarView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        imageView = new ShapeableImageView(getContext());
//        imageView.setStrokeColorResource(R.color.borderColor);
//        imageView.setStrokeWidth(AndroidUtilities.dp(1));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setPadding(AndroidUtilities.dp(0.1f), AndroidUtilities.dp(0.1f), AndroidUtilities.dp(0.1f), AndroidUtilities.dp(0.1f));
        imageView.setImageResource(R.drawable.default_view_bg);

        spotView = new View(getContext());
        spotView.setBackgroundResource(R.drawable.online_spot);
        spotView.setVisibility(GONE);

        defaultAvatarTv = new TextView(getContext());
        defaultAvatarTv.setTextSize(20f);
        defaultAvatarTv.setTextColor(0xffffffff);
        defaultAvatarTv.setBackgroundResource(R.drawable.shape_rand);
        defaultAvatarTv.setTypeface(Typeface.DEFAULT_BOLD);
        defaultAvatarTv.setVisibility(GONE);
        defaultAvatarTv.setGravity(Gravity.CENTER);

        onlineTv = new TextView(getContext());
        onlineTv.setTextColor(0xff02F507);
        onlineTv.setTextSize(9f);
        onlineTv.setPadding(AndroidUtilities.dp(3), 0, AndroidUtilities.dp(3), 0);
        onlineTv.setBackgroundResource(R.drawable.online_bg);
        onlineTv.setVisibility(INVISIBLE);
        addView(imageView, LayoutHelper.createFrame(40, 40, Gravity.CENTER));
        addView(onlineTv, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.END, 0, 0, 0, 0));
        addView(spotView, LayoutHelper.createFrame(15, 15, Gravity.BOTTOM | Gravity.END, 0, 0, 0, 0));
        addView(defaultAvatarTv, LayoutHelper.createFrame(40, 40, Gravity.CENTER));
        setSize(40);
    }

    public void setStrokeWidth(float width) {
        imageView.setStrokeWidth(AndroidUtilities.dp(width));
    }

    public void setStrokeColor(int colorResource) {
        imageView.setStrokeColorResource(colorResource);
    }

    public void setSize(float size) {
        float cornerSize = size * 0.4F;
        imageView.getLayoutParams().width = AndroidUtilities.dp(size);
        imageView.getLayoutParams().height = AndroidUtilities.dp(size);
        imageView.setShapeAppearanceModel(imageView.getShapeAppearanceModel()
                .toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, AndroidUtilities.dp(cornerSize))
                .build());
        defaultAvatarTv.getLayoutParams().height = AndroidUtilities.dp(size);
        defaultAvatarTv.getLayoutParams().width = AndroidUtilities.dp(size);
    }

    public void setSize(float size, float cornerSize) {
        imageView.getLayoutParams().width = AndroidUtilities.dp(size);
        imageView.getLayoutParams().height = AndroidUtilities.dp(size);
        imageView.setShapeAppearanceModel(imageView.getShapeAppearanceModel()
                .toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, AndroidUtilities.dp(cornerSize))
                .build());

        defaultAvatarTv.getLayoutParams().height = AndroidUtilities.dp(size);
        defaultAvatarTv.getLayoutParams().width = AndroidUtilities.dp(size);

    }

    public void showAvatar(String channelID, byte channelType, String avatarCacheKey) {
        String url = getAvatarURL(channelID, channelType);
        url = WKApiConfig.appendAvatarCacheKey(url, avatarCacheKey);
        GlideUtils.getInstance().showAvatarImg(getContext(), url, avatarCacheKey, imageView);
    }

    public void showAvatar(String channelID, byte channelType, boolean showOnlineStatus) {
        spotView.setVisibility(GONE);
        onlineTv.setVisibility(INVISIBLE);
        WKChannel channel = WKIM.getInstance().getChannelManager().getChannel(channelID, channelType);
        if (channel != null) {
            showAvatar(channel, showOnlineStatus);
        } else {
            String url = getAvatarURL(channelID, channelType);
            GlideUtils.getInstance().showAvatarImg(getContext(), url, "", imageView);
        }
    }

    public void showAvatar(String channelID, byte channelType) {
        spotView.setVisibility(GONE);
        onlineTv.setVisibility(INVISIBLE);
        WKChannel channel = WKIM.getInstance().getChannelManager().getChannel(channelID, channelType);
        if (channel != null) {
            showAvatar(channel, false);
        } else {
            String url = getAvatarURL(channelID, channelType);
            GlideUtils.getInstance().showAvatarImg(getContext(), url, "", imageView);
        }
    }

    public void showAvatar(WKChannel channel) {
        showAvatar(channel, false);
    }

    public void showAvatar(WKChannel channel, boolean showOnlineStatus) {
        // 重要修复：会话列表 / 联系人列表 / 聊天气泡 等 Adapter 持有的是
        // WKChannel 的 *旧实例引用*。对方上传新头像时 wk_userAvatarUpdate CMD 只会
        // 更新 SDK 持久化行 + 触发 fetchChannelInfo，不一定原地改写这些旧实例的
        // avatarCacheKey 字段。如果继续按旧实例的 avatarCacheKey 拼 URL/缓存键，
        // Glide 会一直命中旧默认头像的缓存。这里以 channelID 重新去 ChannelManager
        // 取一份「最新」WKChannel，只要 ID 非空就用最新，保证 avatarCacheKey 与
        // online/lastOffline 等也都是新的。
        WKChannel use = channel;
        if (channel != null && !TextUtils.isEmpty(channel.channelID)) {
            WKChannel fresh = WKIM.getInstance().getChannelManager().getChannel(channel.channelID, channel.channelType);
            if (fresh != null && !TextUtils.isEmpty(fresh.channelID)) {
                use = fresh;
            }
        }
        String avatarCacheKey = use.avatarCacheKey;
        // 重要修复：以前的逻辑是优先用 SDK 下发的 use.avatar 字段来拼 URL，
        // 但实测服务端对部分用户（如 u_10000 系统号）返回的 avatar 形如
        //   /v1/users/u_10000/avatar.png
        // 这条 URL 服务端虽然 200 + Content-Type: image/jpeg，
        // 但实际 body 被 Glide / 系统 ImageDecoder 判定为非法图（DecodePath
        // 的 Bitmap / GifDrawable / BitmapDrawable 全部失败），表现：
        //   - 列表 / 聊天气泡永远空白；
        //   - 放大弹窗走的是 getAvatarUrl(uid)+"?key=" 的规范地址，能正常解码。
        // 所以这里统一优先走规范头像接口 users/<id>/avatar（与放大逻辑完全一致），
        // 仅当 use.avatar 是带协议头的完整外链（真正的 CDN/外部头像）时才采用。
        String url;
        if (!TextUtils.isEmpty(use.avatar)
                && (use.avatar.startsWith("http://") || use.avatar.startsWith("https://"))) {
            url = use.avatar;
        } else {
            url = getAvatarURL(use.channelID, use.channelType);
        }
        // 把 avatarCacheKey 拼进 URL，保证 server 一旦换了头像、本地 key 一变，
        // Glide / OkHttp / HTTP 协议缓存就全部失效（详见 WKApiConfig.appendAvatarCacheKey）。
        url = WKApiConfig.appendAvatarCacheKey(url, avatarCacheKey);
        GlideUtils.getInstance().showAvatarImg(imageView.getContext(), url, avatarCacheKey, imageView);
        if (showOnlineStatus) {
            if (use.online == 1) {
                spotView.setVisibility(VISIBLE);
                onlineTv.setVisibility(INVISIBLE);
            } else {
                spotView.setVisibility(GONE);
                String showTime = WKTimeUtils.getInstance().getOnlineTime(use.lastOffline);
                if (TextUtils.isEmpty(showTime)) {
                    onlineTv.setVisibility(INVISIBLE);
                } else {
                    onlineTv.setVisibility(VISIBLE);
                    onlineTv.setText(showTime);
                }
            }
        } else {
            spotView.setVisibility(GONE);
            onlineTv.setVisibility(INVISIBLE);
        }
    }

    private String getAvatarURL(String channelID, byte channelType) {
        // 修复：列表页曾优先读取本地固定路径头像缓存（avatarCacheDir/type_id）。
        // 当该本地文件是旧头像/损坏文件时，详情页能显示新头像（走网络 URL），
        // 但返回列表又会回退命中旧本地文件，表现为“关闭后又变空白/默认”。
        // 这里统一返回服务端头像 URL，并配合 appendAvatarCacheKey 做版本失效。
        return WKApiConfig.getShowAvatar(channelID, channelType);
    }

    public static void clearCache(String channelID, byte channelType) {
        String filePath = WKConstants.avatarCacheDir + channelType + "_" + channelID;
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }
}
