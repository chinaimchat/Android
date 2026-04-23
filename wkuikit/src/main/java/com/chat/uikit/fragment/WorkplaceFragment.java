package com.chat.uikit.fragment;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.chat.base.act.WorkplaceWebViewActivity;
import com.chat.base.base.WKBaseFragment;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.uikit.databinding.FragWorkplaceLayoutBinding;
import com.chat.uikit.workplace.WorkplaceApp;
import com.chat.uikit.workplace.WorkplaceAppAdapter;
import com.chat.uikit.workplace.WorkplaceCategory;
import com.chat.uikit.workplace.WorkplaceModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorkplaceFragment extends WKBaseFragment<FragWorkplaceLayoutBinding> {

    private WorkplaceAppAdapter adapter;

    @Override
    protected FragWorkplaceLayoutBinding getViewBinding() {
        return FragWorkplaceLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        adapter = new WorkplaceAppAdapter();
        initAdapter(wkVBinding.recyclerView, adapter);
    }

    @Override
    protected void initListener() {
        adapter.setOnItemClickListener((adapter1, view, position) -> SingleClickUtil.determineTriggerSingleClick(view, v -> {
            WorkplaceApp app = adapter.getItem(position);
            if (app == null) return;
            if (!TextUtils.isEmpty(app.app_id)) {
                WorkplaceModel.getInstance().addRecord(app.app_id);
            }
            openApp(app);
        }));
    }

    @Override
    protected void initData() {
        WorkplaceModel.getInstance().getCategory(this::showCategory);
    }

    private void showCategory(List<WorkplaceCategory> categories) {
        if (!isAdded() || getActivity() == null || adapter == null) return;
        if (categories == null || categories.isEmpty()) {
            adapter.setList(new ArrayList<>());
            return;
        }
        List<WorkplaceCategory> validCategories = new ArrayList<>();
        for (WorkplaceCategory category : categories) {
            if (category != null && !TextUtils.isEmpty(category.category_no)) {
                validCategories.add(category);
            }
        }
        if (validCategories.isEmpty()) {
            adapter.setList(new ArrayList<>());
            return;
        }
        loadAppsFromAllCategories(validCategories);
    }

    private void loadAppsFromAllCategories(List<WorkplaceCategory> categories) {
        final int total = categories.size();
        final int[] finished = {0};
        final Map<String, WorkplaceApp> appMap = new LinkedHashMap<>();
        for (WorkplaceCategory category : categories) {
            WorkplaceModel.getInstance().getAppsWithCategory(category.category_no, apps -> {
                if (apps != null && !apps.isEmpty()) {
                    for (WorkplaceApp app : apps) {
                        if (app == null || TextUtils.isEmpty(app.app_id)) {
                            continue;
                        }
                        if (!appMap.containsKey(app.app_id)) {
                            appMap.put(app.app_id, app);
                        }
                    }
                }
                finished[0]++;
                if (finished[0] < total) {
                    return;
                }
                List<WorkplaceApp> merged = new ArrayList<>(appMap.values());
                if (!isAdded() || getActivity() == null || adapter == null) return;
                adapter.setList(merged);
            });
        }
    }

    private void openApp(WorkplaceApp app) {
        if (app == null || app.status == 0) {
            return;
        }
        if (app.jump_type == 0) {
            openWeb(app.web_route, app.icon);
            return;
        }
        if (TextUtils.isEmpty(app.app_route)) {
            openWeb(app.web_route, app.icon);
            return;
        }
        if (app.app_route.startsWith("http://") || app.app_route.startsWith("https://")) {
            openWeb(app.app_route, app.icon);
            return;
        }
        if (app.app_route.contains("://")) {
            openSchema(app.app_route);
            return;
        }
        Object result;
        try {
            result = EndpointManager.getInstance().invoke(app.app_route, app);
        } catch (RuntimeException e) {
            Log.e("WorkplaceFragment", "openApp endpoint failed: " + app.app_route, e);
            result = null;
        }
        if (result == null && !TextUtils.isEmpty(app.web_route)) {
            openWeb(app.web_route, app.icon);
        }
    }

    private void openSchema(String schema) {
        FragmentActivity activity = getActivity();
        if (activity == null || !isAdded()) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(schema));
            activity.startActivity(intent);
        } catch (Exception ignored) {
            openWeb(schema, "");
        }
    }

    private void openWeb(String url, String icon) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        Intent intent = new Intent(getActivity(), WorkplaceWebViewActivity.class);
        intent.putExtra("url", url);
        // Pending state for returning to Workplace page bubble.
        intent.putExtra("workplace_bubble_url", url);
        intent.putExtra("workplace_bubble_icon", icon);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void setTitle(TextView titleTv) {
    }
}
