package com.chat.groupmanage.ui;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.config.WKConfig;
import com.chat.base.config.WKSystemAccount;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.msgitem.WKChannelMemberRole;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.groupmanage.R;
import com.chat.groupmanage.adapter.GroupManagerAdapter;
import com.chat.groupmanage.databinding.ActGroupManageLayoutBinding;
import com.chat.groupmanage.entity.ForbiddenTime;
import com.chat.groupmanage.entity.GroupMemberEntity;
import com.chat.groupmanage.service.GroupManageContract;
import com.chat.groupmanage.service.GroupManagePresenter;
import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.entity.WKChannel;
import com.xinbida.wukongim.entity.WKChannelExtras;
import com.xinbida.wukongim.entity.WKChannelMember;
import com.xinbida.wukongim.entity.WKChannelType;

import java.util.ArrayList;
import java.util.List;

/**
 * 2020-04-06 21:01
 * 群管理
 */
public class GroupManageActivity extends WKBaseActivity<ActGroupManageLayoutBinding> implements GroupManageContract.GroupManageView {
    private String groupId;
    private GroupManagerAdapter adapter;
    private GroupManagePresenter presenter;

    @Override
    protected ActGroupManageLayoutBinding getViewBinding() {
        return ActGroupManageLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.group_manage);
    }

    @Override
    protected void initPresenter() {
        presenter = new GroupManagePresenter(this);
    }

    @Override
    protected void initView() {
        wkVBinding.refreshLayout.setEnableOverScrollDrag(true);
        wkVBinding.refreshLayout.setEnableLoadMore(false);
        wkVBinding.refreshLayout.setEnableRefresh(false);
        adapter = new GroupManagerAdapter(new ArrayList<>());
        initAdapter(wkVBinding.recyclerView, adapter);
        wkVBinding.recyclerView.setNestedScrollingEnabled(false);
    }

    @Override
    protected void initListener() {
        adapter.addChildClickViewIds(R.id.removeIv);
        adapter.setOnItemChildClickListener((adapter1, view1, position) -> {
            GroupMemberEntity groupMemberEntity = (GroupMemberEntity) adapter1.getItem(position);
            if (groupMemberEntity != null) {
                if (groupMemberEntity.channelMember.role == WKChannelMemberRole.manager) {
                    List<String> uids = new ArrayList<>();
                    uids.add(groupMemberEntity.channelMember.memberUID);
                    presenter.removeGroupManager(groupId, uids);
                }
            }
        });
        adapter.setOnItemClickListener((adapter1, view1, position) -> SingleClickUtil.determineTriggerSingleClick(view1, view -> {
            GroupMemberEntity groupMemberEntity = (GroupMemberEntity) adapter1.getItem(position);
            if (groupMemberEntity != null && groupMemberEntity.getItemType() == 2) {
                Intent intent = new Intent(GroupManageActivity.this, ChooseNormalMembersActivity.class);
                intent.putExtra("groupId", groupId);
                startActivity(intent);
            }
        }));
        wkVBinding.invitConfirmationSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                presenter.updateGroupSetting(groupId, "invite", b ? 1 : 0);
            }
        });
        wkVBinding.fullStaffBanSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                presenter.updateGroupSetting(groupId, "forbidden", b ? 1 : 0);
            }
        });
        wkVBinding.forbiddenAddFriendSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                presenter.updateGroupSetting(groupId, "forbidden_add_friend", b ? 1 : 0);
            }
        });
        wkVBinding.allowNewMembersViewHistorMesgSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                presenter.updateGroupSetting(groupId, "allow_view_history_msg", b ? 1 : 0);
            }
        });
        //监听频道改变通知
        WKIM.getInstance().getChannelManager().addOnRefreshChannelInfo("groupManagerChannelRefresh", (channel, isEnd) -> {
            if (channel != null) {
                if (channel.channelID.equalsIgnoreCase(groupId) && channel.channelType == WKChannelType.GROUP) {
                    //同一个会话
                    getManagerList();
                }
            }
        });
        SingleClickUtil.onSingleClick(wkVBinding.groupOwnerTransferLayout, view1 -> {
            Intent intent = new Intent(this, ChooseNormalMembersActivity.class);
            intent.putExtra("groupId", groupId);
            intent.putExtra("type", 2);
            chooseMemberResult.launch(intent);
        });
        SingleClickUtil.onSingleClick(wkVBinding.blackListLayout, view1 -> {
            Intent intent = new Intent(this, GroupBlacklistActivity.class);
            intent.putExtra("groupId", groupId);
            startActivity(intent);
        });
        SingleClickUtil.onSingleClick(wkVBinding.outUserLayout, view1 -> {
            Intent intent = new Intent(this, OutGroupMembersActivity.class);
            intent.putExtra("groupId", groupId);
            startActivity(intent);
        });
        WKIM.getInstance().getChannelMembersManager().addOnRefreshChannelMemberInfo("group_manager_refresh_channel_member", (channelMember, isEnd) -> getManagerList());
        groupId = getIntent().getStringExtra("groupId");
        getManagerList();
    }

    //获取群主或管理员
    private void getManagerList() {
        WKChannel channel = WKIM.getInstance().getChannelManager().getChannel(groupId, WKChannelType.GROUP);
        if (channel != null) {
            wkVBinding.invitConfirmationSwitch.setChecked(channel.invite == 1);
            wkVBinding.fullStaffBanSwitch.setChecked(channel.forbidden == 1);
            if (channel.remoteExtraMap != null) {
                Object object = channel.remoteExtraMap.get(WKChannelExtras.forbiddenAddFriend);
                if (object != null) {
                    int forbiddenAddFriend = (int) object;
                    wkVBinding.forbiddenAddFriendSwitch.setChecked(forbiddenAddFriend == 1);
                }
                Object viewHistoryMsgObject = channel.remoteExtraMap.get(WKChannelExtras.allowViewHistoryMsg);
                if (viewHistoryMsgObject != null) {
                    int viewHistoryMsg = (int) viewHistoryMsgObject;
                    wkVBinding.allowNewMembersViewHistorMesgSwitch.setChecked(viewHistoryMsg == 1);
                }
            }

        }
        List<WKChannelMember> adminList = WKIM.getInstance().getChannelMembersManager().getWithRole(groupId, WKChannelType.GROUP, WKChannelMemberRole.admin);
        List<WKChannelMember> managerList = WKIM.getInstance().getChannelMembersManager().getWithRole(groupId, WKChannelType.GROUP, WKChannelMemberRole.manager);
        List<WKChannelMember> list = new ArrayList<>();
        list.addAll(adminList);
        list.addAll(managerList);
//        List<WKChannelMember> list = WKIM.getInstance().getChannelMembersManager().getMembers(groupId, WKChannelType.GROUP);
        List<GroupMemberEntity> tempList = new ArrayList<>();
        int myRoleInGroup = 0;
        for (int i = 0, size = list.size(); i < size; i++) {
            if (list.get(i).role == WKChannelMemberRole.admin) {
                tempList.add(0, new GroupMemberEntity(list.get(i)));
            } else if (list.get(i).role == WKChannelMemberRole.manager) {
                tempList.add(new GroupMemberEntity(list.get(i)));
            }
            if (list.get(i).memberUID.equalsIgnoreCase(WKConfig.getInstance().getUid())) {
                myRoleInGroup = list.get(i).role;
            }
        }
        if (myRoleInGroup == WKChannelMemberRole.admin) {
            GroupMemberEntity entity = new GroupMemberEntity(null);
            entity.itemType = 2;
            tempList.add(entity);
            wkVBinding.groupOwnerTransferLayout.setVisibility(View.VISIBLE);
        } else if (isPrivilegedAccount()) {
            wkVBinding.groupOwnerTransferLayout.setVisibility(View.VISIBLE);
        } else wkVBinding.groupOwnerTransferLayout.setVisibility(View.GONE);
        adapter.setMyRoleInGroup(myRoleInGroup);
        adapter.setList(tempList);
    }

    private boolean isPrivilegedAccount() {
        String loginUID = WKConfig.getInstance().getUid();
        if (TextUtils.isEmpty(loginUID)) {
            return false;
        }
        WKChannel me = WKIM.getInstance().getChannelManager().getChannel(loginUID, WKChannelType.PERSONAL);
        if (me == null || TextUtils.isEmpty(me.category)) {
            return false;
        }
        return WKSystemAccount.accountCategorySystem.equals(me.category)
                || WKSystemAccount.accountCategoryCustomerService.equals(me.category);
    }

    @Override
    public void refreshData() {

    }

    @Override
    public void forbiddenTimeList(List<ForbiddenTime> list) {

    }

    @Override
    public void showError(String msg) {

    }

    @Override
    public void hideLoading() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WKIM.getInstance().getChannelManager().removeRefreshChannelInfo("groupManagerChannelRefresh");
        WKIM.getInstance().getChannelMembersManager().removeRefreshChannelMemberInfo("group_manager_refresh_channel_member");
    }

    ActivityResultLauncher<Intent> chooseMemberResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            EndpointManager.getInstance().invoke("chat_hide_group_manage_view", null);
            finish();
        }
    });
}
