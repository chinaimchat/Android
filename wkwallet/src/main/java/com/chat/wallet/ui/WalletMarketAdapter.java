package com.chat.wallet.ui;

import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

import androidx.annotation.ColorInt;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.wallet.R;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class WalletMarketAdapter extends BaseQuickAdapter<WalletMarketAdapter.Item, BaseViewHolder> {

    public static final class Item {
        public final String symbol;
        public final String priceLine;
        public final double changePercent;
        @ColorInt
        public final int brandColor;
        public final String iconLetter;

        public Item(String symbol, String priceLine, double changePercent, int brandColor, String iconLetter) {
            this.symbol = symbol;
            this.priceLine = priceLine;
            this.changePercent = changePercent;
            this.brandColor = brandColor;
            this.iconLetter = iconLetter;
        }
    }

    public WalletMarketAdapter() {
        super(R.layout.item_wallet_market);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder h, Item item) {
        TextView iconTv = h.getView(R.id.cryptoIconTv);
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(item.brandColor);
        iconTv.setBackground(gd);
        iconTv.setText(item.iconLetter);

        h.setText(R.id.symbolTv, item.symbol);
        h.setText(R.id.priceTv, item.priceLine);

        boolean up = item.changePercent >= 0;
        int color = androidx.core.content.ContextCompat.getColor(getContext(),
                up ? R.color.wallet_market_positive : R.color.wallet_market_negative);
        String pct = String.format(Locale.US, "%+.2f%%", item.changePercent);
        TextView changeTv = h.getView(R.id.changeTv);
        changeTv.setText(pct);
        changeTv.setTextColor(color);
    }
}
