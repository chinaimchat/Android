package com.chat.moments.activities;

import android.content.Intent;
import android.widget.ImageView;
import android.widget.TextView;

import com.chat.base.base.WKBaseActivity;
import com.chat.moments.R;
import com.chat.moments.databinding.ActPreviewVideoLayoutBinding;

/**
 * 2020-11-10 18:45
 * 视频预览
 */
public class PreviewVideoActivity extends WKBaseActivity<ActPreviewVideoLayoutBinding> {


    @Override
    protected ActPreviewVideoLayoutBinding getViewBinding() {
        return ActPreviewVideoLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {

    }

    @Override
    protected int getRightIvResourceId(ImageView imageView) {
        return R.mipmap.ic_ab_delete;
    }

    @Override
    protected void rightLayoutClick() {
        super.rightLayoutClick();
        showDialog(getString(R.string.delete_video_tips), index -> {
            if (index == 1) {
                Intent intent = new Intent();
                intent.putExtra("isDelete", true);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    @Override
    protected void initPresenter() {
        String path = getIntent().getStringExtra("path");
        wkVBinding.videoView.setVideoPath(path);
        wkVBinding.videoView.start();
    }

    @Override
    protected void initView() {

    }

    @Override
    protected void initListener() {
        wkVBinding.videoView.setOnCompletionListener(mediaPlayer -> wkVBinding.videoView.start());
    }
}
