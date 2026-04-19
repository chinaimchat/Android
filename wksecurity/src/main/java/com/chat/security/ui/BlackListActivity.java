package com.chat.security.ui;

import android.widget.TextView;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.EndpointSID;
import com.chat.base.endpoint.entity.UserDetailMenu;
import com.chat.base.net.HttpResponseCode;
import com.chat.security.R;
import com.chat.security.databinding.ActBlackListLayoutBinding;
import com.chat.security.entity.UserInfo;
import com.chat.security.service.SecurityModel;

import java.util.ArrayList;

/**
 * 2020-07-10 13:18
 * 黑名单
 */
public class BlackListActivity extends WKBaseActivity<ActBlackListLayoutBinding> {
    private BlackListAdapter adapter;

    @Override
    protected ActBlackListLayoutBinding getViewBinding() {
        return ActBlackListLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.black_list);
    }

    @Override
    protected void initPresenter() {

    }

    @Override
    protected void initView() {
        adapter = new BlackListAdapter(new ArrayList<>());
        initAdapter(wkVBinding.recyclerView, adapter);
    }

    @Override
    protected void initListener() {
        adapter.setOnItemClickListener((adapter1, view1, position) -> {
            UserInfo userInfo = (UserInfo) adapter1.getItem(position);
            if (userInfo != null) {
                EndpointManager.getInstance().invoke(EndpointSID.userDetailView, new UserDetailMenu(BlackListActivity.this,userInfo.uid));
            }
        });
    }

    void getData() {
        new SecurityModel().getBlacklists((code, msg, list) -> {
            if (code == HttpResponseCode.success) {
                adapter.setList(list);
            } else showToast(msg);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getData();
    }
}
