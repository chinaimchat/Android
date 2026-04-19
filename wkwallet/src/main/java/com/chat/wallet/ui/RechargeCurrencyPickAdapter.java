package com.chat.wallet.ui;

import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.chat.base.config.WKApiConfig;
import com.chat.wallet.R;
import com.chat.wallet.entity.RechargeChannel;

import java.util.List;

/**
 * 充值页「选择币种」列表：数据来自支付配置 {@link RechargeChannel}。
 */
public class RechargeCurrencyPickAdapter extends RecyclerView.Adapter<RechargeCurrencyPickAdapter.Holder> {

    public interface Listener {
        void onPick(int index);
    }

    private final Fragment host;
    private final List<RechargeChannel> channels;
    private final int selectedIndex;
    private final Listener listener;

    public RechargeCurrencyPickAdapter(Fragment host, List<RechargeChannel> channels,
                                       int selectedIndex, Listener listener) {
        this.host = host;
        this.channels = channels;
        this.selectedIndex = selectedIndex;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recharge_pick_currency, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        RechargeChannel ch = channels.get(position);
        String label = ch.getDisplayName();
        if (TextUtils.isEmpty(label)) {
            label = "#" + ch.id;
        }
        h.labelTv.setText(label);
        h.checkIv.setVisibility(position == selectedIndex ? View.VISIBLE : View.INVISIBLE);

        String iconUrl = ch.icon != null ? ch.icon.trim() : "";
        int brandRes = RechargePayBrandIcons.localBrandDrawableRes(ch);
        if (!TextUtils.isEmpty(iconUrl)) {
            h.letterTv.setVisibility(View.INVISIBLE);
            h.iconIv.setVisibility(View.VISIBLE);
            h.iconIv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(host)
                    .load(WKApiConfig.getShowUrl(iconUrl))
                    .apply(new RequestOptions().circleCrop())
                    .into(h.iconIv);
        } else if (brandRes != 0) {
            Glide.with(host).clear(h.iconIv);
            h.letterTv.setVisibility(View.GONE);
            h.iconIv.setVisibility(View.VISIBLE);
            h.iconIv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            h.iconIv.setImageResource(brandRes);
        } else {
            Glide.with(host).clear(h.iconIv);
            h.iconIv.setVisibility(View.GONE);
            h.letterTv.setVisibility(View.VISIBLE);
            RechargeChannelCoinStyle style = RechargeChannelCoinStyle.fromChannel(ch);
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(style.circleColorArgb);
            h.letterTv.setBackground(gd);
            h.letterTv.setText(style.letter);
        }

        h.itemView.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onPick(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return channels.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView letterTv;
        final ImageView iconIv;
        final TextView labelTv;
        final ImageView checkIv;

        Holder(@NonNull View itemView) {
            super(itemView);
            letterTv = itemView.findViewById(R.id.pickCoinLetterTv);
            iconIv = itemView.findViewById(R.id.pickCoinIconIv);
            labelTv = itemView.findViewById(R.id.pickCoinLabelTv);
            checkIv = itemView.findViewById(R.id.pickCoinCheckIv);
        }
    }
}
