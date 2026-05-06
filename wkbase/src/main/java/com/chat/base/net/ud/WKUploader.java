package com.chat.base.net.ud;


import com.chat.base.base.WKBaseModel;
import com.chat.base.config.WKApiConfig;
import com.chat.base.net.ApiService;
import com.chat.base.net.IRequestResultListener;
import com.chat.base.net.entity.UploadFileUrl;
import com.chat.base.net.entity.UploadResultEntity;
import com.chat.base.utils.WKTimeUtils;

import java.io.File;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * 与 TangSengDaoDaoAndroid 官网一致：GET 取 {@code result.url} 后原样 multipart POST。
 */
public class WKUploader extends WKBaseModel {
    private WKUploader() {
    }

    private static class UploadBinder {
        final static WKUploader upload = new WKUploader();
    }

    public static WKUploader getInstance() {
        return UploadBinder.upload;
    }

    public void upload(String uploadUrl, String filePath, final IUploadBack iUploadBack) {
        upload(uploadUrl, filePath, filePath, iUploadBack);
    }

    public void upload(String uploadUrl, String filePath, Object tag, final IUploadBack iUploadBack) {
        File file = new File(filePath);
        MediaType mediaType = guessPartMediaType(file.getName());
        RequestBody fileBody = RequestBody.Companion.create(file, mediaType);
        FileRequestBody fileRequestBody = new FileRequestBody(fileBody, tag);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(), fileRequestBody);
        request(createService(UploadService.class).upload(uploadUrl, part), new IRequestResultListener<>() {
            @Override
            public void onSuccess(UploadResultEntity result) {
                if (iUploadBack != null) {
                    iUploadBack.onSuccess(result.path);
                }
            }

            @Override
            public void onFail(int code, String msg) {
                if (iUploadBack != null) {
                    iUploadBack.onError();
                }
            }
        });
    }

    /** 与 iOS {@code WKAPIClient fileUpload:... mimeType:} 对齐：multipart 单文件 part 使用真实图片类型，而非 multipart/form-data。 */
    private static MediaType guessPartMediaType(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String ext = dot >= 0 ? fileName.substring(dot + 1).toLowerCase(Locale.US) : "";
        switch (ext) {
            case "jpg":
            case "jpeg":
                return MediaType.Companion.parse("image/jpeg");
            case "png":
                return MediaType.Companion.parse("image/png");
            case "gif":
                return MediaType.Companion.parse("image/gif");
            case "webp":
                return MediaType.Companion.parse("image/webp");
            default:
                return MediaType.Companion.parse("application/octet-stream");
        }
    }

    public void getUploadFileUrl(String channelID, byte channelType, String localPath, final IGetUploadFileUrl iGetUploadFileUrl) {
        File f = new File(localPath);
        String tempFileName = f.getName();
        String prefix = tempFileName.substring(tempFileName.lastIndexOf(".") + 1);
        String path = "/" + channelType + "/" + channelID + "/" + WKTimeUtils.getInstance().getCurrentMills() + "." + prefix;
        request(createService(ApiService.class).getUploadFileUrl(WKApiConfig.baseUrl + "file/upload?type=chat&path=" + path), new IRequestResultListener<UploadFileUrl>() {
            @Override
            public void onSuccess(UploadFileUrl result) {
                iGetUploadFileUrl.onResult(result.url, path);
            }

            @Override
            public void onFail(int code, String msg) {
                iGetUploadFileUrl.onResult(null, path);
            }
        });
    }

    public interface IGetUploadFileUrl {
        void onResult(String url, String fileUrl);
    }

    public interface IUploadBack {
        void onSuccess(String url);

        void onError();
    }
}
