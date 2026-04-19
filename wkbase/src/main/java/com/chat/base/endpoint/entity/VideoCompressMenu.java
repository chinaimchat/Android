package com.chat.base.endpoint.entity;

public class VideoCompressMenu extends BaseEndpoint {
    public String inputPath;
    public IVideoCompressCallback callback;

    public VideoCompressMenu(String inputPath, IVideoCompressCallback callback) {
        this.inputPath = inputPath;
        this.callback = callback;
    }

    public interface IVideoCompressCallback {
        void onSuccess(String compressedPath);

        void onFail(String originalPath);

        void onProgress(float progress);
    }
}
