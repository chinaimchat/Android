package com.chat.uikit.fragment;

import android.content.Intent;
import android.text.TextUtils;

import com.chat.base.base.WKBaseFragment;
import com.chat.base.common.WKCommonModel;
import com.chat.base.config.WKConfig;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.PersonalInfoMenu;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.base.utils.WKLogUtils;
import com.chat.base.utils.WKReader;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.uikit.R;
import com.chat.uikit.databinding.FragMyLayoutBinding;
import com.chat.uikit.user.MyInfoActivity;
import com.xinbida.wukongim.entity.WKChannelType;

import java.util.ArrayList;
import java.util.List;

/**
 * 2019-11-12 14:58
 * 我的
 */
public class MyFragment extends WKBaseFragment<FragMyLayoutBinding> {
    private PersonalItemAdapter adapter;

    @Override
    protected FragMyLayoutBinding getViewBinding() {
        return FragMyLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        wkVBinding.recyclerView.setNestedScrollingEnabled(false);
        adapter = new PersonalItemAdapter(new ArrayList<>());
        initAdapter(wkVBinding.recyclerView, adapter);
        refreshPersonalMenu(WKConfig.getInstance().getAppConfig().register_invite_on);
    }

    @Override
    protected void initPresenter() {
        wkVBinding.avatarView.setSize(90);
        wkVBinding.refreshLayout.setEnableOverScrollDrag(true);
        wkVBinding.refreshLayout.setEnableLoadMore(false);
        wkVBinding.refreshLayout.setEnableRefresh(false);
        Theme.setPressedBackground(wkVBinding.qrIv);
    }

    @Override
    protected void initListener() {
        adapter.setOnItemClickListener((adapter1, view, position) -> SingleClickUtil.determineTriggerSingleClick(view, view1 -> {
            PersonalInfoMenu menu = (PersonalInfoMenu) adapter1.getItem(position);
            if (menu != null && menu.iPersonalInfoMenuClick != null) {
                menu.iPersonalInfoMenuClick.onClick();
            }
        }));
        SingleClickUtil.onSingleClick(wkVBinding.avatarView, view -> gotoMyInfo());
        SingleClickUtil.onSingleClick(wkVBinding.qrIv, view -> gotoMyInfo());
    }

    void gotoMyInfo() {
        startActivity(new Intent(getActivity(), MyInfoActivity.class));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (wkVBinding == null) return;
        wkVBinding.nameTv.setText(WKConfig.getInstance().getUserInfo().name);
        wkVBinding.avatarView.showAvatar(WKConfig.getInstance().getUid(), WKChannelType.PERSONAL);
        if (adapter != null) {
            WKCommonModel.getInstance().getAppNewVersion(false, version -> {
                if (!isAdded() || getActivity() == null || adapter == null) return;
                int index = findCurrencyMenuIndex(adapter.getData());
                if (index == -1) {
                    return;
                }
                boolean showNewVersion = version != null && !TextUtils.isEmpty(version.download_url);
                PersonalInfoMenu menu = adapter.getData().get(index);
                if (menu.isNewVersionIv != showNewVersion) {
                    menu.setIsNewVersionIv(showNewVersion);
                    adapter.notifyItemChanged(index);
                }
            });
        }
        WKCommonModel.getInstance().getAppConfig((code, msg, wkappConfig) -> {
            if (!isAdded() || getActivity() == null || adapter == null) return;
            if (code == HttpResponseCode.success && wkappConfig != null) {
                refreshPersonalMenu(wkappConfig.register_invite_on);
            }
        });
    }

    private int findCurrencyMenuIndex(List<PersonalInfoMenu> data) {
        if (WKReader.isEmpty(data)) {
            return -1;
        }
        for (int i = 0; i < data.size(); i++) {
            PersonalInfoMenu menu = data.get(i);
            if (menu != null && getString(R.string.currency).equals(menu.text)) {
                return i;
            }
        }
        return -1;
    }

    private void refreshPersonalMenu(int registerInviteOn) {
        if (adapter == null) {
            return;
        }
        List<PersonalInfoMenu> endpoints = EndpointManager.getInstance().invokes(EndpointCategory.personalCenter, null);
        if (WKReader.isEmpty(endpoints)) {
            adapter.setList(new ArrayList<>());
            return;
        }
        List<PersonalInfoMenu> menus = new ArrayList<>(endpoints);
        // 业务要求：Android「我的」中固定隐藏「我的邀请码」入口
        removeInviteCodeMenu(menus);
        adapter.setList(menus);
    }

    private void removeInviteCodeMenu(List<PersonalInfoMenu> menus) {
        if (WKReader.isEmpty(menus)) {
            return;
        }
        for (int i = 0; i < menus.size(); i++) {
            PersonalInfoMenu menu = menus.get(i);
            if (menu != null && !TextUtils.isEmpty(menu.sid) && menu.sid.equals("invite_code")) {
                menus.remove(i);
                break;
            }
        }
    }
}
