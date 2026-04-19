package com.chat.uikit.workplace;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface WorkplaceService {

    @GET("workplace/category")
    Observable<List<WorkplaceCategory>> getCategory();

    @GET("workplace/categorys/{category_no}/app")
    Observable<List<WorkplaceApp>> getAppsWithCategory(@Path("category_no") String categoryNo);

    @POST("workplace/apps/{app_id}/record")
    Observable<Object> addRecord(@Path("app_id") String appId);
}
