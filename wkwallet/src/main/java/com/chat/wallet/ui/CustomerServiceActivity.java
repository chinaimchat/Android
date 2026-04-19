package com.chat.wallet.ui;

import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.base.base.WKBaseActivity;
import com.chat.base.config.WKApiConfig;
import com.chat.base.glide.GlideUtils;
import com.chat.base.net.IRequestResultListener;
import com.chat.wallet.R;
import com.chat.wallet.api.WalletModel;
import com.chat.wallet.databinding.ActivityCustomerServiceBinding;
import com.chat.wallet.entity.CustomerService;
import com.chat.wallet.util.WalletChatRouter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CustomerServiceActivity extends WKBaseActivity<ActivityCustomerServiceBinding> {

    @Override
    protected ActivityCustomerServiceBinding getViewBinding() {
        return ActivityCustomerServiceBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView t) {
        t.setText(R.string.wallet_customer_service);
    }

    @Override
    protected void initView() {
        BaseQuickAdapter<CustomerService, BaseViewHolder> adapter = new BaseQuickAdapter<CustomerService, BaseViewHolder>(R.layout.item_customer_service) {
            @Override
            protected void convert(@NotNull BaseViewHolder h, CustomerService item) {
                h.setText(R.id.nameTv, item.name != null ? item.name : "");
                h.setText(R.id.descTv, item.description != null ? item.description : "");
                ImageView iv = h.getView(R.id.avatarIv);
                if (item.uid != null) {
                    GlideUtils.getInstance().showAvatarImg(iv.getContext(), WKApiConfig.getAvatarUrl(item.uid), item.uid, iv);
                }
            }
        };
        adapter.addChildClickViewIds(R.id.chatBtn);
        adapter.setOnItemChildClickListener((a, view, position) -> {
            if (view.getId() != R.id.chatBtn) {
                return;
            }
            CustomerService item = adapter.getItem(position);
            if (item != null && item.uid != null && !item.uid.isEmpty()) {
                WalletChatRouter.openP2PChat(CustomerServiceActivity.this, item.uid);
            }
        });
        initAdapter(wkVBinding.recyclerView, adapter);

        WalletModel.getInstance().getCustomerServices(new IRequestResultListener<List<CustomerService>>() {
            @Override
            public void onSuccess(List<CustomerService> list) {
                adapter.setList(list != null ? list : new ArrayList<>());
            }

            @Override
            public void onFail(int c, String m) {
                showToast(m != null ? m : getString(R.string.wallet_load_fail));
            }
        });
    }
}
