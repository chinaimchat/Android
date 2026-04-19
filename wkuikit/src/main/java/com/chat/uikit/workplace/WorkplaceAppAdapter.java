package com.chat.uikit.workplace;

import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.base.config.WKApiConfig;
import com.chat.base.glide.GlideUtils;
import com.chat.uikit.R;

public class WorkplaceAppAdapter extends BaseQuickAdapter<WorkplaceApp, BaseViewHolder> {

    public WorkplaceAppAdapter() {
        super(R.layout.item_workplace_app_layout);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder holder, WorkplaceApp item) {
        if (item == null) {
            return;
        }
        holder.setText(R.id.nameTv, item.name != null ? item.name : "");
        holder.setText(R.id.descTv, item.description != null ? item.description : "");
        ImageView iconIv = holder.getView(R.id.iconIv);
        if (!TextUtils.isEmpty(item.icon)) {
            GlideUtils.getInstance().showImg(getContext(), WKApiConfig.getShowUrl(item.icon), iconIv);
        } else {
            iconIv.setImageResource(R.drawable.default_view_bg);
        }
    }
}
