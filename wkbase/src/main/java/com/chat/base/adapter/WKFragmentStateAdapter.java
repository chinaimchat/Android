package com.chat.base.adapter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class WKFragmentStateAdapter extends FragmentStateAdapter {
    List<Fragment> fragmentList;

    public WKFragmentStateAdapter(@NonNull FragmentManager fragmentManager, Lifecycle lifecycle, List<Fragment> fragmentList) {
        super(fragmentManager, lifecycle);
        setFragmentList(fragmentList);
    }

    public WKFragmentStateAdapter(@NonNull FragmentActivity fragmentActivity, List<Fragment> fragmentList) {
        super(fragmentActivity);
        setFragmentList(fragmentList);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // FragmentStateAdapter 会在页面滑出视窗后 remove Fragment，再次进入时必须拿到「新实例」；
        // 若重复返回列表中的同一个 Fragment 对象，会在二次进入该 Tab 时触发 IllegalStateException 等崩溃。
        Fragment template = fragmentList.get(position);
        final Fragment fragment;
        try {
            fragment = template.getClass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                    "WKFragmentStateAdapter: cannot instantiate " + template.getClass().getName(), e);
        }
        if (template.getArguments() != null) {
            fragment.setArguments(new Bundle(template.getArguments()));
        }
        return fragment;
    }

    @Override
    public int getItemCount() {
        return fragmentList == null ? 0 : fragmentList.size();
    }

    public void setFragmentList(List<Fragment> fragmentList) {
        if (this.fragmentList != null) {
            this.fragmentList.clear();
        }
        this.fragmentList = fragmentList != null ? fragmentList : new ArrayList<>();
        // 仅在构造时调用、尚未 setAdapter 时无需 notify；若在已附加的 ViewPager 上动态改列表，需另行 notifyItemRange*
    }

    /**
     * 固定 Tab 页顺序不变时用 position 作为稳定 ID，与官方 FragmentStateAdapter 建议一致；
     * 避免依赖模板 Fragment 的 identityHashCode（理论碰撞或状态不一致风险）。
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean containsItem(long itemId) {
        return itemId >= 0 && itemId < getItemCount();
    }

}
