package com.chat.uikit.workplace;

import com.alibaba.fastjson.JSON;
import com.chat.base.base.WKBaseModel;
import com.chat.base.net.IRequestResultListener;

import java.util.ArrayList;
import java.util.List;

public class WorkplaceModel extends WKBaseModel {
    private WorkplaceModel() {
    }

    private static class Holder {
        private static final WorkplaceModel MODEL = new WorkplaceModel();
    }

    public static WorkplaceModel getInstance() {
        return Holder.MODEL;
    }

    public void getCategory(final ICategoryBack back) {
        request(createService(WorkplaceService.class).getCategory(), new IRequestResultListener<>() {
            @Override
            public void onSuccess(List<WorkplaceCategory> result) {
                back.onResult(ensureCategoryList(result));
            }

            @Override
            public void onFail(int code, String msg) {
                back.onResult(null);
            }
        });
    }

    public void getAppsWithCategory(String categoryNo, final IAppBack back) {
        request(createService(WorkplaceService.class).getAppsWithCategory(categoryNo), new IRequestResultListener<>() {
            @Override
            public void onSuccess(List<WorkplaceApp> result) {
                back.onResult(ensureAppList(result));
            }

            @Override
            public void onFail(int code, String msg) {
                back.onResult(null);
            }
        });
    }

    /**
     * FastJSON 对 List&lt;Bean&gt; 可能解析为 List&lt;JSONObject/Map&gt;，直接交给 UI 会在 get(0)/adapter 处 ClassCastException。
     * 经 JSON 字符串再 parseArray 可得到真实 Bean 列表。
     */
    private static List<WorkplaceCategory> ensureCategoryList(List<WorkplaceCategory> result) {
        if (result == null) {
            return null;
        }
        if (result.isEmpty()) {
            return result;
        }
        try {
            return JSON.parseArray(JSON.toJSONString(result), WorkplaceCategory.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static List<WorkplaceApp> ensureAppList(List<WorkplaceApp> result) {
        if (result == null) {
            return null;
        }
        if (result.isEmpty()) {
            return result;
        }
        try {
            return JSON.parseArray(JSON.toJSONString(result), WorkplaceApp.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void addRecord(String appId) {
        request(createService(WorkplaceService.class).addRecord(appId), new IRequestResultListener<>() {
            @Override
            public void onSuccess(Object result) {
            }

            @Override
            public void onFail(int code, String msg) {
            }
        });
    }

    public interface ICategoryBack {
        void onResult(List<WorkplaceCategory> list);
    }

    public interface IAppBack {
        void onResult(List<WorkplaceApp> list);
    }
}
